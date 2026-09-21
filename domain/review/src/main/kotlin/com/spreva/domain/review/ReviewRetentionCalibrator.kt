package com.spreva.domain.review

import com.spreva.core.model.ReviewRating
import javax.inject.Inject

data class ReviewRetentionRecommendation(
    val target: Double,
    val sampleSize: Int,
    val againRate: Double,
    val hardRate: Double,
)

/**
 * Conservative personalization of FSRS desired retention.
 *
 * This does NOT fit the 21 FSRS weights. Weight optimization needs much
 * more learner history and a proper optimizer. Instead, once enough review
 * evidence exists, this policy tunes the desired-retention control
 * parameter within a narrow safe range.
 */
class ReviewRetentionCalibrator @Inject constructor() {

    fun recommend(ratings: List<ReviewRating>): ReviewRetentionRecommendation? {
        if (ratings.size < MIN_HISTORY) return null
        val sample = ratings.take(MAX_HISTORY)
        val n = sample.size.toDouble()
        val againRate = sample.count { it == ReviewRating.AGAIN } / n
        val hardRate = sample.count { it == ReviewRating.HARD } / n
        val easyRate = sample.count { it == ReviewRating.EASY } / n

        val target = when {
            againRate >= 0.20 -> 0.95
            againRate >= 0.15 -> 0.94
            againRate >= 0.11 -> 0.92
            againRate <= 0.035 && easyRate >= 0.20 -> 0.87
            againRate <= 0.055 && (hardRate + againRate) <= 0.16 -> 0.88
            againRate <= 0.075 -> 0.89
            else -> 0.90
        }.coerceIn(MIN_TARGET, MAX_TARGET)

        return ReviewRetentionRecommendation(
            target = target,
            sampleSize = sample.size,
            againRate = againRate,
            hardRate = hardRate,
        )
    }

    companion object {
        const val MIN_HISTORY = 50
        const val MAX_HISTORY = 200
        const val MIN_TARGET = 0.85
        const val MAX_TARGET = 0.95
    }
}
