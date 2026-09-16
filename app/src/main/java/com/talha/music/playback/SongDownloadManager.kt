package com.talha.music.playback

import android.content.Context
import android.net.Uri
import com.talha.music.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {
    private val activeDownloads = ConcurrentHashMap.newKeySet<String>()

    private val downloadDirectory by lazy {
        File(context.filesDir, "downloads_v2").apply { mkdirs() }
    }

    suspend fun download(song: Song, streamUrl: String): Boolean {
        if (!activeDownloads.add(song.id)) return false

        return try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder().url(streamUrl).build()
                val outputFile = fileFor(song.id)
                val temporaryFile = File(outputFile.parentFile, "${outputFile.name}.part")

                repeat(3) { attempt ->
                    val completed = runCatching {
                        temporaryFile.delete()
                        httpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) return@runCatching false
                            val body = response.body ?: return@runCatching false
                            body.byteStream().use { input ->
                                temporaryFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                        if (!temporaryFile.exists() || temporaryFile.length() == 0L) return@runCatching false
                        outputFile.delete()
                        temporaryFile.renameTo(outputFile)
                    }
                        .getOrDefault(false)

                    if (completed && outputFile.exists()) return@withContext true
                    if (attempt < 2) Thread.sleep(500L)
                }

                temporaryFile.delete()
                false
            }
        } finally {
            activeDownloads.remove(song.id)
        }
    }

    fun localUri(songId: String): String? {
        val file = fileFor(songId)
        return file.takeIf(File::exists)?.let { Uri.fromFile(it).toString() }
    }

    fun downloadedIds(): Set<String> {
        return downloadDirectory.listFiles()
            ?.map { it.name.removeSuffix(".audio") }
            ?.toSet()
            .orEmpty()
    }

    fun delete(songId: String) {
        fileFor(songId).delete()
    }

    private fun fileFor(songId: String): File {
        val safeId = songId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(downloadDirectory, "$safeId.audio")
    }
}
