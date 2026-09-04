package dev.chiraitori.mizuki.ui.components

import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CropOriginal
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.mizuki.core.model.AudioFormat
import dev.chiraitori.mizuki.core.model.DownloadConfig
import dev.chiraitori.mizuki.core.model.DownloadType
import dev.chiraitori.mizuki.core.model.StreamFormat
import dev.chiraitori.mizuki.core.model.VideoDetails
import dev.chiraitori.mizuki.core.model.VideoResolution

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormatSelectionDialog(
    videoDetails: VideoDetails,
    initialConfig: DownloadConfig = DownloadConfig(),
    onDismiss: () -> Unit,
    onConfirm: (DownloadConfig) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    var selectedType by remember { mutableStateOf(initialConfig.type) }
    var selectedRes by remember { mutableStateOf(initialConfig.videoResolution) }
    var selectedAudioFmt by remember { mutableStateOf(initialConfig.audioFormat) }
    var selectedFormatId by remember { mutableStateOf(initialConfig.selectedFormatId) }

    var embedThumbnail by remember { mutableStateOf(initialConfig.embedThumbnail) }
    var embedSubtitles by remember { mutableStateOf(initialConfig.embedSubtitles) }
    var embedMetadata by remember { mutableStateOf(initialConfig.embedMetadata) }
    var cropArtworkSquare by remember { mutableStateOf(initialConfig.cropArtworkSquare) }
    var useAria2c by remember { mutableStateOf(initialConfig.useAria2c) }
    var customArgs by remember { mutableStateOf(initialConfig.customArgs) }

    val hasStreams = videoDetails.formats.isNotEmpty()
    val tabCount = if (hasStreams) 3 else 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tùy chỉnh định dạng tải",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = videoDetails.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                // Material 3 Expressive Segmented Tab Control
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = tabCount),
                        label = { Text("Định dạng", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )

                    if (hasStreams) {
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = tabCount),
                            label = { Text("Stream (${videoDetails.formats.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    SegmentedButton(
                        selected = selectedTab == (if (hasStreams) 2 else 1),
                        onClick = { selectedTab = if (hasStreams) 2 else 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = if (hasStreams) 2 else 1, count = tabCount),
                        label = { Text("Tùy chọn", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Tab 0: Format Presets
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = selectedType == DownloadType.VIDEO,
                                    onClick = {
                                        selectedType = DownloadType.VIDEO
                                        selectedFormatId = null
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    icon = {
                                        if (selectedType == DownloadType.VIDEO) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Video HD")
                                    }
                                }

                                SegmentedButton(
                                    selected = selectedType == DownloadType.AUDIO,
                                    onClick = {
                                        selectedType = DownloadType.AUDIO
                                        selectedFormatId = null
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    icon = {
                                        if (selectedType == DownloadType.AUDIO) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Audio MP3")
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    if (selectedType == DownloadType.VIDEO) {
                                        Text(
                                            text = "Độ phân giải video:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            VideoResolution.values().forEach { res ->
                                                FilterChip(
                                                    selected = selectedRes == res && selectedFormatId == null,
                                                    onClick = {
                                                        selectedRes = res
                                                        selectedFormatId = null
                                                    },
                                                    label = { Text(res.label, fontSize = 12.sp) },
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Định dạng âm thanh xuất ra:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AudioFormat.values().forEach { fmt ->
                                                FilterChip(
                                                    selected = selectedAudioFmt == fmt && selectedFormatId == null,
                                                    onClick = {
                                                        selectedAudioFmt = fmt
                                                        selectedFormatId = null
                                                    },
                                                    label = { Text(fmt.label, fontSize = 12.sp) },
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (hasStreams) {
                            // Tab 1: Real Available Streams
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(videoDetails.formats) { fmt ->
                                    val isSelected = selectedFormatId == fmt.formatId
                                    val sizeStr = if (fmt.fileSize > 0) Formatter.formatFileSize(context, fmt.fileSize) else ""

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable {
                                                selectedFormatId = fmt.formatId
                                                selectedType = if (fmt.isAudioOnly) DownloadType.AUDIO else DownloadType.VIDEO
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedFormatId = fmt.formatId
                                                    selectedType = if (fmt.isAudioOnly) DownloadType.AUDIO else DownloadType.VIDEO
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = fmt.resolutionLabel,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                                    ) {
                                                        Text(
                                                            text = fmt.ext.uppercase(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${fmt.codecLabel} ${if (sizeStr.isNotEmpty()) "• $sizeStr" else ""}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // If no streams, show Options Tab directly
                            OptionsTabContent(
                                useAria2c = useAria2c,
                                onUseAria2cChange = { useAria2c = it },
                                embedSubtitles = embedSubtitles,
                                onEmbedSubtitlesChange = { embedSubtitles = it },
                                embedMetadata = embedMetadata,
                                onEmbedMetadataChange = {
                                    embedMetadata = it
                                    embedThumbnail = it
                                },
                                cropArtworkSquare = cropArtworkSquare,
                                onCropArtworkSquareChange = { cropArtworkSquare = it },
                                customArgs = customArgs,
                                onCustomArgsChange = { customArgs = it }
                            )
                        }
                    }

                    2 -> {
                        // Tab 2: Options Tab
                        OptionsTabContent(
                            useAria2c = useAria2c,
                            onUseAria2cChange = { useAria2c = it },
                            embedSubtitles = embedSubtitles,
                            onEmbedSubtitlesChange = { embedSubtitles = it },
                            embedMetadata = embedMetadata,
                            onEmbedMetadataChange = {
                                embedMetadata = it
                                embedThumbnail = it
                            },
                            cropArtworkSquare = cropArtworkSquare,
                            onCropArtworkSquareChange = { cropArtworkSquare = it },
                            customArgs = customArgs,
                            onCustomArgsChange = { customArgs = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = DownloadConfig(
                        type = selectedType,
                        videoResolution = selectedRes,
                        selectedFormatId = selectedFormatId,
                        audioFormat = selectedAudioFmt,
                        embedThumbnail = embedThumbnail,
                        embedSubtitles = embedSubtitles,
                        embedMetadata = embedMetadata,
                        cropArtworkSquare = cropArtworkSquare,
                        useAria2c = useAria2c,
                        customArgs = customArgs
                    )
                    onConfirm(config)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bắt đầu tải", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun OptionsTabContent(
    useAria2c: Boolean,
    onUseAria2cChange: (Boolean) -> Unit,
    embedSubtitles: Boolean,
    onEmbedSubtitlesChange: (Boolean) -> Unit,
    embedMetadata: Boolean,
    onEmbedMetadataChange: (Boolean) -> Unit,
    cropArtworkSquare: Boolean,
    onCropArtworkSquareChange: (Boolean) -> Unit,
    customArgs: String,
    onCustomArgsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DialogOptionSwitchRow(
                    icon = Icons.Rounded.Speed,
                    title = "Aria2c 16 luồng",
                    subtitle = "Tăng tốc tải đa kết nối song song",
                    checked = useAria2c,
                    onCheckedChange = onUseAria2cChange
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                DialogOptionSwitchRow(
                    icon = Icons.Rounded.Subtitles,
                    title = "Nhúng phụ đề",
                    subtitle = "Kèm phụ đề đa ngôn ngữ nếu có",
                    checked = embedSubtitles,
                    onCheckedChange = onEmbedSubtitlesChange
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                DialogOptionSwitchRow(
                    icon = Icons.Rounded.Tag,
                    title = "Nhúng Metadata & Bìa",
                    subtitle = "Gắn thumbnail và thông tin tác giả",
                    checked = embedMetadata,
                    onCheckedChange = onEmbedMetadataChange
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                DialogOptionSwitchRow(
                    icon = Icons.Rounded.CropOriginal,
                    title = "Cắt bìa vuông (Audio)",
                    subtitle = "Crop 1:1 cho file nhạc MP3",
                    checked = cropArtworkSquare,
                    onCheckedChange = onCropArtworkSquareChange
                )
            }
        }

        OutlinedTextField(
            value = customArgs,
            onValueChange = onCustomArgsChange,
            placeholder = { Text("Ví dụ: --geo-bypass") },
            label = { Text("Lệnh yt-dlp tùy chỉnh (CLI Args)") },
            leadingIcon = { Icon(Icons.Rounded.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
}

@Composable
private fun DialogOptionSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
