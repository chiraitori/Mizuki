package dev.chiraitori.mizuki.core.parser

import java.util.regex.Pattern

object UrlParser {

    private val urlPattern = Pattern.compile(
        "https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b[-a-zA-Z0-9()@:%_+.~#?&/=]*"
    )

    fun extractUrl(text: String): String? {
        val matcher = urlPattern.matcher(text.trim())
        return if (matcher.find()) {
            cleanUrl(matcher.group())
        } else {
            null
        }
    }

    private fun cleanUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        // Strip trailing punctuation often appended by chat or share apps
        url = url.trimEnd('.', ',', '!', '?', ';', ':', ')', ']', '>')
        return url
    }

    enum class Platform(val displayName: String) {
        TIKTOK("TikTok / Douyin"),
        YOUTUBE("YouTube / Shorts"),
        FACEBOOK("Facebook / Reels"),
        INSTAGRAM("Instagram / Reels"),
        TWITTER_X("X (Twitter)"),
        BILIBILI("Bilibili"),
        REDDIT("Reddit"),
        THREADS("Threads"),
        PINTEREST("Pinterest"),
        TWITCH("Twitch"),
        SOUNDCLOUD("SoundCloud"),
        VIMEO("Vimeo"),
        GENERIC("Mọi trang web (Hơn 1800+ nền tảng)")
    }

    fun detectPlatform(url: String): Platform {
        val lower = url.lowercase()
        return when {
            lower.contains("tiktok.com") || lower.contains("douyin.com") -> Platform.TIKTOK
            lower.contains("youtube.com") || lower.contains("youtu.be") -> Platform.YOUTUBE
            lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> Platform.FACEBOOK
            lower.contains("instagram.com") -> Platform.INSTAGRAM
            lower.contains("twitter.com") || lower.contains("x.com") || lower.contains("t.co") -> Platform.TWITTER_X
            lower.contains("bilibili.com") || lower.contains("b23.tv") -> Platform.BILIBILI
            lower.contains("reddit.com") || lower.contains("redd.it") -> Platform.REDDIT
            lower.contains("threads.net") -> Platform.THREADS
            lower.contains("pinterest.com") || lower.contains("pin.it") -> Platform.PINTEREST
            lower.contains("twitch.tv") -> Platform.TWITCH
            lower.contains("soundcloud.com") -> Platform.SOUNDCLOUD
            lower.contains("vimeo.com") -> Platform.VIMEO
            else -> Platform.GENERIC
        }
    }
}
