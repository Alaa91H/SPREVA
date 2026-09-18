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
import com.spreva.core.model.ActivityAttempt
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonProgress
import com.spreva.core.model.LessonStatus
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewCardId
import com.spreva.core.model.ReviewRating
import com.spreva.domain.curriculum.CurriculumRepository
import com.spreva.domain.learning.LearningEventLog
import com.spreva.domain.learning.LearningRepository
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
            // First level of the course until multi-level selection ships.
            val summaries = course.levels.firstOrNull()?.units
                ?.flatMap { unit -> unit.lessons }
                ?.map { ref -> contentSource.loadLesson(ref.id) }
                ?.map { lesson ->
                    com.spreva.core.model.LessonSummary(
                        id = lesson.id,
                        title = lesson.title,
                        activityCount = lesson.activities.size,
                    )
                }
                .orEmpty()
            emit(summaries)
        }

    override suspend fun getLesson(id: LessonId) = contentSource.loadLesson(id)

    override suspend fun getNextLesson(): com.spreva.core.model.Lesson? =
        contentSource.loadLesson(com.spreva.core.model.LessonId(FIRST_LESSON_ID))

    override suspend fun contentVersion(): String = contentSource.contentVersion()

    companion object {
        const val COURSE_ID = "de-core"
        const val FIRST_LESSON_ID = "a1_u01_l01"
    }
}

@Singleton
class OfflineFirstLearningRepository @Inject constructor(
    private val database: SprevaDatabase,
) : LearningRepository {

    private val progressDao: LessonProgressDao = database.lessonProgressDao()
    private val attemptsDao: ActivityAttemptDao = database.activityAttemptDao()

    override fun observeLessonProgress(lessonId: LessonId): Flow<LessonProgress?> =
        progressDao.observeByLesson(lessonId.value).map { it?.toModel() }

    override fun observeAllProgress(): Flow<List<LessonProgress>> =
        progressDao.observeAll().map { list -> list.map { it.toModel() } }

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
 * Provisions review cards for lesson vocabulary. Phase 3 uses a static
 * mapping from the bundled demo lesson; idempotent via INSERT OR IGNORE.
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
        val cards = lesson.activities
            .filterIsInstance<com.spreva.core.model.LearningActivity.VocabularyIntro>()
            .flatMap { activity -> activity.words }
            .map { word ->
                ReviewCard(
                    id = ReviewCardId("${lessonId.value}_${word.id}"),
                    knowledgeItemId = com.spreva.core.model.KnowledgeItemId(word.id),
                    prompt = word.german,
                    answer = word.translation.ar ?: word.translation.de,
                    dueAt = Instant.ofEpochMilli(now),
                )
            }
        reviewCardDao.insertAll(cards.map(ReviewCard::toEntity))
    }

    override suspend fun hasCardsFor(knowledgeItemIds: List<String>): Boolean =
        reviewCardDao.countForKnowledgeItems(knowledgeItemIds) > 0
}

@Singleton
class RoomReviewRepository @Inject constructor(
    private val database: SprevaDatabase,
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
