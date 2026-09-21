package com.spreva.domain.learning

import com.spreva.core.model.CefrLevel
import com.spreva.core.model.PlacementAnswer
import com.spreva.core.model.PlacementDecision
import com.spreva.core.model.PlacementQuestion
import javax.inject.Inject

interface PlacementQuestionRepository {
    suspend fun questions(): List<PlacementQuestion>
}

/**
 * Short adaptive placement: four questions per visited level.
 *
 * Starts at B1. Strong evidence (>=3/4) moves upward; weak evidence (<=1/4)
 * moves downward; 2/4 settles at the current level. When a learner moved
 * upward and then fails, the previous demonstrated level is recommended.
 * The result is a starting-point recommendation, never a CEFR certificate.
 */
class AdaptivePlacementEngine @Inject constructor() {

    fun decide(history: List<PlacementAnswer>): PlacementDecision {
        if (history.isEmpty()) {
            return PlacementDecision(
                nextLevel = CefrLevel.B1,
                recommendedLevel = null,
                finished = false,
                confidencePercent = 0,
                answeredCount = 0,
            )
        }

        val visited = history.map { it.level }.distinct()
        val current = history.last().level
        val currentAnswers = history.filter { it.level == current }
        if (currentAnswers.size < BUNDLE_SIZE) {
            return PlacementDecision(
                nextLevel = current,
                recommendedLevel = null,
                finished = false,
                confidencePercent = confidence(history.size),
                answeredCount = history.size,
            )
        }

        val correct = currentAnswers.count { it.correct }
        val currentIndex = LEVELS.indexOf(current)
        val previous = visited.getOrNull(visited.lastIndex - 1)
        val direction = previous?.let { currentIndex.compareTo(LEVELS.indexOf(it)) } ?: 0

        if (correct in 2..2) {
            return finish(current, history.size)
        }

        if (correct >= 3) {
            if (current == CefrLevel.C1) return finish(CefrLevel.C1, history.size)
            if (direction < 0) {
                // We moved down from a harder level and have now demonstrated this level.
                return finish(current, history.size)
            }
            val next = LEVELS[currentIndex + 1]
            return continueAt(next, history.size)
        }

        // 0..1 correct
        if (current == CefrLevel.A1) return finish(CefrLevel.A1, history.size)
        if (direction > 0 && previous != null) {
            // Failed after moving upward: previous level is the demonstrated ceiling.
            return finish(previous, history.size)
        }
        val next = LEVELS[currentIndex - 1]
        return continueAt(next, history.size)
    }

    private fun continueAt(level: CefrLevel, answered: Int) = PlacementDecision(
        nextLevel = level,
        recommendedLevel = null,
        finished = false,
        confidencePercent = confidence(answered),
        answeredCount = answered,
    )

    private fun finish(level: CefrLevel, answered: Int) = PlacementDecision(
        nextLevel = null,
        recommendedLevel = level,
        finished = true,
        confidencePercent = confidence(answered).coerceAtLeast(65),
        answeredCount = answered,
    )

    private fun confidence(answered: Int): Int =
        (45 + answered * 4).coerceIn(0, 90)

    companion object {
        const val BUNDLE_SIZE = 4
        val LEVELS = listOf(
            CefrLevel.A1,
            CefrLevel.A2,
            CefrLevel.B1,
            CefrLevel.B2,
            CefrLevel.C1,
        )
    }
}
