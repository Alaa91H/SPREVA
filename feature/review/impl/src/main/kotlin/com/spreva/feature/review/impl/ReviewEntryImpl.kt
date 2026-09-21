package com.spreva.feature.review.impl

import com.spreva.feature.review.api.PracticeEntry
import com.spreva.feature.review.api.PlacementEntry
import com.spreva.feature.review.api.ReviewEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReviewEntryModule {

    @Provides
    @Singleton
    fun providePracticeEntry(): PracticeEntry = PracticeEntry { onStartReview, onStartPlacement, onOpenLesson ->
        PracticeRoute(
            onStartReview = onStartReview,
            onStartPlacement = onStartPlacement,
            onOpenLesson = onOpenLesson,
        )
    }

    @Provides
    @Singleton
    fun providePlacementEntry(): PlacementEntry = PlacementEntry { onOpenCourse, onFinished ->
        PlacementRoute(
            onOpenCourse = onOpenCourse,
            onFinished = onFinished,
        )
    }

    @Provides
    @Singleton
    fun provideReviewEntry(): ReviewEntry = ReviewEntry { onFinished ->
        ReviewSessionRoute(onFinished = onFinished)
    }
}
