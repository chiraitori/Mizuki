package dev.chiraitori.mizuki.core.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import dev.chiraitori.mizuki.core.engine.YtDlpWrapper
import dev.chiraitori.mizuki.core.model.DownloadType
import dev.chiraitori.mizuki.data.local.DownloadedMedia
import dev.chiraitori.mizuki.data.local.MediaDatabaseHelper
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {
    private const val TAG = "BackupManager"

    suspend fun exportBackup(context: Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dbHelper = MediaDatabaseHelper.getInstance(context)
            val settingsRepo = SettingsRepository.getInstance(context)

            val mediaList = dbHelper.getAllMedia()
            val config = settingsRepo.loadConfig()
            val templates = dbHelper.getAllTemplates()

            val rootJson = JSONObject()
            rootJson.put("version", 1)
            rootJson.put("timestamp", System.currentTimeMillis())

            // Settings JSON
            val configJson = JSONObject().apply {
                put("videoResolution", config.videoResolution.name)
                put("audioFormat", config.audioFormat.name)
                put("useAria2c", config.useAria2c)
                put("aria2cConnections", config.aria2cConnections)
                put("embedThumbnail", config.embedThumbnail)
                put("embedSubtitles", config.embedSubtitles)
                put("embedMetadata", config.embedMetadata)
                put("wifiOnly", config.wifiOnly)
            }
            rootJson.put("settings", configJson)

            // Media items JSON
            val mediaArray = JSONArray()
            mediaList.forEach { m ->
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("title", m.title)
                    put("uploader", m.uploader)
                    put("duration", m.duration)
                    put("thumbnailUrl", m.thumbnailUrl)
                    put("filePath", m.filePath)
                    put("fileSize", m.fileSize)
                    put("originalUrl", m.originalUrl)
                    put("type", m.type.name)
                    put("downloadedAt", m.downloadedAt)
                }
                mediaArray.put(obj)
            }
            rootJson.put("media", mediaArray)

            // Templates JSON
            val templatesArray = JSONArray()
            templates.forEach { t ->
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("description", t.description)
                    put("customArgs", t.customArgs)
                    put("isBuiltIn", t.isBuiltIn)
                }
                templatesArray.put(obj)
            }
            rootJson.put("templates", templatesArray)

            val outDir = YtDlpWrapper.getDefaultDownloadDir()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "Mizuki_Backup_${dateFormat.format(Date())}.json"
            val backupFile = File(outDir, fileName)

            backupFile.writeText(rootJson.toString(2))
            Result.success(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Export backup failed", e)
            Result.failure(e)
        }
    }

    suspend fun importBackup(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Không thể đọc file backup"))

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val rootJson = JSONObject(jsonString)

            val dbHelper = MediaDatabaseHelper.getInstance(context)
            var restoredCount = 0

            if (rootJson.has("media")) {
                val mediaArray = rootJson.getJSONArray("media")
                for (i in 0 until mediaArray.length()) {
                    val obj = mediaArray.getJSONObject(i)
                    val media = DownloadedMedia(
                        id = obj.optString("id", "${System.currentTimeMillis()}_$i"),
                        title = obj.getString("title"),
                        uploader = if (obj.has("uploader") && !obj.isNull("uploader")) obj.getString("uploader") else null,
                        duration = obj.optLong("duration", 0),
                        thumbnailUrl = if (obj.has("thumbnailUrl") && !obj.isNull("thumbnailUrl")) obj.getString("thumbnailUrl") else null,
                        filePath = obj.getString("filePath"),
                        fileSize = obj.optLong("fileSize", 0),
                        originalUrl = obj.getString("originalUrl"),
                        type = try {
                            DownloadType.valueOf(obj.optString("type", DownloadType.VIDEO.name))
                        } catch (_: Exception) {
                            DownloadType.VIDEO
                        },
                        downloadedAt = obj.optLong("downloadedAt", System.currentTimeMillis())
                    )
                    dbHelper.insertOrUpdate(media)
                    restoredCount++
                }
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Log.e(TAG, "Import backup failed", e)
            Result.failure(e)
        }
    }
}
