package dev.chiraitori.mizuki.ui.screens.home

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.chiraitori.mizuki.R
import dev.chiraitori.mizuki.core.engine.DownloaderEngine
import dev.chiraitori.mizuki.core.engine.YtDlpWrapper
import dev.chiraitori.mizuki.core.model.DownloadConfig
import dev.chiraitori.mizuki.core.model.DownloadTask
import dev.chiraitori.mizuki.core.model.DownloadType
import dev.chiraitori.mizuki.core.model.PlaylistItem
import dev.chiraitori.mizuki.core.model.TaskStatus
import dev.chiraitori.mizuki.core.model.VideoDetails
import dev.chiraitori.mizuki.core.parser.UrlParser
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import dev.chiraitori.mizuki.ui.components.FormatSelectionDialog
import dev.chiraitori.mizuki.ui.components.PlaylistSelectionDialog
import dev.chiraitori.mizuki.ui.components.TaskLogDialog
import dev.chiraitori.mizuki.ui.components.bounceClick
import dev.chiraitori.mizuki.ui.components.bounceOnTouch
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialUrl: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloaderEngine = remember { DownloaderEngine.getInstance(context) }
    val settingsRepo = remember { SettingsRepository.getInstance(context) }

    var inputUrl by remember { mutableStateOf(initialUrl ?: "") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var videoDetails by remember { mutableStateOf<VideoDetails?>(null) }
    var analyzeError by remember { mutableStateOf<String?>(null) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var taskForLogs by remember { mutableStateOf<DownloadTask?>(null) }

    val activeTasks by downloaderEngine.tasks.collectAsState()
    val savedConfig by settingsRepo.configFlow.collectAsState()

    val detectedPlatform = remember(inputUrl) {
        if (inputUrl.isNotBlank()) UrlParser.detectPlatform(inputUrl) else null
    }

    fun analyze(urlToAnalyze: String) {
        val extracted = UrlParser.extractUrl(urlToAnalyze) ?: urlToAnalyze.trim()
        if (extracted.isEmpty()) {
            Toast.makeText(context, "Vui lòng nhập hoặc dán link video", Toast.LENGTH_SHORT).show()
            return
        }

        inputUrl = extracted
        scope.launch {
            isAnalyzing = true
            analyzeError = null
            videoDetails = null

            val result = YtDlpWrapper.fetchVideoDetails(extracted)
            result.onSuccess {
                videoDetails = it
                if (it.isPlaylist && it.playlistItems.isNotEmpty()) {
                    showPlaylistDialog = true
                }
            }.onFailure {
                analyzeError = it.message ?: "Không thể lấy thông tin video"
            }
            isAnalyzing = false
        }
    }

    fun pasteClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            val extracted = UrlParser.extractUrl(text) ?: text
            if (extracted.isNotEmpty()) {
                inputUrl = extracted
                analyze(extracted)
            } else {
                Toast.makeText(context, "Không tìm thấy link trong bộ nhớ tạm", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startDownloadWithConfig(config: DownloadConfig) {
        val details = videoDetails ?: return
        val directUrl = if (config.type == DownloadType.AUDIO && !details.directAudioUrl.isNullOrEmpty()) {
            details.directAudioUrl
        } else {
            details.directDownloadUrl
        }
        val finalConfig = config.copy(directStreamUrl = directUrl)

        downloaderEngine.enqueueTask(
            url = details.originalUrl,
            title = details.title,
            author = details.author,
            thumbUrl = details.thumbnailUrl,
            duration = details.duration,
            config = finalConfig
        )
        Toast.makeText(context, "Đã thêm vào hàng đợi tải", Toast.LENGTH_SHORT).show()
        videoDetails = null
        inputUrl = ""
    }

    fun batchDownloadPlaylist(items: List<PlaylistItem>) {
        items.forEach { item ->
            downloaderEngine.enqueueTask(
                url = item.url,
                title = item.title,
                author = item.author,
                thumbUrl = item.thumbnailUrl,
                duration = item.duration,
                config = savedConfig
            )
        }
        Toast.makeText(context, "Đã thêm ${items.size} video vào hàng đợi tải!", Toast.LENGTH_SHORT).show()
        videoDetails = null
        inputUrl = ""
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrEmpty()) {
            analyze(initialUrl)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Row
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mizuki_avatar),
                    contentDescription = "Mizuki",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Smart URL Input Card (Material 3 Expressive)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (detectedPlatform != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(detectedPlatform.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        placeholder = { Text("Dán link video TikTok, YouTube, Reels...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (inputUrl.isNotEmpty()) {
                                IconButton(onClick = { inputUrl = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Xóa")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { pasteClipboard() },
                            modifier = Modifier
                                .bounceOnTouch()
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dán link", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { analyze(inputUrl) },
                            modifier = Modifier
                                .bounceOnTouch()
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quét link", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Analyzing State
        if (isAnalyzing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Đang bóc tách thông tin stream video...",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Video Details Preview Card
        videoDetails?.let { details ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column {
                        details.thumbnailUrl?.let { thumb ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            ) {
                                AsyncImage(
                                    model = thumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (details.duration > 0) {
                                    val mins = details.duration / 60
                                    val secs = details.duration % 60
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = String.format("%02d:%02d", mins, secs),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = details.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            details.author?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "@$it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { startDownloadWithConfig(savedConfig.copy(type = DownloadType.VIDEO)) },
                                    modifier = Modifier
                                        .bounceOnTouch()
                                        .weight(1f)
                                        .height(48.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Video", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                }

                                FilledTonalButton(
                                    onClick = { startDownloadWithConfig(savedConfig.copy(type = DownloadType.AUDIO)) },
                                    modifier = Modifier
                                        .bounceOnTouch()
                                        .weight(1f)
                                        .height(48.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Audio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                }

                                OutlinedButton(
                                    onClick = { showFormatDialog = true },
                                    modifier = Modifier
                                        .bounceOnTouch()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = "Tùy chỉnh")
                                }

                                if (details.isPlaylist && details.playlistItems.isNotEmpty()) {
                                    FilledTonalButton(
                                        onClick = { showPlaylistDialog = true },
                                        modifier = Modifier
                                            .bounceOnTouch()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = "Playlist")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Tasks Sections
        val inProgressTasks = activeTasks.filter {
            it.status == TaskStatus.DOWNLOADING || it.status == TaskStatus.PROCESSING || it.status == TaskStatus.IDLE
        }
        val finishedTasks = activeTasks.filter {
            it.status == TaskStatus.COMPLETED || it.status == TaskStatus.FAILED || it.status == TaskStatus.CANCELED
        }

        // 1. In-Progress Downloads
        if (inProgressTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đang tải (${inProgressTasks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(inProgressTasks, key = { it.id }) { task ->
                ActiveTaskItemCard(
                    task = task,
                    onCancel = { downloaderEngine.cancelTask(task.id) },
                    onRetry = { downloaderEngine.retryTask(task.id) },
                    onDismiss = { downloaderEngine.removeTask(task.id) },
                    onShowLogs = { taskForLogs = task }
                )
            }
        }

        // 2. Finished Downloads (with quick Clear/Dọn dẹp action)
        if (finishedTasks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vừa hoàn thành (${finishedTasks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { downloaderEngine.clearFinishedTasks() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ClearAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dọn dẹp", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items(finishedTasks, key = { it.id }) { task ->
                ActiveTaskItemCard(
                    task = task,
                    onCancel = { downloaderEngine.cancelTask(task.id) },
                    onRetry = { downloaderEngine.retryTask(task.id) },
                    onDismiss = { downloaderEngine.removeTask(task.id) },
                    onShowLogs = { taskForLogs = task }
                )
            }
        }
    }

    if (showFormatDialog && videoDetails != null) {
        FormatSelectionDialog(
            videoDetails = videoDetails!!,
            initialConfig = savedConfig,
            onDismiss = { showFormatDialog = false },
            onConfirm = { customConfig ->
                showFormatDialog = false
                startDownloadWithConfig(customConfig)
            }
        )
    }

    if (showPlaylistDialog && videoDetails != null) {
        PlaylistSelectionDialog(
            playlistDetails = videoDetails!!,
            onDismiss = { showPlaylistDialog = false },
            onConfirm = { selectedItems ->
                showPlaylistDialog = false
                batchDownloadPlaylist(selectedItems)
            }
        )
    }

    taskForLogs?.let { task ->
        TaskLogDialog(
            task = task,
            onDismiss = { taskForLogs = null }
        )
    }
}

@Composable
fun ActiveTaskItemCard(
    task: DownloadTask,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onShowLogs: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                task.thumbnailUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = task.title.ifEmpty { task.url },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (task.status) {
                            TaskStatus.IDLE -> "Đang chờ trong hàng đợi..."
                            TaskStatus.DOWNLOADING -> {
                                if (task.progress > 0f) {
                                    "${task.progress.toInt()}% ${if (task.speed.isNotEmpty()) "• ${task.speed}" else ""} ${if (task.eta.isNotEmpty()) "• Còn ${task.eta}" else ""}"
                                } else {
                                    if (task.speed.isNotEmpty()) task.speed else "Đang khởi tạo kết nối stream..."
                                }
                            }
                            TaskStatus.PROCESSING -> "Đang ghép luồng & xử lý file..."
                            TaskStatus.COMPLETED -> "Đã tải xong"
                            TaskStatus.FAILED -> "Lỗi: ${task.errorMessage ?: "Thất bại"}"
                            TaskStatus.CANCELED -> "Đã hủy"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (task.status) {
                            TaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            TaskStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onShowLogs,
                    modifier = Modifier.bounceOnTouch()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = "Xem Log",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                when (task.status) {
                    TaskStatus.DOWNLOADING, TaskStatus.IDLE, TaskStatus.PROCESSING -> {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.bounceOnTouch()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Hủy", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TaskStatus.FAILED, TaskStatus.CANCELED -> {
                        IconButton(
                            onClick = onRetry,
                            modifier = Modifier.bounceOnTouch()
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Thử lại")
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.bounceOnTouch()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TaskStatus.COMPLETED -> {
                        task.filePath?.let { path ->
                            FilledTonalIconButton(
                                onClick = {
                                    val file = File(path)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, if (file.extension == "mp3") "audio/*" else "video/*")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier
                                    .bounceOnTouch()
                                    .size(38.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Phát", modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.bounceOnTouch()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {}
                }
            }

            if (task.status == TaskStatus.DOWNLOADING || task.status == TaskStatus.PROCESSING) {
                Spacer(modifier = Modifier.height(10.dp))
                if (task.status == TaskStatus.PROCESSING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                } else if (task.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { (task.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}
