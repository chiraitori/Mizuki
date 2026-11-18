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
        VIMEO("Vimeo")
    }

    internal fun extractHost(rawUrl: String): String? {
        val trimmed = (extractUrl(rawUrl) ?: rawUrl.trim()).lowercase()
        if (trimmed.isEmpty() || !trimmed.contains(".")) return null
        val withoutScheme = trimmed
            .removePrefix("https://")
            .removePrefix("http://")
        val authority = withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
            .substringBefore(':')
            .trim('.')
        return authority.takeIf { it.contains('.') }
    }

    private fun String.matchesDomain(vararg domains: String): Boolean =
        domains.any { domain -> this == domain || this.endsWith(".$domain") }

    fun detectPlatform(url: String): Platform? {
        val host = extractHost(url) ?: return null
        return when {
            host.matchesDomain("tiktok.com", "douyin.com") -> Platform.TIKTOK
            host.matchesDomain("youtube.com", "youtu.be") -> Platform.YOUTUBE
            host.matchesDomain("facebook.com", "fb.watch", "fb.com") -> Platform.FACEBOOK
            host.matchesDomain("instagram.com") -> Platform.INSTAGRAM
            host.matchesDomain("twitter.com", "x.com", "t.co") -> Platform.TWITTER_X
            host.matchesDomain("bilibili.com", "b23.tv") -> Platform.BILIBILI
            host.matchesDomain("reddit.com", "redd.it") -> Platform.REDDIT
            host.matchesDomain("threads.net", "threads.com") -> Platform.THREADS
            host.matchesDomain("pinterest.com", "pin.it") -> Platform.PINTEREST
            host.matchesDomain("twitch.tv") -> Platform.TWITCH
            host.matchesDomain("soundcloud.com") -> Platform.SOUNDCLOUD
            host.matchesDomain("vimeo.com") -> Platform.VIMEO
            else -> null
        }
    }
}
