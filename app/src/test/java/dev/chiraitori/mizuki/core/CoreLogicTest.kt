package dev.chiraitori.mizuki.core

import dev.chiraitori.mizuki.core.engine.YtDlpWrapper
import dev.chiraitori.mizuki.core.model.DownloadConfig
import dev.chiraitori.mizuki.core.model.DownloadType
import dev.chiraitori.mizuki.core.model.StreamFormat
import dev.chiraitori.mizuki.core.model.VideoDetails
import dev.chiraitori.mizuki.core.model.VideoResolution
import dev.chiraitori.mizuki.core.model.cleanCodec
import dev.chiraitori.mizuki.core.parser.UrlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoreLogicTest {

    @Test
    fun urlParser_extractsAndCleansUrlsFromSharedText() {
        val raw = "Check this out: https://vt.tiktok.com/ZSVnXcVwb/! Amazing"
        assertEquals("https://vt.tiktok.com/ZSVnXcVwb/", UrlParser.extractUrl(raw))

        val bracketed = "Link (https://www.youtube.com/watch?v=dQw4w9WgXcQ)"
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", UrlParser.extractUrl(bracketed))

        assertNull(UrlParser.extractUrl("no url here"))
    }

    @Test
    fun urlParser_detectsMajorPlatforms() {
        assertEquals(UrlParser.Platform.YOUTUBE, UrlParser.detectPlatform("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(UrlParser.Platform.TIKTOK, UrlParser.detectPlatform("https://vt.tiktok.com/ZSVnXcVwb/"))
        assertEquals(UrlParser.Platform.TWITTER_X, UrlParser.detectPlatform("https://x.com/user/status/123"))
        assertEquals(UrlParser.Platform.INSTAGRAM, UrlParser.detectPlatform("https://www.instagram.com/reel/abc/"))
        assertEquals(UrlParser.Platform.REDDIT, UrlParser.detectPlatform("https://www.reddit.com/r/all/comments/abc/"))
        assertEquals(UrlParser.Platform.PINTEREST, UrlParser.detectPlatform("https://www.pinterest.com/pin/123/"))
        assertNull(UrlParser.detectPlatform("https://yandex.com/video/123"))
        assertNull(UrlParser.detectPlatform("https://box.com/s/123"))
        assertNull(UrlParser.detectPlatform("https://example.org/video.mp4"))
    }

    @Test
    fun parseCustomArguments_handlesSimpleQuotedAndEqualsFlags() {
        assertEquals(
            listOf("-f" to "137+140", "--embed-subs" to null),
            YtDlpWrapper.parseCustomArguments("-f 137+140 --embed-subs")
        )

        assertEquals(
            listOf(
                "--downloader" to "libaria2c.so",
                "--downloader-args" to "aria2c:-c -j 32 -x 32 -s 32 -k 1M"
            ),
            YtDlpWrapper.parseCustomArguments(
                "--downloader libaria2c.so --downloader-args aria2c:\"-c -j 32 -x 32 -s 32 -k 1M\""
            )
        )

        assertEquals(
            listOf(
                "--sub-langs" to "vi,en",
                "--geo-bypass" to null,
                "--output" to "%(title)s - test.%(ext)s"
            ),
            YtDlpWrapper.parseCustomArguments(
                "--sub-langs=vi,en --geo-bypass --output \"%(title)s - test.%(ext)s\""
            )
        )
    }

    @Test
    fun cleanCodecAndLabels_formatStreamMetadataReadable() {
        assertEquals("H.264", cleanCodec("avc1.640028"))
        assertEquals("VP9", cleanCodec("vp09.00.40.08"))
        assertEquals("AV1", cleanCodec("av01.0.08M.08"))
        assertEquals("AAC", cleanCodec("mp4a.40.2"))
        assertEquals("Opus", cleanCodec("opus"))

        val uhd = StreamFormat("401", "mp4", width = 3840, height = 2160, fps = 60.0, vcodec = "av01.0.12M.08", isVideoOnly = true)
        assertEquals("4K (2160p) @60fps", uhd.resolutionLabel)
        assertEquals("Video (AV1)", uhd.codecLabel)

        val audio = StreamFormat("251", "webm", acodec = "opus", tbr = 160.4, isAudioOnly = true)
        assertEquals("Audio (~160 kbps)", audio.resolutionLabel)
        assertEquals("Audio (Opus)", audio.codecLabel)
    }

    @Test
    fun resolveConfigForDetails_handlesSyntheticDirectAndYtDlpFormats() {
        val details = VideoDetails(
            id = "123",
            title = "Sample",
            originalUrl = "https://www.tiktok.com/@u/video/123",
            directDownloadUrl = "https://cdn.example.com/video.mp4",
            directAudioUrl = "https://cdn.example.com/audio.mp3",
            formats = listOf(
                StreamFormat("direct_tiktok_hd", "mp4", height = 1080, directUrl = "https://cdn.example.com/video_hd.mp4"),
                StreamFormat("direct_tiktok_audio", "mp3", isAudioOnly = true, directUrl = "https://cdn.example.com/audio.mp3"),
                StreamFormat("137", "mp4", height = 1080, vcodec = "avc1.640028", isVideoOnly = true)
            )
        )

        val syntheticAudio = YtDlpWrapper.resolveConfigForDetails(
            details,
            DownloadConfig(type = DownloadType.AUDIO, selectedFormatId = "direct_tiktok_audio")
        )
        assertNull(syntheticAudio.selectedFormatId)
        assertEquals("https://cdn.example.com/audio.mp3", syntheticAudio.directStreamUrl)

        val realFormat = YtDlpWrapper.resolveConfigForDetails(
            details,
            DownloadConfig(type = DownloadType.VIDEO, selectedFormatId = "137")
        )
        assertEquals("137", realFormat.selectedFormatId)
        assertNull(realFormat.directStreamUrl)

        val resOverride = YtDlpWrapper.resolveConfigForDetails(
            details,
            DownloadConfig(type = DownloadType.VIDEO, videoResolution = VideoResolution.RES_720P)
        )
        assertNull(resOverride.directStreamUrl)

        val defaultAudio = YtDlpWrapper.resolveConfigForDetails(
            details,
            DownloadConfig(type = DownloadType.AUDIO)
        )
        assertEquals("https://cdn.example.com/audio.mp3", defaultAudio.directStreamUrl)
    }
}
