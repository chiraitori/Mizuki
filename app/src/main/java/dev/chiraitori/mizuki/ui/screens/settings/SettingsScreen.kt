package dev.chiraitori.mizuki.ui.screens.settings

import android.content.Context
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import dev.chiraitori.mizuki.ui.components.bounceClick
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.CropOriginal
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chiraitori.mizuki.core.backup.BackupManager
import dev.chiraitori.mizuki.AppLocaleController
import dev.chiraitori.mizuki.R
import dev.chiraitori.mizuki.core.cookies.CookieManager
import dev.chiraitori.mizuki.core.engine.YtDlpWrapper
import dev.chiraitori.mizuki.core.model.AudioFormat
import dev.chiraitori.mizuki.core.model.VideoResolution
import dev.chiraitori.mizuki.data.local.MediaDatabaseHelper
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import dev.chiraitori.mizuki.data.repository.ThemeMode
import dev.chiraitori.mizuki.ui.components.CommandTemplateDialog
import dev.chiraitori.mizuki.ui.components.CookieDialog
import kotlinx.coroutines.launch

private enum class SettingsPage(
    @param:androidx.annotation.StringRes val titleRes: Int,
    @param:androidx.annotation.StringRes val subtitleRes: Int,
    val icon: ImageVector
) {
    APPEARANCE(R.string.settings_category_appearance, R.string.settings_category_appearance_summary, Icons.Rounded.Palette),
    MEDIA(R.string.settings_category_media, R.string.settings_category_media_summary, Icons.Rounded.Movie),
    NETWORK(R.string.settings_category_network, R.string.settings_category_network_summary, Icons.Rounded.NetworkCheck),
    STORAGE(R.string.settings_category_storage, R.string.settings_category_storage_summary, Icons.Rounded.Storage),
    DATA(R.string.settings_category_data, R.string.settings_category_data_summary, Icons.Rounded.Backup),
    DEVELOPER(R.string.settings_category_developer, R.string.settings_category_developer_summary, Icons.Rounded.BugReport),
    ABOUT(R.string.settings_category_about, R.string.settings_category_about_summary, Icons.Rounded.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenSetupScreen: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val dbHelper = remember { MediaDatabaseHelper.getInstance(context) }

    val appPrefs by settingsRepo.appPrefsFlow.collectAsState()
    val config = appPrefs.downloadConfig
    var selectedPageName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPage = selectedPageName?.let { name -> SettingsPage.entries.find { it.name == name } }

    var isUpdatingEngine by remember { mutableStateOf(false) }

    // Dialog States
    var showCookieDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showUserAgentDialog by remember { mutableStateOf(false) }
    var showExtractorArgsDialog by remember { mutableStateOf(false) }
    var showCustomArgsDialog by remember { mutableStateOf(false) }
    var showOutputTemplateDialog by remember { mutableStateOf(false) }
    var showSubLangsDialog by remember { mutableStateOf(false) }
    var showSponsorBlockCatsDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Dropdown States
    var themeDropdownExpanded by remember { mutableStateOf(false) }
    var languageDropdownExpanded by remember { mutableStateOf(false) }
    var videoDropdownExpanded by remember { mutableStateOf(false) }
    var videoContainerExpanded by remember { mutableStateOf(false) }
    var videoCodecExpanded by remember { mutableStateOf(false) }
    var audioDropdownExpanded by remember { mutableStateOf(false) }
    var audioBitrateExpanded by remember { mutableStateOf(false) }
    var subFormatExpanded by remember { mutableStateOf(false) }
    var rateLimitExpanded by remember { mutableStateOf(false) }
    var updateChannelExpanded by remember { mutableStateOf(false) }

    val restoreSuccessMessage = stringResource(R.string.toast_restore_success)
    val restoreErrorMessage = stringResource(R.string.toast_restore_error)
    val engineUpdatingMessage = stringResource(R.string.toast_engine_updating)
    val engineUpdatedMessage = stringResource(R.string.toast_engine_updated)
    val engineErrorMessage = stringResource(R.string.toast_engine_error)
    val backupExportedMessage = stringResource(R.string.toast_backup_exported)
    val backupErrorMessage = stringResource(R.string.toast_backup_error)
    val historyClearedMessage = stringResource(R.string.toast_history_cleared)
    val settingsResetMessage = stringResource(R.string.toast_settings_reset)
    val unknownValue = stringResource(R.string.value_unknown)

    // Backup Picker
    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = BackupManager.importBackup(context, it)
                result.onSuccess { count ->
                    Toast.makeText(context, restoreSuccessMessage.format(count), Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, restoreErrorMessage.format(err.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun updateEngine() {
        scope.launch {
            isUpdatingEngine = true
            Toast.makeText(context, engineUpdatingMessage.format(appPrefs.updateChannel), Toast.LENGTH_SHORT).show()
            val result = YtDlpWrapper.updateEngine(context)
            result.onSuccess {
                Toast.makeText(context, engineUpdatedMessage, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, engineErrorMessage.format(it.message ?: ""), Toast.LENGTH_LONG).show()
            }
            isUpdatingEngine = false
        }
    }

    fun exportBackupFile() {
        scope.launch {
            val result = BackupManager.exportBackup(context)
            result.onSuccess { file ->
                Toast.makeText(context, backupExportedMessage.format(file.name), Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(context, backupErrorMessage.format(err.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val freeSpace = remember {
        try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            Formatter.formatFileSize(context, available)
        } catch (_: Exception) {
            unknownValue
        }
    }

        val cookieSummary = remember(showCookieDialog) {
        CookieManager.getCookieSummary(context)
    }
    BackHandler(enabled = selectedPage != null) { selectedPageName = null }
    val primaryAbi = remember {
        if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "Unknown"
    }
    val easeOutCubic = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }

    AnimatedContent(
        targetState = selectedPage,
        transitionSpec = {
            if (initialState == null && targetState != null) {
                (slideInHorizontally(
                    animationSpec = tween(340, easing = easeOutCubic),
                    initialOffsetX = { (it * 0.35f).toInt() }
                ) + fadeIn(animationSpec = tween(280)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(340, easing = easeOutCubic)
                )).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(280, easing = easeOutCubic),
                        targetOffsetX = { (-it * 0.15f).toInt() }
                    ) + fadeOut(animationSpec = tween(200))
                )
            } else if (initialState != null && targetState == null) {
                (slideInHorizontally(
                    animationSpec = tween(320, easing = easeOutCubic),
                    initialOffsetX = { (-it * 0.15f).toInt() }
                ) + fadeIn(animationSpec = tween(260)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(320, easing = easeOutCubic)
                )).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(300, easing = easeOutCubic),
                        targetOffsetX = { (it * 0.35f).toInt() }
                    ) + fadeOut(animationSpec = tween(220))
                )
            } else {
                (fadeIn(animationSpec = tween(260))).togetherWith(fadeOut(animationSpec = tween(220)))
            }
        },
        label = "SettingsPageTransition",
        modifier = Modifier.fillMaxSize()
    ) { currentPage ->
        val pageScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(pageScrollState)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentPage == null) {
                SettingsHub(onSelect = { selectedPageName = it.name })
            } else {
                SettingsDetailHeader(
                    page = currentPage,
                    onBack = { selectedPageName = null }
                )
            }

        // ----------------------------------------------------
        // SETUP SCREEN BANNER (PIXELPLAYER STYLE)
        // ----------------------------------------------------
        if (currentPage == SettingsPage.DEVELOPER) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.97f) { onOpenSetupScreen() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.developer_setup_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.developer_setup_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ----------------------------------------------------
        // CATEGORY 1: GIAO DIỆN & CHỦ ĐỀ
        // ----------------------------------------------------
        if (currentPage == SettingsPage.APPEARANCE) {
            SettingsCategoryHeader(stringResource(R.string.settings_section_appearance), Icons.Rounded.Palette)
        
        
            SettingsGroupCard {
                val themeOptions = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ThemeMode.AMOLED to stringResource(R.string.theme_amoled)
                )
                ExposedDropdownMenuBox(
                    expanded = themeDropdownExpanded,
                    onExpandedChange = { themeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = themeOptions.first { it.first == appPrefs.themeMode }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_theme_label)) },
                        leadingIcon = { Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = themeDropdownExpanded,
                        onDismissRequest = { themeDropdownExpanded = false }
                    ) {
                        themeOptions.forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    settingsRepo.updateAppPreferences(appPrefs.copy(themeMode = mode))
                                    themeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.ColorLens,
                    title = stringResource(R.string.dynamic_color_title),
                    subtitle = stringResource(R.string.dynamic_color_summary),
                    checked = appPrefs.dynamicColor,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(dynamicColor = it)) }
                )

                SettingsDivider()

                val languageOptions = listOf(
                    "SYSTEM" to stringResource(R.string.language_system),
                    "vi" to stringResource(R.string.language_vietnamese),
                    "en" to stringResource(R.string.language_english),
                    "ja" to stringResource(R.string.language_japanese),
                    "ko" to stringResource(R.string.language_korean),
                    "zh-Hans" to stringResource(R.string.language_chinese)
                )
                ExposedDropdownMenuBox(
                    expanded = languageDropdownExpanded,
                    onExpandedChange = { languageDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = languageOptions.firstOrNull { it.first == appPrefs.language }?.second
                            ?: stringResource(R.string.language_system),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_language_title)) },
                        leadingIcon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(languageDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = languageDropdownExpanded,
                        onDismissRequest = { languageDropdownExpanded = false }
                    ) {
                        languageOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    settingsRepo.updateAppPreferences(appPrefs.copy(language = code))
                                    languageDropdownExpanded = false
                                    (context as? Activity)?.let { AppLocaleController.apply(it, code) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // CATEGORY 2: LÕI YT-DLP & BỘ BÓC TÁCH (SEAL DEDICATED SUITE)
        // ----------------------------------------------------
        if (currentPage == SettingsPage.DEVELOPER) {
            SettingsCategoryHeader(stringResource(R.string.settings_section_engine), Icons.Rounded.Terminal)
        
        
            SettingsGroupCard {
                // yt-dlp Version & Channel Updater
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
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.settings_ytdlp_engine), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = stringResource(R.string.settings_update_channel_summary, appPrefs.updateChannel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (!isUpdatingEngine) updateEngine() },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isUpdatingEngine) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(stringResource(R.string.action_update))
                        }
                    }
                }

                SettingsDivider()

                // Update Channel Selector (Stable, Nightly, Master)
                ExposedDropdownMenuBox(
                    expanded = updateChannelExpanded,
                    onExpandedChange = { updateChannelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (appPrefs.updateChannel) {
                            "NIGHTLY" -> stringResource(R.string.update_channel_nightly)
                            "MASTER" -> stringResource(R.string.update_channel_master)
                            else -> stringResource(R.string.update_channel_stable)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_update_channel)) },
                        leadingIcon = { Icon(Icons.Rounded.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = updateChannelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = updateChannelExpanded,
                        onDismissRequest = { updateChannelExpanded = false }
                    ) {
                        listOf(
                            "STABLE" to stringResource(R.string.update_channel_stable),
                            "NIGHTLY" to stringResource(R.string.update_channel_nightly),
                            "MASTER" to stringResource(R.string.update_channel_master)
                        ).forEach { (code, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = {
                                    settingsRepo.updateAppPreferences(appPrefs.copy(updateChannel = code))
                                    updateChannelExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                // Command Templates
                SettingsClickableRow(
                    icon = Icons.Rounded.Terminal,
                    title = stringResource(R.string.settings_command_templates),
                    subtitle = stringResource(R.string.settings_command_templates_summary),
                    onClick = { showTemplateDialog = true }
                )

                SettingsDivider()

                // Netscape Cookies
                SettingsClickableRow(
                    icon = Icons.Rounded.Cookie,
                    title = stringResource(R.string.settings_cookies),
                    subtitle = cookieSummary,
                    onClick = { showCookieDialog = true }
                )

                SettingsDivider()

                // Extractor Args Dialog
                SettingsClickableRow(
                    icon = Icons.Rounded.Extension,
                    title = stringResource(R.string.settings_extractor_args),
                    subtitle = if (appPrefs.extractorArgs.isNotBlank()) appPrefs.extractorArgs else stringResource(R.string.value_automatic_default),
                    onClick = { showExtractorArgsDialog = true }
                )

                SettingsDivider()

                // Global Custom CLI Args
                SettingsClickableRow(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.settings_global_args),
                    subtitle = if (config.customArgs.isNotBlank()) config.customArgs else stringResource(R.string.value_no_extra_args),
                    onClick = { showCustomArgsDialog = true }
                )

                SettingsDivider()

                // SponsorBlock Switch
                SettingsSwitchRow(
                    icon = Icons.Rounded.Block,
                    title = stringResource(R.string.settings_sponsorblock),
                    subtitle = stringResource(R.string.settings_sponsorblock_summary),
                    checked = config.removeSponsorSegments,
                    onCheckedChange = { settingsRepo.updateConfig(config.copy(removeSponsorSegments = it)) }
                )

                AnimatedVisibility(
                    visible = config.removeSponsorSegments,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
                ) {
                    Column {
                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.settings_sponsorblock_categories),
                            subtitle = appPrefs.sponsorBlockCategories.ifBlank { "sponsor,selfpromo,intro,outro" },
                            onClick = { showSponsorBlockCatsDialog = true }
                        )
                    }
                }

                SettingsDivider()

                // Force IPv4 Switch
                SettingsSwitchRow(
                    icon = Icons.Rounded.NetworkCheck,
                    title = stringResource(R.string.settings_force_ipv4),
                    subtitle = stringResource(R.string.settings_force_ipv4_summary),
                    checked = appPrefs.forceIpv4,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(forceIpv4 = it)) }
                )

                SettingsDivider()

                // Verbose Debug Logging Switch
                SettingsSwitchRow(
                    icon = Icons.Rounded.BugReport,
                    title = stringResource(R.string.settings_verbose_logging),
                    subtitle = stringResource(R.string.settings_verbose_logging_summary),
                    checked = appPrefs.verboseLogging,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(verboseLogging = it)) }
                )
            }
        }

        // ----------------------------------------------------
        // CATEGORY 3: VIDEO & CONTAINER
        // ----------------------------------------------------
        if (currentPage == SettingsPage.MEDIA) {
            SettingsCategoryHeader(stringResource(R.string.settings_section_video), Icons.Rounded.Movie)
        
        
            SettingsGroupCard {
                // Video Resolution Dropdown
                ExposedDropdownMenuBox(
                    expanded = videoDropdownExpanded,
                    onExpandedChange = { videoDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = config.videoResolution.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_default_video_resolution)) },
                        leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = videoDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = videoDropdownExpanded,
                        onDismissRequest = { videoDropdownExpanded = false }
                    ) {
                        VideoResolution.values().forEach { res ->
                            DropdownMenuItem(
                                text = { Text(res.label) },
                                onClick = {
                                    settingsRepo.updateConfig(config.copy(videoResolution = res))
                                    videoDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                // Video Container Dropdown (MP4, MKV, WebM)
                ExposedDropdownMenuBox(
                    expanded = videoContainerExpanded,
                    onExpandedChange = { videoContainerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (appPrefs.videoContainer) {
                            "MP4" -> "MP4"
                            "MKV" -> "MKV"
                            "WEBM" -> "WebM"
                            else -> stringResource(R.string.value_automatic)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_video_container)) },
                        leadingIcon = { Icon(Icons.Rounded.VideoFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = videoContainerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = videoContainerExpanded,
                        onDismissRequest = { videoContainerExpanded = false }
                    ) {
                        listOf("ANY" to stringResource(R.string.value_automatic), "MP4" to "MP4", "MKV" to "MKV", "WEBM" to "WebM").forEach { (code, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = {
                                    settingsRepo.updateAppPreferences(appPrefs.copy(videoContainer = code))
                                    videoContainerExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                // Video Codec Dropdown
                ExposedDropdownMenuBox(
                    expanded = videoCodecExpanded,
                    onExpandedChange = { videoCodecExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (appPrefs.videoCodec) {
                            "H264" -> "H.264 / AVC"
                            "VP9" -> "VP9"
                            "AV1" -> "AV1"
                            else -> stringResource(R.string.value_automatic)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_video_codec)) },
                        leadingIcon = { Icon(Icons.Rounded.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = videoCodecExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = videoCodecExpanded,
                        onDismissRequest = { videoCodecExpanded = false }
                    ) {
                        listOf("ANY" to stringResource(R.string.value_automatic), "H264" to "H.264 / AVC", "VP9" to "VP9", "AV1" to "AV1").forEach { (code, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = {
                                    settingsRepo.updateAppPreferences(appPrefs.copy(videoCodec = code))
                                    videoCodecExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.FolderSpecial,
                    title = stringResource(R.string.settings_playlist_folder),
                    subtitle = stringResource(R.string.settings_playlist_folder_summary),
                    checked = appPrefs.playlistSubfolder,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(playlistSubfolder = it)) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Replay,
                    title = stringResource(R.string.settings_overwrite),
                    subtitle = stringResource(R.string.settings_overwrite_summary),
                    checked = appPrefs.overwriteExisting,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(overwriteExisting = it)) }
                )
            }
        

        // ----------------------------------------------------
        // CATEGORY 4: ÂM THANH & BÌA NHẠC
        // ----------------------------------------------------
        
            SettingsCategoryHeader(stringResource(R.string.settings_section_audio), Icons.Rounded.Audiotrack)
        
        
            SettingsGroupCard {
                ExposedDropdownMenuBox(
                    expanded = audioDropdownExpanded,
                    onExpandedChange = { audioDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = config.audioFormat.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_default_audio_format)) },
                        leadingIcon = { Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = audioDropdownExpanded,
                        onDismissRequest = { audioDropdownExpanded = false }
                    ) {
                        AudioFormat.values().forEach { fmt ->
                            DropdownMenuItem(
                                text = { Text(fmt.label) },
                                onClick = {
                                    settingsRepo.updateConfig(config.copy(audioFormat = fmt))
                                    audioDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                ExposedDropdownMenuBox(
                    expanded = audioBitrateExpanded,
                    onExpandedChange = { audioBitrateExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${appPrefs.audioBitrate}bps",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_audio_bitrate)) },
                        leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audioBitrateExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = audioBitrateExpanded,
                        onDismissRequest = { audioBitrateExpanded = false }
                    ) {
                        listOf("320k", "256k", "192k", "128k").forEach { br ->
                            DropdownMenuItem(
                                text = { Text("${br}bps") },
                                onClick = {
                                    settingsRepo.updateAppPreferences(appPrefs.copy(audioBitrate = br))
                                    audioBitrateExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.PlayCircle,
                    title = stringResource(R.string.settings_keep_original_audio),
                    subtitle = stringResource(R.string.settings_keep_original_audio_summary),
                    checked = appPrefs.keepOriginalAudio,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(keepOriginalAudio = it)) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.CropOriginal,
                    title = stringResource(R.string.settings_crop_cover),
                    subtitle = stringResource(R.string.settings_crop_cover_summary),
                    checked = config.cropArtworkSquare,
                    onCheckedChange = { settingsRepo.updateConfig(config.copy(cropArtworkSquare = it)) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Tag,
                    title = stringResource(R.string.settings_embed_metadata),
                    subtitle = stringResource(R.string.settings_embed_metadata_summary),
                    checked = config.embedMetadata,
                    onCheckedChange = { settingsRepo.updateConfig(config.copy(embedMetadata = it, embedThumbnail = it)) }
                )
            }
        

        // ----------------------------------------------------
        // CATEGORY 5: PHỤ ĐỀ (SUBTITLES)
        // ----------------------------------------------------
        
            SettingsCategoryHeader(stringResource(R.string.settings_section_subtitles), Icons.Rounded.Subtitles)
        
        
            SettingsGroupCard {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Subtitles,
                    title = stringResource(R.string.settings_embed_subtitles),
                    subtitle = stringResource(R.string.settings_embed_subtitles_summary),
                    checked = config.embedSubtitles,
                    onCheckedChange = { settingsRepo.updateConfig(config.copy(embedSubtitles = it)) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.ClosedCaption,
                    title = stringResource(R.string.settings_keep_subtitles),
                    subtitle = stringResource(R.string.settings_keep_subtitles_summary),
                    checked = appPrefs.keepSubtitleFiles,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(keepSubtitleFiles = it)) }
                )

                AnimatedVisibility(
                    visible = config.embedSubtitles,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsDivider()

                        SettingsClickableRow(
                            icon = Icons.Rounded.Language,
                            title = stringResource(R.string.settings_subtitle_languages),
                            subtitle = appPrefs.subLangs,
                            onClick = { showSubLangsDialog = true }
                        )

                        SettingsDivider()

                        // Subtitle Format Dropdown
                        ExposedDropdownMenuBox(
                            expanded = subFormatExpanded,
                            onExpandedChange = { subFormatExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = appPrefs.subFormat.uppercase(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.settings_subtitle_format)) },
                                leadingIcon = { Icon(Icons.Rounded.ClosedCaption, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subFormatExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = subFormatExpanded,
                                onDismissRequest = { subFormatExpanded = false }
                            ) {
                                listOf("srt" to "SRT", "vtt" to "VTT", "ass" to "ASS", "lrc" to "LRC").forEach { (code, lbl) ->
                                    DropdownMenuItem(
                                        text = { Text(lbl) },
                                        onClick = {
                                            settingsRepo.updateAppPreferences(appPrefs.copy(subFormat = code))
                                            subFormatExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        SettingsDivider()

                        SettingsSwitchRow(
                            icon = Icons.Rounded.ClosedCaption,
                            title = stringResource(R.string.settings_auto_subtitles),
                            subtitle = stringResource(R.string.settings_auto_subtitles_summary),
                            checked = appPrefs.autoGeneratedSubs,
                            onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(autoGeneratedSubs = it)) }
                        )
                    }
                }
            }
        

        // ----------------------------------------------------
        // CATEGORY 6: TĂNG TỐC & MẠNG
        // ----------------------------------------------------
        }
        if (currentPage == SettingsPage.NETWORK) {
            SettingsCategoryHeader(stringResource(R.string.settings_section_network), Icons.Rounded.Speed)
        
        
            SettingsGroupCard {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.settings_aria2c),
                    subtitle = stringResource(R.string.settings_aria2c_summary, config.aria2cConnections),
                    checked = config.useAria2c,
                    onCheckedChange = { settingsRepo.updateConfig(config.copy(useAria2c = it)) }
                )

                AnimatedVisibility(
                    visible = config.useAria2c,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.settings_aria2c_connections), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.settings_connections_value, config.aria2cConnections), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = config.aria2cConnections.toFloat(),
                            onValueChange = { settingsRepo.updateConfig(config.copy(aria2cConnections = it.toInt())) },
                            valueRange = 4f..32f,
                            steps = 6
                        )
                    }
                }

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Wifi,
                    title = stringResource(R.string.settings_wifi_only),
                    subtitle = stringResource(R.string.settings_wifi_only_summary),
                    checked = config.wifiOnly,
                    onCheckedChange = { settingsRepo.updateConfig(config.copy(wifiOnly = it)) }
                )

                SettingsDivider()

                // Rate Limit Dropdown
                ExposedDropdownMenuBox(
                    expanded = rateLimitExpanded,
                    onExpandedChange = { rateLimitExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (appPrefs.rateLimit.isEmpty()) stringResource(R.string.value_unlimited) else "${appPrefs.rateLimit}B/s",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_rate_limit)) },
                        leadingIcon = { Icon(Icons.Rounded.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateLimitExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = rateLimitExpanded,
                        onDismissRequest = { rateLimitExpanded = false }
                    ) {
                        listOf("" to stringResource(R.string.value_unlimited), "10M" to "10 MB/s", "5M" to "5 MB/s", "2M" to "2 MB/s", "1M" to "1 MB/s").forEach { (code, lbl) ->
                            DropdownMenuItem(
                                text = { Text(lbl) },
                                onClick = {
                                    settingsRepo.updateAppPreferences(appPrefs.copy(rateLimit = code))
                                    rateLimitExpanded = false
                                }
                            )
                        }
                    }
                }

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Sync,
                    title = stringResource(R.string.settings_auto_retry),
                    subtitle = stringResource(R.string.settings_auto_retry_summary, appPrefs.retryCount),
                    checked = appPrefs.autoRetry,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(autoRetry = it)) }
                )

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Rounded.VpnKey,
                    title = stringResource(R.string.settings_proxy),
                    subtitle = if (appPrefs.proxyUrl.isNotBlank()) appPrefs.proxyUrl else stringResource(R.string.value_not_configured),
                    onClick = { showProxyDialog = true }
                )

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Rounded.NetworkCheck,
                    title = stringResource(R.string.settings_user_agent),
                    subtitle = if (appPrefs.customUserAgent.isNotBlank()) appPrefs.customUserAgent else stringResource(R.string.value_default_user_agent),
                    onClick = { showUserAgentDialog = true }
                )
            }
        }

        // ----------------------------------------------------
        // CATEGORY 7: LƯU TRỮ & TÊN FILE
        // ----------------------------------------------------
        if (currentPage == SettingsPage.STORAGE) {
            SettingsCategoryHeader(stringResource(R.string.settings_section_storage), Icons.Rounded.Storage)
        
        
            SettingsGroupCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_default_folder), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = YtDlpWrapper.getDefaultDownloadDir().absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                SettingsDivider()

                SettingsClickableRow(
                    icon = Icons.Rounded.Title,
                    title = stringResource(R.string.settings_output_template),
                    subtitle = config.outputTemplate,
                    onClick = { showOutputTemplateDialog = true }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Title,
                    title = stringResource(R.string.settings_restrict_filenames),
                    subtitle = stringResource(R.string.settings_restrict_filenames_summary),
                    checked = appPrefs.restrictFilenames,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(restrictFilenames = it)) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Storage,
                    title = stringResource(R.string.settings_download_archive),
                    subtitle = stringResource(R.string.settings_download_archive_summary),
                    checked = appPrefs.useDownloadArchive,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(useDownloadArchive = it)) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    icon = Icons.Rounded.Audiotrack,
                    title = stringResource(R.string.settings_separate_audio_dir),
                    subtitle = stringResource(R.string.settings_separate_audio_dir_summary),
                    checked = !appPrefs.separateAudioDir.isNullOrEmpty(),
                    onCheckedChange = { isEnabled ->
                        val dir = if (isEnabled) {
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).resolve("Mizuki").absolutePath
                        } else {
                            null
                        }
                        settingsRepo.updateAppPreferences(appPrefs.copy(separateAudioDir = dir))
                    }
                )
            }
        }

        // ----------------------------------------------------
        // CATEGORY 8: SAO LƯU & DỮ LIỆU
        // ----------------------------------------------------
        if (currentPage == SettingsPage.DATA) {
            SettingsCategoryHeader(stringResource(R.string.settings_section_data), Icons.Rounded.Backup)
        
        
            SettingsGroupCard {
                SettingsSwitchRow(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.settings_private_mode),
                    subtitle = stringResource(R.string.settings_private_mode_summary),
                    checked = appPrefs.privateMode,
                    onCheckedChange = { settingsRepo.updateAppPreferences(appPrefs.copy(privateMode = it)) }
                )

                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { exportBackupFile() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_export_backup))
                    }

                    OutlinedButton(
                        onClick = { backupPicker.launch("application/json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_restore))
                    }
                }

                SettingsDivider()

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
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.settings_clear_history), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.settings_clear_history_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            dbHelper.clearAll()
                            Toast.makeText(context, historyClearedMessage, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }

                SettingsDivider()

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
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.settings_reset_defaults), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.settings_reset_defaults_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.action_reset), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // ----------------------------------------------------
        // CATEGORY 9: VỀ ỨNG DỤNG & CHẨN ĐOÁN
        // ----------------------------------------------------
        if (currentPage == SettingsPage.ABOUT) {
            SettingsCategoryHeader(stringResource(R.string.settings_section_about), Icons.Rounded.Info)
        
        
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.about_app_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.about_version), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Text(stringResource(R.string.about_engine), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.about_free_space, freeSpace), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.about_cpu, primaryAbi), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.about_interface), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

    // Dialogs
    if (showCookieDialog) {
        CookieDialog(onDismiss = { showCookieDialog = false })
    }

    if (showTemplateDialog) {
        CommandTemplateDialog(onDismiss = { showTemplateDialog = false })
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.dialog_reset_title)) },
            text = { Text(stringResource(R.string.dialog_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.resetToDefaults()
                        showResetDialog = false
                        Toast.makeText(context, settingsResetMessage, Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showExtractorArgsDialog) {
        var tempExtractorArgs by remember { mutableStateOf(appPrefs.extractorArgs) }
        AlertDialog(
            onDismissRequest = { showExtractorArgsDialog = false },
            title = { Text(stringResource(R.string.settings_extractor_args)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempExtractorArgs,
                        onValueChange = { tempExtractorArgs = it },
                        label = { Text(stringResource(R.string.settings_extractor_args)) },
                        placeholder = { Text("youtube:player_client=android") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.extractor_args_example),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.updateAppPreferences(appPrefs.copy(extractorArgs = tempExtractorArgs.trim()))
                        showExtractorArgsDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExtractorArgsDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showCustomArgsDialog) {
        var tempCustomArgs by remember { mutableStateOf(config.customArgs) }
        AlertDialog(
            onDismissRequest = { showCustomArgsDialog = false },
            title = { Text(stringResource(R.string.settings_global_args)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempCustomArgs,
                        onValueChange = { tempCustomArgs = it },
                        label = { Text(stringResource(R.string.global_args_label)) },
                        placeholder = { Text("--no-check-certificates --geo-bypass") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.updateConfig(config.copy(customArgs = tempCustomArgs.trim()))
                        showCustomArgsDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomArgsDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showOutputTemplateDialog) {
        var tempTemplate by remember { mutableStateOf(config.outputTemplate) }
        AlertDialog(
            onDismissRequest = { showOutputTemplateDialog = false },
            title = { Text(stringResource(R.string.dialog_output_template_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempTemplate,
                        onValueChange = { tempTemplate = it },
                        label = { Text(stringResource(R.string.output_template_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.output_template_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.updateConfig(config.copy(outputTemplate = tempTemplate))
                        showOutputTemplateDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showOutputTemplateDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showProxyDialog) {
        var tempProxy by remember { mutableStateOf(appPrefs.proxyUrl) }
        AlertDialog(
            onDismissRequest = { showProxyDialog = false },
            title = { Text(stringResource(R.string.dialog_proxy_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempProxy,
                        onValueChange = { tempProxy = it },
                        label = { Text(stringResource(R.string.proxy_url_label)) },
                        placeholder = { Text("http://127.0.0.1:8080 / socks5://…") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.updateAppPreferences(appPrefs.copy(proxyUrl = tempProxy.trim()))
                        showProxyDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showProxyDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showUserAgentDialog) {
        var tempUA by remember { mutableStateOf(appPrefs.customUserAgent) }
        AlertDialog(
            onDismissRequest = { showUserAgentDialog = false },
            title = { Text(stringResource(R.string.dialog_user_agent_title)) },
            text = {
                OutlinedTextField(
                    value = tempUA,
                    onValueChange = { tempUA = it },
                    label = { Text("User-Agent") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.updateAppPreferences(appPrefs.copy(customUserAgent = tempUA.trim()))
                        showUserAgentDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUserAgentDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSponsorBlockCatsDialog) {
        val allCategories = listOf(
            "sponsor" to "Sponsor (Nhà tài trợ)",
            "selfpromo" to "Self-promotion (Tự quảng cáo)",
            "intro" to "Intro (Đoạn mở đầu)",
            "outro" to "Outro (Đoạn kết thúc)",
            "preview" to "Preview (Xem trước)",
            "filler" to "Filler (Phân đoạn thừa)",
            "music_offtopic" to "Music offtopic (Đoạn ngoài nhạc)"
        )
        val currentCats = remember(appPrefs.sponsorBlockCategories) {
            appPrefs.sponsorBlockCategories.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        var selectedCats by remember { mutableStateOf(currentCats) }

        AlertDialog(
            onDismissRequest = { showSponsorBlockCatsDialog = false },
            title = { Text(stringResource(R.string.dialog_sponsorblock_cats_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allCategories.forEach { (catKey, catLabel) ->
                        val isChecked = selectedCats.contains(catKey)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedCats = if (isChecked) {
                                        selectedCats - catKey
                                    } else {
                                        selectedCats + catKey
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedCats = if (checked) {
                                        selectedCats + catKey
                                    } else {
                                        selectedCats - catKey
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = selectedCats.joinToString(",")
                        settingsRepo.updateAppPreferences(appPrefs.copy(sponsorBlockCategories = formatted))
                        showSponsorBlockCatsDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSponsorBlockCatsDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSubLangsDialog) {
        var tempSubLangs by remember { mutableStateOf(appPrefs.subLangs) }
        AlertDialog(
            onDismissRequest = { showSubLangsDialog = false },
            title = { Text(stringResource(R.string.settings_subtitle_languages)) },
            text = {
                OutlinedTextField(
                    value = tempSubLangs,
                    onValueChange = { tempSubLangs = it },
                    label = { Text(stringResource(R.string.subtitle_language_codes_label)) },
                    placeholder = { Text("vi,en,all") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        settingsRepo.updateAppPreferences(appPrefs.copy(subLangs = tempSubLangs.trim()))
                        showSubLangsDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubLangsDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsHub(onSelect: (SettingsPage) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingsPage.entries.forEachIndexed { index, page ->
            val shape = when {
                SettingsPage.entries.size == 1 -> RoundedCornerShape(24.dp)
                index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                index == SettingsPage.entries.lastIndex -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                else -> RoundedCornerShape(6.dp)
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .bounceClick(scaleDown = 0.97f) { onSelect(page) },
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(page.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(page.subtitleRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = stringResource(
                            R.string.settings_open_category,
                            stringResource(page.titleRes)
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDetailHeader(page: SettingsPage, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.bounceClick(scaleDown = 0.88f) { onBack() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(page.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------
// REUSABLE MATERIAL 3 EXPRESSIVE COMPONENTS
// ----------------------------------------------------
@Composable
fun SettingsCategoryHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SettingsGroupCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchRow(
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
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
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
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .bounceClick(scaleDown = 0.97f) { onClick() }
            .padding(vertical = 4.dp),
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
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
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
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(stringResource(R.string.action_edit), fontSize = 12.sp)
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}
