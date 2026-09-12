package com.talha.music.data.repository

import com.talha.music.data.local.SongDao
import com.talha.music.data.local.PlaybackHistoryDao
import com.talha.music.data.local.PlaylistDao
import com.talha.music.data.local.SearchHistoryDao
import com.talha.music.data.local.PlaylistSongCrossRef
import com.talha.music.data.model.PlaybackHistory
import com.talha.music.data.model.Playlist
import com.talha.music.data.model.Song
import com.talha.music.data.model.SearchHistory
import com.talha.music.data.remote.InnerTubeService
import com.talha.music.data.remote.LyricsApi
import com.talha.music.playback.SongDownloadManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val playbackHistoryDao: PlaybackHistoryDao,
    private val playlistDao: PlaylistDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val innerTubeService: InnerTubeService,
    private val lyricsApi: LyricsApi,
    private val songDownloadManager: SongDownloadManager
) : MusicRepository {

    override fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    override fun getFavoriteSongs(): Flow<List<Song>> = songDao.getFavoriteSongs()

    override fun getRecentlyPlayed(): Flow<List<Song>> = playbackHistoryDao.getRecent(25)
        .let { flow -> kotlinx.coroutines.flow.flow { flow.collect { entries -> emit(entries.map(PlaybackHistory::toSong)) } } }

    override fun getTopPlayed(): Flow<List<Song>> = songDao.getTopPlayed()

    override fun getPlaylists(): Flow<List<Playlist>> = playlistDao.getPlaylists()

    override suspend fun getPlaylistSongs(playlistId: Long): List<Song> =
        playlistDao.getSongs(playlistId)

    override suspend fun searchSongs(query: String): List<Song> {
        return searchWithFilter(query, "music_songs")
    }

    override suspend fun searchArtists(query: String): List<Song> {
        return searchWithFilter(query, "music_artists")
    }

    override suspend fun searchAlbums(query: String): List<Song> {
        return searchWithFilter(query, "music_albums")
    }

    override suspend fun searchPlaylists(query: String): List<InnerTubeService.CollectionResult> =
        innerTubeService.searchPlaylists(query)

    private suspend fun searchWithFilter(query: String, filter: String): List<Song> {
        val remoteSongs = innerTubeService.search(query, filter)
        if (remoteSongs.isEmpty()) return emptyList()

        val favoriteIds = songDao.getSongs(remoteSongs.map(Song::id))
            .filter(Song::isFavorite)
            .map(Song::id)
            .toSet()
        return remoteSongs.map { song -> song.copy(isFavorite = song.id in favoriteIds) }
    }

    override suspend fun resolveYouTubeUrl(url: String): Song? {
        val videoId = extractVideoId(url) ?: return null
        val response = innerTubeService.getStreamDetails(videoId) ?: return null
        return Song(
            id = videoId,
            title = response.title ?: "YouTube video",
            artist = response.uploader ?: "YouTube",
            album = null,
            durationText = response.duration?.let(::formatDuration),
            thumbnailUrl = response.thumbnailUrl,
            isFavorite = songDao.getSong(videoId)?.isFavorite == true
        )
    }

    override suspend fun toggleFavorite(song: Song) {
        val isFavorite = songDao.getSong(song.id)?.isFavorite ?: song.isFavorite
        val updatedSong = song.copy(isFavorite = !isFavorite)
        songDao.insertSong(updatedSong)
    }

    override suspend fun recordPlay(song: Song) {
        songDao.insertSong(song)
        songDao.incrementPlayCount(song.id)
        playbackHistoryDao.insert(
            PlaybackHistory(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationText = song.durationText,
                thumbnailUrl = song.thumbnailUrl,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.createPlaylist(Playlist(name = name.trim()))
    }

    override suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, song: Song, position: Int) {
        songDao.insertSong(song)
        playlistDao.addSong(PlaylistSongCrossRef(playlistId, song.id, position))
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistDao.removeSong(playlistId, songId)
    }

    override suspend fun getStreamUrl(songId: String): String? {
        return innerTubeService.getStreamUrl(songId)
    }

    override suspend fun getPlaybackUri(songId: String): String? {
        return songDownloadManager.localUri(songId) ?: getStreamUrl(songId)
    }

    override suspend fun downloadSong(song: Song): Boolean {
        songDao.insertSong(song)
        val streamUrl = getStreamUrl(song.id) ?: return false
        return songDownloadManager.download(song, streamUrl)
    }

    override suspend fun getDownloadedSongs(): List<Song> {
        val downloadedIds = songDownloadManager.downloadedIds()
        return songDao.getAllSongs().first().filter { it.id in downloadedIds }
    }

    override suspend fun deleteDownload(songId: String) {
        songDownloadManager.delete(songId)
    }

    override fun getSearchHistory(): Flow<List<SearchHistory>> = searchHistoryDao.getRecent()

    override suspend fun saveSearch(query: String) {
        query.trim().takeIf { it.isNotBlank() }?.let { searchHistoryDao.insert(SearchHistory(it)) }
    }

    override suspend fun deleteSearch(query: String) = searchHistoryDao.delete(query)

    override suspend fun clearSearchHistory() = searchHistoryDao.clear()

    override suspend fun clearPlaybackHistory() = playbackHistoryDao.clear()

    override suspend fun resetTopPlayed() = songDao.resetPlayCounts()

    override suspend fun getLyrics(title: String, artist: String): String? {
        return try {
            lyricsApi.getLyrics(title, artist).plainLyrics?.takeIf { it.isNotBlank() }
        } catch (exception: Exception) {
            null
        }
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?[^#]*v=|youtu\\.be/|youtube\\.com/shorts/)([A-Za-z0-9_-]{11})"),
            Regex("(?:youtube\\.com/embed/)([A-Za-z0-9_-]{11})")
        )
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(url)?.groupValues?.getOrNull(1)
        }
    }

    private fun formatDuration(seconds: Int): String {
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}
