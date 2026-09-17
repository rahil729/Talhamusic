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
    private val youtubeApi: YouTubeApi,
    private val audiusApi: AudiusApi,
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
        val audiusResults = runCatching { searchAudius(query) }
            .onFailure { Log.w("InnerTubeService", "Audius search failed", it) }
            .getOrDefault(emptyList())
        if (audiusResults.isNotEmpty()) return audiusResults

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
        Log.d("InnerTubeService", "Fetching stream URL for: $songId")

        if (songId.startsWith(AUDIUS_ID_PREFIX)) {
            return "${AudiusApi.BASE_URL}v1/tracks/${songId.removePrefix(AUDIUS_ID_PREFIX)}/stream?app_name=${AudiusApi.APP_NAME}"
        }
        
        // 1. Try NewPipeExtractor
        val newPipeUrl: String? = newPipeExtractor.getAudioUrl(songId)
        if (newPipeUrl != null) {
            Log.d("InnerTubeService", "Stream URL resolved via NewPipeExtractor")
            return newPipeUrl
        }
        
        // 2. Try Piped fallbacks
        val pipedUrl = runCatching {
            fetchStreamUrlViaFallback(songId)
        }.onFailure {
            Log.w("InnerTubeService", "Piped fallback failed for: $songId", it)
        }.getOrNull()
        
        if (pipedUrl != null) {
            Log.d("InnerTubeService", "Stream URL resolved via Piped fallback")
            return pipedUrl
        }

        // 3. Try direct YouTube for legacy YouTube IDs.
        val youtubeUrl = runCatching {
            fetchStreamUrlViaYouTube(songId)
        }.onFailure {
            Log.e("InnerTubeService", "Direct YouTube fallback failed for: $songId", it)
        }.getOrNull()
        
        if (youtubeUrl != null) {
            Log.d("InnerTubeService", "Stream URL resolved via direct YouTube")
            return youtubeUrl
        }

        Log.e("InnerTubeService", "All sources failed to resolve stream URL for: $songId")
        return null
    }

    private suspend fun fetchStreamUrlViaYouTube(songId: String): String? {
        val request = YouTubePlayerRequest(
            context = YouTubeContext(
                client = YouTubeClient(
                    clientName = "WEB_REMIX",
                    clientVersion = "1.20240910.01.00",
                    visitorData = "Cgtud1BfbG5sandFRSjh", // Example visitor data, ideally should be dynamic
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
            ),
            videoId = songId,
        )
        val response = youtubeApi.getPlayer(request)
        val formats = response.streamingData?.adaptiveFormats
        
        if (formats.isNullOrEmpty()) {
            Log.w("InnerTubeService", "YouTube player response has no streaming data or formats for: $songId")
            return null
        }
        
        return formats
            .filter { it.mimeType.contains("audio") }
            .maxByOrNull { it.bitrate }
            ?.url
    }

    private suspend fun searchAudius(query: String): List<Song> {
        return audiusApi.searchTracks(query).data.mapNotNull { track ->
            if (track.id.isBlank() || track.title.isBlank()) return@mapNotNull null
            Song(
                id = AUDIUS_ID_PREFIX + track.id,
                title = track.title,
                artist = track.user?.name ?: "Audius artist",
                album = null,
                durationText = track.duration.takeIf { it > 0 }?.let(::formatDuration),
                thumbnailUrl = track.artwork?.`480x480` ?: track.artwork?.`150x150`
            )
        }
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
        private const val AUDIUS_ID_PREFIX = "audius_"
        private val pipedHostCandidates = listOf(
            "https://pipedapi.kavin.rocks/",
            "https://api.piped.privacydev.net/",
            "https://piped-api.lunar.icu/",
            "https://pipedapi.tokhmi.xyz/",
            "https://api.piped.projectsegfau.lt/",
            "https://pipedapi.colivier.nl/",
            "https://api-piped.mha.fi/"
        )
    }
}
