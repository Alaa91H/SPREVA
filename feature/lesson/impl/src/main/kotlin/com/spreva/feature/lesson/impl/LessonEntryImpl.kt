package com.spreva.feature.lesson.impl

import com.spreva.feature.lesson.api.LessonEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LessonEntryModule {

    @Provides
    @Singleton
    fun provideLessonEntry(): LessonEntry = LessonEntry { lessonId, onFinished ->
        LessonRoute(lessonId = lessonId, onFinished = onFinished)
    }
}
