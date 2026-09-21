package com.spreva.domain.learning

import com.spreva.core.model.CefrLevel
import com.spreva.core.model.PlacementAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePlacementEngineTest {

    private val engine = AdaptivePlacementEngine()

    private fun answers(level: CefrLevel, correct: Int): List<PlacementAnswer> =
        (0 until 4).map { i ->
            PlacementAnswer(
                questionId = "${level.name}_$i",
                level = level,
                correct = i < correct,
            )
        }

    @Test
    fun `starts at B1`() {
        val result = engine.decide(emptyList())
        assertEquals(CefrLevel.B1, result.nextLevel)
        assertFalse(result.finished)
    }

    @Test
    fun `strong B1 moves to B2 and failed B2 settles at B1`() {
        val b1 = answers(CefrLevel.B1, 4)
        assertEquals(CefrLevel.B2, engine.decide(b1).nextLevel)

        val result = engine.decide(b1 + answers(CefrLevel.B2, 1))
        assertTrue(result.finished)
        assertEquals(CefrLevel.B1, result.recommendedLevel)
    }

    @Test
    fun `weak B1 moves down and strong A2 settles at A2`() {
        val b1 = answers(CefrLevel.B1, 1)
        assertEquals(CefrLevel.A2, engine.decide(b1).nextLevel)

        val result = engine.decide(b1 + answers(CefrLevel.A2, 3))
        assertTrue(result.finished)
        assertEquals(CefrLevel.A2, result.recommendedLevel)
    }

    @Test
    fun `middle score settles current level`() {
        val result = engine.decide(answers(CefrLevel.B1, 2))
        assertTrue(result.finished)
        assertEquals(CefrLevel.B1, result.recommendedLevel)
    }

    @Test
    fun `strong path can reach C1`() {
        val history = answers(CefrLevel.B1, 4) +
            answers(CefrLevel.B2, 4) +
            answers(CefrLevel.C1, 3)
        val result = engine.decide(history)
        assertTrue(result.finished)
        assertEquals(CefrLevel.C1, result.recommendedLevel)
    }
}
