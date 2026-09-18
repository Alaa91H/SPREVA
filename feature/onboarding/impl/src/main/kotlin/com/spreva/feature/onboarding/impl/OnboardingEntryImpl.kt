package com.spreva.feature.onboarding.impl

import com.spreva.feature.onboarding.api.OnboardingEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the onboarding entry impl to the api contract (plan section 7). */
@Module
@InstallIn(SingletonComponent::class)
object OnboardingEntryModule {

    @Provides
    @Singleton
    fun provideOnboardingEntry(): OnboardingEntry = OnboardingEntry { onFinished ->
        OnboardingRoute(onFinished = onFinished)
    }
}
