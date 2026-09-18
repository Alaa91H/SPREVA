package com.spreva.app.data

import android.content.Context
import androidx.room3.Room
import com.spreva.core.audio.AndroidTtsProvider
import com.spreva.core.audio.TtsProvider
import com.spreva.core.common.AppClock
import com.spreva.core.common.IdGenerator
import com.spreva.core.common.SystemClock
import com.spreva.core.common.UuidGenerator
import com.spreva.core.database.SprevaDatabase
import com.spreva.core.datastore.SettingsDataSource
import com.spreva.core.memory.DemoReviewScheduler
import com.spreva.core.memory.ReviewScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/**
 * Core singletons: Room database, DataStore settings, clock/id abstractions
 * and the demo review scheduler (plan sections 63/87/97/98).
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SprevaDatabase =
        Room.databaseBuilder(context, SprevaDatabase::class.java, SprevaDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideSettingsDataSource(@ApplicationContext context: Context): SettingsDataSource =
        SettingsDataSource(context)

    @Provides
    @Singleton
    fun provideClock(): AppClock = SystemClock()

    @Provides
    @Singleton
    fun provideIdGenerator(): IdGenerator = UuidGenerator()

    @Provides
    @Singleton
    fun provideScheduler(json: Json): ReviewScheduler = DemoReviewScheduler(json)

    @Provides
    @Singleton
    fun provideTtsProvider(provider: AndroidTtsProvider): TtsProvider = provider
}
