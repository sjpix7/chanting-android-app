package com.sjpixels.mantramala.voice

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.sjpixels.mantramala.data.VoiceSample
import com.sjpixels.mantramala.data.VoiceSampleDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Result of importing an audio file chosen by the user. */
sealed interface ImportResult {
    data class Added(val sample: VoiceSample) : ImportResult
    data object TooLarge : ImportResult
    data object Unplayable : ImportResult
}

/**
 * Voice samples on disk (filesDir/voice/, private to the app, never uploaded anywhere)
 * plus their rows in the database.
 */
class VoiceStore(context: Context, private val dao: VoiceSampleDao) {
    private val appContext = context.applicationContext
    private val dir: File get() = File(appContext.filesDir, DIR).apply { mkdirs() }

    fun file(sample: VoiceSample): File = File(dir, sample.fileName)

    fun newRecordingFile(): File = File(dir, "rec_${System.currentTimeMillis()}.m4a")

    /** Saves a finished recording. Returns null (and deletes the file) if it holds no playable audio. */
    suspend fun addRecording(mantraKey: String, name: String, file: File): VoiceSample? = withContext(Dispatchers.IO) {
        val duration = durationOf(file)
        if (duration < MIN_DURATION_MS) {
            file.delete()
            return@withContext null
        }
        val sample = VoiceSample(mantraKey = mantraKey, name = name, fileName = file.name, durationMs = duration, recorded = true)
        sample.copy(id = dao.insert(sample))
    }

    /** Copies an audio file the user picked into app storage. */
    suspend fun import(mantraKey: String, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val resolver = appContext.contentResolver
        var displayName: String? = null
        var size = -1L
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    displayName = c.getString(0)
                    if (!c.isNull(1)) size = c.getLong(1)
                }
            }
        }
        if (size > MAX_IMPORT_BYTES) return@withContext ImportResult.TooLarge

        val ext = resolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: displayName?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() && it.length <= 5 }
            ?: "audio"
        val target = File(dir, "imp_${System.currentTimeMillis()}.$ext")
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_IMPORT_BYTES) return@use false
                        output.write(buf, 0, n)
                    }
                    true
                }
            } ?: false
        }.getOrDefault(false)
        if (!copied) {
            val tooBig = target.length() > MAX_IMPORT_BYTES
            target.delete()
            return@withContext if (tooBig) ImportResult.TooLarge else ImportResult.Unplayable
        }

        val duration = durationOf(target)
        if (duration < MIN_DURATION_MS) {
            target.delete()
            return@withContext ImportResult.Unplayable
        }
        val name = displayName?.substringBeforeLast('.')?.trim()?.take(60)?.ifEmpty { null } ?: "Audio"
        val sample = VoiceSample(mantraKey = mantraKey, name = name, fileName = target.name, durationMs = duration, recorded = false)
        ImportResult.Added(sample.copy(id = dao.insert(sample)))
    }

    suspend fun delete(sample: VoiceSample) = withContext(Dispatchers.IO) {
        dao.delete(sample)
        file(sample).delete()
    }

    suspend fun deleteForMantra(mantraKey: String) = withContext(Dispatchers.IO) {
        dao.forMantra(mantraKey).forEach { delete(it) }
    }

    // MediaMetadataRetriever is only AutoCloseable from API 29, so release it by hand (minSdk is 26).
    private fun durationOf(file: File): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { r.release() }
        }
    }

    companion object {
        private const val DIR = "voice"
        const val MAX_IMPORT_BYTES = 50L * 1024 * 1024
        private const val MIN_DURATION_MS = 300L
    }
}
