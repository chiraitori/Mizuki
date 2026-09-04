package dev.chiraitori.mizuki.core.model

data class StreamFormat(
    val formatId: String,
    val ext: String,
    val formatNote: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Double = 0.0,
    val vcodec: String? = null,
    val acodec: String? = null,
    val fileSize: Long = 0,
    val tbr: Double = 0.0,
    val isVideoOnly: Boolean = false,
    val isAudioOnly: Boolean = false,
    val hasBoth: Boolean = false,
    val directUrl: String? = null
) {
    val resolutionLabel: String
        get() = when {
            height > 0 && width > 0 -> "${width}x${height}" + (if (fps > 30) " @${fps.toInt()}fps" else "")
            height > 0 -> "${height}p" + (if (fps > 30) " @${fps.toInt()}fps" else "")
            isAudioOnly -> "Audio ($acodec)"
            else -> formatNote ?: ext
        }

    val codecLabel: String
        get() = when {
            isVideoOnly -> "Video ($vcodec)"
            isAudioOnly -> "Audio ($acodec)"
            else -> "$vcodec / $acodec"
        }
}

enum class DownloadType {
    VIDEO,
    AUDIO,
    CUSTOM
}

enum class VideoResolution(val label: String, val height: Int, val formatKey: String) {
    BEST("Tự động (Cao nhất)", 0, "bestvideo+bestaudio/best"),
    RES_2160P("4K (2160p)", 2160, "bestvideo[height<=2160]+bestaudio/best[height<=2160]"),
    RES_1440P("2K (1440p)", 1440, "bestvideo[height<=1440]+bestaudio/best[height<=1440]"),
    RES_1080P("Full HD (1080p)", 1080, "bestvideo[height<=1080]+bestaudio/best[height<=1080]"),
    RES_720P("HD (720p)", 720, "bestvideo[height<=720]+bestaudio/best[height<=720]"),
    RES_480P("SD (480p)", 480, "bestvideo[height<=480]+bestaudio/best[height<=480]"),
    RES_360P("Tiết kiệm (360p)", 360, "bestvideo[height<=360]+bestaudio/best[height<=360]")
}

enum class AudioFormat(val label: String, val ext: String) {
    MP3("MP3 (Chất lượng cao 320k)", "mp3"),
    M4A("M4A (AAC)", "m4a"),
    OPUS("OPUS", "opus"),
    FLAC("FLAC (Lossless)", "flac"),
    WAV("WAV (Lossless)", "wav")
}

data class PlaylistItem(
    val id: String,
    val url: String,
    val title: String,
    val author: String? = null,
    val duration: Long = 0,
    val thumbnailUrl: String? = null,
    val index: Int = 0,
    val isSelected: Boolean = true
)

data class VideoDetails(
    val id: String,
    val title: String,
    val author: String? = null,
    val authorUrl: String? = null,
    val duration: Long = 0,
    val thumbnailUrl: String? = null,
    val originalUrl: String,
    val webpageUrl: String? = null,
    val ext: String = "mp4",
    val fileSizeApprox: Long = 0,
    val description: String? = null,
    val formats: List<StreamFormat> = emptyList(),
    val isPlaylist: Boolean = false,
    val playlistCount: Int = 0,
    val playlistItems: List<PlaylistItem> = emptyList(),
    val directDownloadUrl: String? = null,
    val directAudioUrl: String? = null
)

data class CommandTemplate(
    val id: String,
    val name: String,
    val description: String,
    val customArgs: String,
    val isBuiltIn: Boolean = false
)

enum class OutputTemplatePreset(val label: String, val template: String) {
    STANDARD("Tiêu chuẩn (Tên video)", "%(title)s.%(ext)s"),
    WITH_AUTHOR("Kèm tác giả (Tác giả - Tên video)", "%(uploader)s - %(title)s.%(ext)s"),
    WITH_ID("Kèm ID (Tên video [ID])", "%(title)s [%(id)s].%(ext)s"),
    PLAYLIST_FOLDER("Thư mục Playlist (Playlist/Thứ tự - Tên)", "%(playlist)s/%(playlist_index)s - %(title)s.%(ext)s"),
    DATE_PREFIX("Kèm ngày đăng (YYYYMMDD_Tên video)", "%(upload_date)s_%(title)s.%(ext)s")
}

data class DownloadConfig(
    val type: DownloadType = DownloadType.VIDEO,
    val videoResolution: VideoResolution = VideoResolution.BEST,
    val selectedFormatId: String? = null,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val embedThumbnail: Boolean = true,
    val embedSubtitles: Boolean = false,
    val embedMetadata: Boolean = true,
    val cropArtworkSquare: Boolean = false,
    val useAria2c: Boolean = true,
    val aria2cConnections: Int = 16,
    val customArgs: String = "",
    val outputTemplate: String = "%(title)s.%(ext)s",
    val customOutDir: String? = null,
    val cookiesFilePath: String? = null,
    val wifiOnly: Boolean = false,
    val clipSectionStart: String? = null,
    val clipSectionEnd: String? = null,
    val removeSponsorSegments: Boolean = false,
    val sponsorBlockCategories: String = "sponsor,selfpromo,intro,outro",
    val restrictFilenames: Boolean = false,
    val useDownloadArchive: Boolean = false,
    val keepSubtitleFiles: Boolean = false,
    val privateMode: Boolean = false,
    val separateAudioDir: String? = null,
    val directStreamUrl: String? = null
)

enum class TaskStatus {
    IDLE,
    FETCHING_INFO,
    READY,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED
}

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String = "",
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long = 0,
    val progress: Float = 0f,
    val speed: String = "",
    val eta: String = "",
    val status: TaskStatus = TaskStatus.IDLE,
    val filePath: String? = null,
    val errorMessage: String? = null,
    val rawLogs: List<String> = emptyList(),
    val config: DownloadConfig = DownloadConfig(),
    val timestamp: Long = System.currentTimeMillis()
)
