package com.spreva.core.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a learner recording session (Phase 4.3 shadowing starter).
 * Only the derived amplitude envelope and duration are kept — never raw
 * audio long-term (privacy-by-design, plan sections 101-102, 108).
 */
data class VoiceRecording(
    /** Temporary file with the recording; deleted after comparison. */
    val file: File,
    /** Amplitude envelope normalized to 0..1, one value per sample window. */
    val envelope: List<Float>,
    val durationMs: Long,
)

/**
 * Outcome of attempting to start a recording (audit §11): MediaRecorder
 * prepare()/start() fail on real devices (mic already in use, encoder
 * errors) and a device-specific exception must never crash the lesson.
 */
sealed interface RecordingStartResult {
    data object Started : RecordingStartResult
    data class Failed(val reason: String? = null) : RecordingStartResult
}

/**
 * Records the learner's voice for repeat-after-me practice. Raw files are
 * temporary by design; callers delete them after deriving the envelope.
 */
interface VoiceRecorder {
    fun start(outputFile: File): RecordingStartResult
    fun stop(): VoiceRecording?
    fun cancel()
}

@Singleton
class MediaRecorderVoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0

    override fun start(outputFile: File): RecordingStartResult {
        cancel()
        this.outputFile = outputFile
        outputFile.parentFile?.mkdirs()
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioSamplingRate(22_050)
            rec.setAudioEncodingBitRate(64_000)
            rec.setAudioChannels(1)
            rec.setOutputFile(outputFile.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            startedAtMs = System.currentTimeMillis()
            RecordingStartResult.Started
        } catch (t: Exception) {
            runCatching { rec.release() }
            outputFile.delete()
            this.outputFile = null
            RecordingStartResult.Failed(t.message)
        }
    }

    override fun stop(): VoiceRecording? {
        val rec = recorder ?: return null
        val file = outputFile ?: return null
        return try {
            rec.stop()
            val envelope = AudioEnvelope.fromFile(file)
            VoiceRecording(
                file = file,
                envelope = envelope,
                durationMs = System.currentTimeMillis() - startedAtMs,
            )
        } catch (_: Exception) {
            // stop() throws when the recording was too short — treat as cancel.
            file.delete()
            null
        } finally {
            rec.release()
            recorder = null
            outputFile = null
        }
    }

    override fun cancel() {
        recorder?.let { rec ->
            runCatching { rec.stop() }
            rec.release()
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
