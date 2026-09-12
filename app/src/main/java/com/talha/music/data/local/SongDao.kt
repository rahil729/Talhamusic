package com.talha.music.data.local

import androidx.room.*
import com.talha.music.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY playCount DESC LIMIT 50")
    fun getTopPlayed(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSong(songId: String): Song?

    @Query("SELECT * FROM songs WHERE id IN (:songIds)")
    suspend fun getSongs(songIds: List<String>): List<Song>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)

    @Query("UPDATE songs SET playCount = 0")
    suspend fun resetPlayCounts()

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun updateFavoriteStatus(songId: String, isFavorite: Boolean)
}
