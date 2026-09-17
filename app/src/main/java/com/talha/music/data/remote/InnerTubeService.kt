package com.talha.music.data.remote

import android.util.Log
import com.talha.music.data.model.Song
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A service to interact with YouTube Music via Piped API.
 */
@Singleton
class InnerTubeService @Inject constructor(
    private val pipedApi: PipedApi,
    private val newPipeExtractor: NewPipeExtractor,
    private val okHttpClient: OkHttpClient
) {
    data class CollectionResult(
        val id: String,
        val title: String,
        val subtitle: String,
        val thumbnailUrl: String?
    )
    
    suspend fun search(query: String, filter: String = "music_songs"): List<Song> {
        if (filter == "music_songs") {
            val extractorResults = newPipeExtractor.searchSongs(query)
            if (extractorResults.isNotEmpty()) return extractorResults
        }

        return runCatching {
            searchViaFallback(query, filter)
        }.onFailure {
            Log.e("InnerTubeService", "All search sources failed for query: $query", it)
        }.getOrDefault(emptyList())
    }

    suspend fun searchPlaylists(query: String): List<CollectionResult> {
        return runCatching {
            val items = searchViaFallback(query, "music_playlists", allowExtractor = false)
            items.map { song ->
                CollectionResult(song.id.ifBlank { song.title }, song.title, song.artist, song.thumbnailUrl)
            }.distinctBy(CollectionResult::id)
        }.onFailure {
            Log.e("InnerTubeService", "Playlist search failed for query: $query", it)
        }.getOrDefault(emptyList())
    }

    suspend fun getStreamUrl(songId: String): String? {
        return newPipeExtractor.getAudioUrl(songId)
            ?: runCatching {
                fetchStreamUrlViaFallback(songId)
            }.onFailure {
                Log.e("InnerTubeService", "Failed to get fallback stream URL for: $songId", it)
            }.getOrNull()
    }

    suspend fun getStreamDetails(songId: String): PipedStreamResponse? {
        return try {
            val response = fetchStreamDetailsViaFallback(songId)
            if (response == null) {
                Log.e("InnerTubeService", "Failed to resolve video: $songId")
            }
            response
        } catch (e: Exception) {
            Log.e("InnerTubeService", "Failed to resolve video: $songId", e)
            null
        }
    }

    private suspend fun searchViaFallback(query: String, filter: String, allowExtractor: Boolean = true): List<Song> {
        val lastError = mutableListOf<Throwable>()
        for (host in pipedHostCandidates) {
            try {
                val api = buildApi(host)
                val items = api.search(query, filter).items
                return items.map { item ->
                    Song(
                        id = item.videoId,
                        title = item.title,
                        artist = item.uploaderName,
                        album = null,
                        durationText = formatDuration(item.duration),
                        thumbnailUrl = item.thumbnail
                    )
                }
            } catch (exception: Exception) {
                lastError += exception
            }
        }
        if (allowExtractor && filter == "music_songs") {
            val extractorResults = newPipeExtractor.searchSongs(query)
            if (extractorResults.isNotEmpty()) return extractorResults
        }
        throw lastError.lastOrNull() ?: RuntimeException("No Piped host available")
    }

    private suspend fun fetchStreamUrlViaFallback(songId: String): String? {
        val lastError = mutableListOf<Throwable>()
        for (host in pipedHostCandidates) {
            try {
                val api = buildApi(host)
                return api.getStreams(songId).audioStreams
                    .maxByOrNull { it.quality.toQualityScore() }
                    ?.url
            } catch (exception: Exception) {
                lastError += exception
            }
        }
        throw lastError.lastOrNull() ?: RuntimeException("No stream host available")
    }

    private suspend fun fetchStreamDetailsViaFallback(songId: String): PipedStreamResponse? {
        val lastError = mutableListOf<Throwable>()
        for (host in pipedHostCandidates) {
            try {
                val api = buildApi(host)
                return api.getStreams(songId)
            } catch (exception: Exception) {
                lastError += exception
            }
        }
        if (lastError.isNotEmpty()) {
            throw lastError.last()
        }
        return null
    }

    private fun buildApi(baseUrl: String): PipedApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    private fun String.toQualityScore(): Int {
        return Regex("(\\d+)").find(this)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    companion object {
        private val pipedHostCandidates = listOf(
            "https://piped.video/",
            "https://pipedapi.kavin.rocks/",
            "https://piped-api.lunar.icu/",
            "https://pipedapi-libre.kavin.rocks/"
        )
    }
}
