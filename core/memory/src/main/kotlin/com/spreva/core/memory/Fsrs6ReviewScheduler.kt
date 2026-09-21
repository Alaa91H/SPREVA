package com.spreva.core.memory

import com.spreva.core.model.FsrsSchedulerState
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewRating
import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * FSRS-6 scheduler using the public default 21-parameter model.
 *
 * The implementation intentionally keeps the algorithm behind
 * [ReviewScheduler]. Cards written by the old demo scheduler are migrated
 * lazily: their non-FSRS state is treated as a fresh memory state on the
 * next grade and the resulting card is persisted as FSRS-6.
 */
class Fsrs6ReviewScheduler(
    private val json: Json = Json,
    private val desiredRetentionProvider: () -> Double = { 0.90 },
) : ReviewScheduler {

    override val version: String = ReviewCard.SCHEDULER_VERSION_FSRS6

    override fun preview(card: ReviewCard, now: Instant): Map<ReviewRating, Instant> =
        ReviewRating.entries.associateWith { rating ->
            schedule(card, rating, now).dueAt
        }

    override fun grade(card: ReviewCard, rating: ReviewRating, now: Instant): ReviewCard {
        val scheduled = schedule(card, rating, now)
        return scheduled.copy(
            reviewCount = card.reviewCount + 1,
            lapseCount = card.lapseCount + if (rating == ReviewRating.AGAIN) 1 else 0,
            schedulerVersion = version,
        )
    }

    private fun schedule(card: ReviewCard, rating: ReviewRating, now: Instant): ReviewCard {
        val grade = rating.grade
        val previous = parseState(card)
        val nextState = if (previous == null) {
            initialState(grade, now)
        } else {
            nextState(previous, grade, now)
        }

        val intervalDays = intervalDays(nextState.stabilityDays)
        val dueAt = now.plusMillis(daysToMillis(intervalDays))
        return card.copy(
            dueAt = dueAt,
            schedulerVersion = version,
            schedulerState = json.encodeToString(nextState),
        )
    }

    private fun parseState(card: ReviewCard): FsrsSchedulerState? {
        val stateJson = card.schedulerState
        if (card.schedulerVersion != version || stateJson.isNullOrBlank()) return null
        return runCatching {
            json.decodeFromString<FsrsSchedulerState>(stateJson)
        }.getOrNull()?.takeIf {
            it.stabilityDays.isFinite() && it.stabilityDays > 0.0 &&
                it.difficulty.isFinite() && it.difficulty in 1.0..10.0
        }
    }

    private fun initialState(grade: Int, now: Instant): FsrsSchedulerState =
        FsrsSchedulerState(
            stabilityDays = W[grade - 1].coerceAtLeast(MIN_STABILITY_DAYS),
            difficulty = initialDifficulty(grade),
            lastReviewAtEpochMs = now.toEpochMilli(),
        )

    private fun nextState(previous: FsrsSchedulerState, grade: Int, now: Instant): FsrsSchedulerState {
        val elapsedDays = Duration.between(
            Instant.ofEpochMilli(previous.lastReviewAtEpochMs),
            now,
        ).toMillis().coerceAtLeast(0L) / DAY_MS.toDouble()

        val retrievability = retrievability(
            elapsedDays = elapsedDays,
            stabilityDays = previous.stabilityDays,
        )
        val difficulty = nextDifficulty(previous.difficulty, grade)

        val stability = if (elapsedDays < 1.0) {
            sameDayStability(previous.stabilityDays, grade)
        } else if (grade == 1) {
            forgettingStability(
                stability = previous.stabilityDays,
                difficulty = difficulty,
                retrievability = retrievability,
            )
        } else {
            recallStability(
                stability = previous.stabilityDays,
                difficulty = difficulty,
                retrievability = retrievability,
                grade = grade,
            )
        }.coerceIn(MIN_STABILITY_DAYS, MAX_STABILITY_DAYS)

        return FsrsSchedulerState(
            stabilityDays = stability,
            difficulty = difficulty,
            lastReviewAtEpochMs = now.toEpochMilli(),
        )
    }

    /** FSRS-5/6 initial difficulty; FSRS-6 retains this part of the model. */
    private fun initialDifficulty(grade: Int): Double =
        (W[4] - exp(W[5] * (grade - 1)) + 1.0).coerceIn(1.0, 10.0)

    /** Linear damping plus mean reversion toward D0(Easy). */
    private fun nextDifficulty(current: Double, grade: Int): Double {
        val delta = -W[6] * (grade - 3)
        val damped = current + delta * (10.0 - current) / 9.0
        val target = initialDifficulty(4)
        return (W[7] * target + (1.0 - W[7]) * damped).coerceIn(1.0, 10.0)
    }

    private fun sameDayStability(stability: Double, grade: Int): Double {
        val multiplier = exp(W[17] * (grade - 3 + W[18])) * stability.pow(-W[19])
        val candidate = stability * multiplier
        return if (grade >= 2) max(stability, candidate) else candidate
    }

    private fun recallStability(
        stability: Double,
        difficulty: Double,
        retrievability: Double,
        grade: Int,
    ): Double {
        val hardPenalty = if (grade == 2) W[15] else 1.0
        val easyBonus = if (grade == 4) W[16] else 1.0
        val increase = exp(W[8]) *
            (11.0 - difficulty) *
            stability.pow(-W[9]) *
            (exp(W[10] * (1.0 - retrievability)) - 1.0) *
            hardPenalty *
            easyBonus
        return stability * (1.0 + increase.coerceAtLeast(0.0))
    }

    private fun forgettingStability(
        stability: Double,
        difficulty: Double,
        retrievability: Double,
    ): Double =
        W[11] *
            difficulty.pow(-W[12]) *
            ((stability + 1.0).pow(W[13]) - 1.0) *
            exp(W[14] * (1.0 - retrievability))

    internal fun retrievability(elapsedDays: Double, stabilityDays: Double): Double {
        val decay = W[20]
        val factor = 0.9.pow(-1.0 / decay) - 1.0
        return (1.0 + factor * elapsedDays.coerceAtLeast(0.0) / stabilityDays)
            .pow(-decay)
            .coerceIn(0.0, 1.0)
    }

    /** Solve the FSRS-6 forgetting curve for the requested retention. */
    internal fun intervalDays(stabilityDays: Double): Double {
        val decay = W[20]
        val factor = 0.9.pow(-1.0 / decay) - 1.0
        val desiredRetention = desiredRetentionProvider().coerceIn(0.70, 0.99)
        val raw = stabilityDays / factor *
            (desiredRetention.pow(-1.0 / decay) - 1.0)
        return raw.coerceIn(MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS)
    }

    private fun daysToMillis(days: Double): Long =
        (days * DAY_MS).toLong().coerceAtLeast(MIN_INTERVAL_MS)

    private val ReviewRating.grade: Int
        get() = when (this) {
            ReviewRating.AGAIN -> 1
            ReviewRating.HARD -> 2
            ReviewRating.GOOD -> 3
            ReviewRating.EASY -> 4
        }

    companion object {
        /** Public FSRS-6 defaults (21 parameters). */
        internal val W = doubleArrayOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194,
            0.001, 1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629,
            1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
        )

        private const val DAY_MS = 86_400_000L
        private const val MIN_INTERVAL_MS = 10 * 60 * 1000L
        private const val MIN_INTERVAL_DAYS = MIN_INTERVAL_MS / DAY_MS.toDouble()
        private const val MAX_INTERVAL_DAYS = 36_500.0
        private const val MIN_STABILITY_DAYS = 1.0 / (24.0 * 60.0)
        private const val MAX_STABILITY_DAYS = 36_500.0
    }
}
