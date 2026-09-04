package dev.chiraitori.mizuki.core.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dev.chiraitori.mizuki.core.model.DownloadConfig
import dev.chiraitori.mizuki.core.model.DownloadTask
import dev.chiraitori.mizuki.core.model.TaskStatus
import dev.chiraitori.mizuki.data.local.DownloadedMedia
import dev.chiraitori.mizuki.data.local.MediaDatabaseHelper
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import dev.chiraitori.mizuki.service.DownloadService
import dev.chiraitori.mizuki.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.UUID

class DownloaderEngine private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dbHelper = MediaDatabaseHelper.getInstance(appContext)

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val concurrencySemaphore = Semaphore(
        SettingsRepository.getInstance(appContext).appPrefsFlow.value.maxConcurrentDownloads.coerceIn(1, 5)
    )

    init {
        NotificationHelper.createChannels(appContext)
    }

    fun enqueueTask(
        url: String,
        title: String,
        author: String? = null,
        thumbUrl: String? = null,
        duration: Long = 0,
        config: DownloadConfig
    ): String {
        val taskId = "task_${UUID.randomUUID()}"
        val task = DownloadTask(
            id = taskId,
            url = url,
            title = title,
            author = author,
            thumbnailUrl = thumbUrl,
            duration = duration,
            status = TaskStatus.IDLE,
            config = config
        )

        _tasks.update { it + task }

        scope.launch {
            processTask(taskId)
        }

        return taskId
    }

    private fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private suspend fun processTask(taskId: String) {
        concurrencySemaphore.withPermit {
            val currentTask = _tasks.value.find { it.id == taskId } ?: return@withPermit
            if (currentTask.status == TaskStatus.CANCELED) return@withPermit

            // Check Wi-Fi only restriction
            if (currentTask.config.wifiOnly && !isWifiConnected()) {
                updateTask(taskId) {
                    it.copy(
                        status = TaskStatus.FAILED,
                        errorMessage = "Đang bật chế độ 'Chỉ tải qua Wi-Fi', vui lòng kết nối Wi-Fi để tiếp tục"
                    )
                }
                return@withPermit
            }

            updateTask(taskId) { it.copy(status = TaskStatus.DOWNLOADING, progress = 0f, rawLogs = listOf("[Mizuki] Khởi chạy tác vụ tải...")) }

            // Ensure Foreground Service is active with actual Live Notification
            DownloadService.start(
                context = appContext,
                title = currentTask.title,
                statusText = "Đang kết nối luồng tải...",
                progress = 0,
                taskId = taskId
            )

            val logBuffer = mutableListOf<String>()
            var lastNotificationUpdateAt = 0L
            var lastNotificationProgress = -1
            var lastNotificationMerging = false

            val result = YtDlpWrapper.executeDownload(
                context = appContext,
                url = currentTask.url,
                title = currentTask.title,
                config = currentTask.config,
                processId = taskId
            ) progressCallback@ { progress, speed, eta, line, isProcessingStream ->
                if (_tasks.value.find { it.id == taskId }?.status == TaskStatus.CANCELED) {
                    return@progressCallback
                }
                logBuffer.add(line)
                if (logBuffer.size > 200) logBuffer.removeAt(0)

                val isMerging = isProcessingStream || line.contains("[Merger]") || line.contains("[ExtractAudio]") || line.contains("[Fixup]") || line.contains("[Metadata]")

                updateTask(taskId) {
                    it.copy(
                        progress = progress,
                        speed = speed,
                        eta = eta,
                        status = if (isMerging) TaskStatus.PROCESSING else TaskStatus.DOWNLOADING,
                        rawLogs = logBuffer.toList()
                    )
                }

                val statusText = when {
                    isMerging -> "Đang ghép luồng video & audio..."
                    progress > 0f && speed.isNotEmpty() -> "${progress.toInt()}% • $speed ${if (eta.isNotEmpty()) "• Còn $eta" else ""}"
                    line.contains("[download]") -> line.replace("[download]", "").trim()
                    else -> if (progress > 0) "${progress.toInt()}% đang tải..." else "Đang bóc tách luồng media..."
                }

                // Flud does not post on every engine callback. Mizuki previously flooded
                // NotificationManager and HyperOS recorded thousands of rate violations.
                val now = System.currentTimeMillis()
                val notificationProgress = if (isMerging) 0 else progress.toInt()
                val phaseChanged = isMerging != lastNotificationMerging
                val progressChanged = notificationProgress != lastNotificationProgress
                if (phaseChanged || (progressChanged && now - lastNotificationUpdateAt >= 1_000L)) {
                    lastNotificationUpdateAt = now
                    lastNotificationProgress = notificationProgress
                    lastNotificationMerging = isMerging
                    NotificationHelper.notifyProgress(
                        context = appContext,
                        title = currentTask.title,
                        progress = notificationProgress,
                        text = statusText,
                        taskId = taskId
                    )
                }
            }

            // Cancellation destroys yt-dlp, which normally surfaces as an exception.
            // Keep the user-selected CANCELED state instead of rewriting it to FAILED.
            if (_tasks.value.find { it.id == taskId }?.status == TaskStatus.CANCELED) {
                stopServiceIfIdle()
                return@withPermit
            }

            result.onSuccess { file ->
                logBuffer.add("[Mizuki] Tải và xử lý hoàn tất thành công: ${file.name}")
                updateTask(taskId) {
                    it.copy(
                        status = TaskStatus.COMPLETED,
                        progress = 100f,
                        filePath = file.absolutePath,
                        speed = "",
                        eta = "",
                        rawLogs = logBuffer.toList()
                    )
                }

                val nextActiveTask = firstActiveTask()

                // Complete this task without taking another concurrent task's
                // Live Activity down with it.
                NotificationHelper.finishNotification(
                    context = appContext,
                    title = currentTask.title,
                    filePath = file.absolutePath,
                    taskId = taskId,
                    cancelLiveNotification = nextActiveTask == null
                )
                nextActiveTask?.let(::publishLiveNotification)

                // Scan MediaStore for Gallery visibility
                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(file.absolutePath),
                    null
                ) { path, uri ->
                    Log.d(TAG, "Scanned $path -> $uri")
                }

                // Insert into persistent database (Skip if Private Mode is enabled)
                val isPrivate = currentTask.config.privateMode || SettingsRepository.getInstance(appContext).appPrefsFlow.value.privateMode
                if (!isPrivate) {
                    val media = DownloadedMedia(
                        id = taskId,
                        title = currentTask.title,
                        uploader = currentTask.author,
                        duration = currentTask.duration,
                        thumbnailUrl = currentTask.thumbnailUrl,
                        filePath = file.absolutePath,
                        fileSize = if (file.exists()) file.length() else 0,
                        originalUrl = currentTask.url,
                        type = currentTask.config.type
                    )
                    dbHelper.insertOrUpdate(media)
                }
            }.onFailure { err ->
                logBuffer.add("[Mizuki Lỗi] ${err.message}")
                updateTask(taskId) {
                    it.copy(
                        status = TaskStatus.FAILED,
                        errorMessage = err.message ?: "Tải thất bại",
                        rawLogs = logBuffer.toList()
                    )
                }
                val nextActiveTask = firstActiveTask()
                if (nextActiveTask == null) {
                    NotificationHelper.cancelNotification(appContext)
                } else {
                    publishLiveNotification(nextActiveTask)
                }
            }

            stopServiceIfIdle()
        }
    }

    fun cancelTask(taskId: String) {
        YtDlpWrapper.cancel(taskId)
        updateTask(taskId) { it.copy(status = TaskStatus.CANCELED, speed = "", eta = "") }
        val nextActiveTask = firstActiveTask()
        if (nextActiveTask == null) {
            NotificationHelper.cancelNotification(appContext)
            DownloadService.stop(appContext)
        } else {
            publishLiveNotification(nextActiveTask)
        }
    }

    fun retryTask(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        updateTask(taskId) { it.copy(status = TaskStatus.IDLE, progress = 0f, errorMessage = null, rawLogs = emptyList()) }
        scope.launch {
            processTask(taskId)
        }
    }

    fun removeTask(taskId: String) {
        val task = _tasks.value.find { it.id == taskId }
        if (task != null && (task.status == TaskStatus.DOWNLOADING || task.status == TaskStatus.PROCESSING)) {
            cancelTask(taskId)
        }
        _tasks.update { tasks -> tasks.filter { it.id != taskId } }
    }

    fun clearFinishedTasks() {
        _tasks.update { tasks ->
            tasks.filter {
                it.status == TaskStatus.DOWNLOADING ||
                it.status == TaskStatus.PROCESSING ||
                it.status == TaskStatus.IDLE
            }
        }
    }

    private fun updateTask(taskId: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { tasks ->
            tasks.map { task ->
                if (task.id == taskId) transform(task) else task
            }
        }
    }

    private fun firstActiveTask(): DownloadTask? = _tasks.value.firstOrNull {
        it.status == TaskStatus.DOWNLOADING || it.status == TaskStatus.PROCESSING
    }

    private fun publishLiveNotification(task: DownloadTask) {
        val isProcessing = task.status == TaskStatus.PROCESSING
        val progress = if (isProcessing) 0 else task.progress.toInt().coerceIn(0, 99)
        val text = when {
            isProcessing -> "Đang ghép luồng video & audio..."
            task.speed.isNotBlank() -> "${progress}% • ${task.speed}${if (task.eta.isNotBlank()) " • Còn ${task.eta}" else ""}"
            else -> "${progress}% đang tải..."
        }
        NotificationHelper.notifyProgress(appContext, task.title, progress, text, task.id)
    }

    private fun stopServiceIfIdle() {
        if (firstActiveTask() == null) DownloadService.stop(appContext)
    }

    companion object {
        private const val TAG = "DownloaderEngine"
        @Volatile
        private var INSTANCE: DownloaderEngine? = null

        fun getInstance(context: Context): DownloaderEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloaderEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
