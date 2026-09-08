package dev.chiraitori.mizuki

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import dev.chiraitori.mizuki.service.NotificationHelper
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CancellationException

class MizukiApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var engineInitialization: Deferred<Result<Unit>>

    suspend fun awaitEngines() {
        engineInitialization.await().getOrThrow()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        NotificationHelper.createChannels(this)
        initEngines()
    }

    private fun initEngines() {
        engineInitialization = applicationScope.async {
            try {
                YoutubeDL.getInstance().init(this@MizukiApplication)
                FFmpeg.getInstance().init(this@MizukiApplication)
                Aria2c.getInstance().init(this@MizukiApplication)
                Log.d(TAG, "YoutubeDL, FFmpeg & Aria2c initialized successfully")
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LinkageError) {
                Log.e(TAG, "Engine runtime could not be initialized", e)
                Result.failure(IllegalStateException("Không thể khởi tạo bộ tải. Vui lòng cập nhật ứng dụng.", e))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize YoutubeDL / FFmpeg / Aria2c", e)
                Result.failure(IllegalStateException("Không thể khởi tạo bộ tải: ${e.message}", e))
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DOWNLOAD,
                "Tiến trình Tải & Live Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Hiển thị tiến trình tải video Live Activity trong thời gian thực"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "MizukiApp"
        const val CHANNEL_DOWNLOAD = "mizuki_download_channel"

        lateinit var instance: MizukiApplication
            private set
    }
}
