package com.talha.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val playedAt: Long
) {
    fun toSong(): Song = Song(
        id = songId,
        title = title,
        artist = artist,
        album = album,
        durationText = durationText,
        thumbnailUrl = thumbnailUrl
    )
}
