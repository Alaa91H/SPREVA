package com.spreva.core.model

import java.time.Instant

/** Status of a lesson in the learner's progress. */
enum class LessonStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
}

/** Progress of a single lesson for the local learner. */
data class LessonProgress(
    val lessonId: LessonId,
    val status: LessonStatus,
    val completedActivities: Int,
    val totalActivities: Int,
    val startedAt: Instant?,
    val updatedAt: Instant,
    val completedAt: Instant?,
)

/** A single recorded attempt on a lesson activity. */
data class ActivityAttempt(
    val id: Long,
    val lessonId: LessonId,
    val activityId: ActivityId,
    val attemptNumber: Int,
    /** Whether the attempt succeeded. */
    val correct: Boolean,
    val usedHint: Boolean,
    val responseTimeMs: Long,
    val createdAt: Instant,
)

/** Append-only learning event (Phase 3: local only). */
data class LearningEvent(
    val id: LearningEventId,
    val type: String,
    val subjectId: String,
    val occurredAt: Instant,
    val payloadVersion: Int,
    val payloadJson: String,
)
