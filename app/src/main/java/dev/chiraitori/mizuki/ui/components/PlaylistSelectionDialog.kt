package dev.chiraitori.mizuki.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.chiraitori.mizuki.core.model.PlaylistItem
import dev.chiraitori.mizuki.core.model.VideoDetails

@Composable
fun PlaylistSelectionDialog(
    playlistDetails: VideoDetails,
    onDismiss: () -> Unit,
    onConfirm: (List<PlaylistItem>) -> Unit
) {
    val selectionMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            playlistDetails.playlistItems.forEach { put(it.id, true) }
        }
    }

    val selectedCount = selectionMap.values.count { it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Danh sách phát (Playlist)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${playlistDetails.title} • $selectedCount/${playlistDetails.playlistItems.size} video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        playlistDetails.playlistItems.forEach { selectionMap[it.id] = true }
                    }) {
                        Text("Chọn tất cả")
                    }

                    TextButton(onClick = {
                        playlistDetails.playlistItems.forEach { selectionMap[it.id] = false }
                    }) {
                        Text("Bỏ chọn tất cả")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlistDetails.playlistItems, key = { it.id }) { item ->
                        val isSelected = selectionMap[item.id] ?: false

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectionMap[item.id] = !isSelected },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { selectionMap[item.id] = it }
                                )

                                item.thumbnailUrl?.let { thumb ->
                                    AsyncImage(
                                        model = thumb,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.index}. ${item.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.duration > 0) {
                                        val mins = item.duration / 60
                                        val secs = item.duration % 60
                                        Text(
                                            text = String.format("%02d:%02d", mins, secs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedItems = playlistDetails.playlistItems.filter { selectionMap[it.id] == true }
                    onConfirm(selectedItems)
                },
                enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tải $selectedCount video")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
