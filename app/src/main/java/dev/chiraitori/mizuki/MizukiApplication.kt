package dev.chiraitori.mizuki

import android.app.Application
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dev.chiraitori.mizuki.service.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

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

    companion object {
        const val TAG = "MizukiApp"

        lateinit var instance: MizukiApplication
            private set
    }
}
