package com.talha.music.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.talha.music.data.local.MusicDatabase
import com.talha.music.data.local.PlaybackHistoryDao
import com.talha.music.data.local.PlaylistDao
import com.talha.music.data.local.SongDao
import com.talha.music.data.local.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS playback_history (" +
                    "songId TEXT NOT NULL PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "artist TEXT NOT NULL, " +
                    "album TEXT, " +
                    "durationText TEXT, " +
                    "thumbnailUrl TEXT, " +
                    "playedAt INTEGER NOT NULL)"
            )
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS playlists (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL)"
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS playlist_songs (" +
                    "playlistId INTEGER NOT NULL, " +
                    "songId TEXT NOT NULL, " +
                    "position INTEGER NOT NULL, " +
                    "PRIMARY KEY(playlistId, songId))"
            )
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS search_history (" +
                    "query TEXT NOT NULL PRIMARY KEY, " +
                    "searchedAt INTEGER NOT NULL)"
            )
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE songs ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "talha_music_db"
        ).addMigrations(migration1To2, migration2To3, migration3To4, migration4To5).build()
    }

    @Provides
    fun provideSongDao(database: MusicDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun providePlaybackHistoryDao(database: MusicDatabase): PlaybackHistoryDao {
        return database.playbackHistoryDao()
    }

    @Provides
    fun providePlaylistDao(database: MusicDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: MusicDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }
}
