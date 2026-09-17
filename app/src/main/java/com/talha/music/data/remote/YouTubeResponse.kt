package com.talha.music.data.remote

data class YouTubePlayerRequest(
    val context: YouTubeContext,
    val videoId: String,
    val serviceIntegrityDimensions: ServiceIntegrityDimensions? = null
)

data class YouTubeContext(
    val client: YouTubeClient
)

data class YouTubeClient(
    val clientName: String = "ANDROID",
    val clientVersion: String = "19.09.37",
    val androidSdkVersion: Int = 30,
    val hl: String = "en",
    val gl: String = "US",
    val visitorData: String? = null
)

data class ServiceIntegrityDimensions(
    val poToken: String
)

data class YouTubePlayerResponse(
    val streamingData: StreamingData?
)

data class StreamingData(
    val adaptiveFormats: List<YouTubePlayerFormat>?
)

data class YouTubePlayerFormat(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val contentLength: Long?,
    val audioQuality: String?,
    val audioSampleRate: String?
)
