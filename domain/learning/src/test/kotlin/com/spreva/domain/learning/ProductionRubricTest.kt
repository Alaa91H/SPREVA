package com.spreva.domain.learning

import com.spreva.core.model.ActivityId
import com.spreva.core.model.CefrLevel
import com.spreva.core.model.LearningActivity
import com.spreva.core.model.LessonId
import com.spreva.core.model.LocalizedText
import com.spreva.core.model.ProductionMode
import com.spreva.core.model.RubricRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionRubricTest {

    private val factory = ProductionRubricFactory()
    private val scorer = ProductionRubricScorer()

    @Test
    fun `writing rubric has four criteria at every supported level`() {
        ProductionRubricFactory.SUPPORTED_LEVELS.forEach { level ->
            val rubric = factory.build(level, ProductionMode.WRITING)
            assertEquals(4, rubric.criteria.size)
            assertEquals(4, rubric.criteria.map { it.id }.distinct().size)
        }
    }

    @Test
    fun `speaking rubric becomes level specific`() {
        val a1 = factory.build(CefrLevel.A1, ProductionMode.SPEAKING)
        val c1 = factory.build(CefrLevel.C1, ProductionMode.SPEAKING)
        val a1Guidance = a1.criteria.first { it.id == "flow" }.guidance.de
        val c1Guidance = c1.criteria.first { it.id == "flow" }.guidance.de
        assertNotEquals(a1Guidance, c1Guidance)
        assertTrue(c1Guidance.contains("Diskurs", ignoreCase = true))
    }

    @Test
    fun `factory detects mode and level from real activity`() {
        val activity = LearningActivity.FreeWrite(
            id = ActivityId("b2_u16_l03_act12"),
            prompt = LocalizedText("Schreiben"),
            minWords = 140,
        )
        val rubric = factory.forActivity(LessonId("b2_u16_l03"), activity)!!
        assertEquals(CefrLevel.B2, rubric.level)
        assertEquals(ProductionMode.WRITING, rubric.mode)
    }

    @Test
    fun `full consistent self review scores one hundred`() {
        val rubric = factory.build(CefrLevel.B1, ProductionMode.WRITING)
        val ratings = rubric.criteria.associate { it.id to RubricRating.CONSISTENT }
        assertEquals(100, scorer.score(rubric, ratings))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `partial rubric cannot be scored`() {
        val rubric = factory.build(CefrLevel.C1, ProductionMode.SPEAKING)
        scorer.score(rubric, mapOf("task" to RubricRating.MOSTLY_EFFECTIVE))
    }
}
