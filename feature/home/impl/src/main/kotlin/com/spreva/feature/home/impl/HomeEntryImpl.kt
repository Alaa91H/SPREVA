package com.spreva.feature.home.impl

import com.spreva.feature.home.api.HomeEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeEntryModule {

    @Provides
    @Singleton
    fun provideHomeEntry(): HomeEntry = HomeEntry { onOpenLearn, onStartReview ->
        HomeRoute(onOpenLearn = onOpenLearn, onStartReview = onStartReview)
    }
}
