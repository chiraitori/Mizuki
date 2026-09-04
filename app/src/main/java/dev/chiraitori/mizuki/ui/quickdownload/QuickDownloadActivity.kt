package dev.chiraitori.mizuki.ui.quickdownload

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.chiraitori.mizuki.core.parser.UrlParser
import dev.chiraitori.mizuki.ui.theme.MizukiTheme

class QuickDownloadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent?.dataString
            ?: ""

        val extractedUrl = UrlParser.extractUrl(rawText) ?: rawText

        setContent {
            MizukiTheme {
                var isVisible by remember { mutableStateOf(true) }

                if (isVisible) {
                    QuickDownloadSheet(
                        initialRawUrl = extractedUrl,
                        onDismiss = {
                            isVisible = false
                            finish()
                        }
                    )
                }
            }
        }
    }
}
