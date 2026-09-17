package com.talha.music.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface YouTubeApi {

    @POST("youtubei/v1/player")
    suspend fun getPlayer(
        @Body request: YouTubePlayerRequest
    ): YouTubePlayerResponse

    companion object {
        const val BASE_URL = "https://www.youtube.com/"
    }
}
