package com.spreva.domain.learning

import com.spreva.core.model.ActivityAttempt
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonProgress
import kotlinx.coroutines.flow.Flow

/**
 * Progress persistence contract. Room is the single source of truth;
 * the implementation lives in the data layer.
 */
interface LearningRepository {
    fun observeLessonProgress(lessonId: LessonId): Flow<LessonProgress?>
    fun observeAllProgress(): Flow<List<LessonProgress>>
    suspend fun recordAttempt(attempt: ActivityAttempt)
    suspend fun completeLesson(lessonId: LessonId, totalActivities: Int)
    suspend fun ensureStarted(lessonId: LessonId, totalActivities: Int)
    suspend fun clearAll()
}
