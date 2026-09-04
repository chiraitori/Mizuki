package dev.chiraitori.mizuki

import android.content.Intent
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chiraitori.mizuki.core.parser.UrlParser
import dev.chiraitori.mizuki.ui.screens.MainAppScaffold
import dev.chiraitori.mizuki.ui.theme.MizukiTheme
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import dev.chiraitori.mizuki.data.repository.ThemeMode

class MainActivity : ComponentActivity() {

    private var currentUrl by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleController.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val settingsRepository = remember { SettingsRepository.getInstance(this) }
            val appPreferences by settingsRepository.appPrefsFlow.collectAsState()
            val setupCompleted by settingsRepository.setupCompletedFlow.collectAsState()
            var isSetupDone by remember { mutableStateOf(settingsRepository.isSetupCompleted()) }

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
                if (!isSetupDone && !setupCompleted) {
                    dev.chiraitori.mizuki.ui.screens.setup.SetupScreen(
                        onFinishSetup = {
                            settingsRepository.markSetupCompleted()
                            isSetupDone = true
                        }
                    )
                } else {
                    MainAppScaffold(initialUrl = currentUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent?.dataString
            ?: ""

        val extractedUrl = UrlParser.extractUrl(rawText)
        if (!extractedUrl.isNullOrEmpty()) {
            currentUrl = extractedUrl
        }
    }
}
