package com.spreva.domain.learning

import com.spreva.core.model.ActivityAttempt
import com.spreva.core.model.ActivityId
import com.spreva.core.model.LessonId
import com.spreva.core.model.LocalizedText
import com.spreva.core.model.MistakePattern
import com.spreva.core.model.SkillArea
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningIntelligenceEngineTest {

    private val engine = LearningIntelligenceEngine()
    private val now = Instant.parse("2026-09-21T09:00:00Z")

    private fun attempt(n: Int, correct: Boolean, ms: Long = 5_000) = ActivityAttempt(
        id = n.toLong(),
        lessonId = LessonId("b1_u01_l03"),
        activityId = ActivityId("b1_u01_l03_act03"),
        attemptNumber = n,
        correct = correct,
        usedHint = false,
        responseTimeMs = ms,
        createdAt = now.plusSeconds(n.toLong()),
    )

    @Test
    fun `mastery is confidence weighted instead of instantly perfect`() {
        val profile = engine.analyze(
            listOf(
                ActivityEvidence(
                    lessonId = LessonId("b1_u01_l03"),
                    lessonTitle = LocalizedText("Zeitformen"),
                    activityId = ActivityId("a"),
                    skill = SkillArea.GRAMMAR,
                    objective = true,
                    productive = false,
                    attempts = listOf(attempt(1, true)),
                ),
            ),
        )
        val grammar = profile.mastery.single()
        assertEquals(1, grammar.objectiveAttempts)
        assertTrue(grammar.scorePercent!! < 100)
        assertTrue(grammar.confidencePercent < 20)
    }

    @Test
    fun `repeated failures become focus recommendation`() {
        val row = ActivityEvidence(
            lessonId = LessonId("b1_u01_l03"),
            lessonTitle = LocalizedText("Zeitformen"),
            activityId = ActivityId("a"),
            skill = SkillArea.GRAMMAR,
            objective = true,
            productive = false,
            attempts = listOf(attempt(1, false), attempt(2, false), attempt(3, true)),
        )
        val profile = engine.analyze(listOf(row))
        assertTrue(profile.mistakes.any { it.pattern == MistakePattern.REPEATED_ERROR })
        assertEquals("b1_u01_l03", profile.focus.first().lessonId.value)
    }

    @Test
    fun `productive evidence is not converted into linguistic score`() {
        val profile = engine.analyze(
            listOf(
                ActivityEvidence(
                    lessonId = LessonId("b2_u16_l02"),
                    lessonTitle = LocalizedText("Schreiben"),
                    activityId = ActivityId("w"),
                    skill = SkillArea.WRITING,
                    objective = false,
                    productive = true,
                    attempts = listOf(attempt(1, true)),
                ),
            ),
        )
        val writing = profile.mastery.single()
        assertEquals(null, writing.scorePercent)
        assertEquals(1, writing.productiveCompleted)
    }
}
