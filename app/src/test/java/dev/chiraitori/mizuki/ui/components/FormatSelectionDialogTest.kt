package dev.chiraitori.mizuki.ui.components

import dev.chiraitori.mizuki.core.model.StreamFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FormatSelectionDialogTest {
    @Test
    fun usefulFormats_removeStoryboardsInvalidAudioAndDuplicateQualities() {
        val formats = listOf(
            StreamFormat("storyboard", "mhtml", width = 48, height = 27, vcodec = "images", isVideoOnly = true),
            StreamFormat("tiny", "mp4", width = 80, height = 45, vcodec = "h264", isVideoOnly = true),
            StreamFormat("1080-low", "mp4", width = 1920, height = 1080, fps = 30.0, vcodec = "h264", tbr = 1500.0, isVideoOnly = true),
            StreamFormat("1080-best", "mp4", width = 1920, height = 1080, fps = 60.0, vcodec = "vp9", tbr = 2500.0, isVideoOnly = true),
            StreamFormat("720", "mp4", width = 1280, height = 720, vcodec = "h264", acodec = "aac", hasBoth = true),
            StreamFormat("audio-null", "mp4", acodec = null, isAudioOnly = true),
            StreamFormat("opus-low", "webm", acodec = "opus", tbr = 64.0, isAudioOnly = true),
            StreamFormat("opus-best", "webm", acodec = "opus", tbr = 128.0, isAudioOnly = true)
        )

        val result = selectUsefulStreamFormats(formats)

        assertEquals(listOf("1080-best", "720", "opus-best"), result.map { it.formatId })
        assertFalse(result.any { it.ext.equals("mhtml", ignoreCase = true) })
    }
}
