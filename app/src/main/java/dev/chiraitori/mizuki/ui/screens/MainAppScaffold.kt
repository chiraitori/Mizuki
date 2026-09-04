package dev.chiraitori.mizuki.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import dev.chiraitori.mizuki.R
import dev.chiraitori.mizuki.core.engine.DownloaderEngine
import dev.chiraitori.mizuki.core.model.DownloadTask
import dev.chiraitori.mizuki.core.model.TaskStatus
import dev.chiraitori.mizuki.data.repository.SettingsRepository
import dev.chiraitori.mizuki.ui.components.PixelPlayerStyleNavigationBar
import dev.chiraitori.mizuki.ui.screens.history.HistoryScreen
import dev.chiraitori.mizuki.ui.screens.home.HomeScreen
import dev.chiraitori.mizuki.ui.screens.settings.SettingsScreen

enum class MainTab(
    @param:androidx.annotation.StringRes val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    HOME(R.string.nav_home, Icons.Rounded.Home),
    HISTORY(R.string.nav_library, Icons.Rounded.History),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    initialUrl: String? = null
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.HOME) }

    val downloaderEngine = remember { DownloaderEngine.getInstance(context) }
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    var showSetupScreenOnDemand by remember { mutableStateOf(false) }
    val tasks by downloaderEngine.tasks.collectAsState()
    val activeCount = tasks.count {
        it.status == TaskStatus.DOWNLOADING ||
            it.status == TaskStatus.PROCESSING ||
            it.status == TaskStatus.IDLE
    }
    val activeTask = tasks.firstOrNull { it.status == TaskStatus.DOWNLOADING || it.status == TaskStatus.PROCESSING }

    // Request Notification Permission for Live Activity on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Cần cấp quyền thông báo để hiển thị Live Activity khi tải", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Switch to Home tab if a link was shared into the main app
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrEmpty()) {
            currentTab = MainTab.HOME
        }
    }

    if (showSetupScreenOnDemand) {
        dev.chiraitori.mizuki.ui.screens.setup.SetupScreen(
            onFinishSetup = {
                settingsRepository.markSetupCompleted()
                showSetupScreenOnDemand = false
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.mizuki_avatar),
                                contentDescription = "Mizuki",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (currentTab == MainTab.SETTINGS) {
                                    stringResource(R.string.settings_title)
                                } else {
                                    stringResource(R.string.app_name)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                // PixelPlayer-style Shared-Axis Slide & Fade Screen Transitions
                // The screen content takes the full viewport so scrolling seamlessly flows
                // behind the floating navigation bar layer.
                val motionEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }

                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val slideDistance = 250

                        (slideInHorizontally(
                            animationSpec = tween(320, easing = motionEasing),
                            initialOffsetX = { if (forward) slideDistance else -slideDistance }
                        ) + fadeIn(animationSpec = tween(280, easing = motionEasing))).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(300, easing = motionEasing),
                                targetOffsetX = { if (forward) -slideDistance else slideDistance }
                            ) + fadeOut(animationSpec = tween(220, easing = motionEasing))
                        )
                    },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (targetScreen) {
                            MainTab.HOME -> HomeScreen(initialUrl = initialUrl)
                            MainTab.HISTORY -> HistoryScreen()
                            MainTab.SETTINGS -> SettingsScreen(onOpenSetupScreen = { showSetupScreenOnDemand = true })
                        }
                    }
                }

                // PixelPlayer-style Floating Overlay Navigation Layer
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Floating InApp Live Activity Pill (floats right above the nav bar when downloading)
                    AnimatedVisibility(
                        visible = activeTask != null && currentTab != MainTab.HOME,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        activeTask?.let { task ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                InAppLiveActivityPill(
                                    task = task,
                                    onCancel = { downloaderEngine.cancelTask(task.id) },
                                    onTap = { currentTab = MainTab.HOME }
                                )
                            }
                        }
                    }

                    // Floating Navigation Bar
                    PixelPlayerStyleNavigationBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        activeDownloadCount = activeCount
                    )
                }
            }
        }
    }
}

@Composable
fun InAppLiveActivityPill(
    task: DownloadTask,
    onCancel: () -> Unit,
    onTap: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape = RoundedCornerShape(26.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            .clickable { onTap() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (task.thumbnailUrl != null) {
                        AsyncImage(
                            model = task.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(7.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (task.progress > 0) "${task.progress.toInt()}% • ${task.speed.ifEmpty { "Đang tải..." }}" else "Đang bóc tách stream...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Hủy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (task.progress > 0) {
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
