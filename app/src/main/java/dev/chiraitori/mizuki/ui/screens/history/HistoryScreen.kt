package dev.chiraitori.mizuki.ui.screens.history

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.chiraitori.mizuki.core.model.DownloadType
import dev.chiraitori.mizuki.data.local.DownloadedMedia
import dev.chiraitori.mizuki.data.local.MediaDatabaseHelper
import dev.chiraitori.mizuki.ui.components.bounceClick
import dev.chiraitori.mizuki.ui.components.bounceOnTouch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val dbHelper = remember { MediaDatabaseHelper.getInstance(context) }
    val allMedia by dbHelper.mediaListFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<DownloadType?>(null) }
    var mediaToDelete by remember { mutableStateOf<DownloadedMedia?>(null) }

    val filteredList = allMedia.filter { item ->
        val matchesQuery = searchQuery.isEmpty() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                (item.uploader?.contains(searchQuery, ignoreCase = true) == true)
        val matchesType = selectedFilter == null || item.type == selectedFilter
        matchesQuery && matchesType
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        // App Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
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
                text = stringResource(R.string.nav_library),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm media đã tải...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Xóa")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("Tất cả (${allMedia.size})", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.bounceOnTouch(),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = selectedFilter == DownloadType.VIDEO,
                    onClick = { selectedFilter = if (selectedFilter == DownloadType.VIDEO) null else DownloadType.VIDEO },
                    label = { Text("Video", fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.bounceOnTouch(),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = selectedFilter == DownloadType.AUDIO,
                    onClick = { selectedFilter = if (selectedFilter == DownloadType.AUDIO) null else DownloadType.AUDIO },
                    label = { Text("Audio", fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(Icons.Rounded.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.bounceOnTouch(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (filteredList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (searchQuery.isNotEmpty()) "Không tìm thấy kết quả phù hợp" else "Chưa có file nào trong thư viện",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    HistoryItemCard(
                        media = item,
                        onDelete = { mediaToDelete = item }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    mediaToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { mediaToDelete = null },
            title = { Text("Xóa khỏi thư viện & bộ nhớ?") },
            text = { Text("Hành động này sẽ xóa vĩnh viễn tệp \"${item.title}\" khỏi bộ nhớ máy và thư viện ảnh/video.") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val file = File(item.filePath)
                            if (file.exists()) {
                                file.delete()
                            }

                            // Remove from Android MediaStore indexes
                            try {
                                val resolver = context.contentResolver
                                resolver.delete(
                                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                    "${MediaStore.Video.Media.DATA}=?",
                                    arrayOf(item.filePath)
                                )
                                resolver.delete(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    "${MediaStore.Audio.Media.DATA}=?",
                                    arrayOf(item.filePath)
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    resolver.delete(
                                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                        "${MediaStore.Downloads.DATA}=?",
                                        arrayOf(item.filePath)
                                    )
                                }
                                resolver.delete(
                                    MediaStore.Files.getContentUri("external"),
                                    "${MediaStore.Files.FileColumns.DATA}=?",
                                    arrayOf(item.filePath)
                                )
                            } catch (_: Exception) {}

                            // Notify MediaScanner that file is gone
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(item.filePath),
                                null
                            ) { _, _ -> }

                            dbHelper.deleteMedia(item.id)
                            Toast.makeText(context, "Đã xóa vĩnh viễn tệp", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi khi xóa: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        mediaToDelete = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Xóa vĩnh viễn")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mediaToDelete = null }, shape = RoundedCornerShape(12.dp)) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun HistoryItemCard(
    media: DownloadedMedia,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    fun openMedia() {
        val file = File(media.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "File không tồn tại trên bộ nhớ", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (media.type == DownloadType.AUDIO) "audio/*" else "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareMedia() {
        val file = File(media.filePath)
        if (!file.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (media.type == DownloadType.AUDIO) "audio/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Chia sẻ ${media.title}"))
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(media.downloadedAt) { dateFormat.format(Date(media.downloadedAt)) }
    val formattedSize = remember(media.fileSize) {
        if (media.fileSize > 0) Formatter.formatFileSize(context, media.fileSize) else ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { openMedia() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                media.thumbnailUrl?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = if (media.type == DownloadType.AUDIO) "AUDIO" else "VIDEO HD",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (formattedSize.isNotEmpty()) {
                            Text(
                                text = "• $formattedSize",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "• $formattedDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action strip with full width
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { openMedia() },
                    modifier = Modifier.bounceOnTouch(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Phát media", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = { shareMedia() },
                        modifier = Modifier
                            .bounceOnTouch()
                            .size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Chia sẻ", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .bounceOnTouch()
                            .size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
