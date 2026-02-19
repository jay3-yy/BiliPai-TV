package com.android.purebilibili.feature.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable // [New]
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.AnalyticsHelper
import com.android.purebilibili.core.util.CacheUtils
import com.android.purebilibili.core.util.CrashReporter
import com.android.purebilibili.core.util.EasterEggs
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.LogCollector
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.plugin.PluginManager

import dev.chrisbanes.haze.hazeSource
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.launch

import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.animation.staggeredEntrance

const val GITHUB_URL = OFFICIAL_GITHUB_URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onOpenSourceLicensesClick: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onPermissionClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onNavigateToBottomBarSettings: () -> Unit = {},
    onTipsClick: () -> Unit = {}, // [Feature] Tips
    onReplayOnboardingClick: () -> Unit = {},
    mainHazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTvDevice = rememberIsTvDevice()
    val versionClickThreshold = EasterEggs.VERSION_EASTER_EGG_THRESHOLD
    
    // State Collection
    val state by viewModel.state.collectAsState()
    val privacyModeEnabled by SettingsManager.getPrivacyModeEnabled(context).collectAsState(initial = false)
    val crashTrackingEnabled by SettingsManager.getCrashTrackingEnabled(context).collectAsState(initial = true)
    val analyticsEnabled by SettingsManager.getAnalyticsEnabled(context).collectAsState(initial = true)
    val easterEggEnabled by SettingsManager.getEasterEggEnabled(context).collectAsState(initial = true)
    val customDownloadPath by SettingsManager.getDownloadPath(context).collectAsState(initial = null)
    val feedApiType by SettingsManager.getFeedApiType(context).collectAsState(
        initial = SettingsManager.FeedApiType.WEB
    )
    val incrementalTimelineRefreshEnabled by SettingsManager.getIncrementalTimelineRefresh(context)
        .collectAsState(initial = false)
    
    // Local UI State
    var showCacheDialog by remember { mutableStateOf(false) }
    var showCacheAnimation by remember { mutableStateOf(false) }
    var cacheProgress by remember { mutableStateOf<CacheClearProgress?>(null) }
    var versionClickCount by remember { mutableIntStateOf(0) }
    var showEasterEggDialog by remember { mutableStateOf(false) }
    var showPathDialog by remember { mutableStateOf(false) }
    // [新增] 权限引导弹窗状态
    var showPermissionDialog by remember { mutableStateOf(false) }
    // [新增] 目录选择对话框状态
    var showDirectoryPicker by remember { mutableStateOf(false) }
    // [新增] 打赏对话框
    var showDonateDialog by remember { mutableStateOf(false) }
    var showReleaseDisclaimerDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("点击检查") }
    var updateCheckResult by remember { mutableStateOf<AppUpdateCheckResult?>(null) }
    
    // [新增] 黑名单页面状态
    var showBlockedList by remember { mutableStateOf(false) }

    // Haze State for this screen
    val activeHazeState = mainHazeState ?: remember { dev.chrisbanes.haze.HazeState() }

    // Directory Picker - 使用文件系统 API
    val defaultPath = remember { SettingsManager.getDefaultDownloadPath(context) }

    // Callbacks
    val onClearCacheAction = { showCacheDialog = true }
    val onDownloadPathAction = { showPathDialog = true }
    
    // Logic Callbacks
    val onPrivacyModeChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setPrivacyModeEnabled(context, enabled) }
    }
    val onCrashTrackingChange: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsManager.setCrashTrackingEnabled(context, enabled)
            CrashReporter.setEnabled(enabled)
        }
    }
    val onAnalyticsChange: (Boolean) -> Unit = { enabled ->
        scope.launch {
            SettingsManager.setAnalyticsEnabled(context, enabled)
            AnalyticsHelper.setEnabled(enabled)
        }
    }
    val onEasterEggChange: (Boolean) -> Unit = { enabled ->
        scope.launch { SettingsManager.setEasterEggEnabled(context, enabled) }
    }
    
    val onVersionClickAction: () -> Unit = {
        versionClickCount++
        val message = EasterEggs.getVersionClickMessage(
            clickCount = versionClickCount,
            threshold = versionClickThreshold
        )
        val remainingClicks = (versionClickThreshold - versionClickCount).coerceAtLeast(0)
        val hapticType = if (remainingClicks <= 1) {
            HapticFeedbackType.LongPress
        } else {
            HapticFeedbackType.TextHandleMove
        }
        hapticFeedback.performHapticFeedback(hapticType)

        if (EasterEggs.isVersionEasterEggTriggered(versionClickCount, versionClickThreshold)) {
            showEasterEggDialog = true
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } else if (versionClickCount >= 2 || remainingClicks <= 3) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    val onExportLogsAction: () -> Unit = { LogCollector.exportAndShare(context) }
    val onTelegramClick: () -> Unit = { uriHandler.openUri(OFFICIAL_TELEGRAM_URL) }
    val onTwitterClick: () -> Unit = { uriHandler.openUri("https://x.com/YangY_0x00") }
    val onGithubClick: () -> Unit = { uriHandler.openUri(OFFICIAL_GITHUB_URL) }
    val onDisclaimerClick: () -> Unit = { showReleaseDisclaimerDialog = true }
    val onBlockedListClickAction: () -> Unit = { showBlockedList = true }
    val onCheckUpdateAction: () -> Unit = {
        if (isCheckingUpdate) {
            Toast.makeText(context, "正在检查更新，请稍候", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isCheckingUpdate = true
                updateStatusText = "检查中..."
                val result = AppUpdateChecker.check(com.android.purebilibili.BuildConfig.VERSION_NAME)
                result.onSuccess { info ->
                    updateStatusText = info.message
                    if (info.isUpdateAvailable) {
                        updateCheckResult = info
                    } else {
                        Toast.makeText(context, info.message, Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    updateStatusText = "检查失败"
                    Toast.makeText(context, error.message ?: "更新检查失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
                isCheckingUpdate = false
            }
        }
    }

    // Effects
    LaunchedEffect(showCacheAnimation) {
        if (showCacheAnimation) {
            val breakdown = CacheUtils.getCacheBreakdown(context)
            val totalSize = breakdown.totalSize
            val clearedSizeStr = breakdown.format()
            for (i in 0..100 step 10) {
                cacheProgress = CacheClearProgress(
                    current = (totalSize * i / 100),
                    total = totalSize,
                    isComplete = false,
                    clearedSize = clearedSizeStr
                )
                kotlinx.coroutines.delay(150)
            }
            viewModel.clearCache()
            cacheProgress = CacheClearProgress(
                current = totalSize,
                total = totalSize,
                isComplete = true,
                clearedSize = clearedSizeStr
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
        AnalyticsHelper.logScreenView("SettingsScreen")
    }
    
    //  Transparent Navigation Bar
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        if (window != null) window.navigationBarColor = android.graphics.Color.TRANSPARENT
        onDispose { if (window != null) window.navigationBarColor = originalNavBarColor }
    }

    // Dialogs
    if (showCacheDialog) {
        CacheClearConfirmDialog(
            cacheSize = state.cacheSize,
            onConfirm = { showCacheDialog = false; showCacheAnimation = true },
            onDismiss = { showCacheDialog = false }
        )
    }
    
    if (showCacheAnimation && cacheProgress != null) {
        CacheClearAnimationDialog(progress = cacheProgress!!, onDismiss = { showCacheAnimation = false; cacheProgress = null })
    }
    
    if (showPathDialog) {
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { Text("下载位置", color = MaterialTheme.colorScheme.onSurface) },
            text = { 
                Column {
                    Text("默认位置（应用私有目录）：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(defaultPath.substringAfterLast("Android/"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(" 默认位置随应用卸载而删除，选择自定义位置可保留下载文件", style = MaterialTheme.typography.bodySmall, color = com.android.purebilibili.core.theme.iOSOrange)
                }
            },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { 
                    // [修复] 检查 MANAGE_EXTERNAL_STORAGE 权限
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
                        showPathDialog = false
                        showPermissionDialog = true
                    } else {
                        showPathDialog = false
                        showDirectoryPicker = true  // [修复] 使用 DirectorySelectionDialog
                    }
                }) { Text("选择自定义目录") }
            },
            dismissButton = { 
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { 
                    scope.launch { SettingsManager.setDownloadPath(context, null) }
                    showPathDialog = false
                    Toast.makeText(context, "已重置为默认路径", Toast.LENGTH_SHORT).show()
                }) { Text("使用默认") } 
            }
        )
    }
    
    // [新增] 权限引导弹窗
    if (showPermissionDialog) {
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要权限", fontWeight = FontWeight.Bold) },
            text = { 
                Text("为了将视频下载到自定义目录（如 /Download/BiliPai），应用需要“管理所有文件”的权限。\n\n请在接下来的系统设置中允许此权限。")
            },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = {
                    showPermissionDialog = false
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开设置页面，请手动授权", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("去授权", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { showPermissionDialog = false }) { Text("取消") }
            }
        )
    }
    
    // [新增] 目录选择对话框（使用 File API）
    if (showDirectoryPicker) {
        com.android.purebilibili.feature.download.DirectorySelectionDialog(
            initialPath = android.os.Environment.getExternalStorageDirectory().absolutePath,
            onPathSelected = { path ->
                scope.launch { SettingsManager.setDownloadPath(context, path) }
                Toast.makeText(context, "下载路径已更新: $path", Toast.LENGTH_SHORT).show()
                showDirectoryPicker = false
            },
            onDismiss = { showDirectoryPicker = false }
        )
    }
    
    if (showEasterEggDialog) {
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showEasterEggDialog = false; versionClickCount = 0 },
            title = { Text(" 你发现了彩蛋！", fontWeight = FontWeight.Bold) },
            text = { Text("感谢你使用 BiliPai！这是一个用爱发电的开源项目。") },
            confirmButton = { com.android.purebilibili.core.ui.IOSDialogAction(onClick = { showEasterEggDialog = false; versionClickCount = 0 }) { Text("我知道了！") } }
        )
    }

    if (showDonateDialog) {
        DonateDialog(onDismiss = { showDonateDialog = false })
    }

    if (showReleaseDisclaimerDialog) {
        ReleaseChannelDisclaimerDialog(
            onDismiss = { showReleaseDisclaimerDialog = false },
            onOpenGithub = onGithubClick,
            onOpenTelegram = onTelegramClick
        )
    }

    updateCheckResult?.let { info ->
        val notesPreview = info.releaseNotes.trim().let { notes ->
            if (notes.isBlank()) "暂无更新说明" else notes.take(240)
        }
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { updateCheckResult = null },
            title = { Text("发现新版本 v${info.latestVersion}") },
            text = {
                Text(
                    "当前版本 v${info.currentVersion}\n\n$notesPreview",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = {
                    updateCheckResult = null
                    uriHandler.openUri(info.releaseUrl)
                }) { Text("前往下载") }
            },
            dismissButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = {
                    updateCheckResult = null
                }) { Text("稍后") }
            }
        )
    }

    val onOpenLinksAction: () -> Unit = {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开设置", Toast.LENGTH_SHORT).show()
        }
    }

    // 页面跳转逻辑
    if (showBlockedList) {
        BlockedListScreen(onBack = { showBlockedList = false })
    } else {
        // Layout Switching
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = activeHazeState)
        ) {
            if (shouldUseSettingsSplitLayout(widthDp = configuration.screenWidthDp, isTv = isTvDevice)) {
                TabletSettingsLayout(
                    onBack = onBack,
                    onAppearanceClick = onAppearanceClick,
                    onPlaybackClick = onPlaybackClick,
                    onPermissionClick = onPermissionClick,
                    onPluginsClick = onPluginsClick,
                    onExportLogsClick = onExportLogsAction,
                    onLicenseClick = onOpenSourceLicensesClick,
                    onDisclaimerClick = onDisclaimerClick,
                    onGithubClick = onGithubClick,
                    onCheckUpdateClick = onCheckUpdateAction,
                    onVersionClick = onVersionClickAction,
                    onReplayOnboardingClick = onReplayOnboardingClick,
                    onTipsClick = onTipsClick, // [Feature]
                    onTelegramClick = onTelegramClick,
                    onTwitterClick = onTwitterClick,
                    onDownloadPathClick = onDownloadPathAction,
                    onClearCacheClick = onClearCacheAction,
                    onPrivacyModeChange = onPrivacyModeChange,
                    onCrashTrackingChange = onCrashTrackingChange,
                    onAnalyticsChange = onAnalyticsChange,
                    onEasterEggChange = onEasterEggChange,
                    privacyModeEnabled = privacyModeEnabled,
                    customDownloadPath = customDownloadPath,
                    cacheSize = state.cacheSize,
                    crashTrackingEnabled = crashTrackingEnabled,
                    analyticsEnabled = analyticsEnabled,
                    pluginCount = PluginManager.getEnabledCount(),
                    versionName = com.android.purebilibili.BuildConfig.VERSION_NAME,
                    versionClickCount = versionClickCount,
                    versionClickThreshold = versionClickThreshold,
                    easterEggEnabled = easterEggEnabled,
                    updateStatusText = updateStatusText,
                    isCheckingUpdate = isCheckingUpdate,
                    onDonateClick = { showDonateDialog = true },
                    onOpenLinksClick = onOpenLinksAction,
                    onBlockedListClick = onBlockedListClickAction, // Pass to tablet layout
                    feedApiType = feedApiType,
                    onFeedApiTypeChange = { type ->
                        scope.launch {
                            SettingsManager.setFeedApiType(context, type)
                            android.widget.Toast.makeText(
                                context,
                                "已切换为${type.label}，下拉刷新生效",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                    onIncrementalTimelineRefreshChange = { enabled ->
                        scope.launch {
                            SettingsManager.setIncrementalTimelineRefresh(context, enabled)
                        }
                    }
                )
            } else {
                MobileSettingsLayout(
                    onBack = onBack,
                    onAppearanceClick = onAppearanceClick,
                    onPlaybackClick = onPlaybackClick,
                    onPermissionClick = onPermissionClick,
                    onNavigateToBottomBarSettings = onNavigateToBottomBarSettings,
                    onPluginsClick = onPluginsClick,
                    onExportLogsClick = onExportLogsAction,
                    onLicenseClick = onOpenSourceLicensesClick,
                    onDisclaimerClick = onDisclaimerClick,
                    onGithubClick = onGithubClick,
                    onCheckUpdateClick = onCheckUpdateAction,
                    onVersionClick = onVersionClickAction,
                    onTipsClick = onTipsClick, // [Feature]
                    onReplayOnboardingClick = onReplayOnboardingClick,
                    onTelegramClick = onTelegramClick,
                    onTwitterClick = onTwitterClick,
                    onDownloadPathClick = onDownloadPathAction,
                    onClearCacheClick = onClearCacheAction,
                    onPrivacyModeChange = onPrivacyModeChange,
                    onCrashTrackingChange = onCrashTrackingChange,
                    onAnalyticsChange = onAnalyticsChange,
                    onEasterEggChange = onEasterEggChange,
                    privacyModeEnabled = privacyModeEnabled,
                    customDownloadPath = customDownloadPath,
                    cacheSize = state.cacheSize,
                    crashTrackingEnabled = crashTrackingEnabled,
                    analyticsEnabled = analyticsEnabled,
                    pluginCount = PluginManager.getEnabledCount(),
                    versionName = com.android.purebilibili.BuildConfig.VERSION_NAME,
                    versionClickCount = versionClickCount,
                    versionClickThreshold = versionClickThreshold,
                    easterEggEnabled = easterEggEnabled,
                    updateStatusText = updateStatusText,
                    isCheckingUpdate = isCheckingUpdate,
                    cardAnimationEnabled = state.cardAnimationEnabled,
                    feedApiType = feedApiType,
                    onFeedApiTypeChange = { type ->
                        scope.launch {
                            SettingsManager.setFeedApiType(context, type)
                            android.widget.Toast.makeText(
                                context, 
                                "已切换为${type.label}，下拉刷新生效", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                    onIncrementalTimelineRefreshChange = { enabled ->
                        scope.launch {
                            SettingsManager.setIncrementalTimelineRefresh(context, enabled)
                        }
                    },
                    onDonateClick = { showDonateDialog = true },
                    onOpenLinksClick = onOpenLinksAction,
                    onBlockedListClick = onBlockedListClickAction // Pass to mobile layout
                )
            }
            
            // Onboarding Bottom Sheet (Shared)
    
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private enum class MobileSettingsSubpage(val title: String) {
    CONTENT_AND_STORAGE("内容与存储"),
    PRIVACY_AND_SECURITY("隐私与安全"),
    EXTENSIONS_AND_DEBUG("扩展与调试"),
    ABOUT_AND_SUPPORT("关于与支持")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsLayout(
    onBack: () -> Unit,
    // Callbacks
    onAppearanceClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onNavigateToBottomBarSettings: () -> Unit,
    onTipsClick: () -> Unit, // [Feature]
    onPluginsClick: () -> Unit,
    onExportLogsClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onDisclaimerClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onTwitterClick: () -> Unit,
    onDownloadPathClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onDonateClick: () -> Unit,
    onOpenLinksClick: () -> Unit, // [New]
    onBlockedListClick: () -> Unit, // [New]
    
    // Logic Callbacks
    onPrivacyModeChange: (Boolean) -> Unit,
    onCrashTrackingChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onEasterEggChange: (Boolean) -> Unit,
    
    // State
    privacyModeEnabled: Boolean,
    customDownloadPath: String?,
    cacheSize: String,
    crashTrackingEnabled: Boolean,
    analyticsEnabled: Boolean,
    pluginCount: Int,
    versionName: String,
    versionClickCount: Int,
    versionClickThreshold: Int,
    easterEggEnabled: Boolean,
    updateStatusText: String,
    isCheckingUpdate: Boolean,
    cardAnimationEnabled: Boolean,
    feedApiType: SettingsManager.FeedApiType,
    onFeedApiTypeChange: (SettingsManager.FeedApiType) -> Unit,
    incrementalTimelineRefreshEnabled: Boolean,
    onIncrementalTimelineRefreshChange: (Boolean) -> Unit
) {
    var activeSubpage by rememberSaveable { mutableStateOf<MobileSettingsSubpage?>(null) }
    var isVisible by remember(activeSubpage) { mutableStateOf(false) }
    val context = LocalContext.current
    val isTvDevice = rememberIsTvDevice()
    val isTvPerformanceProfileEnabled by SettingsManager.getTvPerformanceProfileEnabled(context).collectAsState(
        initial = isTvDevice
    )
    val windowSizeClass = LocalWindowSizeClass.current
    val deviceUiProfile = remember(isTvDevice, windowSizeClass.widthSizeClass, isTvPerformanceProfileEnabled) {
        resolveDeviceUiProfile(
            isTv = isTvDevice,
            widthSizeClass = windowSizeClass.widthSizeClass,
            tvPerformanceProfileEnabled = isTvPerformanceProfileEnabled
        )
    }
    val effectiveMotionTier = remember(deviceUiProfile.motionTier, cardAnimationEnabled) {
        resolveEffectiveMotionTier(
            baseTier = deviceUiProfile.motionTier,
            animationEnabled = cardAnimationEnabled
        )
    }

    BackHandler(enabled = activeSubpage != null) {
        activeSubpage = null
    }

    LaunchedEffect(activeSubpage) { isVisible = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(activeSubpage?.title ?: "设置", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (activeSubpage == null) {
                                onBack()
                            } else {
                                activeSubpage = null
                            }
                        }
                    ) {
                        Icon(
                            CupertinoIcons.Outlined.ChevronBackward,
                            contentDescription = if (activeSubpage == null) "返回" else "返回上一级"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        AnimatedContent(
            targetState = activeSubpage,
            transitionSpec = {
                val enteringFromRight = targetState != null
                if (enteringFromRight) {
                    (slideInHorizontally { it / 3 } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it / 3 } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()).togetherWith(
                        slideOutHorizontally { it / 3 } + fadeOut()
                    )
                }
            },
            label = "MobileSettingsSubpage"
        ) { subpage ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (subpage == null) {
                    item {
                        Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                            IOSSectionTitle("核心设置")
                        }
                    }
                    item {
                        Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                            GeneralSection(
                                onAppearanceClick = onAppearanceClick,
                                onPlaybackClick = onPlaybackClick,
                                onBottomBarClick = onNavigateToBottomBarSettings
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.staggeredEntrance(2, isVisible, motionTier = effectiveMotionTier)) {
                            IOSSectionTitle("更多分类")
                        }
                    }
                    item {
                        Box(modifier = Modifier.staggeredEntrance(3, isVisible, motionTier = effectiveMotionTier)) {
                            SettingsSubpageEntrySection(
                                onContentAndStorageClick = {
                                    activeSubpage = MobileSettingsSubpage.CONTENT_AND_STORAGE
                                },
                                onPrivacyAndSecurityClick = {
                                    activeSubpage = MobileSettingsSubpage.PRIVACY_AND_SECURITY
                                },
                                onExtensionsAndDebugClick = {
                                    activeSubpage = MobileSettingsSubpage.EXTENSIONS_AND_DEBUG
                                },
                                onAboutAndSupportClick = {
                                    activeSubpage = MobileSettingsSubpage.ABOUT_AND_SUPPORT
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                } else {
                    when (subpage) {
                        MobileSettingsSubpage.CONTENT_AND_STORAGE -> {
                            item {
                                Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("推荐流")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                                    FeedApiSection(
                                        feedApiType = feedApiType,
                                        onFeedApiTypeChange = onFeedApiTypeChange,
                                        incrementalTimelineRefreshEnabled = incrementalTimelineRefreshEnabled,
                                        onIncrementalTimelineRefreshChange = onIncrementalTimelineRefreshChange
                                    )
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(2, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("数据与存储")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(3, isVisible, motionTier = effectiveMotionTier)) {
                                    DataStorageSection(
                                        customDownloadPath = customDownloadPath,
                                        cacheSize = cacheSize,
                                        onDownloadPathClick = onDownloadPathClick,
                                        onClearCacheClick = onClearCacheClick
                                    )
                                }
                            }
                        }

                        MobileSettingsSubpage.PRIVACY_AND_SECURITY -> {
                            item {
                                Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("隐私与安全")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                                    PrivacySection(
                                        privacyModeEnabled = privacyModeEnabled,
                                        onPrivacyModeChange = onPrivacyModeChange,
                                        onPermissionClick = onPermissionClick,
                                        onBlockedListClick = onBlockedListClick
                                    )
                                }
                            }
                        }

                        MobileSettingsSubpage.EXTENSIONS_AND_DEBUG -> {
                            item {
                                Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("扩展与调试")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                                    DeveloperSection(
                                        crashTrackingEnabled = crashTrackingEnabled,
                                        analyticsEnabled = analyticsEnabled,
                                        pluginCount = pluginCount,
                                        onCrashTrackingChange = onCrashTrackingChange,
                                        onAnalyticsChange = onAnalyticsChange,
                                        onPluginsClick = onPluginsClick,
                                        onExportLogsClick = onExportLogsClick
                                    )
                                }
                            }
                        }

                        MobileSettingsSubpage.ABOUT_AND_SUPPORT -> {
                            item {
                                Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("发布渠道")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                                    ReleaseChannelPinnedCard(
                                        onGithubClick = onGithubClick,
                                        onTelegramClick = onTelegramClick,
                                        onDisclaimerClick = onDisclaimerClick
                                    )
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(2, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("关注作者")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(3, isVisible, motionTier = effectiveMotionTier)) {
                                    FollowAuthorSection(
                                        onTelegramClick = onTelegramClick,
                                        onTwitterClick = onTwitterClick,
                                        onDonateClick = onDonateClick
                                    )
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(4, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("关于")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(5, isVisible, motionTier = effectiveMotionTier)) {
                                    AboutSection(
                                        versionName = versionName,
                                        easterEggEnabled = easterEggEnabled,
                                        onDisclaimerClick = onDisclaimerClick,
                                        onLicenseClick = onLicenseClick,
                                        onGithubClick = onGithubClick,
                                        onCheckUpdateClick = onCheckUpdateClick,
                                        onVersionClick = onVersionClick,
                                        onReplayOnboardingClick = onReplayOnboardingClick,
                                        onEasterEggChange = onEasterEggChange,
                                        updateStatusText = updateStatusText,
                                        isCheckingUpdate = isCheckingUpdate,
                                        versionClickCount = versionClickCount,
                                        versionClickThreshold = versionClickThreshold
                                    )
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(6, isVisible, motionTier = effectiveMotionTier)) {
                                    IOSSectionTitle("帮助与系统")
                                }
                            }
                            item {
                                Box(modifier = Modifier.staggeredEntrance(7, isVisible, motionTier = effectiveMotionTier)) {
                                    SupportToolsSection(
                                        onTipsClick = onTipsClick,
                                        onOpenLinksClick = onOpenLinksClick
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

// Imports... (Ensure clickable is imported)

@Composable
fun DonateDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false, // Full screen
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            // QR Code Container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.TopStart) {
                    Image(
                        painter = painterResource(id = com.android.purebilibili.R.drawable.author_qr),
                        contentDescription = "Donate QR Code",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onDismiss() }, // [New] Click to dismiss
                        contentScale = ContentScale.Fit
                    )

                    // Close Button (Top Left of Image)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Xmark, // Fixed: Filled.Xmark -> Default.Xmark or correct path
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "感谢您的支持！",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "点击二维码或关闭按钮退出",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
