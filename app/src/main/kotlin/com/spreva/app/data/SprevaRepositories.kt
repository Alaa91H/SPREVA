package com.spreva.app.data

import androidx.room3.withWriteTransaction
import com.spreva.core.content.ContentSource
import com.spreva.core.database.dao.ActivityAttemptDao
import com.spreva.core.database.dao.LearningEventDao
import com.spreva.core.database.dao.LessonProgressDao
import com.spreva.core.database.dao.ReviewCardDao
import com.spreva.core.database.dao.ReviewLogDao
import com.spreva.core.database.entity.ActivityAttemptEntity
import com.spreva.core.database.entity.LearningEventEntity
import com.spreva.core.database.entity.ReviewCardEntity
import com.spreva.core.database.entity.ReviewLogEntity
import com.spreva.core.database.mapper.toEntity
import com.spreva.core.database.mapper.toModel
import com.spreva.core.database.SprevaDatabase
import com.spreva.core.datastore.SettingsDataSource
import com.spreva.core.model.ActivityAttempt
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonProgress
import com.spreva.core.model.LessonStatus
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewCardId
import com.spreva.core.model.ReviewRating
import com.spreva.core.model.SkillArea
import com.spreva.core.model.LearningActivity
import com.spreva.core.model.PlacementQuestion
import com.spreva.domain.curriculum.CurriculumRepository
import com.spreva.domain.learning.LearningEventLog
import com.spreva.domain.learning.LearningRepository
import com.spreva.domain.learning.ActivityEvidence
import com.spreva.domain.learning.LearningIntelligenceEngine
import com.spreva.domain.learning.LearningIntelligenceRepository
import com.spreva.domain.learning.PlacementQuestionRepository
import com.spreva.domain.learning.ReviewCardProvisioner
import com.spreva.domain.review.ReviewRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Offline-first Room-backed implementations of the domain repositories.
 * Room is the single source of truth; the UI observes flows directly
 * (plan sections 62/64).
 */

@Singleton
class OfflineFirstCurriculumRepository @Inject constructor(
    private val contentSource: ContentSource,
) : CurriculumRepository {

    override fun observeCourse() = kotlinx.coroutines.flow.flow {
        emit(contentSource.loadCourse(COURSE_ID))
    }

    override fun observeLessonSummaries(): Flow<List<com.spreva.core.model.LessonSummary>> =
        kotlinx.coroutines.flow.flow {
            val course = contentSource.loadCourse(COURSE_ID)
            val summaries = course.levels
                .flatMap { level -> level.units }
                .flatMap { unit -> unit.lessons }
                .map { ref ->
                    if (ref.title.de.isNotBlank() && ref.activityCount > 0) {
                        ref
                    } else {
                        val lesson = contentSource.loadLesson(ref.id)
                        com.spreva.core.model.LessonSummary(
                            id = lesson.id,
                            title = lesson.title,
                            activityCount = lesson.activities.size,
                        )
                    }
                }
            emit(summaries)
        }

    override suspend fun getLesson(id: LessonId) = contentSource.loadLesson(id)

    override suspend fun getNextLesson(): com.spreva.core.model.Lesson? =
        contentSource.loadLesson(com.spreva.core.model.LessonId(FIRST_LESSON_ID))

    override suspend fun contentVersion(): String = contentSource.contentVersion()

    override suspend fun getMediaAttributions(): List<com.spreva.domain.curriculum.MediaAttribution> =
        contentSource.loadMediaLicenses()?.files.orEmpty().map { file ->
            com.spreva.domain.curriculum.MediaAttribution(
                path = file.path,
                sourceUrl = file.sourceUrl,
                license = file.license,
                licenseUrl = file.licenseUrl,
                attribution = file.attribution,
            )
        }

    companion object {
        const val COURSE_ID = "de-core"
        const val FIRST_LESSON_ID = "a1_u01_l01"
    }
}

@Singleton
class OfflineFirstLearningRepository @Inject constructor(
    private val database: SprevaDatabase,
    private val contentSource: ContentSource,
) : LearningRepository {

    private val progressDao: LessonProgressDao = database.lessonProgressDao()
    private val attemptsDao: ActivityAttemptDao = database.activityAttemptDao()
    private val reviewCardDao: ReviewCardDao = database.reviewCardDao()

    override fun observeLessonProgress(lessonId: LessonId): Flow<LessonProgress?> =
        progressDao.observeByLesson(lessonId.value).map { it?.toModel() }

    override fun observeAllProgress(): Flow<List<LessonProgress>> =
        progressDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeAttempts(): Flow<List<ActivityAttempt>> =
        attemptsDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun recordAttempt(attempt: ActivityAttempt) {
        attemptsDao.insert(
            ActivityAttemptEntity(
                id = attempt.id,
                lessonId = attempt.lessonId.value,
                activityId = attempt.activityId.value,
                attemptNumber = attempt.attemptNumber,
                correct = attempt.correct,
                usedHint = attempt.usedHint,
                responseTimeMs = attempt.responseTimeMs,
                createdAtEpochMs = attempt.createdAt.toEpochMilli(),
            ),
        )
        if (!attempt.correct) provisionRepeatedMistakeCard(attempt)
    }

    private suspend fun provisionRepeatedMistakeCard(attempt: ActivityAttempt) {
        val incorrectCount = attemptsDao.countIncorrectForActivity(
            lessonId = attempt.lessonId.value,
            activityId = attempt.activityId.value,
        )
        if (incorrectCount < 2) return

        val lesson = runCatching { contentSource.loadLesson(attempt.lessonId) }.getOrNull() ?: return
        val activity = lesson.activities.firstOrNull { it.id == attempt.activityId } ?: return
        val card = when (activity) {
            is LearningActivity.MultipleChoice -> {
                val correct = activity.options.firstOrNull { it.id == activity.correctOptionId } ?: return
                ReviewCard(
                    id = ReviewCardId("mistake_${attempt.lessonId.value}_${activity.id.value}"),
                    knowledgeItemId = com.spreva.core.model.KnowledgeItemId("mistake:${activity.id.value}"),
                    prompt = activity.question.de,
                    answer = correct.text.de,
                    dueAt = attempt.createdAt,
                )
            }

            is LearningActivity.Cloze -> ReviewCard(
                id = ReviewCardId("mistake_${attempt.lessonId.value}_${activity.id.value}"),
                knowledgeItemId = com.spreva.core.model.KnowledgeItemId("mistake:${activity.id.value}"),
                prompt = activity.sentenceTemplate.de,
                answer = activity.acceptedAnswers.firstOrNull() ?: return,
                dueAt = attempt.createdAt,
            )

            is LearningActivity.ListeningChoice -> {
                val audio = activity.audio ?: return
                val correct = activity.options.firstOrNull { it.id == activity.correctOptionId } ?: return
                ReviewCard(
                    id = ReviewCardId("mistake_${attempt.lessonId.value}_${activity.id.value}"),
                    knowledgeItemId = com.spreva.core.model.KnowledgeItemId("mistake:${activity.id.value}"),
                    prompt = "",
                    answer = activity.text?.de ?: correct.text.de,
                    dueAt = attempt.createdAt,
                    audioPath = audio,
                )
            }

            is LearningActivity.Dictation -> {
                val audio = activity.audio ?: return
                ReviewCard(
                    id = ReviewCardId("mistake_${attempt.lessonId.value}_${activity.id.value}"),
                    knowledgeItemId = com.spreva.core.model.KnowledgeItemId("mistake:${activity.id.value}"),
                    prompt = "",
                    answer = activity.acceptedAnswers.firstOrNull() ?: activity.text.de,
                    dueAt = attempt.createdAt,
                    audioPath = audio,
                )
            }

            else -> return
        }
        reviewCardDao.insertAll(listOf(card.toEntity()))
    }

    override suspend fun completeLesson(lessonId: LessonId, totalActivities: Int) {
        val existing = progressDao.getByLesson(lessonId.value)
        val now = System.currentTimeMillis()
        progressDao.upsert(
            com.spreva.core.database.entity.LessonProgressEntity(
                rowId = existing?.rowId ?: 0,
                lessonId = lessonId.value,
                status = LessonStatus.COMPLETED.name,
                completedActivities = totalActivities,
                totalActivities = totalActivities,
                startedAtEpochMs = existing?.startedAtEpochMs ?: now,
                updatedAtEpochMs = now,
                completedAtEpochMs = now,
            ),
        )
    }

    override suspend fun ensureStarted(lessonId: LessonId, totalActivities: Int) {
        val existing = progressDao.getByLesson(lessonId.value)
        if (existing != null) return
        val now = System.currentTimeMillis()
        progressDao.upsert(
            com.spreva.core.database.entity.LessonProgressEntity(
                rowId = 0,
                lessonId = lessonId.value,
                status = LessonStatus.IN_PROGRESS.name,
                completedActivities = 0,
                totalActivities = totalActivities,
                startedAtEpochMs = now,
                updatedAtEpochMs = now,
                completedAtEpochMs = null,
            ),
        )
    }

    override suspend fun clearAll() {
        progressDao.clear()
        attemptsDao.clear()
    }
}

@Singleton
class OfflineFirstLearningIntelligenceRepository @Inject constructor(
    private val database: SprevaDatabase,
    private val contentSource: ContentSource,
    private val engine: LearningIntelligenceEngine,
) : LearningIntelligenceRepository {

    private val attemptsDao: ActivityAttemptDao = database.activityAttemptDao()

    override fun observeProfile(): Flow<com.spreva.core.model.LearningProfile> =
        attemptsDao.observeAll().map { entities ->
            if (entities.isEmpty()) return@map com.spreva.core.model.LearningProfile()

            val attempts = entities.map { it.toModel() }
            val course = contentSource.loadCourse(OfflineFirstCurriculumRepository.COURSE_ID)

            val unitByLesson = buildMap<String, com.spreva.core.model.Unit> {
                course.levels.forEach { level ->
                    level.units.forEach { unit ->
                        unit.lessons.forEach { lesson -> put(lesson.id.value, unit) }
                    }
                }
            }

            val lessonCache = attempts
                .map { it.lessonId }
                .distinct()
                .associateWith { id -> contentSource.loadLesson(id) }

            val evidence = attempts
                .groupBy { it.activityId }
                .mapNotNull { (_, grouped) ->
                    val first = grouped.first()
                    val lesson = lessonCache[first.lessonId] ?: return@mapNotNull null
                    val activity = lesson.activities.firstOrNull { it.id == first.activityId }
                        ?: return@mapNotNull null
                    val unit = unitByLesson[first.lessonId.value]
                    val pronunciationUnit = unit?.title?.de.orEmpty().containsAnyIgnoreCase(
                        "Aussprache",
                        "Phonetik",
                        "Prosodie",
                    )

                    val skill = when (activity) {
                        is LearningActivity.ListeningChoice,
                        is LearningActivity.Dictation,
                        -> if (pronunciationUnit) SkillArea.PRONUNCIATION else SkillArea.LISTENING

                        is LearningActivity.FreeWrite -> SkillArea.WRITING
                        is LearningActivity.SpeakingPrompt ->
                            if (pronunciationUnit) SkillArea.PRONUNCIATION else SkillArea.SPEAKING

                        is LearningActivity.MultipleChoice ->
                            if (pronunciationUnit) {
                                SkillArea.PRONUNCIATION
                            } else if (unit?.title?.de.orEmpty().containsAnyIgnoreCase("Prüfung", "Exam")) {
                                SkillArea.READING
                            } else {
                                SkillArea.GRAMMAR
                            }

                        is LearningActivity.Cloze ->
                            if (pronunciationUnit) SkillArea.PRONUNCIATION else SkillArea.GRAMMAR

                        else -> return@mapNotNull null
                    }

                    ActivityEvidence(
                        lessonId = lesson.id,
                        lessonTitle = lesson.title,
                        activityId = activity.id,
                        skill = skill,
                        objective = activity is LearningActivity.MultipleChoice ||
                            activity is LearningActivity.Cloze ||
                            activity is LearningActivity.ListeningChoice ||
                            activity is LearningActivity.Dictation,
                        productive = activity is LearningActivity.FreeWrite ||
                            activity is LearningActivity.SpeakingPrompt,
                        attempts = grouped.map { it },
                    )
                }

            engine.analyze(evidence)
        }

    private fun String.containsAnyIgnoreCase(vararg needles: String): Boolean =
        needles.any { contains(it, ignoreCase = true) }
}

@Singleton
class BundledPlacementQuestionRepository @Inject constructor(
    private val contentSource: ContentSource,
) : PlacementQuestionRepository {

    override suspend fun questions(): List<PlacementQuestion> {
        val course = contentSource.loadCourse(OfflineFirstCurriculumRepository.COURSE_ID)
        return course.levels
            .filter { it.cefr in com.spreva.domain.learning.AdaptivePlacementEngine.LEVELS }
            .flatMap { level ->
                val examUnit = level.units.firstOrNull { unit ->
                    unit.title.de.contains("Prüfungs-", ignoreCase = true) ||
                        unit.title.en?.contains("Exam & Skills", ignoreCase = true) == true
                } ?: return@flatMap emptyList()

                examUnit.lessons
                    .flatMap { summary ->
                        contentSource.loadLesson(summary.id).activities
                    }
                    .filterIsInstance<LearningActivity.MultipleChoice>()
                    .distinctBy { it.id.value }
                    .take(12)
                    .map { activity ->
                        PlacementQuestion(
                            id = activity.id.value,
                            level = level.cefr,
                            prompt = activity.prompt,
                            question = activity.question,
                            options = activity.options,
                            correctOptionId = activity.correctOptionId,
                        )
                    }
            }
    }
}

@Singleton
class RoomLearningEventLog @Inject constructor(
    private val database: SprevaDatabase,
) : LearningEventLog {

    private val eventsDao: LearningEventDao = database.learningEventDao()

    override suspend fun log(
        id: com.spreva.core.model.LearningEventId,
        type: String,
        subjectId: String,
        occurredAt: Instant,
        payloadJson: String,
    ) {
        eventsDao.insert(
            LearningEventEntity(
                eventId = id.value,
                eventType = type,
                subjectId = subjectId,
                occurredAtEpochMs = occurredAt.toEpochMilli(),
                payloadVersion = PAYLOAD_VERSION,
                payloadJson = payloadJson,
            ),
        )
    }

    private companion object {
        const val PAYLOAD_VERSION = 1
    }
}

/**
 * Provisions review cards for vocabulary found in any bundled lesson.
 * Card IDs are deterministic and INSERT OR IGNORE keeps completion retries idempotent.
 */
@Singleton
class DefaultReviewCardProvisioner @Inject constructor(
    private val database: SprevaDatabase,
    private val contentSource: ContentSource,
) : ReviewCardProvisioner {

    private val reviewCardDao: ReviewCardDao = database.reviewCardDao()

    override suspend fun provisionCardsForLesson(lessonId: LessonId) {
        val lesson = contentSource.loadLesson(lessonId) ?: return
        val now = System.currentTimeMillis()
        val words = lesson.activities
            .filterIsInstance<com.spreva.core.model.LearningActivity.VocabularyIntro>()
            .flatMap { activity -> activity.words }
        // Two modalities per word (plan section 90): text recognition and —
        // when a recording exists — audio recall. Idempotent via fixed IDs:
        // re-inserting the same cardIds is a no-op (OnConflictStrategy ignore).
        val cards = words.flatMap { word ->
            buildList {
                add(
                    ReviewCard(
                        id = ReviewCardId("${lessonId.value}_${word.id}"),
                        knowledgeItemId = com.spreva.core.model.KnowledgeItemId(word.id),
                        prompt = word.german,
                        answer = word.translation.ar ?: word.translation.de,
                        dueAt = Instant.ofEpochMilli(now),
                    ),
                )
                word.audio?.let { audio ->
                    add(
                        ReviewCard(
                            id = ReviewCardId("${lessonId.value}_${word.id}_audio"),
                            knowledgeItemId = com.spreva.core.model.KnowledgeItemId(word.id),
                            prompt = "",
                            answer = word.german,
                            dueAt = Instant.ofEpochMilli(now),
                            audioPath = audio,
                        ),
                    )
                }
            }
        }
        reviewCardDao.insertAll(cards.map(ReviewCard::toEntity))
    }

    override suspend fun hasCardsFor(knowledgeItemIds: List<String>): Boolean =
        reviewCardDao.countForKnowledgeItems(knowledgeItemIds) > 0
}

@Singleton
class RoomReviewRepository @Inject constructor(
    private val database: SprevaDatabase,
    private val settingsDataSource: SettingsDataSource,
    private val retentionCalibrator: com.spreva.domain.review.ReviewRetentionCalibrator,
) : ReviewRepository {

    private val cardDao: ReviewCardDao = database.reviewCardDao()
    private val logDao: ReviewLogDao = database.reviewLogDao()

    override fun observeDueCards(nowEpochMs: Long): Flow<List<ReviewCard>> =
        cardDao.observeDue(nowEpochMs).map { list -> list.map { it.toModel() } }

    override fun observeDueCount(nowEpochMs: Long): Flow<Int> =
        cardDao.observeDueCount(nowEpochMs)

    override suspend fun getCard(id: ReviewCardId): ReviewCard? =
        cardDao.getById(id.value)?.toModel()

    override suspend fun saveGradedCard(
        card: ReviewCard,
        rating: ReviewRating,
        reviewedAtEpochMs: Long,
        durationMs: Long,
    ) {
        database.withWriteTransaction {
            cardDao.update(card.toEntity())
            logDao.insert(
                ReviewLogEntity(
                    reviewId = "${card.id.value}_$reviewedAtEpochMs",
                    cardId = card.id.value,
                    rating = rating.name,
                    reviewedAtEpochMs = reviewedAtEpochMs,
                    durationMs = durationMs,
                    schedulerVersion = card.schedulerVersion,
                ),
            )
        }

        val ratings = logDao.getRecentRatings(com.spreva.domain.review.ReviewRetentionCalibrator.MAX_HISTORY)
            .mapNotNull { runCatching { ReviewRating.valueOf(it) }.getOrNull() }
        retentionCalibrator.recommend(ratings)?.let { recommendation ->
            settingsDataSource.setReviewRetentionTarget(recommendation.target)
        }
    }

    override suspend fun insertCardsIfAbsent(cards: List<ReviewCard>): Int =
        cardDao.insertAll(cards.map(ReviewCard::toEntity)).count { it != -1L }

    override suspend fun hasCardsForKnowledgeItems(knowledgeItemIds: List<String>): Boolean =
        cardDao.countForKnowledgeItems(knowledgeItemIds) > 0

    override suspend fun clearAll() {
        cardDao.clear()
        logDao.clear()
    }
}
