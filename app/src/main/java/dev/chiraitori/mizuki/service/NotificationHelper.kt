package dev.chiraitori.mizuki.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.FileProvider
import dev.chiraitori.mizuki.MainActivity
import java.io.File

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    // New ID resets the immutable channel configuration left by older builds.
    // Flud's working foreground channel uses DEFAULT importance, not HIGH.
    const val CHANNEL_ID = "mizuki_live_download_v2"
    const val CHANNEL_NAME = "Tiến trình tải trực tiếp"
    const val NOTIFICATION_ID = 1001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Tiến trình tải trên Live Update và Hyper Island"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Build the same kind of standard promoted ongoing notification used by Flud.
     */
    fun buildLiveNotification(
        context: Context,
        title: String,
        text: String,
        progress: Int,
        taskId: String = ""
    ): Notification {
        createChannels(context)
        val safeProgress = progress.coerceIn(0, 100)

        val cancelIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_CANCEL_TASK
            putExtra(NotificationReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isProcessing = text.contains("ghép", ignoreCase = true) ||
            text.contains("xử lý", ignoreCase = true)
        val shortCriticalText = when {
            isProcessing -> "Ghép"
            safeProgress > 0 -> "$safeProgress%"
            else -> "0%"
        }

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setColor(0xFF2196F3.toInt())
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        // Add Cancel action
        val cancelAction = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
            "Hủy",
            cancelPendingIntent
        ).build()
        builder.addAction(cancelAction)

        // Keep a standard progress notification. Standard style is eligible for Live
        // Updates and renders correctly on Android versions before 16 as well.
        builder.setProgress(100, safeProgress, safeProgress <= 0)

        // Flud always writes the compatibility extras. HyperOS 3.1 consumes these
        // Android fields directly; no private miui.focus JSON is involved.
        val liveUpdateExtras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
            putString("android.shortCriticalText", shortCriticalText)
        }
        builder.addExtras(liveUpdateExtras)

        // These methods were added in Android 16 QPR1 (API 36.1), not the
        // original API 36 release. Reflection keeps this build safe on 16.0
        // while still opting in through the public methods whenever present.
        if (Build.VERSION.SDK_INT >= 36) {
            runCatching {
                builder.javaClass
                    .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
            }
            runCatching {
                builder.javaClass
                    .getMethod("setShortCriticalText", CharSequence::class.java)
                    .invoke(builder, shortCriticalText)
            }
        }

        return builder.build().also { notification ->
            if (Build.VERSION.SDK_INT >= 36) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val promotable = runCatching {
                    notification.javaClass
                        .getMethod("hasPromotableCharacteristics")
                        .invoke(notification)
                }.getOrNull()
                val allowed = runCatching {
                    manager?.javaClass
                        ?.getMethod("canPostPromotedNotifications")
                        ?.invoke(manager)
                }.getOrNull()
                Log.i(
                    TAG,
                    "Live Update promotable=$promotable, allowed=$allowed, short=$shortCriticalText"
                )
            }
        }
    }

    fun notifyProgress(
        context: Context,
        title: String,
        progress: Int,
        text: String,
        taskId: String = ""
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = buildLiveNotification(context, title, text, progress, taskId)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun finishNotification(
        context: Context,
        title: String,
        filePath: String,
        taskId: String,
        cancelLiveNotification: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (cancelLiveNotification) manager.cancel(NOTIFICATION_ID)

        val completionId = COMPLETION_NOTIFICATION_BASE +
            ((taskId.hashCode() and Int.MAX_VALUE) % COMPLETION_NOTIFICATION_RANGE)

        val file = File(filePath)
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val mime = if (file.extension.equals("mp3", ignoreCase = true) || file.extension.equals("m4a", ignoreCase = true)) "audio/*" else "video/*"
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            completionId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeNotification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("Tải hoàn tất • Bấm để mở media ngay")
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openPendingIntent)
            .build()

        manager.notify(completionId, completeNotification)
    }

    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.cancel(NOTIFICATION_ID)
    }

    private const val COMPLETION_NOTIFICATION_BASE = 2_000
    private const val COMPLETION_NOTIFICATION_RANGE = 1_000_000
}
