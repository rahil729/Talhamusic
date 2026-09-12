package com.talha.music.data.repository

import com.talha.music.data.model.Song
import com.talha.music.data.model.Playlist
import com.talha.music.data.model.SearchHistory
import com.talha.music.data.remote.InnerTubeService
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getAllSongs(): Flow<List<Song>>
    fun getFavoriteSongs(): Flow<List<Song>>
    fun getRecentlyPlayed(): Flow<List<Song>>
    fun getTopPlayed(): Flow<List<Song>>
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistSongs(playlistId: Long): List<Song>
    suspend fun searchSongs(query: String): List<Song>
    suspend fun searchArtists(query: String): List<Song>
    suspend fun searchAlbums(query: String): List<Song>
    suspend fun searchPlaylists(query: String): List<InnerTubeService.CollectionResult>
    suspend fun resolveYouTubeUrl(url: String): Song?
    suspend fun getLyrics(title: String, artist: String): String?
    suspend fun toggleFavorite(song: Song)
    suspend fun recordPlay(song: Song)
    suspend fun createPlaylist(name: String): Long
    suspend fun deletePlaylist(playlist: Playlist)
    suspend fun addSongToPlaylist(playlistId: Long, song: Song, position: Int)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)
    suspend fun getStreamUrl(songId: String): String?
    suspend fun getPlaybackUri(songId: String): String?
    suspend fun downloadSong(song: Song): Boolean
    suspend fun getDownloadedSongs(): List<Song>
    suspend fun deleteDownload(songId: String)
    fun getSearchHistory(): Flow<List<SearchHistory>>
    suspend fun saveSearch(query: String)
    suspend fun deleteSearch(query: String)
    suspend fun clearSearchHistory()
    suspend fun clearPlaybackHistory()
    suspend fun resetTopPlayed()
}
