package com.spreva.core.memory

import com.spreva.core.model.KnowledgeItemId
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewCardId
import com.spreva.core.model.ReviewRating
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic behaviour tests for the demo scheduler (plan section 94:
 * demo scheduler is intentionally simple; FSRS lands behind the same
 * interface later).
 */
class DemoReviewSchedulerTest {

    private val scheduler = DemoReviewScheduler(Json)
    private val now: Instant = Instant.ofEpochMilli(1_700_000_000_000)

    private fun card(schedulerState: String? = null, lapses: Int = 0) = ReviewCard(
        id = ReviewCardId("card_1"),
        knowledgeItemId = KnowledgeItemId("word_1"),
        prompt = "der Tisch",
        answer = "الطاولة",
        dueAt = now,
        schedulerState = schedulerState,
        lapseCount = lapses,
    )

    @Test
    fun `again schedules ten minutes later and counts a lapse`() {
        val graded = scheduler.grade(card(), ReviewRating.AGAIN, now)

        assertEquals(now.plusSeconds(10 * 60), graded.dueAt)
        assertEquals(1, graded.lapseCount)
        assertEquals(1, graded.reviewCount)
    }

    @Test
    fun `good schedules three days later from a fresh card`() {
        val graded = scheduler.grade(card(), ReviewRating.GOOD, now)

        assertEquals(now.plusSeconds(3 * 24 * 3600), graded.dueAt)
        assertEquals(0, graded.lapseCount)
    }

    @Test
    fun `easy schedules seven days later from a fresh card`() {
        val graded = scheduler.grade(card(), ReviewRating.EASY, now)

        assertEquals(now.plusSeconds(7 * 24 * 3600), graded.dueAt)
    }

    @Test
    fun `interval grows after consecutive good grades`() {
        val first = scheduler.grade(card(), ReviewRating.GOOD, now)
        val second = scheduler.grade(first, ReviewRating.GOOD, now)

        assertEquals(now.plusSeconds(7 * 24 * 3600), second.dueAt)
    }

    @Test
    fun `again resets the interval ladder`() {
        val first = scheduler.grade(card(), ReviewRating.GOOD, now)
        val second = scheduler.grade(first, ReviewRating.GOOD, now)
        val reset = scheduler.grade(second, ReviewRating.AGAIN, now)

        assertEquals(now.plusSeconds(10 * 60), reset.dueAt)
        // Ladder restarts from index 0 but review/lapse counters accumulate.
        assertTrue(reset.lapseCount == 1)
    }

    @Test
    fun `scheduler version is demo-v1`() {
        assertEquals(ReviewCard.SCHEDULER_VERSION_DEMO, scheduler.version)
    }

    @Test
    fun `preview returns options for all ratings`() {
        val preview = scheduler.preview(card(), now)

        assertEquals(ReviewRating.entries.size, preview.size)
        assertTrue(preview.containsKey(ReviewRating.GOOD))
    }

    @Test
    fun `scheduler state round-trips through json`() {
        val first = scheduler.grade(card(), ReviewRating.GOOD, now)
        val reloaded = first.copy(schedulerState = first.schedulerState)

        val next = scheduler.grade(reloaded, ReviewRating.GOOD, now)
        assertEquals(now.plusSeconds(7 * 24 * 3600), next.dueAt)
    }
}
