package com.spreva.core.memory

import com.spreva.core.model.DemoSchedulerState
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewRating
import java.time.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Simple deterministic scheduler used for the Phase 3 demo flow.
 * It is intentionally NOT FSRS and does not pretend to be (plan section 94).
 *
 * Intervals: Again 10 min, Hard 1 day, Good 3 days, Easy 7 days, expanding
 * with each successful review.
 */
class DemoReviewScheduler(
    private val json: Json = Json,
) : ReviewScheduler {

    override val version: String = ReviewCard.SCHEDULER_VERSION_DEMO

    override fun preview(card: ReviewCard, now: Instant): Map<ReviewRating, Instant> =
        ReviewRating.entries.associateWith { nextDue(card, it, now) }

    override fun grade(card: ReviewCard, rating: ReviewRating, now: Instant): ReviewCard {
        val state = card.schedulerState?.let {
            runCatching { json.decodeFromString<DemoSchedulerState>(it) }.getOrNull()
        } ?: DemoSchedulerState()

        val newState = when (rating) {
            ReviewRating.AGAIN -> state.copy(intervalIndex = 0)
            ReviewRating.HARD -> state.copy(intervalIndex = (state.intervalIndex + 1).coerceAtMost(3))
            ReviewRating.GOOD -> state.copy(intervalIndex = (state.intervalIndex + 1).coerceAtMost(3))
            ReviewRating.EASY -> state.copy(intervalIndex = (state.intervalIndex + 2).coerceAtMost(3))
        }

        val lapses = if (rating == ReviewRating.AGAIN) card.lapseCount + 1 else card.lapseCount

        return card.copy(
            dueAt = nextDue(card, rating, now),
            reviewCount = card.reviewCount + 1,
            lapseCount = lapses,
            schedulerState = json.encodeToString(newState),
        )
    }

    private fun nextDue(card: ReviewCard, rating: ReviewRating, now: Instant): Instant {
        val state = card.schedulerState?.let {
            runCatching { json.decodeFromString<DemoSchedulerState>(it) }.getOrNull()
        } ?: DemoSchedulerState()
        val baseIndex = state.intervalIndex
        return when (rating) {
            ReviewRating.AGAIN -> now.plusSeconds(10 * 60)
            ReviewRating.HARD -> now.plusSeconds(BASE_INTERVALS_SECONDS[(baseIndex + 1).coerceAtMost(3)])
            ReviewRating.GOOD -> now.plusSeconds(BASE_INTERVALS_SECONDS[(baseIndex + 1).coerceAtMost(3)])
            ReviewRating.EASY -> now.plusSeconds(BASE_INTERVALS_SECONDS[(baseIndex + 2).coerceAtMost(3)])
        }
    }

    private companion object {
        // 1 day, 3 days, 7 days, 14 days
        val BASE_INTERVALS_SECONDS = longArrayOf(
            24 * 3600,
            3 * 24 * 3600,
            7 * 24 * 3600,
            14 * 24 * 3600,
        )
    }
}
