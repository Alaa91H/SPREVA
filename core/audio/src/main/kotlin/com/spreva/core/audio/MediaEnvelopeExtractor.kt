package com.spreva.core.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Runtime envelope extraction from arbitrary audio files (Ogg Vorbis,
 * AAC/M4A — anything the platform can decode) using [MediaExtractor] +
 * [MediaCodec]. This closes the gap left in the first Phase 4.3 cut, where
 * [AudioEnvelope.fromFile] could only parse PCM WAV: the bundled model
 * recordings are Ogg and learner takes are AAC, so direct comparison was
 * impossible. Decoding both sides to PCM envelopes here makes
 * [WaveformComparator.similarity] usable end-to-end.
 *
 * Extracted to its own object so JVM unit tests can exercise
 * [AudioEnvelope.fromPcm16Bytes] and the comparator without Android
 * framework deps.
 */
object MediaEnvelopeExtractor {

    /**
     * Decodes the first audio track of [file] to a normalized envelope.
     * Returns an empty list when the file has no decodable audio track or
     * decoding fails — callers treat that as "no automatic comparison".
     */
    fun extract(file: File): List<Float> = runCatching {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return emptyList()

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            try {
                val pcm = drain(extractor, codec)
                if (pcm.size < AudioEnvelope.SAMPLE_COUNT * 2) return emptyList()
                AudioEnvelope.fromPcm16Bytes(pcm)
            } finally {
                codec.stop()
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }.getOrDefault(emptyList())

    /** Decodes the whole track, collecting 16-bit PCM bytes. */
    private fun drain(extractor: MediaExtractor, codec: MediaCodec): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false

        // Hard cap on decoded bytes: bounded memory for long takes (a
        // normal short utterance decodes to well under 1 MB).
        val maxBytes = 4 * 1024 * 1024

        while (!sawOutputEos && out.size() < maxBytes) {
            if (!sawInputEos) {
                val inIndex = codec.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawInputEos = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            when (val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit // new format; not needed for envelope
                MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                else -> {
                    if (outIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outIndex)!!
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.get(chunk)
                        out.write(chunk)
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            sawOutputEos = true
                        }
                    }
                }
            }
        }
        return convertToLittleEndianPcm16(out.toByteArray(), codec.outputFormat)
    }

    /**
     * Platform decoders can emit float PCM or big-endian-int PCM depending
     * on format; normalize to little-endian 16-bit signed so
     * [AudioEnvelope.fromPcm16Bytes] can parse uniformly.
     */
    private fun convertToLittleEndianPcm16(raw: ByteArray, format: MediaFormat): ByteArray {
        val keyPcmEncoding = MediaFormat.KEY_PCM_ENCODING
        if (format.containsKey(keyPcmEncoding) &&
            format.getInteger(keyPcmEncoding) == android.media.AudioFormat.ENCODING_PCM_FLOAT
        ) {
            // float32 → int16 (clamp)
            val out = ByteArray(raw.size / 2)
            val floatBuffer = java.nio.ByteBuffer.wrap(raw).order(ByteOrder.nativeOrder()).asFloatBuffer()
            for (i in 0 until floatBuffer.remaining()) {
                val f = min(1f, maxOf(-1f, floatBuffer.get(i)))
                val s = (f * 32767f).toInt()
                out[i * 2] = (s and 0xFF).toByte()
                out[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
            }
            return out
        }
        // int16 already (native order on all Android targets is little-endian)
        return raw
    }
}
