package com.sjpixels.mantramala.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records the microphone to an AAC (.m4a) file. The caller must hold RECORD_AUDIO. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    val isRecording: Boolean get() = recorder != null

    /** Returns false if the microphone could not be opened. */
    fun start(target: File): Boolean {
        cancel()
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(128_000)
            r.setMaxDuration(MAX_DURATION_MS)
            r.setOutputFile(target.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = target
            true
        } catch (_: Exception) {
            r.release()
            target.delete()
            false
        }
    }

    /** Peak level since the last call, 0..1, for a simple level meter. */
    fun level(): Float = runCatching { (recorder?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0f).coerceIn(0f, 1f)

    /** Stops and returns the finished file, or null if nothing usable was recorded. */
    fun stop(): File? {
        val r = recorder ?: return null
        val f = file
        recorder = null
        file = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        if (!ok) f?.delete()
        return if (ok) f else null
    }

    fun cancel() {
        val r = recorder ?: return
        val f = file
        recorder = null
        file = null
        runCatching { r.stop() }
        r.release()
        f?.delete()
    }

    companion object {
        const val MAX_DURATION_MS = 10 * 60 * 1000
    }
}
