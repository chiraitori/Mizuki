package dev.chiraitori.mizuki.core.engine

import android.util.Log
import dev.chiraitori.mizuki.core.model.StreamFormat
import dev.chiraitori.mizuki.core.model.VideoDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object DirectFastExtractor {
    private const val TAG = "DirectFastExtractor"
    private const val USER_AGENT_ANDROID = "com.zhiliaoapp.musically/2022600030 (Linux; U; Android 14; en_US; Pixel 8; Build/UQ1A.240205.004; Cronet/58.0.2991.0)"

    suspend fun tryDirectExtract(url: String): Result<VideoDetails>? = withContext(Dispatchers.IO) {
        val lower = url.lowercase()
        try {
            when {
                lower.contains("tiktok.com") || lower.contains("douyin.com") -> extractTikTok(url)
                lower.contains("twitter.com") || lower.contains("x.com") -> extractTwitter(url)
                lower.contains("instagram.com") -> extractInstagram(url)
                lower.contains("reddit.com") || lower.contains("redd.it") -> extractReddit(url)
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct extraction exception for $url, fallback to yt-dlp: ${e.message}")
            null
        }
    }

    // 1. TikTok Multi-tier Direct Extractor (TikWM + Mobile Feed)
    private fun extractTikTok(rawUrl: String): Result<VideoDetails>? {
        // Tier 1: TikWM Direct CDN API
        try {
            val encoded = URLEncoder.encode(rawUrl, "UTF-8")
            val apiUrl = "https://www.tikwm.com/api/?url=$encoded"
            val jsonStr = httpGet(apiUrl, mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"))
            if (jsonStr != null) {
                val root = JSONObject(jsonStr)
                if (root.optInt("code", -1) == 0 && root.has("data")) {
                    val data = root.getJSONObject("data")
                    val id = data.optString("id", "${System.currentTimeMillis()}")
                    val title = data.optString("title", "TikTok Video")
                    val authorObj = data.optJSONObject("author")
                    val author = authorObj?.optString("nickname") ?: authorObj?.optString("unique_id") ?: "TikTok Creator"
                    val authorUrl = authorObj?.optString("unique_id")?.let { "@$it" }
                    val cover = data.optString("cover")
                    val duration = data.optLong("duration", 0)

                    val directPlayUrl = data.optString("play")
                    val directHdUrl = data.optString("hdplay").ifEmpty { directPlayUrl }
                    val directAudioUrl = data.optString("music")
                    val size = data.optLong("size", 0)

                    val formats = mutableListOf<StreamFormat>()
                    if (directHdUrl.isNotEmpty()) {
                        formats.add(
                            StreamFormat(
                                formatId = "direct_tiktok_hd",
                                ext = "mp4",
                                formatNote = "HD Gốc không logo (CDN)",
                                width = 1080,
                                height = 1920,
                                fps = 60.0,
                                vcodec = "h264/h265",
                                acodec = "aac",
                                fileSize = size,
                                hasBoth = true,
                                directUrl = directHdUrl
                            )
                        )
                    }

                    if (directAudioUrl.isNotEmpty()) {
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
                        fileSizeApprox = size,
                        formats = formats,
                        directDownloadUrl = directHdUrl.ifEmpty { directPlayUrl },
                        directAudioUrl = directAudioUrl
                    )
                    return Result.success(details)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TikWM tier failed: ${e.message}")
        }

        // Tier 2: TikTok Mobile Feed API
        try {
            val finalUrl = resolveRedirects(rawUrl)
            val videoId = Regex("video/(\\d+)").find(finalUrl)?.groupValues?.get(1)
                ?: Regex("/(\\d{15,22})").find(finalUrl)?.groupValues?.get(1)

            if (videoId != null) {
                val apiUrl = "https://api16-normal-c-useast1a.tiktokv.com/aweme/v1/feed/?aweme_id=$videoId&version_code=34.1.2&app_name=musical_ly&channel=googleplay&device_platform=android&aid=1233"
                val jsonStr = httpGet(apiUrl, mapOf("User-Agent" to USER_AGENT_ANDROID))
                if (jsonStr != null && !jsonStr.contains("ratelimit")) {
                    val root = JSONObject(jsonStr)
                    val awemeList = root.optJSONArray("aweme_list")
                    if (awemeList != null && awemeList.length() > 0) {
                        val aweme = awemeList.getJSONObject(0)
                        val title = aweme.optString("desc", "TikTok Video")
                        val authorObj = aweme.optJSONObject("author")
                        val author = authorObj?.optString("nickname") ?: "TikTok Creator"
                        val videoObj = aweme.optJSONObject("video")
                        val playAddr = videoObj?.optJSONObject("play_addr")?.optJSONArray("url_list")
                        val directUrl = if (playAddr != null && playAddr.length() > 0) playAddr.getString(0) else null

                        if (!directUrl.isNullOrEmpty()) {
                            val details = VideoDetails(
                                id = videoId,
                                title = title,
                                author = author,
                                authorUrl = authorObj?.optString("unique_id")?.let { "@$it" },
                                duration = (videoObj?.optLong("duration", 0) ?: 0) / 1000,
                                thumbnailUrl = videoObj?.optJSONObject("cover")?.optJSONArray("url_list")?.optString(0),
                                originalUrl = rawUrl,
                                webpageUrl = finalUrl,
                                ext = "mp4",
                                directDownloadUrl = directUrl
                            )
                            return Result.success(details)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TikTok feed tier failed: ${e.message}")
        }

        return null
    }

    // 2. Twitter / X Direct Extractor
    private fun extractTwitter(rawUrl: String): Result<VideoDetails>? {
        try {
            val tweetId = Regex("status/(\\d+)").find(rawUrl)?.groupValues?.get(1) ?: return null
            val apiUrl = "https://api.fxtwitter.com/status/$tweetId"

            val jsonStr = httpGet(apiUrl, mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) ?: return null
            val root = JSONObject(jsonStr)
            val tweet = root.optJSONObject("tweet") ?: return null
            val media = tweet.optJSONObject("media")
            val videos = media?.optJSONArray("videos") ?: return null
            if (videos.length() == 0) return null

            val video = videos.getJSONObject(0)
            val videoUrl = video.optString("url")
            val thumbUrl = video.optString("thumbnail_url")
            val duration = (video.optDouble("duration", 0.0)).toLong()
            val text = tweet.optString("text", "Twitter Video")
            val authorObj = tweet.optJSONObject("author")
            val authorName = authorObj?.optString("name") ?: "Twitter User"

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
                authorUrl = authorObj?.optString("screen_name")?.let { "@$it" },
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
            val title = item.optJSONObject("caption")?.optString("text") ?: "Instagram Video"
            val user = item.optJSONObject("user")?.optString("username") ?: "Instagram User"
            val videoVersions = item.optJSONArray("video_versions") ?: return null
            if (videoVersions.length() == 0) return null

            val bestVideo = videoVersions.getJSONObject(0)
            val videoUrl = bestVideo.getString("url")
            val width = bestVideo.optInt("width", 1080)
            val height = bestVideo.optInt("height", 1920)

            val details = VideoDetails(
                id = item.optString("id", "${System.currentTimeMillis()}"),
                title = title,
                author = user,
                authorUrl = "@$user",
                duration = 0,
                thumbnailUrl = item.optJSONObject("image_versions2")?.optJSONArray("candidates")?.optJSONObject(0)?.optString("url"),
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
            val cleanUrl = rawUrl.split("?")[0].trimEnd('/') + ".json"
            val jsonStr = httpGet(cleanUrl, mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")) ?: return null
            val rootArray = org.json.JSONArray(jsonStr)
            if (rootArray.length() == 0) return null

            val post = rootArray.getJSONObject(0).getJSONObject("data").getJSONArray("children").getJSONObject(0).getJSONObject("data")
            val title = post.getString("title")
            val author = post.optString("author", "Reddit User")
            val media = post.optJSONObject("secure_media") ?: post.optJSONObject("media")
            val redditVideo = media?.optJSONObject("reddit_video") ?: return null

            val fallbackUrl = redditVideo.getString("fallback_url")
            val duration = redditVideo.optLong("duration", 0)
            val height = redditVideo.optInt("height", 720)
            val width = redditVideo.optInt("width", 1280)

            val details = VideoDetails(
                id = post.optString("id", "${System.currentTimeMillis()}"),
                title = title,
                author = author,
                authorUrl = "u/$author",
                duration = duration,
                thumbnailUrl = post.optString("thumbnail"),
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
        var conn: HttpURLConnection? = null
        try {
            for (i in 0 until 5) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", USER_AGENT_ANDROID)
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location") ?: break
                    currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                } else {
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            conn?.disconnect()
        }
        return currentUrl
    }

    private fun httpGet(urlStr: String, headers: Map<String, String>): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.connect()

            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
