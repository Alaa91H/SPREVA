package com.spreva.feature.review.impl

import com.spreva.feature.review.api.PracticeEntry
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
    fun providePracticeEntry(): PracticeEntry = PracticeEntry { onStartReview ->
        PracticeRoute(onStartReview = onStartReview)
    }

    @Provides
    @Singleton
    fun provideReviewEntry(): ReviewEntry = ReviewEntry { onFinished ->
        ReviewSessionRoute(onFinished = onFinished)
    }
}
