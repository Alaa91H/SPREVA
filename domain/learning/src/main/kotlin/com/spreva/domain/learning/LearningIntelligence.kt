package com.spreva.domain.learning

import com.spreva.core.model.ActivityAttempt
import com.spreva.core.model.ActivityId
import com.spreva.core.model.FocusRecommendation
import com.spreva.core.model.LearningProfile
import com.spreva.core.model.LessonId
import com.spreva.core.model.LocalizedText
import com.spreva.core.model.MistakeInsight
import com.spreva.core.model.MistakePattern
import com.spreva.core.model.SkillArea
import com.spreva.core.model.SkillMastery
import com.spreva.core.model.TopicMastery
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/** Raw evidence enriched with curriculum semantics before analysis. */
data class ActivityEvidence(
    val lessonId: LessonId,
    val lessonTitle: LocalizedText,
    val activityId: ActivityId,
    val skill: SkillArea,
    val objective: Boolean,
    val productive: Boolean,
    val attempts: List<ActivityAttempt>,
)

interface LearningIntelligenceRepository {
    fun observeProfile(): Flow<LearningProfile>
}

/**
 * Pure deterministic analytics over local attempt history.
 *
 * Objective mastery uses a Beta(2,2) prior and confidence weighting so a
 * single lucky answer never becomes "100% mastery". Writing/speaking are
 * completion evidence only until a real rubric/ASR evaluator exists.
 */
class LearningIntelligenceEngine @Inject constructor() {

    fun analyze(evidence: List<ActivityEvidence>): LearningProfile {
        val mastery = SkillArea.entries.mapNotNull { skill ->
            val rows = evidence.filter { it.skill == skill }
            if (rows.isEmpty()) return@mapNotNull null

            val objectiveAttempts = rows.filter { it.objective }.flatMap { it.attempts }
            val productiveAttempts = rows.filter { it.productive }.flatMap { it.attempts }

            val correct = objectiveAttempts.count { it.correct }
            val n = objectiveAttempts.size
            val posterior = if (n > 0) (correct + 2.0) / (n + 4.0) else null
            val confidence = ((n / 12.0).coerceIn(0.0, 1.0) * 100).roundToInt()
            val score = posterior?.let {
                val shrink = confidence / 100.0
                (50.0 + (it * 100.0 - 50.0) * shrink).roundToInt().coerceIn(0, 100)
            }

            SkillMastery(
                skill = skill,
                scorePercent = score,
                confidencePercent = confidence,
                objectiveCorrect = correct,
                objectiveAttempts = n,
                productiveCompleted = productiveAttempts.count { it.correct },
                productiveAttempts = productiveAttempts.size,
            )
        }

        val topics = evidence
            .groupBy { it.lessonId }
            .mapNotNull { (lessonId, rows) ->
                val objective = rows.filter { it.objective }.flatMap { it.attempts }
                if (objective.isEmpty()) return@mapNotNull null
                val correct = objective.count { it.correct }
                val n = objective.size
                val confidence = ((n / 12.0).coerceIn(0.0, 1.0) * 100).roundToInt()
                val posterior = (correct + 2.0) / (n + 4.0)
                val shrink = confidence / 100.0
                val score = (50.0 + (posterior * 100.0 - 50.0) * shrink)
                    .roundToInt()
                    .coerceIn(0, 100)
                val representative = rows.first()
                TopicMastery(
                    lessonId = lessonId,
                    lessonTitle = representative.lessonTitle,
                    skill = representative.skill,
                    scorePercent = score,
                    confidencePercent = confidence,
                    objectiveCorrect = correct,
                    objectiveAttempts = n,
                )
            }
            .sortedWith(compareBy<TopicMastery> { it.scorePercent }.thenByDescending { it.confidencePercent })

        val mistakes = evidence.flatMap { row -> mistakeSignals(row) }
            .sortedByDescending { it.severityPercent }

        val byLesson = evidence.associateBy { it.lessonId }
        val focus = mistakes
            .groupBy { it.lessonId }
            .mapNotNull { (lessonId, signals) ->
                val representative = signals.maxByOrNull { it.severityPercent } ?: return@mapNotNull null
                val row = byLesson[lessonId] ?: return@mapNotNull null
                val repeated = signals.count { it.pattern == MistakePattern.REPEATED_ERROR }
                val priority = (representative.severityPercent + repeated * 8).coerceAtMost(100)
                FocusRecommendation(
                    lessonId = lessonId,
                    lessonTitle = row.lessonTitle,
                    skill = representative.skill,
                    priorityPercent = priority,
                    reason = representative.pattern,
                )
            }
            .sortedByDescending { it.priorityPercent }
            .take(5)

        return LearningProfile(
            mastery = mastery,
            topics = topics,
            mistakes = mistakes.take(20),
            focus = focus,
            totalAttempts = evidence.sumOf { it.attempts.size },
        )
    }

    private fun mistakeSignals(row: ActivityEvidence): List<MistakeInsight> {
        if (row.attempts.isEmpty()) return emptyList()
        val attempts = row.attempts.sortedBy { it.createdAt }
        val incorrect = attempts.count { !it.correct }
        val total = attempts.size
        val result = mutableListOf<MistakeInsight>()

        fun add(pattern: MistakePattern, extra: Int = 0) {
            val errorRate = incorrect.toDouble() / total
            val evidenceWeight = (total / 4.0).coerceAtMost(1.0)
            val severity = (errorRate * 65 + evidenceWeight * 20 + extra).roundToInt().coerceIn(1, 100)
            result += MistakeInsight(
                lessonId = row.lessonId,
                activityId = row.activityId,
                skill = row.skill,
                pattern = pattern,
                severityPercent = severity,
                incorrectAttempts = incorrect,
                totalAttempts = total,
            )
        }

        if (incorrect >= 2) add(MistakePattern.REPEATED_ERROR, 15)
        if (incorrect > 0 && attempts.last().correct) add(MistakePattern.REPAIRED_AFTER_ERROR, 5)
        if (attempts.any { !it.correct && it.responseTimeMs in 1..1_500 }) {
            add(MistakePattern.RUSHED_GUESS, 10)
        }
        val correctTimes = attempts.filter { it.correct }.map { it.responseTimeMs }.filter { it > 0 }
        if (row.objective && correctTimes.isNotEmpty() && correctTimes.average() > 45_000) {
            add(MistakePattern.SLOW_RECALL, 8)
        }
        if (row.productive && attempts.any { !it.correct }) {
            add(MistakePattern.PRODUCTIVE_INCOMPLETE, 12)
        }

        return result
    }
}
