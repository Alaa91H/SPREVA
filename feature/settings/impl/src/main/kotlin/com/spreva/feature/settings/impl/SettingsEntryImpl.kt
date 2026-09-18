package com.spreva.feature.settings.impl

import com.spreva.feature.settings.api.SettingsEntry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsEntryModule {

    @Provides
    @Singleton
    fun provideSettingsEntry(): SettingsEntry = SettingsEntry {
        SettingsRoute()
    }
}
