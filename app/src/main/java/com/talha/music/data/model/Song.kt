package com.talha.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val isFavorite: Boolean = false,
    val playCount: Int = 0
)
