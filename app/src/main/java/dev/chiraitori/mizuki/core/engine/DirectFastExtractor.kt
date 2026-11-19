package dev.chiraitori.mizuki.core.engine

import android.util.Log
import dev.chiraitori.mizuki.core.model.StreamFormat
import dev.chiraitori.mizuki.core.model.VideoDetails
import dev.chiraitori.mizuki.core.parser.UrlParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object DirectFastExtractor {
    private const val TAG = "DirectFastExtractor"
    private const val USER_AGENT_ANDROID = "com.zhiliaoapp.musically/2022600030 (Linux; U; Android 14; en_US; Pixel 8; Build/UQ1A.240205.004; Cronet/58.0.2991.0)"
    private const val TIKTOK_VIDEO_HEIGHT = 1920
    private const val TIKTOK_VIDEO_WIDTH = 1080
    private val TIKTOK_FEED_HOSTS = listOf(
        "api16-normal-c-useast1a.tiktokv.com",
        "api16-core-c-useast1a.tiktokv.com"
    )

    suspend fun tryDirectExtract(
        url: String,
        preferStandardPlay: Boolean = false
    ): Result<VideoDetails>? = withContext(Dispatchers.IO) {
        try {
            when (UrlParser.detectPlatform(url)) {
                UrlParser.Platform.TIKTOK -> extractTikTok(url, preferStandardPlay)
                UrlParser.Platform.TWITTER_X -> extractTwitter(url)
                UrlParser.Platform.INSTAGRAM -> extractInstagram(url)
                UrlParser.Platform.REDDIT -> extractReddit(url)
                else -> null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Direct extraction exception for $url, fallback to yt-dlp: ${e.message}")
            null
        }
    }

    private fun JSONObject.optCleanString(key: String): String =
        if (isNull(key)) "" else optString(key, "").trim()

    private suspend fun fetchTikWmRoot(encodedUrl: String): JSONObject? {
        val headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        val apiUrl = "https://www.tikwm.com/api/?url=$encodedUrl&hd=1"
        val firstJson = httpGet(apiUrl, headers) ?: return null
        val firstRoot = JSONObject(firstJson)
        if (firstRoot.optInt("code", -1) == -1) {
            val msg = firstRoot.optCleanString("msg")
            if (msg.contains("limit", ignoreCase = true) || msg.contains("second", ignoreCase = true)) {
                Log.w(TAG, "TikWM rate-limited ($msg), retrying after delay")
                delay(1_300L)
                val retryJson = httpGet(apiUrl, headers) ?: return null
                return JSONObject(retryJson)
            }
        }
        return firstRoot
    }

    // 1. TikTok Multi-tier Direct Extractor (TikWM + Mobile Feed)
    private suspend fun extractTikTok(
        rawUrl: String,
        preferStandardPlay: Boolean = false
    ): Result<VideoDetails>? {
        // Tier 1: TikWM Direct CDN API
        try {
            val encoded = URLEncoder.encode(rawUrl, "UTF-8")
            val root = fetchTikWmRoot(encoded)
            if (root != null && root.optInt("code", -1) == 0 && root.has("data")) {
                val data = root.getJSONObject("data")
                val id = data.optCleanString("id").ifBlank { "${System.currentTimeMillis()}" }
                val title = data.optCleanString("title").ifBlank { "TikTok Video" }
                val authorObj = data.optJSONObject("author")
                val uniqueId = authorObj?.optCleanString("unique_id").orEmpty()
                val author = authorObj?.optCleanString("nickname")
                    ?.ifBlank { uniqueId }
                    ?.ifBlank { "TikTok Creator" }
                    ?: "TikTok Creator"
                val authorUrl = uniqueId.takeIf { it.isNotBlank() }?.let { "@$it" }
                val cover = data.optCleanString("cover").ifBlank { data.optCleanString("origin_cover") }.ifBlank { null }
                val duration = data.optLong("duration", 0)
                val imageUrls = data.optJSONArray("images").toUrlList()

                if (imageUrls.isNotEmpty()) {
                    return Result.success(
                        VideoDetails(
                            id = id,
                            title = title.ifBlank { "TikTok Photos" },
                            author = author,
                            authorUrl = authorUrl,
                            thumbnailUrl = imageUrls.firstOrNull() ?: cover,
                            originalUrl = rawUrl,
                            webpageUrl = rawUrl,
                            ext = "jpg",
                            imageUrls = imageUrls
                        )
                    )
                }

                val directPlayUrl = data.optCleanString("play")
                val directHdUrl = data.optCleanString("hdplay")
                val directAudioUrl = data.optCleanString("music")
                val playSize = data.optLong("size", 0)
                val hdSize = data.optLong("hd_size", 0).takeIf { it > 0 } ?: playSize

                if (directHdUrl.isBlank() && directPlayUrl.isBlank() && directAudioUrl.isBlank()) {
                    Log.w(TAG, "TikWM returned empty media URLs for $rawUrl")
                } else {
                    val formats = mutableListOf<StreamFormat>()
                    if (directHdUrl.isNotBlank()) {
                        formats.add(
                            StreamFormat(
                                formatId = "direct_tiktok_hd",
                                ext = "mp4",
                                formatNote = "HD Gốc không logo (CDN)",
                                width = TIKTOK_VIDEO_WIDTH,
                                height = TIKTOK_VIDEO_HEIGHT,
                                fps = 60.0,
                                vcodec = "h264/h265",
                                acodec = "aac",
                                fileSize = hdSize,
                                hasBoth = true,
                                directUrl = directHdUrl
                            )
                        )
                    }

                    if (directPlayUrl.isNotBlank() && directPlayUrl != directHdUrl) {
                        if (directHdUrl.isBlank()) {
                            formats.add(
                                StreamFormat(
                                    formatId = "direct_tiktok_hd",
                                    ext = "mp4",
                                    formatNote = "HD Gốc không logo (CDN)",
                                    width = TIKTOK_VIDEO_WIDTH,
                                    height = TIKTOK_VIDEO_HEIGHT,
                                    fps = 60.0,
                                    vcodec = "h264/h265",
                                    acodec = "aac",
                                    fileSize = playSize,
                                    hasBoth = true,
                                    directUrl = directPlayUrl
                                )
                            )
                        } else {
                            formats.add(
                                StreamFormat(
                                    formatId = "direct_tiktok_sd",
                                    ext = "mp4",
                                    formatNote = "Tiêu chuẩn không logo (CDN)",
                                    width = 720,
                                    height = 1280,
                                    fps = 30.0,
                                    vcodec = "h264",
                                    acodec = "aac",
                                    fileSize = playSize,
                                    hasBoth = true,
                                    directUrl = directPlayUrl
                                )
                            )
                        }
                    }

                    if (directAudioUrl.isNotBlank()) {
                        formats.add(
                            StreamFormat(
                                formatId = "direct_tiktok_audio",
                                ext = "mp3",
                                formatNote = "Âm thanh gốc MP3",
                                acodec = "mp3",
                                isAudioOnly = true,
                                directUrl = directAudioUrl
                            )
                        )
                    }

                    val chosenVideoUrl = (if (preferStandardPlay) directPlayUrl.ifBlank { directHdUrl } else directHdUrl.ifBlank { directPlayUrl }).ifBlank { null }
                    val chosenAudioUrl = directAudioUrl.ifBlank { null }
                    val chosenSize = if (preferStandardPlay) playSize else hdSize

                    val details = VideoDetails(
                        id = id,
                        title = title,
                        author = author,
                        authorUrl = authorUrl,
                        duration = duration,
                        thumbnailUrl = cover,
                        originalUrl = rawUrl,
                        webpageUrl = rawUrl,
                        ext = "mp4",
                        fileSizeApprox = chosenSize,
                        formats = formats,
                        directDownloadUrl = chosenVideoUrl,
                        directAudioUrl = chosenAudioUrl
                    )
                    return Result.success(details)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "TikWM tier failed: ${e.message}")
        }

        // Tier 2: TikTok Mobile Feed API
        try {
            val finalUrl = resolveRedirects(rawUrl)
            val videoId = Regex("video/(\\d+)").find(finalUrl)?.groupValues?.get(1)
                ?: Regex("/(\\d{15,22})").find(finalUrl)?.groupValues?.get(1)

            if (videoId != null) {
                for (host in TIKTOK_FEED_HOSTS) {
                    val apiUrl = "https://$host/aweme/v1/feed/?aweme_id=$videoId&version_code=34.1.2&app_name=musical_ly&channel=googleplay&device_platform=android&aid=1233"
                    val jsonStr = httpGet(apiUrl, mapOf("User-Agent" to USER_AGENT_ANDROID)) ?: continue
                    if (jsonStr.contains("ratelimit")) continue

                    val root = JSONObject(jsonStr)
                    val awemeList = root.optJSONArray("aweme_list") ?: continue
                    val aweme = (0 until awemeList.length())
                        .asSequence()
                        .mapNotNull { awemeList.optJSONObject(it) }
                        .firstOrNull { it.optCleanString("aweme_id") == videoId }
                        ?: continue

                    val title = aweme.optCleanString("desc").ifBlank { "TikTok Video" }
                    val authorObj = aweme.optJSONObject("author")
                    val uniqueId = authorObj?.optCleanString("unique_id").orEmpty()
                    val author = authorObj?.optCleanString("nickname")
                        ?.ifBlank { uniqueId }
                        ?.ifBlank { "TikTok Creator" }
                        ?: "TikTok Creator"
                    val authorUrl = uniqueId.takeIf { it.isNotBlank() }?.let { "@$it" }
                    val imageUrls = extractFeedImageUrls(aweme)
                    if (imageUrls.isNotEmpty()) {
                        return Result.success(
                            VideoDetails(
                                id = videoId,
                                title = title.ifBlank { "TikTok Photos" },
                                author = author,
                                authorUrl = authorUrl,
                                thumbnailUrl = imageUrls.first(),
                                originalUrl = rawUrl,
                                webpageUrl = finalUrl,
                                ext = "jpg",
                                imageUrls = imageUrls
                            )
                        )
                    }

                    val videoObj = aweme.optJSONObject("video")
                    val directUrl = videoObj?.optJSONObject("play_addr")?.optJSONArray("url_list").toUrlList().firstOrNull()
                    val directAudioUrl = aweme.optJSONObject("music")?.optJSONObject("play_url")?.optJSONArray("url_list").toUrlList().firstOrNull()
                    val cover = videoObj?.optJSONObject("cover")?.optJSONArray("url_list").toUrlList().firstOrNull()

                    if (!directUrl.isNullOrBlank() || !directAudioUrl.isNullOrBlank()) {
                        val formats = buildList {
                            if (!directUrl.isNullOrBlank()) {
                                add(
                                    StreamFormat(
                                        formatId = "direct_tiktok_hd",
                                        ext = "mp4",
                                        formatNote = "HD Gốc không logo (CDN)",
                                        width = TIKTOK_VIDEO_WIDTH,
                                        height = TIKTOK_VIDEO_HEIGHT,
                                        fps = 30.0,
                                        vcodec = "h264",
                                        acodec = "aac",
                                        hasBoth = true,
                                        directUrl = directUrl
                                    )
                                )
                            }
                            if (!directAudioUrl.isNullOrBlank()) {
                                add(
                                    StreamFormat(
                                        formatId = "direct_tiktok_audio",
                                        ext = "mp3",
                                        formatNote = "Âm thanh gốc MP3",
                                        acodec = "mp3",
                                        isAudioOnly = true,
                                        directUrl = directAudioUrl
                                    )
                                )
                            }
                        }

                        return Result.success(
                            VideoDetails(
                                id = videoId,
                                title = title,
                                author = author,
                                authorUrl = authorUrl,
                                duration = (videoObj?.optLong("duration", 0) ?: 0) / 1000,
                                thumbnailUrl = cover,
                                originalUrl = rawUrl,
                                webpageUrl = finalUrl,
                                ext = "mp4",
                                formats = formats,
                                directDownloadUrl = directUrl,
                                directAudioUrl = directAudioUrl
                            )
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "TikTok feed tier failed: ${e.message}")
        }

        return null
    }

    private fun JSONArray?.toUrlList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.startsWith("http") }?.let(::add)
            }
        }.distinct()
    }

    private fun extractFeedImageUrls(aweme: JSONObject): List<String> {
        val images = aweme.optJSONObject("image_post_info")?.optJSONArray("images")
            ?: return emptyList()
        return buildList {
            for (index in 0 until images.length()) {
                val image = images.optJSONObject(index) ?: continue
                val candidates = listOf("display_image", "origin_image")
                val url = candidates.firstNotNullOfOrNull { key ->
                    image.optJSONObject(key)
                        ?.optJSONArray("url_list")
                        .toUrlList()
                        .firstOrNull()
                }
                if (!url.isNullOrBlank()) add(url)
            }
        }.distinct()
    }

    // 2. Twitter / X Direct Extractor
    private fun extractTwitter(rawUrl: String): Result<VideoDetails>? {
        try {
            val tweetId = Regex("status/(\\d+)").find(rawUrl)?.groupValues?.get(1)
                ?: Regex("status/(\\d+)").find(resolveRedirects(rawUrl))?.groupValues?.get(1)
                ?: return null
            val apiUrl = "https://api.fxtwitter.com/status/$tweetId"

            val jsonStr = httpGet(apiUrl, mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) ?: return null
            val root = JSONObject(jsonStr)
            val tweet = root.optJSONObject("tweet") ?: return null
            val media = tweet.optJSONObject("media")
            val videos = media?.optJSONArray("videos") ?: return null
            if (videos.length() == 0) return null

            val video = videos.getJSONObject(0)
            val videoUrl = video.optCleanString("url").takeIf { it.isNotBlank() } ?: return null
            val thumbUrl = video.optCleanString("thumbnail_url").ifBlank { null }
            val duration = (video.optDouble("duration", 0.0)).toLong()
            val text = tweet.optCleanString("text").ifBlank { "Twitter Video" }
            val authorObj = tweet.optJSONObject("author")
            val authorName = authorObj?.optCleanString("name")?.ifBlank { "Twitter User" } ?: "Twitter User"
            val authorUrl = authorObj?.optCleanString("screen_name")?.takeIf { it.isNotBlank() }?.let { "@$it" }

            val formats = listOf(
                StreamFormat(
                    formatId = "twitter_hd",
                    ext = "mp4",
                    formatNote = "Twitter HD (Direct MP4)",
                    vcodec = "h264",
                    acodec = "aac",
                    hasBoth = true,
                    directUrl = videoUrl
                )
            )

            val details = VideoDetails(
                id = tweetId,
                title = text,
                author = authorName,
                authorUrl = authorUrl,
                duration = duration,
                thumbnailUrl = thumbUrl,
                originalUrl = rawUrl,
                webpageUrl = rawUrl,
                ext = "mp4",
                formats = formats,
                directDownloadUrl = videoUrl
            )

            return Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Twitter extract error: ${e.message}", e)
            return null
        }
    }

    // 3. Instagram Direct Extractor
    private fun extractInstagram(rawUrl: String): Result<VideoDetails>? {
        try {
            val cleanUrl = rawUrl.split("?")[0]
            val apiHost = cleanUrl.replace("instagram.com", "ddinstagram.com") + "?__a=1"
            val jsonStr = httpGet(apiHost, mapOf("User-Agent" to "Mozilla/5.0")) ?: return null
            val root = JSONObject(jsonStr)
            val items = root.optJSONArray("items") ?: return null
            if (items.length() == 0) return null

            val item = items.getJSONObject(0)
            val title = item.optJSONObject("caption")?.optCleanString("text")?.ifBlank { "Instagram Video" } ?: "Instagram Video"
            val user = item.optJSONObject("user")?.optCleanString("username")?.ifBlank { "Instagram User" } ?: "Instagram User"
            val videoVersions = item.optJSONArray("video_versions") ?: return null
            if (videoVersions.length() == 0) return null

            val bestVideo = videoVersions.getJSONObject(0)
            val videoUrl = bestVideo.optCleanString("url").takeIf { it.isNotBlank() } ?: return null
            val width = bestVideo.optInt("width", 1080)
            val height = bestVideo.optInt("height", 1920)
            val thumbUrl = item.optJSONObject("image_versions2")?.optJSONArray("candidates")?.optJSONObject(0)?.optCleanString("url")?.ifBlank { null }

            val details = VideoDetails(
                id = item.optCleanString("id").ifBlank { "${System.currentTimeMillis()}" },
                title = title,
                author = user,
                authorUrl = "@$user",
                duration = 0,
                thumbnailUrl = thumbUrl,
                originalUrl = rawUrl,
                webpageUrl = rawUrl,
                ext = "mp4",
                formats = listOf(
                    StreamFormat(
                        formatId = "instagram_hd",
                        ext = "mp4",
                        formatNote = "Instagram Video HD",
                        width = width,
                        height = height,
                        hasBoth = true,
                        directUrl = videoUrl
                    )
                ),
                directDownloadUrl = videoUrl
            )
            return Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Instagram extract error: ${e.message}", e)
            return null
        }
    }

    // 4. Reddit Direct Extractor
    private fun extractReddit(rawUrl: String): Result<VideoDetails>? {
        try {
            val targetUrl = if (rawUrl.contains("redd.it/", ignoreCase = true) || rawUrl.contains("/s/", ignoreCase = true)) {
                resolveRedirects(rawUrl)
            } else {
                rawUrl
            }
            val cleanUrl = targetUrl.split("?")[0].trimEnd('/') + ".json"
            val jsonStr = httpGet(cleanUrl, mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) ?: return null
            val rootArray = JSONArray(jsonStr)
            if (rootArray.length() == 0) return null

            val post = rootArray.getJSONObject(0).getJSONObject("data").getJSONArray("children").getJSONObject(0).getJSONObject("data")
            val title = post.optCleanString("title").ifBlank { "Reddit Video" }
            val author = post.optCleanString("author").ifBlank { "Reddit User" }
            val media = post.optJSONObject("secure_media") ?: post.optJSONObject("media")
            val redditVideo = media?.optJSONObject("reddit_video") ?: return null
            if (!redditVideo.optBoolean("is_gif", false)) return null

            val fallbackUrl = redditVideo.optCleanString("fallback_url").takeIf { it.isNotBlank() } ?: return null
            val duration = redditVideo.optLong("duration", 0)
            val height = redditVideo.optInt("height", 720)
            val width = redditVideo.optInt("width", 1280)
            val thumbUrl = post.optCleanString("thumbnail").takeIf { it.startsWith("http") }

            val details = VideoDetails(
                id = post.optCleanString("id").ifBlank { "${System.currentTimeMillis()}" },
                title = title,
                author = author,
                authorUrl = "u/$author",
                duration = duration,
                thumbnailUrl = thumbUrl,
                originalUrl = rawUrl,
                webpageUrl = rawUrl,
                ext = "mp4",
                formats = listOf(
                    StreamFormat(
                        formatId = "reddit_direct",
                        ext = "mp4",
                        formatNote = "Reddit Video",
                        width = width,
                        height = height,
                        hasBoth = true,
                        directUrl = fallbackUrl
                    )
                ),
                directDownloadUrl = fallbackUrl
            )
            return Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "Reddit extract error: ${e.message}", e)
            return null
        }
    }

    private fun resolveRedirects(urlStr: String): String {
        var currentUrl = urlStr
        try {
            for (i in 0 until 5) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                try {
                    conn.instanceFollowRedirects = false
                    conn.setRequestProperty("User-Agent", USER_AGENT_ANDROID)
                    conn.connectTimeout = 5_000
                    conn.readTimeout = 5_000
                    conn.connect()

                    val code = conn.responseCode
                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location") ?: break
                        currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                    } else {
                        break
                    }
                } finally {
                    conn.disconnect()
                }
            }
        } catch (_: Exception) {
        }
        return currentUrl
    }

    private fun httpGet(urlStr: String, headers: Map<String, String>): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.connect()

            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } else {
                Log.w(TAG, "HTTP ${conn.responseCode} for $urlStr")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "httpGet failed for $urlStr: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }
}
