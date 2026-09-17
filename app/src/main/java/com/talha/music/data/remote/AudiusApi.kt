package com.talha.music.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface AudiusApi {
    @GET("v1/tracks/search")
    suspend fun searchTracks(
        @Query("query") query: String,
        @Query("app_name") appName: String = APP_NAME,
        @Query("limit") limit: Int = 30
    ): AudiusSearchResponse

    companion object {
        const val BASE_URL = "https://api.audius.co/"
        const val APP_NAME = "TalhaMusic"
    }
}

data class AudiusSearchResponse(
    val data: List<AudiusTrack> = emptyList()
)

data class AudiusTrack(
    val id: String = "",
    val title: String = "",
    val duration: Int = 0,
    val user: AudiusUser? = null,
    val artwork: AudiusArtwork? = null
)

data class AudiusUser(
    val name: String = "Audius artist"
)

data class AudiusArtwork(
    val `150x150`: String? = null,
    val `480x480`: String? = null
)
