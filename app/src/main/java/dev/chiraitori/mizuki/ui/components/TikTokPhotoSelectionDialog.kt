@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.chiraitori.mizuki.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.LocalOverscrollFactory
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.chiraitori.mizuki.R
import dev.chiraitori.mizuki.core.engine.DownloaderEngine
import dev.chiraitori.mizuki.core.model.VideoDetails
import kotlin.math.ceil

private enum class PhotoPickerLayout { GRID, QUICK_ROW }

@Composable
fun TikTokPhotoSelectionDialog(
    videoDetails: VideoDetails,
    onDismiss: () -> Unit,
    onQueued: (count: Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .heightIn(max = screenHeight * 0.86f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            TikTokPhotoPickerBody(
                videoDetails = videoDetails,
                layout = PhotoPickerLayout.GRID,
                onDismiss = onDismiss,
                onQueued = onQueued
            )
        }
    }
}

@Composable
fun TikTokPhotoQuickPicker(
    videoDetails: VideoDetails,
    onDismiss: () -> Unit,
    onQueued: (count: Int) -> Unit
) {
    TikTokPhotoPickerBody(
        videoDetails = videoDetails,
        layout = PhotoPickerLayout.QUICK_ROW,
        onDismiss = onDismiss,
        onQueued = onQueued
    )
}

@Composable
private fun TikTokPhotoPickerBody(
    videoDetails: VideoDetails,
    layout: PhotoPickerLayout,
    onDismiss: () -> Unit,
    onQueued: (count: Int) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val downloaderEngine = remember { DownloaderEngine.getInstance(context) }
    var selectedIndices by remember(videoDetails.imageUrls) {
        mutableStateOf(videoDetails.imageUrls.indices.toSet())
    }
    val isQuick = layout == PhotoPickerLayout.QUICK_ROW

    fun startDownload() {
        if (selectedIndices.isEmpty()) return
        val selectedUrls = selectedIndices.sorted().map(videoDetails.imageUrls::get)
        downloaderEngine.enqueuePhotoTask(
            postId = videoDetails.id,
            url = videoDetails.originalUrl,
            title = videoDetails.title,
            author = videoDetails.author,
            thumbnailUrl = videoDetails.thumbnailUrl,
            imageUrls = selectedUrls
        )
        onQueued(selectedUrls.size)
        onDismiss()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!isQuick) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.padding(11.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tiktok_photo_select_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.tiktok_photo_selected_count,
                            selectedIndices.size,
                            videoDetails.imageUrls.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_cancel))
                }
            }
        }

        if (isQuick) {
            Text(
                text = stringResource(R.string.tiktok_photo_quick_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
            )
            // Disable the platform stretch/release effect so this strip tracks
            // the finger directly instead of snapping back after a fling.
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = videoDetails.imageUrls,
                        key = { index, url -> "$index-$url" }
                    ) { index, imageUrl ->
                        PhotoSelectionTile(
                            imageUrl = imageUrl,
                            index = index,
                            total = videoDetails.imageUrls.size,
                            selected = index in selectedIndices,
                            enabled = true,
                            width = 116.dp,
                            crossfadeEnabled = false,
                            onSelectedChange = { checked ->
                                selectedIndices = selectedIndices.updated(index, checked)
                            }
                        )
                    }
                }
            }
        } else {
            val dialogWidth = configuration.screenWidthDp.dp * 0.92f
            val contentWidth = dialogWidth - 32.dp
            val columns = when {
                dialogWidth >= 560.dp -> 4
                dialogWidth >= 420.dp -> 3
                else -> 2
            }
            val cellWidth = (contentWidth - 8.dp * (columns - 1)) / columns
            val rows = ceil(videoDetails.imageUrls.size / columns.toFloat()).toInt().coerceAtLeast(1)
            val wantedGridHeight = cellWidth * (4f / 3f) * rows + 8.dp * (rows - 1)
            val maxGridHeight = (configuration.screenHeightDp.dp * 0.86f - 210.dp).coerceAtLeast(180.dp)
            val gridHeight = wantedGridHeight.coerceAtMost(maxGridHeight)

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = videoDetails.imageUrls,
                    key = { index, url -> "$index-$url" }
                ) { index, imageUrl ->
                    PhotoSelectionTile(
                        imageUrl = imageUrl,
                        index = index,
                        total = videoDetails.imageUrls.size,
                        selected = index in selectedIndices,
                        enabled = true,
                        width = null,
                        crossfadeEnabled = true,
                        onSelectedChange = { checked ->
                            selectedIndices = selectedIndices.updated(index, checked)
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(
                start = if (isQuick) 2.dp else 20.dp,
                end = if (isQuick) 2.dp else 20.dp,
                top = 14.dp,
                bottom = if (isQuick) 2.dp else 18.dp
            )
        ) {
            if (isQuick) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val allSelected = selectedIndices.size == videoDetails.imageUrls.size
                    Row(
                        modifier = Modifier
                            .height(52.dp)
                            .toggleable(
                                value = allSelected,
                                enabled = true,
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    selectedIndices = if (checked) videoDetails.imageUrls.indices.toSet() else emptySet()
                                }
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = if (allSelected) null else androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            if (allSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.padding(5.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.tiktok_photo_select_all),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = ::startDownload,
                        enabled = selectedIndices.isNotEmpty(),
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp)
                    ) {
                        DownloadButtonContent(selectedIndices.size, showCount = false)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = ::startDownload,
                        enabled = selectedIndices.isNotEmpty(),
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DownloadButtonContent(selectedIndices.size, showCount = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSelectionTile(
    imageUrl: String,
    index: Int,
    total: Int,
    selected: Boolean,
    enabled: Boolean,
    width: Dp?,
    crossfadeEnabled: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val selectionState = stringResource(
        if (selected) R.string.tiktok_photo_selected else R.string.tiktok_photo_not_selected
    )
    val sizeModifier = if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()

    Box(
        modifier = sizeModifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(if (width != null) 14.dp else 18.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(if (width != null) 14.dp else 18.dp)
            )
            .toggleable(
                value = selected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onSelectedChange
            )
            .semantics { stateDescription = selectionState }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(crossfadeEnabled)
                .setHeader("Referer", "https://www.tiktok.com/")
                .build(),
            contentDescription = stringResource(R.string.tiktok_photo_item_description, index + 1, total),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (width != null) 6.dp else 8.dp)
                    .size(if (width != null) 26.dp else 32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 3.dp
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(if (width != null) 4.dp else 6.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun DownloadButtonContent(count: Int, showCount: Boolean) {
    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.size(7.dp))
    Text(
        text = if (showCount) {
            stringResource(R.string.tiktok_photo_download_count, count)
        } else {
            stringResource(R.string.tiktok_photo_download)
        },
        maxLines = 1
    )
}

private fun Set<Int>.updated(index: Int, selected: Boolean): Set<Int> =
    if (selected) this + index else this - index
