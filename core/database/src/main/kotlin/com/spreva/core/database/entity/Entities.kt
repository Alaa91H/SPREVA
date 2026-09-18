package com.spreva.core.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "lesson_progress", indices = [Index(value = ["lessonId"], unique = true)])
data class LessonProgressEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val lessonId: String,
    /** Stored as the name of [com.spreva.core.model.LessonStatus]. */
    val status: String,
    val completedActivities: Int,
    val totalActivities: Int,
    val startedAtEpochMs: Long?,
    val updatedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
)

@Entity(tableName = "activity_attempts", indices = [Index(value = ["lessonId", "activityId"])])
data class ActivityAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String,
    val activityId: String,
    val attemptNumber: Int,
    val correct: Boolean,
    val usedHint: Boolean,
    val responseTimeMs: Long,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "learning_events")
data class LearningEventEntity(
    @PrimaryKey val eventId: String,
    val eventType: String,
    val subjectId: String,
    val occurredAtEpochMs: Long,
    val payloadVersion: Int,
    val payloadJson: String,
    /** Phase 3: always LOCAL. Sync states arrive with the sync phase. */
    val syncState: String = "LOCAL",
)

@Entity(tableName = "review_cards", indices = [Index(value = ["dueAtEpochMs"])])
data class ReviewCardEntity(
    @PrimaryKey val cardId: String,
    val knowledgeItemId: String,
    val prompt: String,
    val answer: String,
    val dueAtEpochMs: Long,
    val reviewCount: Int,
    val lapseCount: Int,
    val schedulerVersion: String,
    val schedulerState: String?,
    /** Schema v2: audio-recall variant — bundled path relative to content/. */
    val audioPath: String? = null,
)

@Entity(tableName = "review_logs")
data class ReviewLogEntity(
    @PrimaryKey val reviewId: String,
    val cardId: String,
    /** Stored as the name of [com.spreva.core.model.ReviewRating]. */
    val rating: String,
    val reviewedAtEpochMs: Long,
    val durationMs: Long,
    val schedulerVersion: String,
)
