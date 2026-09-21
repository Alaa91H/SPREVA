package com.spreva.core.model

import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * A review card scheduled by the memory engine.
 *
 * The card is decoupled from the knowledge item: one lexeme can yield
 * several cards (recognition, production, article, ...).
 */
data class ReviewCard(
    val id: ReviewCardId,
    val knowledgeItemId: KnowledgeItemId,
    /** German prompt shown to the learner (empty for audio cards). */
    val prompt: String,
    /** Expected answer / translation hint. */
    val answer: String,
    val dueAt: Instant,
    val reviewCount: Int = 0,
    val lapseCount: Int = 0,
    /** Version of the scheduler that produced the current state. */
    val schedulerVersion: String = SCHEDULER_VERSION_FSRS6,
    /** Opaque serialized scheduler state for recompute/migration. */
    val schedulerState: String? = null,
    /**
     * Audio-card variant (Phase 4.3): bundled recording path, relative to
     * content/. Non-null turns the review into listening recall — play the
     * recording, type the German phrase. Prompt is then empty.
     */
    val audioPath: String? = null,
) {
    companion object {
        const val SCHEDULER_VERSION_DEMO = "demo-v1"
        const val SCHEDULER_VERSION_FSRS6 = "fsrs-6-default-v1"
    }
}

/** Learner's self-assessed recall quality. */
enum class ReviewRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

/** Result of grading a card: the updated card with its next due instant. */
data class SchedulingResult(
    val card: ReviewCard,
    val nextDueAt: Instant,
)

/** Serializable state persistence for the legacy demo scheduler internals. */
@Serializable
data class DemoSchedulerState(
    val intervalIndex: Int = 0,
)

/**
 * FSRS-6 memory state persisted inside [ReviewCard.schedulerState].
 *
 * Stability is measured in days and represents the interval at which the
 * model predicts 90% retrievability. Difficulty is clamped to 1..10.
 * Keeping this opaque JSON in the existing card column lets scheduler
 * upgrades happen without changing the general Room schema.
 */
@Serializable
data class FsrsSchedulerState(
    val stabilityDays: Double,
    val difficulty: Double,
    val lastReviewAtEpochMs: Long,
)
