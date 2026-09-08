package dev.chiraitori.mizuki.core.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

data class SavedTikTokPhoto(
    val uri: Uri,
    val displayName: String,
    val byteCount: Long
)

data class TikTokPhotoDownloadResult(
    val saved: List<SavedTikTokPhoto>,
    val failedCount: Int
)

object TikTokPhotoDownloader {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36"

    suspend fun download(
        context: Context,
        postId: String,
        title: String,
        imageUrls: List<String>,
        onProgress: (completed: Int, total: Int, currentFraction: Float) -> Unit
    ): TikTokPhotoDownloadResult = withContext(Dispatchers.IO) {
        val saved = mutableListOf<SavedTikTokPhoto>()
        var failed = 0
        val safeTitle = sanitizeTitle(title)

        imageUrls.forEachIndexed { index, imageUrl ->
            coroutineContext.ensureActive()
            runCatching {
                downloadOne(
                    context = context,
                    imageUrl = imageUrl,
                    displayNameBase = "${safeTitle}_${postId.takeLast(8)}_${(index + 1).toString().padStart(2, '0')}",
                    onProgress = { fraction -> onProgress(index, imageUrls.size, fraction) }
                )
            }.onSuccess(saved::add)
                .onFailure { failed++ }
            onProgress(index + 1, imageUrls.size, 0f)
        }

        TikTokPhotoDownloadResult(saved = saved, failedCount = failed)
    }

    private fun downloadOne(
        context: Context,
        imageUrl: String,
        displayNameBase: String,
        onProgress: (Float) -> Unit
    ): SavedTikTokPhoto {
        val connection = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Referer", "https://www.tiktok.com/")
        }

        var outputUri: Uri? = null
        try {
            connection.connect()
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }

            val mimeType = connection.contentType?.substringBefore(';')?.trim()
                ?.takeIf { it.startsWith("image/") }
                ?: "image/jpeg"
            val extension = when (mimeType.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/avif" -> "avif"
                else -> "jpg"
            }
            val displayName = "$displayNameBase.$extension"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Mizuki")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            outputUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Không tạo được file ảnh")

            var copied = 0L
            val contentLength = connection.contentLengthLong
            connection.inputStream.buffered().use { input ->
                resolver.openOutputStream(outputUri, "w")?.buffered()?.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        copied += count
                        if (contentLength > 0) {
                            onProgress((copied.toFloat() / contentLength).coerceIn(0f, 1f))
                        }
                    }
                } ?: error("Không mở được file ảnh")
            }

            resolver.update(
                outputUri,
                ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                null,
                null
            )
            return SavedTikTokPhoto(outputUri, displayName, copied)
        } catch (error: Throwable) {
            outputUri?.let { context.contentResolver.delete(it, null, null) }
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun sanitizeTitle(title: String): String {
        val sanitized = title
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.')
            .take(72)
        return sanitized.ifBlank { "TikTok_Photo" }
    }
}
