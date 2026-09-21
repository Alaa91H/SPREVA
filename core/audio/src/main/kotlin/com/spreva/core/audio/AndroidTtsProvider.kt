package com.spreva.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Availability of a TTS engine for German learning content. */
enum class TtsStatus { NOT_READY, READY, MISSING_GERMAN }

/**
 * Android TextToSpeech provider (Phase 4.1 built-in TTS source for
 * hearing German words in the lesson). Offline on every device that
 * ships a German voice; no network, no external keys.
 *
 * Not a `fun interface` with composable members — a plain injectable
 * singleton consumed from ViewModels (see ADR-0003 on JVM/compose
 * signature boundaries).
 */
interface TtsProvider {
    /** Emits the current engine availability; consumers gate the UI on it. */
    val status: StateFlow<TtsStatus>

    /** Speaks [text]; if [article] is present it is spoken first (die/das/der). */
    fun speakGerman(article: String?, text: String, rate: Float = 1f)

    fun stop()

    fun shutdown()
}

@Singleton
class AndroidTtsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : TtsProvider {

    private val _status = MutableStateFlow(TtsStatus.NOT_READY)
    override val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private var engine: TextToSpeech? = null
    private var germanReady = false

    init {
        engine = TextToSpeech(context) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val current = engine ?: return@TextToSpeech
                val german = Locale("de", "DE")
                val langResult = current.setLanguage(german)
                germanReady = when (langResult) {
                    TextToSpeech.LANG_MISSING_DATA,
                    TextToSpeech.LANG_NOT_SUPPORTED,
                    -> false

                    else -> true
                }
                // de-DE is the learning voice; app UI language stays untouched.
                _status.value = if (germanReady) TtsStatus.READY else TtsStatus.MISSING_GERMAN
            } else {
                _status.value = TtsStatus.NOT_READY
            }
        }
    }

    override fun speakGerman(article: String?, text: String, rate: Float) {
        if (_status.value != TtsStatus.READY) return
        val utterance = SpeechText.forVocabulary(article, text)
        if (utterance.isBlank()) return
        engine?.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
        engine?.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, "spreva-${System.nanoTime()}")
    }

    override fun stop() {
        engine?.stop()
    }

    override fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        germanReady = false
        _status.value = TtsStatus.NOT_READY
    }
}
