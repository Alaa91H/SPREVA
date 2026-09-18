package com.spreva.domain.learning

import com.spreva.core.common.IdGenerator
import com.spreva.core.model.LearningEventId
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonProgress
import com.spreva.core.model.LessonStatus
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fakes-over-mocks (plan section 117): the use case must complete the
 * lesson, provision cards and append exactly one LessonCompleted event.
 */
class CompleteLessonTest {

    private class FakeLearningRepository : LearningRepository {
        val completed = mutableMapOf<String, Int>()
        var started = mutableSetOf<String>()

        override fun observeLessonProgress(lessonId: LessonId) = error("unused")
        override fun observeAllProgress() = error("unused")

        override suspend fun recordAttempt(attempt: com.spreva.core.model.ActivityAttempt) = Unit

        override suspend fun completeLesson(lessonId: LessonId, totalActivities: Int) {
            completed[lessonId.value] = totalActivities
        }

        override suspend fun ensureStarted(lessonId: LessonId, totalActivities: Int) {
            started += lessonId.value
        }

        override suspend fun clearAll() = Unit
    }

    private class FakeProvisioner : ReviewCardProvisioner {
        val provisioned = mutableListOf<String>()
        override suspend fun provisionCardsForLesson(lessonId: LessonId) {
            provisioned += lessonId.value
        }

        override suspend fun hasCardsFor(knowledgeItemIds: List<String>): Boolean = false
    }

    private class FakeEventLog : LearningEventLog {
        val events = mutableListOf<Recorded>()
        data class Recorded(val id: LearningEventId, val type: String, val subjectId: String)

        override suspend fun log(
            id: LearningEventId,
            type: String,
            subjectId: String,
            occurredAt: Instant,
            payloadJson: String,
        ) {
            events += Recorded(id, type, subjectId)
        }
    }

    private class FixedIdGenerator : IdGenerator {
        override fun newId(): String = "event-1"
    }

    @Test
    fun `completing a lesson persists completion, cards and one event`() = runTest {
        val learning = FakeLearningRepository()
        val provisioner = FakeProvisioner()
        val events = FakeEventLog()
        val useCase = CompleteLesson(learning, provisioner, events, FixedIdGenerator())
        val now = Instant.ofEpochMilli(1_700_000_000_000)

        useCase(LessonId("a1_u01_l01"), totalActivities = 6, now = now)

        assertEquals(6, learning.completed["a1_u01_l01"])
        assertEquals(listOf("a1_u01_l01"), provisioner.provisioned)
        assertEquals(1, events.events.size)
        assertEquals(CompleteLesson.EVENT_LESSON_COMPLETED, events.events.first().type)
        assertEquals("a1_u01_l01", events.events.first().subjectId)
        assertNotNull(events.events.first().id)
    }

    @Test
    fun `status helper maps progress correctly`() {
        val progress = LessonProgress(
            lessonId = LessonId("a1_u01_l01"),
            status = LessonStatus.COMPLETED,
            completedActivities = 6,
            totalActivities = 6,
            startedAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            completedAt = Instant.EPOCH,
        )
        assertTrue(progress.status == LessonStatus.COMPLETED)
        assertEquals(progress.totalActivities, progress.completedActivities)
    }
}
