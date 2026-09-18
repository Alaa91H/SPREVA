package com.spreva.domain.curriculum

import com.spreva.core.model.Course
import com.spreva.core.model.Lesson
import com.spreva.core.model.LessonId
import com.spreva.core.model.LessonSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Observes the active demo course. */
class ObserveCourse @javax.inject.Inject constructor(private val repository: CurriculumRepository) {
    operator fun invoke(): Flow<Course?> = repository.observeCourse()
}

/** Observes flat lesson summaries for the Learn tab. */
class ObserveLessonSummaries(private val repository: CurriculumRepository) {
    operator fun invoke(): Flow<List<LessonSummary>> = repository.observeLessonSummaries()
}

/** Loads a full lesson by id. */
class GetLesson @javax.inject.Inject constructor(private val repository: CurriculumRepository) {
    suspend operator fun invoke(id: LessonId): Lesson? = repository.getLesson(id)
}

/** Returns the lesson the learner should continue with. */
class GetNextLesson @javax.inject.Inject constructor(private val repository: CurriculumRepository) {
    suspend operator fun invoke(): Lesson? = repository.getNextLesson()
}
