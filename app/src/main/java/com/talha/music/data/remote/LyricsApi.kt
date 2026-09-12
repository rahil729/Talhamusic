package com.talha.music.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class LyricsResponse(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

interface LyricsApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): LyricsResponse

    companion object {
        const val BASE_URL = "https://lrclib.net/"
    }
}
