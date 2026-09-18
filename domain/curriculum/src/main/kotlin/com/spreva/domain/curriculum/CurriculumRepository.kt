package com.spreva.domain.curriculum

import com.spreva.core.model.Course
import com.spreva.core.model.Lesson
import com.spreva.core.model.LessonId
import kotlinx.coroutines.flow.Flow

/**
 * Attribution record for one bundled media file (CC BY-SA etc.). Surfaced
 * in-app to satisfy license attribution requirements (plan sections 99-100).
 */
data class MediaAttribution(
    val path: String,
    val sourceUrl: String,
    val license: String,
    val licenseUrl: String?,
    val attribution: String,
)

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

    /** Media attribution records; empty when the package ships no registry. */
    suspend fun getMediaAttributions(): List<MediaAttribution>
}
