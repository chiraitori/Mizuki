package dev.chiraitori.mizuki.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.mizuki.core.model.DownloadTask

@Composable
fun TaskLogDialog(
    task: DownloadTask,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun copyLogs() {
        val fullLog = task.rawLogs.joinToString("\n")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("yt-dlp log", fullLog)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép log vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nhật ký yt-dlp (Terminal Log)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = task.title.take(30),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E1E24),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 380.dp)
            ) {
                if (task.rawLogs.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Chưa có dữ liệu log.",
                            color = Color(0xFF9E9E9E),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(task.rawLogs) { line ->
                            Text(
                                text = line,
                                color = when {
                                    line.contains("ERROR", ignoreCase = true) || line.contains("Lỗi") -> Color(0xFFFF6B6B)
                                    line.contains("WARNING", ignoreCase = true) -> Color(0xFFFFD166)
                                    line.contains("Mizuki") -> Color(0xFF06D6A0)
                                    else -> Color(0xFFE0E0E0)
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { copyLogs() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sao chép Log")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Đóng")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
