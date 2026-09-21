package com.spreva.core.memory

import com.spreva.core.model.FsrsSchedulerState
import com.spreva.core.model.KnowledgeItemId
import com.spreva.core.model.ReviewCard
import com.spreva.core.model.ReviewCardId
import com.spreva.core.model.ReviewRating
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Fsrs6ReviewSchedulerTest {

    private val json = Json
    private val scheduler = Fsrs6ReviewScheduler(json)
    private val now = Instant.parse("2026-09-21T09:00:00Z")

    private fun fresh() = ReviewCard(
        id = ReviewCardId("c1"),
        knowledgeItemId = KnowledgeItemId("k1"),
        prompt = "der Termin",
        answer = "الموعد",
        dueAt = now,
    )

    @Test
    fun `fresh card intervals are ordered by rating`() {
        val preview = scheduler.preview(fresh(), now)
        val again = preview.getValue(ReviewRating.AGAIN)
        val hard = preview.getValue(ReviewRating.HARD)
        val good = preview.getValue(ReviewRating.GOOD)
        val easy = preview.getValue(ReviewRating.EASY)

        assertTrue(again < hard)
        assertTrue(hard < good)
        assertTrue(good < easy)
    }

    @Test
    fun `grading persists fsrs state and counters`() {
        val graded = scheduler.grade(fresh(), ReviewRating.GOOD, now)
        val state = json.decodeFromString<FsrsSchedulerState>(graded.schedulerState!!)

        assertEquals(ReviewCard.SCHEDULER_VERSION_FSRS6, graded.schedulerVersion)
        assertEquals(1, graded.reviewCount)
        assertEquals(0, graded.lapseCount)
        assertEquals(Fsrs6ReviewScheduler.W[2], state.stabilityDays, 1e-9)
        assertTrue(state.difficulty in 1.0..10.0)
        assertEquals(now.toEpochMilli(), state.lastReviewAtEpochMs)
    }

    @Test
    fun `successful delayed review increases stability`() {
        val first = scheduler.grade(fresh(), ReviewRating.GOOD, now)
        val firstState = json.decodeFromString<FsrsSchedulerState>(first.schedulerState!!)
        val later = now.plus(Duration.ofDays(3))
        val second = scheduler.grade(first, ReviewRating.GOOD, later)
        val secondState = json.decodeFromString<FsrsSchedulerState>(second.schedulerState!!)

        assertTrue(secondState.stabilityDays > firstState.stabilityDays)
        assertTrue(second.dueAt > later)
    }

    @Test
    fun `again increments lapse and schedules a future retry`() {
        val first = scheduler.grade(fresh(), ReviewRating.GOOD, now)
        val later = now.plus(Duration.ofDays(3))
        val lapsed = scheduler.grade(first, ReviewRating.AGAIN, later)

        assertEquals(1, lapsed.lapseCount)
        assertEquals(2, lapsed.reviewCount)
        assertTrue(lapsed.dueAt > later)
    }

    @Test
    fun `legacy demo card lazily migrates to fsrs`() {
        val legacy = fresh().copy(
            schedulerVersion = ReviewCard.SCHEDULER_VERSION_DEMO,
            schedulerState = """{"intervalIndex":2}""",
            reviewCount = 5,
        )
        val migrated = scheduler.grade(legacy, ReviewRating.HARD, now)

        assertEquals(ReviewCard.SCHEDULER_VERSION_FSRS6, migrated.schedulerVersion)
        assertEquals(6, migrated.reviewCount)
        assertTrue(migrated.schedulerState!!.contains("stabilityDays"))
    }

    @Test
    fun `retrievability is ninety percent at stability`() {
        val stability = 10.0
        val r = scheduler.retrievability(elapsedDays = stability, stabilityDays = stability)
        assertEquals(0.9, r, 1e-9)
    }
}
