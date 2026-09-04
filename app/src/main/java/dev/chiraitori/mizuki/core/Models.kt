package dev.chiraitori.mizuki.core

enum class DownloadStatus {
    IDLE,
    ANALYZING,
    READY,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class DownloadType {
    VIDEO,
    AUDIO
}

data class VideoMetadata(
    val id: String,
    val title: String,
    val author: String? = null,
    val duration: Long = 0,
    val thumbnailUrl: String? = null,
    val originalUrl: String,
    val webpageUrl: String? = null,
    val ext: String = "mp4",
    val fileSizeApprox: Long = 0
)

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val progress: Float = 0f,
    val speedText: String = "",
    val etaText: String = "",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val type: DownloadType = DownloadType.VIDEO,
    val timestamp: Long = System.currentTimeMillis()
)
