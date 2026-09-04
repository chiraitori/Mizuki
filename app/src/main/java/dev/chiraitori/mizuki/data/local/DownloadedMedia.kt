package dev.chiraitori.mizuki.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dev.chiraitori.mizuki.core.model.CommandTemplate
import dev.chiraitori.mizuki.core.model.DownloadType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownloadedMedia(
    val id: String,
    val title: String,
    val uploader: String? = null,
    val duration: Long = 0,
    val thumbnailUrl: String? = null,
    val filePath: String,
    val fileSize: Long = 0,
    val originalUrl: String,
    val type: DownloadType = DownloadType.VIDEO,
    val downloadedAt: Long = System.currentTimeMillis()
)

class MediaDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val _mediaListFlow = MutableStateFlow<List<DownloadedMedia>>(emptyList())
    val mediaListFlow = _mediaListFlow.asStateFlow()

    private val _templatesFlow = MutableStateFlow<List<CommandTemplate>>(emptyList())
    val templatesFlow = _templatesFlow.asStateFlow()

    init {
        refreshFlow()
        refreshTemplates()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createMediaTable = """
            CREATE TABLE $TABLE_MEDIA (
                $COL_MEDIA_ID TEXT PRIMARY KEY,
                $COL_MEDIA_TITLE TEXT NOT NULL,
                $COL_MEDIA_UPLOADER TEXT,
                $COL_MEDIA_DURATION INTEGER DEFAULT 0,
                $COL_MEDIA_THUMBNAIL_URL TEXT,
                $COL_MEDIA_FILE_PATH TEXT NOT NULL,
                $COL_MEDIA_FILE_SIZE INTEGER DEFAULT 0,
                $COL_MEDIA_ORIGINAL_URL TEXT NOT NULL,
                $COL_MEDIA_TYPE TEXT NOT NULL,
                $COL_MEDIA_DOWNLOADED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createTemplatesTable = """
            CREATE TABLE $TABLE_TEMPLATES (
                $COL_TMPL_ID TEXT PRIMARY KEY,
                $COL_TMPL_NAME TEXT NOT NULL,
                $COL_TMPL_DESC TEXT,
                $COL_TMPL_ARGS TEXT NOT NULL,
                $COL_TMPL_BUILTIN INTEGER DEFAULT 0
            )
        """.trimIndent()

        db.execSQL(createMediaTable)
        db.execSQL(createTemplatesTable)

        insertBuiltInTemplates(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MEDIA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEMPLATES")
        onCreate(db)
    }

    private fun insertBuiltInTemplates(db: SQLiteDatabase) {
        val builtIns = listOf(
            CommandTemplate("tmpl_tiktok_best", "TikTok Gốc Không Watermark", "Bóc tách stream chất lượng tối đa", "-f b/bv*+ba --no-check-certificates", true),
            CommandTemplate("tmpl_yt_sponsorblock", "YouTube + SponsorBlock", "Tự động cắt bỏ đoạn quảng cáo, tài trợ", "--sponsorblock-remove sponsor,selfpromo,intro,outro", true),
            CommandTemplate("tmpl_audio_flac", "Audio Lossless FLAC", "Tách âm thanh định dạng Lossless", "-x --audio-format flac --audio-quality 0", true),
            CommandTemplate("tmpl_fast_aria2c", "Tối đa tốc độ Aria2c (32 Luồng)", "Tăng tốc tải 32 kết nối", "--downloader libaria2c.so --downloader-args aria2c:\"-c -j 32 -x 32 -s 32 -k 1M\"", true)
        )

        builtIns.forEach { tmpl ->
            val values = ContentValues().apply {
                put(COL_TMPL_ID, tmpl.id)
                put(COL_TMPL_NAME, tmpl.name)
                put(COL_TMPL_DESC, tmpl.description)
                put(COL_TMPL_ARGS, tmpl.customArgs)
                put(COL_TMPL_BUILTIN, if (tmpl.isBuiltIn) 1 else 0)
            }
            db.insert(TABLE_TEMPLATES, null, values)
        }
    }

    private fun refreshFlow() {
        _mediaListFlow.value = getAllMedia()
    }

    private fun refreshTemplates() {
        _templatesFlow.value = getAllTemplates()
    }

    // --- Media Operations ---
    fun insertOrUpdate(media: DownloadedMedia) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MEDIA_ID, media.id)
            put(COL_MEDIA_TITLE, media.title)
            put(COL_MEDIA_UPLOADER, media.uploader)
            put(COL_MEDIA_DURATION, media.duration)
            put(COL_MEDIA_THUMBNAIL_URL, media.thumbnailUrl)
            put(COL_MEDIA_FILE_PATH, media.filePath)
            put(COL_MEDIA_FILE_SIZE, media.fileSize)
            put(COL_MEDIA_ORIGINAL_URL, media.originalUrl)
            put(COL_MEDIA_TYPE, media.type.name)
            put(COL_MEDIA_DOWNLOADED_AT, media.downloadedAt)
        }
        db.insertWithOnConflict(TABLE_MEDIA, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshFlow()
    }

    fun getAllMedia(): List<DownloadedMedia> {
        val list = mutableListOf<DownloadedMedia>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MEDIA,
            null,
            null,
            null,
            null,
            null,
            "$COL_MEDIA_DOWNLOADED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val item = DownloadedMedia(
                    id = it.getString(it.getColumnIndexOrThrow(COL_MEDIA_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_MEDIA_TITLE)),
                    uploader = it.getString(it.getColumnIndexOrThrow(COL_MEDIA_UPLOADER)),
                    duration = it.getLong(it.getColumnIndexOrThrow(COL_MEDIA_DURATION)),
                    thumbnailUrl = it.getString(it.getColumnIndexOrThrow(COL_MEDIA_THUMBNAIL_URL)),
                    filePath = it.getString(it.getColumnIndexOrThrow(COL_MEDIA_FILE_PATH)),
                    fileSize = it.getLong(it.getColumnIndexOrThrow(COL_MEDIA_FILE_SIZE)),
                    originalUrl = it.getString(it.getColumnIndexOrThrow(COL_MEDIA_ORIGINAL_URL)),
                    type = try {
                        DownloadType.valueOf(it.getString(it.getColumnIndexOrThrow(COL_MEDIA_TYPE)))
                    } catch (_: Exception) {
                        DownloadType.VIDEO
                    },
                    downloadedAt = it.getLong(it.getColumnIndexOrThrow(COL_MEDIA_DOWNLOADED_AT))
                )
                list.add(item)
            }
        }
        return list
    }

    fun deleteMedia(id: String): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_MEDIA, "$COL_MEDIA_ID = ?", arrayOf(id))
        refreshFlow()
        return rows > 0
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_MEDIA, null, null)
        refreshFlow()
    }

    // --- Command Template Operations ---
    fun getAllTemplates(): List<CommandTemplate> {
        val list = mutableListOf<CommandTemplate>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TEMPLATES, null, null, null, null, null, "$COL_TMPL_BUILTIN DESC, $COL_TMPL_NAME ASC")
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    CommandTemplate(
                        id = it.getString(it.getColumnIndexOrThrow(COL_TMPL_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_TMPL_NAME)),
                        description = it.getString(it.getColumnIndexOrThrow(COL_TMPL_DESC)) ?: "",
                        customArgs = it.getString(it.getColumnIndexOrThrow(COL_TMPL_ARGS)),
                        isBuiltIn = it.getInt(it.getColumnIndexOrThrow(COL_TMPL_BUILTIN)) == 1
                    )
                )
            }
        }
        return list
    }

    fun saveTemplate(template: CommandTemplate) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TMPL_ID, template.id)
            put(COL_TMPL_NAME, template.name)
            put(COL_TMPL_DESC, template.description)
            put(COL_TMPL_ARGS, template.customArgs)
            put(COL_TMPL_BUILTIN, if (template.isBuiltIn) 1 else 0)
        }
        db.insertWithOnConflict(TABLE_TEMPLATES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshTemplates()
    }

    fun deleteTemplate(id: String): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_TEMPLATES, "$COL_TMPL_ID = ? AND $COL_TMPL_BUILTIN = 0", arrayOf(id))
        refreshTemplates()
        return rows > 0
    }

    companion object {
        private const val DATABASE_NAME = "mizuki_media.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_MEDIA = "downloaded_media"
        private const val COL_MEDIA_ID = "id"
        private const val COL_MEDIA_TITLE = "title"
        private const val COL_MEDIA_UPLOADER = "uploader"
        private const val COL_MEDIA_DURATION = "duration"
        private const val COL_MEDIA_THUMBNAIL_URL = "thumbnail_url"
        private const val COL_MEDIA_FILE_PATH = "file_path"
        private const val COL_MEDIA_FILE_SIZE = "file_size"
        private const val COL_MEDIA_ORIGINAL_URL = "original_url"
        private const val COL_MEDIA_TYPE = "type"
        private const val COL_MEDIA_DOWNLOADED_AT = "downloaded_at"

        private const val TABLE_TEMPLATES = "command_templates"
        private const val COL_TMPL_ID = "id"
        private const val COL_TMPL_NAME = "name"
        private const val COL_TMPL_DESC = "description"
        private const val COL_TMPL_ARGS = "custom_args"
        private const val COL_TMPL_BUILTIN = "is_builtin"

        @Volatile
        private var INSTANCE: MediaDatabaseHelper? = null

        fun getInstance(context: Context): MediaDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaDatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
