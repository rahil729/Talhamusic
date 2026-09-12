package com.talha.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.talha.music.data.model.PlaybackHistory
import com.talha.music.data.model.Playlist
import com.talha.music.data.model.Song
import com.talha.music.data.model.SearchHistory

@Database(
    entities = [Song::class, PlaybackHistory::class, Playlist::class, PlaylistSongCrossRef::class, SearchHistory::class],
    version = 5,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
