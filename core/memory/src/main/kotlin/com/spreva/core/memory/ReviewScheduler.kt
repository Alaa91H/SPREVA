package com.spreva.core.memory

import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewRating
import java.time.Instant

/**
 * Contract for the spaced-repetition engine. Implementations must not leak
 * algorithm details into UI or the general DB schema (plan sections 77/96).
 * The FSRS adapter lands in a later phase behind this exact interface.
 */
interface ReviewScheduler {
    /** Scheduler identity stored with every card and log row. */
    val version: String

    /**
     * Options shown to the learner before committing: for each rating,
     * when the card would come back.
     */
    fun preview(card: ReviewCard, now: Instant): Map<ReviewRating, Instant>

    /**
     * Applies a grade and returns the updated card (with new scheduler
     * state and counters) plus the next due instant.
     */
    fun grade(card: ReviewCard, rating: ReviewRating, now: Instant): ReviewCard
}
