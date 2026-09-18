package com.spreva.domain.learning

import com.spreva.core.common.IdGenerator
import com.spreva.core.model.LearningEventId
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonStatus
import java.time.Instant

/**
 * Marks a lesson complete. Card creation for the lesson's vocabulary is
 * delegated to [ReviewCardProvisioner] so the operation stays idempotent
 * (plan sections 245-247) — the database layer wraps it in a transaction
 * via the repository implementation.
 */
class CompleteLesson(
    private val learningRepository: LearningRepository,
    private val reviewRepository: ReviewCardProvisioner,
    private val eventLog: LearningEventLog,
    private val idGenerator: IdGenerator,
) {

    suspend operator fun invoke(lessonId: LessonId, totalActivities: Int, now: Instant) {
        learningRepository.completeLesson(lessonId, totalActivities)
        reviewRepository.provisionCardsForLesson(lessonId)
        eventLog.log(
            id = LearningEventId(idGenerator.newId()),
            type = EVENT_LESSON_COMPLETED,
            subjectId = lessonId.value,
            occurredAt = now,
            payloadJson = """{"lessonId":"${lessonId.value}"}""",
        )
    }

    companion object {
        const val EVENT_LESSON_COMPLETED = "LessonCompleted"
    }
}

/** Provides review cards for a lesson's vocabulary items, idempotently. */
interface ReviewCardProvisioner {
    /**
     * Creates review cards for the lesson's vocabulary if none exist yet.
     * Must be safe to call repeatedly (no duplicate cards).
     */
    suspend fun provisionCardsForLesson(lessonId: LessonId)

    /** True when cards already exist for the given knowledge items. */
    suspend fun hasCardsFor(knowledgeItemIds: List<String>): Boolean
}

/** Append-only learning event sink. */
interface LearningEventLog {
    suspend fun log(
        id: LearningEventId,
        type: String,
        subjectId: String,
        occurredAt: Instant,
        payloadJson: String,
    )
}
