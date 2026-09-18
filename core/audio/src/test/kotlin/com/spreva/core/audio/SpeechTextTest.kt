package com.spreva.core.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechTextTest {

    @Test
    fun `vocabulary utterance includes article then noun`() {
        val text = SpeechText.forVocabulary("die", "Frau")
        assertEquals("die Frau", text)
    }

    @Test
    fun `vocabulary without article speaks bare noun`() {
        val text = SpeechText.forVocabulary(null, "danke")
        assertEquals("danke", text)
    }

    @Test
    fun `translation dash and hint are stripped`() {
        assertEquals("Guten Morgen", SpeechText.clean("Guten Morgen – صباح الخير"))
        assertEquals("Entschuldigung", SpeechText.clean("Entschuldigung (عفوًا)"))
    }

    @Test
    fun `trailing punctuation is dropped`() {
        assertEquals("Hallo", SpeechText.clean("Hallo!"))
    }

    @Test
    fun `blank after clean falls back to trimmed input`() {
        assertEquals("—", SpeechText.clean("—"))
    }

    @Test
    fun `sentence fills the answer placeholder`() {
        val text = SpeechText.forSentence("Es ist {answer} Uhr.", "{answer}", "acht")
        assertEquals("Es ist acht Uhr", text)
    }

    @Test
    fun `sentence without answer keeps template`() {
        val text = SpeechText.forSentence("Es ist acht Uhr.", "{answer}", null)
        assertEquals("Es ist acht Uhr", text)
    }
}
