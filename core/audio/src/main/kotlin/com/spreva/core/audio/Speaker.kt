package com.spreva.core.audio

/**
 * Language-independent contract for speaking learning content out loud
 * (plan sections 31-34). Phase 4.1 ships the Android TextToSpeech
 * provider; later phases can add bundled-audio playback behind the same
 * contract without touching features.
 *
 * A pure-JVM interface on purpose: the model-facing parameters are plain
 * strings so the contract stays platform-free and unit-testable.
 */
interface Speaker {
    /** Speaks [text] as German learning content; no-op while unavailable. */
    fun speakGerman(text: String)

    /** Releases underlying engine resources. */
    fun shutdown()
}

/**
 * Normalizes arbitrary content strings into TTS-friendly speech text
 * (pure JVM, unit-tested). Strips article decorations from vocabulary
 * cards — German TTS reads "die Frau" fine, but the article is rendered
 * separately in the UI, so the spoken form is the bare noun — and drops
 * inline translation markers like "–" or parenthesized hints.
 */
object SpeechText {

    private val translationMarkers = charArrayOf('–', '—', '-', '(', '（')

    /** Builds the utterance for a vocabulary entry (article + noun). */
    fun forVocabulary(article: String?, german: String): String = buildString {
        article?.takeIf { it.isNotBlank() }?.let { append(it); append(' ') }
        append(clean(german))
    }.trim()

    /** Builds the utterance for a sentence template with [answer] filled in. */
    fun forSentence(template: String, answerPlaceholder: String, answer: String?): String {
        val filled = if (answer.isNullOrBlank()) template else template.replace(answerPlaceholder, answer)
        return clean(filled)
    }

    /** Strips translation markers so TTS never reads hint text. */
    fun clean(text: String): String {
        val cut = text.indexOfAny(translationMarkers)
        val base = if (cut >= 0) text.substring(0, cut) else text
        return base.trim().trimEnd('.', ',', ';', ':', '!', '?')
            .ifBlank { text.trim() }
    }
}
