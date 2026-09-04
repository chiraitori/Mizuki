package dev.chiraitori.mizuki.ui.components

import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.mizuki.core.model.CommandTemplate
import dev.chiraitori.mizuki.data.local.MediaDatabaseHelper

@Composable
fun CommandTemplateDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: ((CommandTemplate) -> Unit)? = null
) {
    val context = LocalContext.current
    val dbHelper = remember { MediaDatabaseHelper.getInstance(context) }
    val templates by dbHelper.templatesFlow.collectAsState()

    var isAddingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newArgs by remember { mutableStateOf("") }

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
                Text(
                    text = "Mẫu lệnh yt-dlp (Command Templates)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isAddingNew) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Tạo mẫu lệnh mới",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Tên mẫu lệnh") },
                            placeholder = { Text("Ví dụ: Tải 1080p + Subtitle") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newDesc,
                            onValueChange = { newDesc = it },
                            label = { Text("Mô tả (tùy chọn)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newArgs,
                            onValueChange = { newArgs = it },
                            label = { Text("Các tham số CLI") },
                            placeholder = { Text("-f 137+140 --embed-subs") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = false
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (newName.isNotBlank() && newArgs.isNotBlank()) {
                                        val tmpl = CommandTemplate(
                                            id = "custom_${System.currentTimeMillis()}",
                                            name = newName.trim(),
                                            description = newDesc.trim(),
                                            customArgs = newArgs.trim(),
                                            isBuiltIn = false
                                        )
                                        dbHelper.saveTemplate(tmpl)
                                        Toast.makeText(context, "Đã lưu mẫu lệnh", Toast.LENGTH_SHORT).show()
                                        isAddingNew = false
                                        newName = ""
                                        newDesc = ""
                                        newArgs = ""
                                    } else {
                                        Toast.makeText(context, "Vui lòng nhập đầy đủ tên và tham số", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Lưu")
                            }

                            OutlinedButton(
                                onClick = { isAddingNew = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Hủy")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(templates, key = { it.id }) { tmpl ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectTemplate?.invoke(tmpl)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = tmpl.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (tmpl.isBuiltIn) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "Hệ thống",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (tmpl.description.isNotEmpty()) {
                                            Text(
                                                text = tmpl.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tmpl.customArgs,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (!tmpl.isBuiltIn) {
                                        IconButton(onClick = {
                                            dbHelper.deleteTemplate(tmpl.id)
                                            Toast.makeText(context, "Đã xóa mẫu lệnh", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Xóa",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isAddingNew) {
                Button(
                    onClick = { isAddingNew = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo mẫu mới")
                }
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
