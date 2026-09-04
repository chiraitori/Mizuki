package dev.chiraitori.mizuki.core

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

object DownloadManager {
    private const val TAG = "DownloadManager"

    private val urlPattern = Pattern.compile(
        "https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*"
    )

    private val _downloadHistory = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadHistory = _downloadHistory.asStateFlow()

    fun extractUrl(text: String): String? {
        val matcher = urlPattern.matcher(text.trim())
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    fun getDownloadDir(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val mizukiDir = File(downloadsDir, "Mizuki")
        if (!mizukiDir.exists()) {
            mizukiDir.mkdirs()
        }
        return mizukiDir
    }

    suspend fun fetchVideoInfo(rawUrl: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        val url = extractUrl(rawUrl) ?: rawUrl.trim()
        if (url.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("URL không hợp lệ"))
        }

        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("--add-header", "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            val info: VideoInfo = YoutubeDL.getInstance().getInfo(request)

            val metadata = VideoMetadata(
                id = info.id ?: System.currentTimeMillis().toString(),
                title = info.title ?: "TikTok Video",
                author = info.uploader ?: info.uploaderId ?: "TikTok",
                duration = info.duration.toLong(),
                thumbnailUrl = info.thumbnail,
                originalUrl = url,
                webpageUrl = info.webpageUrl ?: url,
                ext = info.ext ?: "mp4",
                fileSizeApprox = info.fileSize
            )
            Result.success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch video info: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadMedia(
        context: Context,
        metadata: VideoMetadata,
        downloadType: DownloadType = DownloadType.VIDEO,
        processId: String = "mizuki_${System.currentTimeMillis()}",
        onProgress: (progress: Float, speed: String, eta: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputDir = getDownloadDir()
            val sanitizedTitle = metadata.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(60)

            val request = YoutubeDLRequest(metadata.originalUrl).apply {
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("--no-mtime")
                addOption("--add-header", "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

                if (downloadType == DownloadType.AUDIO) {
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "0")
                    addOption("-o", "${outputDir.absolutePath}/$sanitizedTitle.%(ext)s")
                } else {
                    // Highest quality video without watermark
                    addOption("-f", "b/bv*+ba")
                    addOption("-o", "${outputDir.absolutePath}/$sanitizedTitle.%(ext)s")
                }
            }

            var lastSpeed = ""
            var lastEta = ""

            val response = YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                val etaText = if (etaInSeconds > 0) "${etaInSeconds}s" else ""
                
                // Parse speed if available in output line
                if (line.contains("at") && line.contains("/s")) {
                    val parts = line.split("at")
                    if (parts.size > 1) {
                        lastSpeed = parts[1].trim().split(" ")[0]
                    }
                }
                lastEta = etaText
                onProgress(progress, lastSpeed, lastEta)
            }

            // Find the downloaded file
            val targetExt = if (downloadType == DownloadType.AUDIO) "mp3" else "mp4"
            var downloadedFile = File(outputDir, "$sanitizedTitle.$targetExt")
            if (!downloadedFile.exists()) {
                val matchingFiles = outputDir.listFiles { file ->
                    file.name.startsWith(sanitizedTitle)
                }
                if (!matchingFiles.isNullOrEmpty()) {
                    downloadedFile = matchingFiles.first()
                }
            }

            // Scan media into Android MediaStore so it appears instantly in Gallery
            if (downloadedFile.exists()) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(downloadedFile.absolutePath),
                    null
                ) { path, uri ->
                    Log.d(TAG, "Scanned $path to MediaStore: $uri")
                }
            }

            // Add to history
            val item = DownloadItem(
                id = processId,
                url = metadata.originalUrl,
                title = metadata.title,
                author = metadata.author,
                thumbnailUrl = metadata.thumbnailUrl,
                progress = 100f,
                speedText = "",
                etaText = "",
                status = DownloadStatus.COMPLETED,
                filePath = downloadedFile.absolutePath,
                type = downloadType
            )
            _downloadHistory.value = listOf(item) + _downloadHistory.value.filter { it.id != processId }

            Result.success(downloadedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun cancelDownload(processId: String) {
        try {
            YoutubeDL.getInstance().destroyProcessById(processId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel process $processId", e)
        }
    }

    suspend fun updateEngine(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
            Result.success(status?.name ?: "Updated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update engine: ${e.message}", e)
            Result.failure(e)
        }
    }
}
