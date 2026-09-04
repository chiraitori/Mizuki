package dev.chiraitori.mizuki.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

class DownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Đang tải media..."
        val statusText = intent?.getStringExtra(EXTRA_STATUS) ?: "Đang kết nối luồng tải..."
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID) ?: ""

        val notification = NotificationHelper.buildLiveNotification(
            context = this,
            title = title,
            text = statusText,
            progress = progress,
            taskId = taskId
        )

        // Match livetest exactly: specialUse on API 34+, bare on older
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
            startForeground(NotificationHelper.NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        private const val TAG = "DownloadService"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun start(
            context: Context,
            title: String = "Đang tải media...",
            statusText: String = "Đang kết nối luồng tải...",
            progress: Int = 0,
            taskId: String = ""
        ) {
            try {
                val intent = Intent(context, DownloadService::class.java).apply {
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_STATUS, statusText)
                    putExtra(EXTRA_PROGRESS, progress)
                    putExtra(EXTRA_TASK_ID, taskId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start DownloadService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DownloadService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop DownloadService: ${e.message}")
            }
        }
    }
}
