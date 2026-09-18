package com.spreva.app.data

import android.content.Context
import androidx.room3.Room
import com.spreva.core.audio.AndroidTtsProvider
import com.spreva.core.audio.AudioFileResolver
import com.spreva.core.audio.CourseAudioLocator
import com.spreva.core.audio.CourseAudioPlayer
import com.spreva.core.audio.ExoPlayerCourseAudioPlayer
import com.spreva.core.audio.TtsProvider
import com.spreva.core.audio.VoiceRecorder
import com.spreva.core.audio.MediaRecorderVoiceRecorder
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
            .addMigrations(*SprevaDatabase.MIGRATIONS)
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

    @Provides
    @Singleton
    fun provideCourseAudioLocator(@ApplicationContext context: Context): CourseAudioLocator =
        CourseAudioLocator { path ->
            // Bundled package audio lives in app assets (plan sections 83-85);
            // downloaded packages will swap this opener, not the player.
            // "asset:///" URIs are handled by ExoPlayer's DefaultDataSource.
            runCatching {
                context.assets.open("content/$path").use { "asset:///content/$path" }
            }.getOrNull()
        }

    @Provides
    @Singleton
    fun provideVoiceRecorder(recorder: MediaRecorderVoiceRecorder): VoiceRecorder = recorder

    /**
     * Copies a bundled asset recording to a real file so analysis code
     * (Phase 4.3 waveform comparison) can decode it — MediaExtractor needs a
     * file descriptor, not an asset stream. Cached per file; assets are
     * immutable so a stale copy is always content-identical.
     */
    @Provides
    @Singleton
    fun provideAudioFileResolver(@ApplicationContext context: Context): AudioFileResolver =
        AudioFileResolver { path ->
            runCatching {
                val outDir = java.io.File(context.codeCacheDir, "audio-analysis").also { it.mkdirs() }
                val safeName = path.replace('/', '_')
                val outFile = java.io.File(outDir, safeName)
                if (!outFile.exists() || outFile.length() == 0L) {
                    context.assets.open("content/$path").use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                outFile.takeIf { it.length() > 0 }
            }.getOrNull()
        }

    @Provides
    @Singleton
    fun provideCourseAudioPlayer(
        @ApplicationContext context: Context,
        ttsProvider: TtsProvider,
        locator: CourseAudioLocator,
    ): CourseAudioPlayer = ExoPlayerCourseAudioPlayer(
        context = context,
        ttsProvider = ttsProvider,
        locator = locator,
    )
}
