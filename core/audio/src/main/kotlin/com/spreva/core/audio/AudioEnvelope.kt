package com.spreva.core.audio

import java.io.File

/**
 * Pure amplitude-envelope utilities for the simplified shadowing studio
 * (Phase 4.3). Envelopes are derived locally; no raw audio leaves the
 * device and no ASR/provider scoring is involved yet.
 */
object AudioEnvelope {

    /** Number of buckets the envelope is normalized to. */
    const val SAMPLE_COUNT = 32

    /**
     * Derives a normalized 0..1 amplitude envelope from a PCM WAV file by
     * RMS over equal-length windows. Non-WAV files yield an empty envelope
     * (comparison then degrades to "recorded" feedback only).
     */
    fun fromFile(file: File): List<Float> = runCatching {
        fromPcm16Wav(file.readBytes())
    }.getOrDefault(emptyList())

    /**
     * Envelope from raw little-endian 16-bit PCM bytes (any channel count —
     * adjacent samples are averaged pairwise). Used by the runtime
     * MediaCodec extractor (see [MediaEnvelopeExtractor]).
     */
    fun fromPcm16Bytes(pcm: ByteArray): List<Float> {
        val sampleCount = pcm.size / 2
        val window = sampleCount / SAMPLE_COUNT
        if (window == 0) return emptyList()
        val envelope = FloatArray(SAMPLE_COUNT)
        for (i in 0 until SAMPLE_COUNT) {
            var sum = 0.0
            val startSample = i * window
            for (s in startSample until startSample + window) {
                val j = s * 2
                val sample = ((pcm[j].toInt() and 0xFF) shl 8) or (pcm[j + 1].toInt() and 0xFF)
                val signed = if (sample >= 0x8000) sample - 0x10000 else sample
                sum += (signed / 32768.0) * (signed / 32768.0)
            }
            envelope[i] = kotlin.math.sqrt(sum / window).toFloat()
        }
        val max = envelope.max()
        return if (max > 0f) envelope.map { (it / max).coerceIn(0f, 1f) } else envelope.toList()
    }

    /**
     * Extracts a mono envelope from 16-bit PCM WAV bytes. Resamples by
     * skipping so any input length maps to [SAMPLE_COUNT] buckets.
     */
    fun fromPcm16Wav(bytes: ByteArray): List<Float> {
        if (bytes.size < 44 || bytes.decodeToString(0, 4) != "RIFF") return emptyList()
        val pcm = extractPcm16Mono(bytes) ?: return emptyList()
        // Work in whole samples (2 bytes each) so windows never read out of bounds.
        val sampleCount = pcm.size / 2
        val window = sampleCount / SAMPLE_COUNT
        if (window == 0) return emptyList()
        val envelope = FloatArray(SAMPLE_COUNT)
        for (i in 0 until SAMPLE_COUNT) {
            var sum = 0.0
            val startSample = i * window
            for (s in startSample until startSample + window) {
                val j = s * 2
                val sample = ((pcm[j].toInt() and 0xFF) shl 8) or (pcm[j + 1].toInt() and 0xFF)
                val signed = if (sample >= 0x8000) sample - 0x10000 else sample
                sum += (signed / 32768.0) * (signed / 32768.0)
            }
            envelope[i] = kotlin.math.sqrt(sum / window).toFloat()
        }
        val max = envelope.max()
        return if (max > 0f) envelope.map { (it / max).coerceIn(0f, 1f) } else envelope.toList()
    }

    /** Finds the data chunk and returns mono samples (stereo folded to mono). */
    private fun extractPcm16Mono(bytes: ByteArray): ByteArray? {
        var offset = 12
        var channels = 1
        while (offset + 8 <= bytes.size) {
            val id = bytes.decodeToString(offset, offset + 4)
            val size = readLeInt(bytes, offset + 4)
            if (id == "fmt ") {
                // Channels live at fmt+2 (little-endian 16-bit).
                channels = ((bytes[offset + 10].toInt() and 0xFF) or
                    ((bytes[offset + 11].toInt() and 0xFF) shl 8)).coerceAtLeast(1)
            }
            if (id == "data") {
                val data = bytes.copyOfRange(offset + 8, (offset + 8 + size).coerceAtMost(bytes.size))
                if (channels == 1) return data
                // Stereo: average adjacent sample pairs (frame = 2 samples).
                val out = ByteArray(data.size / 2)
                for (i in out.indices step 2) {
                    val l = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
                    val r = ((data[i + 2].toInt() and 0xFF) shl 8) or (data[i + 3].toInt() and 0xFF)
                    val avg = ((l + r) / 2).toShort()
                    out[i] = ((avg.toInt() shr 8) and 0xFF).toByte()
                    out[i + 1] = (avg.toInt() and 0xFF).toByte()
                }
                return out
            }
            offset += 8 + size + (size % 2)
        }
        return null
    }

    private fun readLeInt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
}

/**
 * Simplified waveform-shape similarity for shadowing feedback (Phase 4.3).
 * Compares normalized amplitude envelopes by mean absolute difference —
 * a rough rhythm/energy cue, NOT pronunciation quality (plan section 94:
 * pronunciation is more than recognition; real assessment lands later).
 *
 * Phase 4.3 honesty note: bundled model recordings are Ogg, and the
 * learner recording is AAC — extracting a comparable PCM envelope from
 * both without a decoder round-trip is not possible, so the comparator
 * ships unit-tested and is activated when both envelopes are available
 * (e.g. WAV/WAV-derived sources). Until then the UI uses self-comparison
 * (play the native model, then your own recording) — shadowing by ear.
 */
object WaveformComparator {

    /**
     * @param model normalized envelope of the native recording
     * @param learner normalized envelope of the learner recording
     * @return similarity 0..1, or null when envelopes are unavailable
     */
    fun similarity(model: List<Float>, learner: List<Float>): Float? {
        if (model.isEmpty() || learner.isEmpty()) return null
        val n = minOf(model.size, learner.size)
        var diff = 0.0
        for (i in 0 until n) diff += kotlin.math.abs(model[i] - learner[i])
        return (1.0 - diff / n).toFloat().coerceIn(0f, 1f)
    }
}
