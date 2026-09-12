package com.talha.music.data.remote

import com.google.gson.annotations.SerializedName

data class PipedSearchResponse(
    val items: List<PipedSearchItem>
)

data class PipedSearchItem(
    val url: String, // format: /watch?v=videoId
    val title: String,
    val thumbnail: String,
    val uploaderName: String,
    val duration: Int = 0 // in seconds
) {
    val videoId: String
    get() = url.substringAfter("v=").substringBefore('&')
}

data class PipedStreamResponse(
    val audioStreams: List<PipedAudioStream>,
    val title: String? = null,
    val uploader: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Int? = null
)

data class PipedAudioStream(
    val url: String,
    val format: String,
    val quality: String,
    val mimeType: String,
    val codec: String
)
