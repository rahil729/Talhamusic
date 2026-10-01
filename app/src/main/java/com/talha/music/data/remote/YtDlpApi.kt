package com.talha.music.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YtDlpApi {
    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): YtDlpSearchResponse

    @GET("stream/{videoId}")
    suspend fun getStream(@Path("videoId") videoId: String): YtDlpStreamResponse
}

data class YtDlpSearchResponse(val items: List<YtDlpSearchItem> = emptyList())

data class YtDlpSearchItem(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: Int? = null,
    val thumbnailUrl: String? = null
)

data class YtDlpStreamResponse(
    val videoId: String = "",
    val title: String? = null,
    val url: String = "",
    val mimeType: String? = null,
    val duration: Int? = null
)