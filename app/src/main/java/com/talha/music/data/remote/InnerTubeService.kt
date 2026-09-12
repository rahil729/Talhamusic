package com.talha.music.data.remote

import com.talha.music.data.model.Song
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A service to interact with YouTube Music via Piped API.
 */
@Singleton
class InnerTubeService @Inject constructor(
    private val pipedApi: PipedApi,
    private val newPipeExtractor: NewPipeExtractor
) {
    data class CollectionResult(
        val id: String,
        val title: String,
        val subtitle: String,
        val thumbnailUrl: String?
    )
    
    suspend fun search(query: String, filter: String = "music_songs"): List<Song> {
        val extractorResults = newPipeExtractor.searchSongs(query)
        if (extractorResults.isNotEmpty()) return extractorResults

        return runCatching {
            pipedApi.search(query, filter).items.map { item ->
                Song(
                    id = item.videoId,
                    title = item.title,
                    artist = item.uploaderName,
                    album = null,
                    durationText = formatDuration(item.duration),
                    thumbnailUrl = item.thumbnail
                )
            }
        }.onFailure {
            Log.e("InnerTubeService", "All search sources failed for query: $query", it)
        }.getOrDefault(emptyList())
    }

    suspend fun searchPlaylists(query: String): List<CollectionResult> {
        return runCatching {
            pipedApi.search(query, "music_playlists").items.map { item ->
                CollectionResult(item.videoId.ifBlank { item.url }, item.title, item.uploaderName, item.thumbnail)
            }.distinctBy(CollectionResult::id)
        }.onFailure {
            Log.e("InnerTubeService", "Playlist search failed for query: $query", it)
        }.getOrDefault(emptyList())
    }

    suspend fun getStreamUrl(songId: String): String? {
        return newPipeExtractor.getAudioUrl(songId)
            ?: runCatching {
                pipedApi.getStreams(songId).audioStreams
                    .maxByOrNull { it.quality.toQualityScore() }
                    ?.url
            }.onFailure {
                Log.e("InnerTubeService", "Failed to get fallback stream URL for: $songId", it)
            }.getOrNull()
    }

    suspend fun getStreamDetails(songId: String): PipedStreamResponse? {
        return try {
            pipedApi.getStreams(songId)
        } catch (e: Exception) {
            Log.e("InnerTubeService", "Failed to resolve video: $songId", e)
            null
        }
    }

    private fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "%d:%02d".format(minutes, remainingSeconds)
    }

    private fun String.toQualityScore(): Int {
        return Regex("(\\d+)").find(this)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }
}
