package dev.chiraitori.mizuki.ui.quickdownload

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.chiraitori.mizuki.AppLocaleController
import dev.chiraitori.mizuki.core.parser.UrlParser
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import dev.chiraitori.mizuki.data.repository.ThemeMode
import dev.chiraitori.mizuki.ui.theme.MizukiTheme

class QuickDownloadActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleController.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent?.dataString
            ?: ""

        val extractedUrl = UrlParser.extractUrl(rawText) ?: rawText

        setContent {
            val settingsRepository = remember { SettingsRepository.getInstance(this) }
            val appPreferences by settingsRepository.appPrefsFlow.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (appPreferences.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
            }

            MizukiTheme(
                darkTheme = useDarkTheme,
                dynamicColor = appPreferences.dynamicColor && appPreferences.themeMode != ThemeMode.AMOLED,
                amoled = appPreferences.themeMode == ThemeMode.AMOLED
            ) {
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
