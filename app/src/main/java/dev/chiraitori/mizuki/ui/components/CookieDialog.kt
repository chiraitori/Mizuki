package dev.chiraitori.mizuki.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chiraitori.mizuki.core.cookies.CookieManager
import kotlinx.coroutines.launch

@Composable
fun CookieDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cookieSummary by remember { mutableStateOf(CookieManager.getCookieSummary(context)) }
    var pastedCookieText by remember { mutableStateOf("") }
    var isPasting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = CookieManager.importCookies(context, it)
                result.onSuccess { count ->
                    Toast.makeText(context, "Đã nạp $count cookies thành công!", Toast.LENGTH_SHORT).show()
                    cookieSummary = CookieManager.getCookieSummary(context)
                }.onFailure { err ->
                    Toast.makeText(context, "Lỗi nạp cookies: ${err.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Cookie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Quản lý Cookies (Netscape format)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Trạng thái cookies hiện tại:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = cookieSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Cookies giúp tải các video bị giới hạn độ tuổi, video riêng tư trên TikTok, YouTube, Instagram hoặc Twitter/X khi đăng nhập.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isPasting) {
                    OutlinedTextField(
                        value = pastedCookieText,
                        onValueChange = { pastedCookieText = it },
                        label = { Text("Nội dung file cookies.txt") },
                        placeholder = { Text(".tiktok.com\tTRUE\t/\t...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (pastedCookieText.isNotBlank()) {
                                scope.launch {
                                    val result = CookieManager.saveCookiesText(context, pastedCookieText)
                                    result.onSuccess { count ->
                                        Toast.makeText(context, "Đã lưu $count cookies", Toast.LENGTH_SHORT).show()
                                        cookieSummary = CookieManager.getCookieSummary(context)
                                        isPasting = false
                                        pastedCookieText = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lưu nội dung")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { filePicker.launch("text/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chọn file")
                        }

                        OutlinedButton(
                            onClick = { isPasting = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Dán text")
                        }
                    }

                    if (CookieManager.hasCookies(context)) {
                        OutlinedButton(
                            onClick = {
                                CookieManager.clearCookies(context)
                                cookieSummary = CookieManager.getCookieSummary(context)
                                Toast.makeText(context, "Đã xóa cookies", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xóa cookies đã lưu", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Xong")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
