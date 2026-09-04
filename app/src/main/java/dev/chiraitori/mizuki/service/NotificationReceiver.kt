package dev.chiraitori.mizuki.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.chiraitori.mizuki.core.engine.DownloaderEngine

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_CANCEL_TASK) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

            if (taskId != null) {
                val engine = DownloaderEngine.getInstance(context.applicationContext)
                engine.cancelTask(taskId)
            }
            NotificationHelper.cancelNotification(context)
        }
    }

    companion object {
        const val ACTION_CANCEL_TASK = "dev.chiraitori.mizuki.ACTION_CANCEL_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
