package com.spreva.feature.course.impl

import com.spreva.feature.course.api.CourseEntry
import com.spreva.feature.course.api.LearnEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CourseEntryModule {

    @Provides
    @Singleton
    fun provideLearnEntry(): LearnEntry = LearnEntry { onOpenCourse ->
        LearnRoute(onOpenCourse = onOpenCourse)
    }

    @Provides
    @Singleton
    fun provideCourseEntry(): CourseEntry = CourseEntry { levelId, onOpenLesson ->
        CourseRoute(levelId = levelId, onOpenLesson = onOpenLesson)
    }
}
