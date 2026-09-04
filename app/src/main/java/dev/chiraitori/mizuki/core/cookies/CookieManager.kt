package dev.chiraitori.mizuki.core.cookies

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object CookieManager {
    private const val TAG = "CookieManager"
    private const val COOKIE_FILE_NAME = "cookies.txt"

    fun getCookieDir(context: Context): File {
        val dir = File(context.filesDir, "cookies")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getCookieFile(context: Context): File {
        return File(getCookieDir(context), COOKIE_FILE_NAME)
    }

    fun hasCookies(context: Context): Boolean {
        val file = getCookieFile(context)
        return file.exists() && file.length() > 0
    }

    suspend fun importCookies(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Không thể mở file cookies"))

            val targetFile = getCookieFile(context)
            FileOutputStream(targetFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            val linesCount = targetFile.readLines().count { line ->
                line.isNotBlank() && !line.startsWith("#")
            }

            Result.success(linesCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import cookies", e)
            Result.failure(e)
        }
    }

    suspend fun saveCookiesText(context: Context, text: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val targetFile = getCookieFile(context)
            targetFile.writeText(text)
            val linesCount = targetFile.readLines().count { line ->
                line.isNotBlank() && !line.startsWith("#")
            }
            Result.success(linesCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cookies text", e)
            Result.failure(e)
        }
    }

    fun clearCookies(context: Context): Boolean {
        val file = getCookieFile(context)
        return if (file.exists()) file.delete() else true
    }

    fun getCookieSummary(context: Context): String {
        val file = getCookieFile(context)
        if (!file.exists() || file.length() == 0L) {
            return "Chưa có file cookies"
        }
        val linesCount = file.readLines().count { line -> line.isNotBlank() && !line.startsWith("#") }
        return "Đã nạp $linesCount cookies (${file.length() / 1024} KB)"
    }
}
