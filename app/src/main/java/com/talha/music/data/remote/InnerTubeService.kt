package com.talha.music.data.remote

import android.util.Log
import com.talha.music.BuildConfig
import com.talha.music.data.model.Song
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** Online catalog and stream resolution used by the app. */
@Singleton
class InnerTubeService @Inject constructor(
    private val ytDlpApi: YtDlpApi
) {
    data class CollectionResult(
        val id: String,
        val title: String,
        val subtitle: String,
        val thumbnailUrl: String?
    )

    suspend fun search(query: String, filter: String = "music_songs"): List<Song> {
        if (filter != "music_songs") return emptyList()
        return try {
            ytDlpApi.search(query.trim()).items.mapNotNull { track ->
                if (track.videoId.isBlank() || track.title.isBlank()) return@mapNotNull null
                Song(
                    id = track.videoId,
                    title = track.title,
                    artist = track.artist.ifBlank { "YouTube" },
                    album = null,
                    durationText = track.duration?.takeIf { it > 0 }?.let(::formatDuration),
                    thumbnailUrl = track.thumbnailUrl
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Log.e("InnerTubeService", "YouTube search failed for $query", exception)
            throw exception
        }
    }

    suspend fun searchPlaylists(query: String): List<CollectionResult> = emptyList()

    suspend fun getStreamUrl(songId: String): String? {
        if (songId.startsWith(AUDIUS_ID_PREFIX)) {
            return "${AudiusApi.BASE_URL}v1/tracks/${songId.removePrefix(AUDIUS_ID_PREFIX)}/stream?app_name=${AudiusApi.APP_NAME}"
        }
        if (!VIDEO_ID_REGEX.matches(songId)) return null
        return "${BuildConfig.STREAM_BACKEND_URL}stream/$songId/audio"
    }

    private fun formatDuration(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    private companion object {
        const val AUDIUS_ID_PREFIX = "audius_"
        val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")
    }
}
