package com.spreva.core.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Playback for bundled native-speaker course audio (Phase 4.2). Sits next
 * to [TtsProvider] in `core:audio`; features stay engine-free. Content
 * audio paths are content-package-relative ("audio/…"), resolved by
 * [CourseAudioLocator] against app assets — later replaced by downloaded
 * package files (plan sections 83-85) without touching this interface.
 */
interface CourseAudioPlayer {
    /** Plays the bundled recording at [contentPath]; falls back to [TtsProvider] if missing. */
    fun play(contentPath: String)

    /** Plays [contentPath] when bundled audio exists, otherwise speaks via TTS. */
    fun playOrSpeak(contentPath: String?, fallbackText: String, article: String? = null)

    /** Plays an arbitrary local [file] (e.g. the learner's recording). */
    fun playFile(file: java.io.File)

    fun stop()
}

/**
 * Resolves content-package-relative audio paths into playable URI strings.
 * Kept pure (no android.net.Uri) for JVM unit testing; the app injects an
 * asset-aware opener. Media3 accepts URI strings directly.
 */
class CourseAudioLocator(
    private val opener: (String) -> String?,
) {
    fun resolve(contentPath: String?): String? =
        contentPath?.takeIf { it.isNotBlank() }?.let(opener)?.takeIf { it.isNotBlank() }
}

@Singleton
class ExoPlayerCourseAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsProvider: TtsProvider,
    private val locator: CourseAudioLocator,
) : CourseAudioPlayer {

    private var player: ExoPlayer? = null

    private fun obtain(): ExoPlayer =
        player ?: ExoPlayer.Builder(context).build().also { created ->
            player = created
        }

    override fun play(contentPath: String) {
        val uri = locator.resolve(contentPath) ?: return
        val exo = obtain()
        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun playOrSpeak(contentPath: String?, fallbackText: String, article: String?) {
        val uri = locator.resolve(contentPath)
        if (uri != null) {
            play(contentPath ?: return)
        } else {
            // Native recording missing → built-in TTS fallback (plan section 65:
            // offline-first with graceful degradation).
            ttsProvider.speakGerman(article = article, text = fallbackText)
        }
    }

    override fun playFile(file: java.io.File) {
        if (!file.exists()) return
        val exo = obtain()
        exo.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
        exo.prepare()
        exo.playWhenReady = true
    }

    override fun stop() {
        player?.stop()
    }
}
