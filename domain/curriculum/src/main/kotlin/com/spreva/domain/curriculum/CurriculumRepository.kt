package com.spreva.domain.curriculum

import com.spreva.core.model.Course
import com.spreva.core.model.Lesson
import com.spreva.core.model.LessonId
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing curriculum contract. Implementations combine content
 * sources (bundled now, downloadable later).
 */
interface CurriculumRepository {
    fun observeCourse(): Flow<Course?>
    fun observeLessonSummaries(): Flow<List<com.spreva.core.model.LessonSummary>>
    suspend fun getLesson(id: LessonId): Lesson?
    suspend fun getNextLesson(): Lesson?
    suspend fun contentVersion(): String?
}
