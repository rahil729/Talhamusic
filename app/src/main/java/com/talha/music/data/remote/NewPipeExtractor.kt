package com.talha.music.data.remote

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonParser
import com.talha.music.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import com.talha.music.data.model.Song
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeExtractor @Inject constructor(
    private val httpClient: OkHttpClient
) {
    @Volatile
    private var initialized = false

    suspend fun getAudioUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            getBackendAudioUrl(videoId) ?: runCatching {
                initialize()
                val extractor: StreamExtractor = NewPipe.getService("YouTube")
                    .getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                extractor.fetchPage()
                extractor.getAudioStreams()
                    .asSequence()
                    .filter { it.getContent().isNotBlank() }
                    .maxWithOrNull(
                        compareBy(
                            { it.getAverageBitrate() },
                            { it.getContent().length }
                        )
                    )
                    ?.getContent()
            }.onFailure {
                Log.e("NewPipeExtractor", "YouTube extraction failed for $videoId", it)
            }.getOrNull()
        }
    }

    private fun getBackendAudioUrl(videoId: String): String? {
        if (BuildConfig.STREAM_BACKEND_URL.isBlank()) return null
        return runCatching {
            val request = OkHttpRequest.Builder()
                .url("${BuildConfig.STREAM_BACKEND_URL.trimEnd('/')}/stream/$videoId")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                JsonParser.parseString(response.body?.string().orEmpty())
                    .asJsonObject
                    .getAsJsonPrimitive("url")
                    ?.asString
            }
        }.onFailure {
            Log.d("NewPipeExtractor", "Local stream backend unavailable", it)
        }.getOrNull()
    }

    suspend fun searchSongs(query: String): List<Song> {
        return withContext(Dispatchers.IO) { runCatching {
            initialize()
            searchYouTubeMusic(query).ifEmpty {
                val shorterQuery = query.trim().split(Regex("\\s+")).take(2).joinToString(" ")
                searchYouTubeMusic(shorterQuery).ifEmpty {
                val extractor = NewPipe.getService("YouTube").getSearchExtractor(query)
                extractor.fetchPage()
                extractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .mapNotNull { item ->
                        val videoId = extractVideoId(item.url)
                        if (videoId.length != 11) return@mapNotNull null
                        Song(
                            id = videoId,
                            title = item.name,
                            artist = item.uploaderName,
                            album = null,
                            durationText = item.duration.takeIf { it > 0 }?.let { seconds ->
                                "%d:%02d".format(seconds / 60, seconds % 60)
                            },
                            thumbnailUrl = item.thumbnails.firstOrNull()?.url
                        )
                    }
                    }
            }
        }.onFailure {
            Log.e("NewPipeExtractor", "YouTube search failed for $query", it)
        }.getOrDefault(emptyList()) }
    }

    private fun searchYouTubeMusic(query: String): List<Song> {
        val requestBody = """
            {
              "context": {
                "client": {
                  "clientName": "WEB_REMIX",
                  "clientVersion": "1.20260910.01.00",
                  "hl": "en",
                  "gl": "US"
                }
              },
              "query": ${JsonPrimitive(query)}
            }
        """.trimIndent().toRequestBody("application/json".toMediaType())
        val request = OkHttpRequest.Builder()
            .url("https://music.youtube.com/youtubei/v1/search?key=$YOUTUBE_INNERTUBE_KEY")
            .post(requestBody)
            .header("Origin", "https://music.youtube.com")
            .header("Referer", "https://music.youtube.com/")
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val root = JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
            val items = root.findObjects("musicResponsiveListItemRenderer")
            val songs = items
                .mapNotNull { it.toSong() }
                .distinctBy(Song::id)
            Log.d("NewPipeExtractor", "InnerTube result items=${items.size}, songs=${songs.size}")
            songs
        }
    }

    private fun JsonObject.toSong(): Song? {
        val videoId = findString("videoId")?.takeIf { it.length == 11 } ?: return null
        val textValues = getAsJsonArray("flexColumns")
            ?.flatMap { column ->
                column.asJsonObject.findObjects("runs").flatMap { runs ->
                    runs.getAsJsonArray("runs").mapNotNull { run ->
                        run.asJsonObject.getAsJsonPrimitive("text")?.asString
                    }
                }
            }
            ?.filter { it.isNotBlank() && it != " • " }
            .orEmpty()
        return Song(
            id = videoId,
            title = textValues.getOrNull(0) ?: return null,
            artist = textValues.getOrNull(2) ?: textValues.getOrNull(1) ?: "YouTube Music",
            album = textValues.getOrNull(3),
            durationText = null,
            thumbnailUrl = findThumbnailUrl()
        )
    }

    private fun JsonObject.findThumbnailUrl(): String? {
        return findObjects("thumbnails")
            .flatMap { thumbnails ->
                thumbnails.getAsJsonArray("thumbnails").mapNotNull { item ->
                    item.asJsonObject.getAsJsonPrimitive("url")?.asString
                }
            }
            .lastOrNull()
    }

    private fun JsonObject.findString(name: String): String? {
        if (has(name) && get(name).isJsonPrimitive && get(name).asJsonPrimitive.isString) {
            return get(name).asString
        }
        for ((_, value) in entrySet()) {
            if (value.isJsonObject) value.asJsonObject.findString(name)?.let { return it }
            if (value.isJsonArray) {
                value.asJsonArray.firstNotNullOfOrNull { item ->
                    if (item.isJsonObject) item.asJsonObject.findString(name) else null
                }?.let { return it }
            }
        }
        return null
    }

    private fun JsonObject.findObjects(name: String): List<JsonObject> {
        val matches = mutableListOf<JsonObject>()
        if (has(name) && get(name).isJsonArray) matches += this
        if (has(name) && get(name).isJsonObject) matches += getAsJsonObject(name)
        for ((_, value) in entrySet()) {
            if (value.isJsonObject) matches += value.asJsonObject.findObjects(name)
            if (value.isJsonArray) {
                value.asJsonArray.forEach { item ->
                    if (item.isJsonObject) matches += item.asJsonObject.findObjects(name)
                }
            }
        }
        return matches
    }

    private fun extractVideoId(url: String): String {
        return Regex("(?:[?&]v=|youtu\\.be/|/shorts/)([A-Za-z0-9_-]{11})")
            .find(url)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
    }

    private fun initialize() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloader(httpClient))
                initialized = true
            }
        }
    }

    private class NewPipeDownloader(
        private val client: OkHttpClient
    ) : Downloader() {
        @Throws(IOException::class, ReCaptchaException::class)
        override fun execute(request: Request): Response {
            val builder = OkHttpRequest.Builder().url(request.url())
            request.headers().forEach { (name, values) ->
                values.forEach { value -> builder.addHeader(name, value) }
            }
            request.dataToSend()?.let { body ->
                builder.method(request.httpMethod(), body.toRequestBody(null))
            }
            client.newCall(builder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                return Response(
                    response.code,
                    response.message,
                    response.headers.toMultimap(),
                    responseBody,
                    response.request.url.toString()
                )
            }
        }
    }

    private companion object {
        const val YOUTUBE_INNERTUBE_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    }
}
