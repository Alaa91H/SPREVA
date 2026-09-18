package com.spreva.core.audio

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEnvelopeTest {

    /** Builds a minimal mono 16-bit PCM WAV with the given half-wave amplitudes. */
    private fun wav(vararg samples: Short): ByteArray {
        val out = ByteArrayOutputStream()
        val data = ByteArrayOutputStream()
        samples.forEach { s ->
            data.write(s.toInt() and 0xFF)
            data.write((s.toInt() shr 8) and 0xFF)
        }
        val pcm = data.toByteArray()
        out.write("RIFF".toByteArray())
        writeLe(out, 36 + pcm.size)
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        writeLe(out, 16)
        writeLe16(out, 1)        // PCM (16-bit field!)
        writeLe16(out, 1)        // mono (16-bit field!)
        writeLe(out, 8_000)      // sample rate
        writeLe(out, 16_000)     // byte rate
        writeLe16(out, 2)        // block align (16-bit field!)
        writeLe16(out, 16)       // bits per sample (16-bit field!)
        out.write("data".toByteArray())
        writeLe(out, pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }

    private fun writeLe16(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }

    private fun writeLe(out: ByteArrayOutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
        out.write((v shr 16) and 0xFF)
        out.write((v shr 24) and 0xFF)
    }

    @Test
    fun `envelope has fixed sample count and is normalized`() {
        val samples = ShortArray(8_000) { i ->
            (kotlin.math.sin(i * 0.1) * 20_000).toInt().toShort()
        }
        val envelope = AudioEnvelope.fromPcm16Wav(wav(*samples))
        assertEquals(AudioEnvelope.SAMPLE_COUNT, envelope.size)
        assertEquals(1f, envelope.max(), 0.001f)
        assertTrue(envelope.min() >= 0f)
    }

    @Test
    fun `non-wav bytes yield empty envelope`() {
        assertTrue(AudioEnvelope.fromPcm16Wav("not a wav".toByteArray()).isEmpty())
    }

    @Test
    fun `identical envelopes score 1`() {
        val e = listOf(0.1f, 0.5f, 0.9f, 0.3f)
        assertEquals(1f, WaveformComparator.similarity(e, e)!!, 0.001f)
    }

    @Test
    fun `missing envelopes yield null similarity`() {
        assertNull(WaveformComparator.similarity(emptyList(), listOf(0.5f)))
    }

    @Test
    fun `opposite envelopes score near 0`() {
        val a = List(32) { if (it < 16) 0.9f else 0.1f }
        val b = List(32) { if (it < 16) 0.1f else 0.9f }
        val score = WaveformComparator.similarity(a, b)!!
        assertTrue("score=$score", score < 0.35f)
    }
}
