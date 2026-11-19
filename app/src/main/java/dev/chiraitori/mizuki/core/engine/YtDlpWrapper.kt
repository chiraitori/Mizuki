package dev.chiraitori.mizuki.core.engine

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import dev.chiraitori.mizuki.MizukiApplication
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.chiraitori.mizuki.core.cookies.CookieManager
import dev.chiraitori.mizuki.core.model.AudioFormat
import dev.chiraitori.mizuki.core.model.DownloadConfig
import dev.chiraitori.mizuki.core.model.DownloadType
import dev.chiraitori.mizuki.core.model.StreamFormat
import dev.chiraitori.mizuki.core.model.VideoDetails
import dev.chiraitori.mizuki.core.model.VideoResolution
import dev.chiraitori.mizuki.core.parser.UrlParser
import dev.chiraitori.mizuki.data.repository.AppPreferences
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

object YtDlpWrapper {

    private const val TAG = "YtDlpWrapper"
    private val canceledProcessIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun getDefaultDownloadDir(): File {
        val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val mizukiDir = File(publicDownloadDir, "Mizuki")
        if (!mizukiDir.exists()) {
            mizukiDir.mkdirs()
        }
        return mizukiDir
    }

    private fun getTempDownloadDir(context: Context): File {
        val tempDir = File(context.cacheDir, "mizuki_temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }

    suspend fun fetchVideoDetails(url: String): Result<VideoDetails> = withContext(Dispatchers.IO) {
        val platform = UrlParser.detectPlatform(url)

        // Tier 1: Fast Direct API for TikTok, Twitter/X, Instagram, Reddit
        val directResult = DirectFastExtractor.tryDirectExtract(url)
        if (directResult != null && directResult.isSuccess) {
            return@withContext directResult
        }

        // Tier 2: yt-dlp Native Engine Extraction (Seal architecture)
        try {
            val appContext = MizukiApplication.instance
            appContext.awaitEngines()
            val globalPrefs = SettingsRepository.getInstance(appContext).appPrefsFlow.value
            val userAgent = globalPrefs.customUserAgent.ifBlank {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            }
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("--geo-bypass")
                addOption("-R", "2")
                addOption("--socket-timeout", "10")
                addOption("--add-header", "User-Agent:$userAgent")
                addOption("--add-header", "Accept-Language:en-US,en;q=0.9,vi;q=0.8")
                resolveCookieFile(appContext, globalPrefs.downloadConfig.cookiesFilePath)?.let {
                    addOption("--cookies", it.absolutePath)
                }
                if (globalPrefs.forceIpv4) addOption("-4")
                if (globalPrefs.proxyUrl.isNotBlank()) addOption("--proxy", globalPrefs.proxyUrl)
                if (globalPrefs.extractorArgs.isNotBlank()) addOption("--extractor-args", globalPrefs.extractorArgs)
            }

            val videoInfo = YoutubeDL.getInstance().getInfo(request)

            val formats = videoInfo.formats?.mapNotNull { f ->
                val formatId = f.formatId ?: return@mapNotNull null
                val ext = (f.ext ?: "").lowercase()

                // 1. Exclude storyboard thumbnail sheets and manifest junk
                if (ext == "mhtml" || ext == "json" || formatId.startsWith("sb", ignoreCase = true)) {
                    return@mapNotNull null
                }
                if (f.url?.contains("mhtml", ignoreCase = true) == true) {
                    return@mapNotNull null
                }

                val rawVcodec = f.vcodec?.takeIf { it != "none" && it.isNotBlank() }
                val rawAcodec = f.acodec?.takeIf { it != "none" && it.isNotBlank() }

                // Non-media tracks (neither video nor audio codec is present)
                if (rawVcodec == null && rawAcodec == null) {
                    return@mapNotNull null
                }

                // Audio-only track: Valid audio codec and no video codec (or height == 0)
                val isAudioOnly = (rawVcodec == null || (f.height ?: 0) <= 0) && rawAcodec != null

                // Video-only track: Valid video codec and no audio codec
                val isVideoOnly = rawVcodec != null && rawAcodec == null

                // Both: Valid video and audio
                val hasBoth = rawVcodec != null && rawAcodec != null

                // Filter out non-audio streams that have no valid dimensions
                if (!isAudioOnly && (f.height ?: 0) <= 0 && (f.width ?: 0) <= 0) {
                    return@mapNotNull null
                }

                // Filter out micro storyboard images (< 144p) that yt-dlp mislabeled as video
                if (isVideoOnly && (f.height ?: 0) < 144 && (f.width ?: 0) < 144) {
                    return@mapNotNull null
                }

                StreamFormat(
                    formatId = formatId,
                    ext = ext.ifBlank { "mp4" },
                    formatNote = f.formatNote,
                    width = f.width ?: 0,
                    height = f.height ?: 0,
                    fps = f.fps?.toDouble() ?: 30.0,
                    vcodec = rawVcodec,
                    acodec = rawAcodec,
                    fileSize = f.fileSize ?: 0L,
                    tbr = f.tbr?.toDouble() ?: 0.0,
                    isVideoOnly = isVideoOnly,
                    isAudioOnly = isAudioOnly,
                    hasBoth = hasBoth,
                    directUrl = f.url
                )
            }?.sortedWith(
                compareByDescending<StreamFormat> { !it.isAudioOnly } // Videos first, Audio after
                    .thenByDescending { it.height }                    // Highest resolution first
                    .thenByDescending { it.tbr }                       // Highest bitrate
                    .thenByDescending { it.fps }                       // Highest fps
            ) ?: emptyList()

            val details = VideoDetails(
                id = videoInfo.id ?: System.currentTimeMillis().toString(),
                title = videoInfo.title ?: "Media_${System.currentTimeMillis()}",
                author = videoInfo.uploader ?: platform?.displayName ?: "Web",
                thumbnailUrl = videoInfo.thumbnail,
                originalUrl = url,
                webpageUrl = videoInfo.webpageUrl ?: url,
                duration = videoInfo.duration?.toLong() ?: 0L,
                formats = formats,
                isPlaylist = false
            )

            Result.success(details)
        } catch (e: Exception) {
            Log.e(TAG, "fetchVideoDetails failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun executeDownload(
        context: Context,
        url: String,
        title: String,
        config: DownloadConfig,
        processId: String,
        onProgress: (
            progress: Float,
            speed: String,
            eta: String,
            line: String,
            isProcessing: Boolean
        ) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        canceledProcessIds.remove(processId)
        try {
            try {
                MizukiApplication.instance.awaitEngines()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }

            val globalPrefs = SettingsRepository.getInstance(context).appPrefsFlow.value
            val outputDir = if (!config.customOutDir.isNullOrEmpty()) {
                File(config.customOutDir).also { if (!it.exists()) it.mkdirs() }
            } else if (config.type == DownloadType.AUDIO && !globalPrefs.separateAudioDir.isNullOrEmpty()) {
                File(globalPrefs.separateAudioDir).also { if (!it.exists()) it.mkdirs() }
            } else {
                getDefaultDownloadDir()
            }
            val tempDir = getTempDownloadDir(context)

            // A private per-task prefix prevents concurrent downloads from claiming
            // each other's newest file. It is removed after yt-dlp fully finishes.
            val outputPrefix = ".mizuki_${processId.replace(Regex("[^A-Za-z0-9_-]"), "_")}."
            val configuredTemplate = config.outputTemplate
                .ifBlank { globalPrefs.downloadConfig.outputTemplate }
                .ifBlank { "%(title).200B.%(ext)s" }

            var resolvedDirectUrl = config.directStreamUrl?.takeIf { it.isNotBlank() }
            if (resolvedDirectUrl == null &&
                UrlParser.detectPlatform(url) == UrlParser.Platform.TIKTOK &&
                config.type != DownloadType.CUSTOM &&
                config.selectedFormatId.isNullOrBlank()
            ) {
                val details = DirectFastExtractor.tryDirectExtract(url)?.getOrNull()
                if (details != null) {
                    resolvedDirectUrl = resolveConfigForDetails(details, config).directStreamUrl
                }
            }

            val initialDownloadUrl = resolvedDirectUrl ?: url
            val initialIsDirectStream = resolvedDirectUrl != null

            val firstResult = runDownloadAttempt(
                context = context,
                downloadUrl = initialDownloadUrl,
                isDirectStream = initialIsDirectStream,
                useAria2cForAttempt = config.useAria2c,
                title = title,
                config = config,
                globalPrefs = globalPrefs,
                outputDir = outputDir,
                tempDir = tempDir,
                outputPrefix = outputPrefix,
                configuredTemplate = configuredTemplate,
                processId = processId,
                onProgress = onProgress
            )

            if (firstResult.isSuccess || canceledProcessIds.contains(processId) || !coroutineContext.isActive) {
                return@withContext firstResult
            }

            if (initialIsDirectStream) {
                if (UrlParser.detectPlatform(url) == UrlParser.Platform.TIKTOK) {
                    Log.w(
                        TAG,
                        "Direct TikTok download failed (${firstResult.exceptionOrNull()?.message}), retrying with standard stream and native downloader"
                    )
                    val fallbackDetails = DirectFastExtractor.tryDirectExtract(url, preferStandardPlay = true)?.getOrNull()
                    if (canceledProcessIds.contains(processId) || !coroutineContext.isActive) {
                        return@withContext firstResult
                    }
                    val fallbackUrl = when {
                        fallbackDetails == null -> initialDownloadUrl
                        config.type == DownloadType.AUDIO ->
                            fallbackDetails.directAudioUrl?.takeIf { it.isNotBlank() } ?: fallbackDetails.directDownloadUrl
                        else -> fallbackDetails.directDownloadUrl
                    }?.takeIf { it.isNotBlank() }

                    if (fallbackUrl != null && (fallbackUrl != initialDownloadUrl || config.useAria2c)) {
                        onProgress(0f, "", "", "[Mizuki] Đang thử lại bằng luồng dự phòng...", false)
                        return@withContext runDownloadAttempt(
                            context = context,
                            downloadUrl = fallbackUrl,
                            isDirectStream = true,
                            useAria2cForAttempt = false,
                            title = title,
                            config = config,
                            globalPrefs = globalPrefs,
                            outputDir = outputDir,
                            tempDir = tempDir,
                            outputPrefix = outputPrefix,
                            configuredTemplate = configuredTemplate,
                            processId = processId,
                            onProgress = onProgress
                        )
                    }
                } else if (config.useAria2c) {
                    Log.w(TAG, "Direct download with aria2c failed, retrying with native downloader")
                    onProgress(0f, "", "", "[Mizuki] Đang thử lại bằng trình tải mặc định...", false)
                    return@withContext runDownloadAttempt(
                        context = context,
                        downloadUrl = initialDownloadUrl,
                        isDirectStream = true,
                        useAria2cForAttempt = false,
                        title = title,
                        config = config,
                        globalPrefs = globalPrefs,
                        outputDir = outputDir,
                        tempDir = tempDir,
                        outputPrefix = outputPrefix,
                        configuredTemplate = configuredTemplate,
                        processId = processId,
                        onProgress = onProgress
                    )
                }
            }

            firstResult
        } finally {
            canceledProcessIds.remove(processId)
        }
    }

    private fun runDownloadAttempt(
        context: Context,
        downloadUrl: String,
        isDirectStream: Boolean,
        useAria2cForAttempt: Boolean,
        title: String,
        config: DownloadConfig,
        globalPrefs: AppPreferences,
        outputDir: File,
        tempDir: File,
        outputPrefix: String,
        configuredTemplate: String,
        processId: String,
        onProgress: (
            progress: Float,
            speed: String,
            eta: String,
            line: String,
            isProcessing: Boolean
        ) -> Unit
    ): Result<File> {
        val outputTemplate = buildOutputTemplate(configuredTemplate, outputPrefix, title, isDirectStream)

        try {
            val request = YoutubeDLRequest(downloadUrl).apply {
                // Core options
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("--no-mtime")
                addOption("--geo-bypass")
                addOption("--compat-options", "no-attach-info-json")
                val userAgent = globalPrefs.customUserAgent.ifBlank {
                    "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/128.0.0.0 Mobile Safari/537.36"
                }
                addOption("--add-header", "User-Agent:$userAgent")
                addOption("--add-header", "Accept-Language:en-US,en;q=0.9,vi;q=0.8")

                // Seal-style Isolated Paths
                addOption("-P", outputDir.absolutePath)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addOption("-P", "temp:${tempDir.absolutePath}")
                }
                addOption("-o", outputTemplate)

                // Format & Stream Selection (Following Seal's exact pipeline)
                if (!config.selectedFormatId.isNullOrEmpty()) {
                    val formatArg = if (config.type == DownloadType.VIDEO && !config.selectedFormatId.contains("+")) {
                        "${config.selectedFormatId}+ba/${config.selectedFormatId}"
                    } else {
                        config.selectedFormatId
                    }
                    addOption("-f", formatArg)
                    if (config.type == DownloadType.VIDEO) {
                        addOption(
                            "--merge-output-format",
                            if (globalPrefs.videoContainer == "ANY") "mp4/mkv" else globalPrefs.videoContainer.lowercase()
                        )
                    }
                    if (config.embedMetadata) {
                        addOption("--add-metadata")
                    }
                    if (!isDirectStream) {
                        applyThumbnailAndCrop(config, globalPrefs, isAudioMode = config.type == DownloadType.AUDIO)
                    }
                } else {
                    when (config.type) {
                        DownloadType.IMAGE -> error("Image downloads use TikTokPhotoDownloader")
                        DownloadType.AUDIO -> {
                            addOption("-x")
                            addOption("--audio-format", if (globalPrefs.keepOriginalAudio) "best" else config.audioFormat.ext)
                            if (!globalPrefs.keepOriginalAudio) {
                                addOption("--audio-quality", globalPrefs.audioBitrate.uppercase())
                            }
                            if (config.embedMetadata) {
                                addOption("--add-metadata")
                                if (!isDirectStream) {
                                    addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")
                                    addOption("--parse-metadata", "%(release_year,upload_date)s:%(meta_date)s")
                                }
                            }
                            if (!isDirectStream) {
                                applyThumbnailAndCrop(config, globalPrefs, isAudioMode = true)
                            }
                        }
                        DownloadType.CUSTOM -> {
                            // Custom mode - relies on customArgs
                        }
                        DownloadType.VIDEO -> {
                            if (!isDirectStream) {
                                val resSorter = if (config.videoResolution.height > 0) "res:${config.videoResolution.height}" else ""
                                val codecSorter = when (globalPrefs.videoCodec) {
                                    "H264" -> "vcodec:h264"
                                    "VP9" -> "vcodec:vp9"
                                    "AV1" -> "vcodec:av01"
                                    else -> ""
                                }
                                if (resSorter.isNotEmpty() || globalPrefs.videoCodec != "ANY") {
                                    addOption("-S", listOf(resSorter, codecSorter, "ext").filter { it.isNotEmpty() }.joinToString(","))
                                }

                                addOption("-f", "bv*+ba/b")
                            }
                            addOption(
                                "--merge-output-format",
                                if (globalPrefs.videoContainer == "ANY") "mp4/mkv" else globalPrefs.videoContainer.lowercase()
                            )

                            if (config.embedMetadata) {
                                addOption("--add-metadata")
                                addOption("--no-embed-info-json")
                            }
                            if (!isDirectStream) {
                                applyThumbnailAndCrop(config, globalPrefs, isAudioMode = false)
                            }
                        }
                    }
                }

                // Video Section Trimming / Clipping
                if (!config.clipSectionStart.isNullOrBlank() || !config.clipSectionEnd.isNullOrBlank()) {
                    val start = config.clipSectionStart?.ifBlank { "00:00:00" } ?: "00:00:00"
                    val end = config.clipSectionEnd?.ifBlank { "inf" } ?: "inf"
                    addOption("--download-sections", "*$start-$end")
                    addOption("--force-keyframes-at-cuts")
                }

                // SponsorBlock integration
                if (!isDirectStream && (config.removeSponsorSegments || globalPrefs.downloadConfig.removeSponsorSegments)) {
                    val categories = if (config.sponsorBlockCategories.isNotBlank()) config.sponsorBlockCategories else globalPrefs.sponsorBlockCategories
                    addOption("--sponsorblock-remove", categories.ifBlank { "sponsor,selfpromo,intro,outro" })
                }

                // Subtitles (Embed or External Files)
                if (!isDirectStream && config.embedSubtitles) {
                    addOption("--embed-subs")
                }
                if (!isDirectStream && (config.embedSubtitles || config.keepSubtitleFiles || globalPrefs.keepSubtitleFiles)) {
                    addOption("--write-subs")
                    if (globalPrefs.autoGeneratedSubs) addOption("--write-auto-subs")
                    addOption("--sub-langs", globalPrefs.subLangs.ifBlank { "all" })
                    addOption("--convert-subs", globalPrefs.subFormat)
                }

                // Restrict Filenames (Seal)
                if (config.restrictFilenames || globalPrefs.restrictFilenames) {
                    addOption("--restrict-filenames")
                }

                // Download Archive (Seal)
                if (!isDirectStream && (config.useDownloadArchive || globalPrefs.useDownloadArchive)) {
                    val archiveFile = File(context.filesDir, "download_archive.txt")
                    addOption("--download-archive", archiveFile.absolutePath)
                }

                // Cookies support
                if (!isDirectStream) {
                    resolveCookieFile(context, config.cookiesFilePath ?: globalPrefs.downloadConfig.cookiesFilePath)?.let {
                        addOption("--cookies", it.absolutePath)
                    }
                }

                if (globalPrefs.forceIpv4) addOption("-4")
                if (globalPrefs.proxyUrl.isNotBlank()) addOption("--proxy", globalPrefs.proxyUrl)
                if (!isDirectStream && globalPrefs.extractorArgs.isNotBlank()) {
                    addOption("--extractor-args", globalPrefs.extractorArgs)
                }
                if (globalPrefs.rateLimit.isNotBlank()) addOption("--limit-rate", globalPrefs.rateLimit)
                if (globalPrefs.overwriteExisting) addOption("--force-overwrites") else addOption("--no-overwrites")
                if (globalPrefs.verboseLogging) addOption("--verbose")

                val retries = if (globalPrefs.autoRetry) globalPrefs.retryCount.coerceIn(1, 20) else 0
                addOption("--retries", retries.toString())
                addOption("--fragment-retries", retries.toString())

                // Download acceleration (Seal-style protocol scoping).
                // aria2c does not provide yt-dlp's normal per-fragment callback for DASH/HLS
                // streams (the usual YouTube video/audio formats), so applying it globally
                // leaves the UI without usable progress. Use it only for direct HTTP/FTP files
                // and let yt-dlp download fragmented streams with its native progress reporter.
                if (useAria2cForAttempt) {
                    try {
                        addOption("--downloader", "http,https,ftp,ftps:libaria2c.so")
                        addOption(
                            "--external-downloader-args",
                            "aria2c:-x ${config.aria2cConnections.coerceIn(1, 32)} -s ${config.aria2cConnections.coerceIn(1, 32)} -k 1M --file-allocation=none --max-tries=5 --retry-wait=2 --console-log-level=warn"
                        )
                        addOption("--concurrent-fragments", "4")
                    } catch (e: Exception) {
                        Log.w(TAG, "Aria2c setup failed, fallback to native yt-dlp downloader", e)
                        addOption("--concurrent-fragments", "4")
                    }
                } else {
                    addOption("--concurrent-fragments", "4")
                }

                // Custom CLI Arguments tokenizer
                if (config.customArgs.isNotBlank()) {
                    parseCustomArguments(config.customArgs).forEach { (opt, value) ->
                        if (value != null) {
                            addOption(opt, value)
                        } else {
                            addOption(opt)
                        }
                    }
                }
            }

            val percentRegex = Regex("""(\d+\.?\d*)\s*%""")
            val aria2cPercentRegex = Regex("""\((\d+)%\)""")
            val speedRegex = Regex("""(?:at|DL:)\s*([0-9.]+\s*[A-Za-z]+(?:/s)?)""")
            val etaRegex = Regex("""ETA:?\s*([0-9A-Za-z:]+)""")

            var currentPercent = 0f
            var currentSpeed = ""
            var currentEta = ""
            var isPostProcessing = false

            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                Log.d(TAG, "[CALLBACK] progress=$progress, eta=$etaInSeconds, line=$line")

                if (line.isPostProcessingLine()) {
                    isPostProcessing = true
                }

                // 1. Progress Percentage
                // yt-dlp reports the percentage of the stream currently being downloaded.
                // Do not manufacture a percentage for FFmpeg post-processing: there is no
                // byte-based progress available for that operation.
                if (!isPostProcessing) {
                    if (progress > 0f) {
                        currentPercent = progress
                    } else {
                        val pMatch = percentRegex.find(line)
                        if (pMatch != null) {
                            val pVal = pMatch.groupValues[1].toFloatOrNull()
                            if (pVal != null) currentPercent = pVal
                        } else {
                            val aMatch = aria2cPercentRegex.find(line)
                            if (aMatch != null) {
                                val aVal = aMatch.groupValues[1].toFloatOrNull()
                                if (aVal != null) currentPercent = aVal
                            }
                        }
                    }

                    // Keep 100% exclusively for a completed task. A video can still need
                    // another stream or a merge after one stream reaches 100%.
                    currentPercent = currentPercent.coerceIn(0f, 99f)
                }

                // 2. Download Speed
                val sMatch = speedRegex.find(line)
                if (sMatch != null) {
                    currentSpeed = sMatch.groupValues[1].trim()
                }

                // 3. Estimated Time Remaining (ETA)
                if (etaInSeconds > 0) {
                    currentEta = "${etaInSeconds}s"
                } else {
                    val eMatch = etaRegex.find(line)
                    if (eMatch != null) {
                        currentEta = eMatch.groupValues[1].trim()
                    }
                }

                // 4. Post-processing / Merge Phases
                if (isPostProcessing) {
                    currentSpeed = "Đang ghép luồng & xử lý file..."
                    currentEta = ""
                }

                Log.d(TAG, "[PARSED] percent=$currentPercent, speed=$currentSpeed, eta=$currentEta, processing=$isPostProcessing")
                onProgress(currentPercent, currentSpeed, currentEta, line, isPostProcessing)
            }

            val completedFile = findTaskOutput(outputDir, outputPrefix)
            return if (completedFile != null) {
                Result.success(removeTaskPrefix(completedFile, outputPrefix, globalPrefs.overwriteExisting))
            } else {
                Result.failure(IllegalStateException("yt-dlp kết thúc nhưng không tạo được file cho '$title'"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "YoutubeDL.execute caught exception: ${e.message}, checking this task's output...")

            // Some extractors return a harmless warning after the final file was
            // moved. Only accept the file explicitly reported/prefixed by this task.
            val completedFile = findTaskOutput(outputDir, outputPrefix)
            if (completedFile != null) {
                val finalFile = removeTaskPrefix(completedFile, outputPrefix, globalPrefs.overwriteExisting)
                Log.i(TAG, "Completed file found despite exception: ${finalFile.name}")
                return Result.success(finalFile)
            }

            Log.e(TAG, "Download execution truly failed: ${e.message}", e)
            return Result.failure(e)
        }
    }

    internal fun buildOutputTemplate(
        configuredTemplate: String,
        outputPrefix: String,
        title: String,
        isDirectStream: Boolean
    ): String {
        if (!isDirectStream) return addTaskPrefix(configuredTemplate, outputPrefix)
        val safeTitle = title
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.')
            .take(120)
            .ifBlank { "Media" }
        val escapedTitle = safeTitle.replace("%", "%%")
        val separatorIndex = maxOf(configuredTemplate.lastIndexOf('/'), configuredTemplate.lastIndexOf('\\'))
        val rawDirPrefix = if (separatorIndex >= 0) configuredTemplate.substring(0, separatorIndex + 1) else ""
        val staticDirPrefix = if (rawDirPrefix.contains("%(")) "" else rawDirPrefix
        return "${staticDirPrefix}${outputPrefix}${escapedTitle}.%(ext)s"
    }

    private fun findTaskOutput(
        outputDir: File,
        outputPrefix: String
    ): File? {
        return outputDir.walkTopDown()
            .maxDepth(4)
            .filter { it.name.startsWith(outputPrefix) && it.isCompletedMediaFile() }
            .maxByOrNull { it.lastModified() }
    }

    private fun File.isCompletedMediaFile(): Boolean =
        isFile && exists() && length() > 1_024L &&
            !name.endsWith(".part", ignoreCase = true) &&
            !name.endsWith(".ytdl", ignoreCase = true) &&
            !name.endsWith(".temp", ignoreCase = true)

    private fun removeTaskPrefix(file: File, outputPrefix: String, overwriteExisting: Boolean): File {
        if (!file.name.startsWith(outputPrefix)) return file

        val cleanName = file.name.removePrefix(outputPrefix).ifBlank { file.name }
        val parent = file.parentFile ?: return file
        val exactFile = File(parent, cleanName)
        if (overwriteExisting && exactFile.exists() && exactFile != file) {
            exactFile.delete()
        }
        val desiredFile = if (overwriteExisting) exactFile else uniqueFile(parent, cleanName)
        return if (file.renameTo(desiredFile)) desiredFile else file
    }

    private fun addTaskPrefix(template: String, outputPrefix: String): String {
        val separatorIndex = maxOf(template.lastIndexOf('/'), template.lastIndexOf('\\'))
        return if (separatorIndex >= 0) {
            template.substring(0, separatorIndex + 1) + outputPrefix + template.substring(separatorIndex + 1)
        } else {
            outputPrefix + template
        }
    }

    private fun uniqueFile(parent: File, fileName: String): File {
        val firstChoice = File(parent, fileName)
        if (!firstChoice.exists()) return firstChoice

        val extension = firstChoice.extension
        val baseName = firstChoice.nameWithoutExtension
        var index = 1
        while (true) {
            val suffix = if (extension.isEmpty()) " ($index)" else " ($index).$extension"
            val candidate = File(parent, "$baseName$suffix")
            if (!candidate.exists()) return candidate
            index++
        }
    }

    private fun resolveCookieFile(context: Context, configuredPath: String?): File? {
        val explicit = configuredPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() && it.length() > 0 }
        if (explicit != null) return explicit
        return CookieManager.getCookieFile(context).takeIf { it.exists() && it.length() > 0 }
    }

    private fun YoutubeDLRequest.applyThumbnailAndCrop(
        config: DownloadConfig,
        globalPrefs: AppPreferences,
        isAudioMode: Boolean
    ) {
        if (!config.embedThumbnail) return
        addOption("--embed-thumbnail")
        addOption("--convert-thumbnails", "jpg")
        if (isAudioMode && (config.cropArtworkSquare || globalPrefs.downloadConfig.cropArtworkSquare)) {
            addOption("--ppa", "EmbedThumbnail+ffmpeg_o:-c:v mjpeg -vf crop='min(iw,ih):min(iw,ih)'")
        }
    }

    private val SYNTHETIC_DIRECT_PREFIXES = listOf("direct_", "twitter_", "instagram_", "reddit_")

    internal fun resolveConfigForDetails(details: VideoDetails, config: DownloadConfig): DownloadConfig {
        if (!config.selectedFormatId.isNullOrEmpty()) {
            val matchedFormat = details.formats.find { it.formatId == config.selectedFormatId }
            val isSyntheticDirectFormat = matchedFormat != null &&
                !matchedFormat.directUrl.isNullOrBlank() &&
                SYNTHETIC_DIRECT_PREFIXES.any { matchedFormat.formatId.startsWith(it) }
            if (isSyntheticDirectFormat) {
                return config.copy(
                    selectedFormatId = null,
                    directStreamUrl = matchedFormat!!.directUrl
                )
            }
            return config.copy(directStreamUrl = null)
        }

        if (config.type == DownloadType.CUSTOM) {
            return config.copy(directStreamUrl = null)
        }

        val directUrl = if (config.type == DownloadType.AUDIO) {
            details.directAudioUrl?.takeIf { it.isNotBlank() }
                ?: details.directDownloadUrl?.takeIf { it.isNotBlank() }
        } else {
            val sdUrl = if (config.videoResolution in listOf(VideoResolution.RES_720P, VideoResolution.RES_480P, VideoResolution.RES_360P)) {
                details.formats.find { it.formatId == "direct_tiktok_sd" && !it.directUrl.isNullOrBlank() }?.directUrl
            } else {
                null
            }
            sdUrl ?: details.directDownloadUrl?.takeIf { it.isNotBlank() }
        } ?: return config.copy(directStreamUrl = null)

        val isTikTok = UrlParser.detectPlatform(details.originalUrl) == UrlParser.Platform.TIKTOK
        val hasRealYtDlpFormats = details.formats.any { fmt ->
            SYNTHETIC_DIRECT_PREFIXES.none { fmt.formatId.startsWith(it) }
        }
        val requiresYtDlpExtraction = !isTikTok && hasRealYtDlpFormats && (
            (config.type == DownloadType.VIDEO && config.videoResolution != VideoResolution.BEST) ||
                config.embedSubtitles ||
                config.removeSponsorSegments ||
                !config.clipSectionStart.isNullOrBlank() ||
                !config.clipSectionEnd.isNullOrBlank()
            )

        if (requiresYtDlpExtraction) {
            return config.copy(directStreamUrl = null)
        }

        return config.copy(directStreamUrl = directUrl)
    }

    private data class CliToken(val text: String, val startedWithQuote: Boolean)

    private fun tokenizeCliArgs(args: String): List<CliToken> {
        val tokens = mutableListOf<CliToken>()
        val current = StringBuilder()
        var activeQuote: Char? = null
        var hasTokenContent = false
        var startedWithQuote = false

        var i = 0
        while (i < args.length) {
            val ch = args[i]
            when {
                activeQuote != null -> {
                    if (ch == '\\' && i + 1 < args.length && args[i + 1] == activeQuote) {
                        current.append(args[i + 1])
                        i += 2
                        continue
                    } else if (ch == activeQuote) {
                        activeQuote = null
                    } else {
                        current.append(ch)
                    }
                }
                ch == '"' || ch == '\'' -> {
                    if (!hasTokenContent) startedWithQuote = true
                    activeQuote = ch
                    hasTokenContent = true
                }
                ch.isWhitespace() -> {
                    if (hasTokenContent) {
                        tokens.add(CliToken(current.toString(), startedWithQuote))
                        current.clear()
                        hasTokenContent = false
                        startedWithQuote = false
                    }
                }
                else -> {
                    current.append(ch)
                    hasTokenContent = true
                }
            }
            i++
        }
        if (hasTokenContent) {
            tokens.add(CliToken(current.toString(), startedWithQuote))
        }
        return tokens
    }

    internal fun parseCustomArguments(args: String): List<Pair<String, String?>> {
        if (args.isBlank()) return emptyList()
        val result = mutableListOf<Pair<String, String?>>()
        val tokens = tokenizeCliArgs(args)
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (!token.startedWithQuote && token.text.startsWith("-")) {
                if (token.text.startsWith("--") && token.text.contains("=")) {
                    val eqIdx = token.text.indexOf('=')
                    result.add(token.text.substring(0, eqIdx) to token.text.substring(eqIdx + 1))
                    i += 1
                } else if (i + 1 < tokens.size && (tokens[i + 1].startedWithQuote || !tokens[i + 1].text.startsWith("-"))) {
                    result.add(token.text to tokens[i + 1].text)
                    i += 2
                } else {
                    result.add(token.text to null)
                    i += 1
                }
            } else {
                i += 1
            }
        }
        return result
    }

    private fun String.isPostProcessingLine(): Boolean =
        contains("[Merger]") ||
            contains("[ExtractAudio]") ||
            contains("[Fixup]") ||
            contains("[Metadata]") ||
            contains("[VideoRemuxer]") ||
            contains("[FFmpeg]")

    fun cancel(processId: String) {
        canceledProcessIds.add(processId)
        try {
            YoutubeDL.getInstance().destroyProcessById(processId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel process: $processId", e)
        }
    }

    suspend fun updateEngine(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            MizukiApplication.instance.awaitEngines()
            val channelPref = SettingsRepository.getInstance(context).appPrefsFlow.value.updateChannel
            val channel = when (channelPref.uppercase()) {
                "NIGHTLY" -> YoutubeDL.UpdateChannel._NIGHTLY
                "MASTER" -> YoutubeDL.UpdateChannel._MASTER
                else -> YoutubeDL.UpdateChannel._STABLE
            }
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, channel)
            Result.success(status.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update yt-dlp: ${e.message}", e)
            Result.failure(e)
        }
    }
}
