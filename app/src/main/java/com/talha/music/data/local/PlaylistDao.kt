package com.talha.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.talha.music.data.model.Playlist
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val position: Int
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getPlaylists(): Flow<List<Playlist>>

    @Insert
    suspend fun createPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSong(song: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSong(playlistId: Long, songId: String)

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    suspend fun getSongIds(playlistId: Long): List<String>

    @Query(
        "SELECT songs.* FROM songs " +
            "INNER JOIN playlist_songs ON songs.id = playlist_songs.songId " +
            "WHERE playlist_songs.playlistId = :playlistId " +
            "ORDER BY playlist_songs.position"
    )
    suspend fun getSongs(playlistId: Long): List<com.talha.music.data.model.Song>
}
