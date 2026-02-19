// 文件路径: app/src/main/java/com/android/purebilibili/MainActivity.kt
package com.android.purebilibili

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.animation.doOnEnd
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.theme.PureBiliBiliTheme
import com.android.purebilibili.core.ui.SharedTransitionProvider
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.plugin.EyeProtectionOverlay
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.video.ui.overlay.FullscreenPlayerOverlay
import com.android.purebilibili.feature.video.ui.overlay.MiniPlayerOverlay
import com.android.purebilibili.navigation.AppNavigation
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "MainActivity"
private const val PREFS_NAME = "app_welcome"
private const val KEY_FIRST_LAUNCH = "first_launch_shown"

internal fun resolveShortcutRoute(host: String): String? {
    return when (host) {
        "search" -> com.android.purebilibili.navigation.ScreenRoutes.Search.route
        "dynamic" -> com.android.purebilibili.navigation.ScreenRoutes.Dynamic.route
        "favorite" -> com.android.purebilibili.navigation.ScreenRoutes.Favorite.route
        "history" -> com.android.purebilibili.navigation.ScreenRoutes.History.route
        "login" -> com.android.purebilibili.navigation.ScreenRoutes.Login.route
        "playback" -> com.android.purebilibili.navigation.ScreenRoutes.PlaybackSettings.route
        "plugins" -> com.android.purebilibili.navigation.ScreenRoutes.PluginsSettings.route
        else -> null
    }
}

@OptIn(androidx.media3.common.util.UnstableApi::class) // 解决 UnsafeOptInUsageError，因为 AppNavigation 内部使用了不稳定的 API
class MainActivity : ComponentActivity() {
    
    //  PiP 状态
    var isInPipMode by mutableStateOf(false)
        private set
    
    //  是否在视频页面 (用于决定是否进入 PiP)
    var isInVideoDetail by mutableStateOf(false)
    
    //  小窗管理器
    private lateinit var miniPlayerManager: MiniPlayerManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        //  安装 SplashScreen
        val splashScreen = installSplashScreen()
        
        //  🚀 [启动优化] 立即开始预加载首页数据
        // 这个必须尽早调用，利用开屏动画的时间并行加载数据
        com.android.purebilibili.data.repository.VideoRepository.preloadHomeData()
        
        super.onCreate(savedInstanceState)
        //  初始调用，后续会根据主题动态更新
        enableEdgeToEdge()
        
        // 初始化小窗管理器
        miniPlayerManager = MiniPlayerManager.getInstance(this)
        
        //  🚀 [启动优化] 保持 Splash 直到数据加载完成或超时
        var isDataReady = false
        val startTime = System.currentTimeMillis()
        
        splashScreen.setKeepOnScreenCondition {
            // 检查数据是否就绪
            if (com.android.purebilibili.data.repository.VideoRepository.isHomeDataReady()) {
                isDataReady = true
            }
            
            // 计算耗时
            val elapsed = System.currentTimeMillis() - startTime
            
            // 条件：数据未就绪 且 未超时(2500ms)
            // 如果超时，强制进入（会显示骨架屏），避免用户以为死机
            val shouldKeep = !isDataReady && elapsed < 2500L
            
            if (!shouldKeep) {
                 Logger.d(TAG, "🚀 Splash dismissed. DataReady=$isDataReady, Elapsed=${elapsed}ms")
            }
            
            shouldKeep
        }
        
        //  [新增] 处理 deep link 或分享意图
        handleIntent(intent)
        
        // --- 📺 DLNA Service Init ---
        // Android 12+ 需要运行时权限
        // requestDlnaPermissionsAndBind()
        
        // Start Local Proxy Server (on IO thread)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Singleton instance to prevent multiple servers? 
                // Simple implementation for now: create and start.
                val proxy = com.android.purebilibili.feature.cast.LocalProxyServer()
                proxy.start()
                com.android.purebilibili.core.util.Logger.d(TAG, "📺 Local Proxy Server started on port 8901")
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e(TAG, "❌ Failed to start Local Proxy Server", e)
            }
        }

        setContent {
            val context = LocalContext.current
            val navController = androidx.navigation.compose.rememberNavController()
            
            //  [新增] 监听 pendingVideoId 并导航到视频详情页
            LaunchedEffect(pendingVideoId) {
                pendingVideoId?.let { videoId ->
                    Logger.d(TAG, "🚀 导航到视频: $videoId")
                    navController.navigate("video/$videoId?cid=0&cover=") {
                        launchSingleTop = true
                    }
                    pendingVideoId = null  // 清除，避免重复导航
                }
            }
            
            // 🚀 [新增] 监听 pendingRoute 并导航到对应页面 (App Shortcuts)
            LaunchedEffect(pendingRoute) {
                pendingRoute?.let { route ->
                    Logger.d(TAG, "🚀 导航到快捷入口: $route")
                    val targetRoute = resolveShortcutRoute(route)
                    targetRoute?.let { 
                        navController.navigate(it) { launchSingleTop = true }
                    }
                    pendingRoute = null  // 清除，避免重复导航
                }
            }
            
            //  首次启动检测已移交 AppNavigation 处理
            // val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
            // var showWelcome by remember { mutableStateOf(!prefs.getBoolean(KEY_FIRST_LAUNCH, false)) }

            // 1. 获取存储的模式 (默认为跟随系统)
            val themeMode by SettingsManager.getThemeMode(context).collectAsState(initial = AppThemeMode.FOLLOW_SYSTEM)

            //  检查并请求所有文件访问权限 (Android 11+)
            //  检查并请求所有文件访问权限 (已移除启动时强制检查，改为按需申请)
            // LaunchedEffect(Unit) { ... }

            //  2. [新增] 获取动态取色设置 (默认为 true)
            val dynamicColor by SettingsManager.getDynamicColor(context).collectAsState(initial = true)
            
            //  3. [新增] 获取主题色索引
            val themeColorIndex by SettingsManager.getThemeColorIndex(context).collectAsState(initial = 0)
            
            // 4. 获取系统当前的深色状态
            val systemInDark = isSystemInDarkTheme()

            // 5. 根据枚举值决定是否开启 DarkTheme
            val useDarkTheme = when (themeMode) {
                AppThemeMode.FOLLOW_SYSTEM -> systemInDark // 跟随系统：系统黑则黑，系统白则白
                AppThemeMode.LIGHT -> false                // 强制浅色
                AppThemeMode.DARK -> true                  // 强制深色
            }

            //  [新增] 根据主题动态更新状态栏样式
            LaunchedEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkTheme) {
                        androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        androidx.activity.SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }
            
            //  全局 Haze 状态，用于实现毛玻璃效果
            // 强制启用 blur，避免部分设备（如 Android 12）默认降级为仅半透明遮罩
            val mainHazeState = remember {
                dev.chrisbanes.haze.HazeState(initialBlurEnabled = true)
            }
            
            //  📐 [平板适配] 计算窗口尺寸类
            val windowSizeClass = com.android.purebilibili.core.util.calculateWindowSizeClass()

            // 6. 传入参数
            PureBiliBiliTheme(
                darkTheme = useDarkTheme,
                dynamicColor = dynamicColor,
                themeColorIndex = themeColorIndex, //  传入主题色索引

            ) {
                //  📐 [平板适配] 提供全局 WindowSizeClass
                androidx.compose.runtime.CompositionLocalProvider(
                    com.android.purebilibili.core.util.LocalWindowSizeClass provides windowSizeClass
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)  // 📐 [修复] 防止平板端返回后出现黑边
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        //  [修复] 移除 .haze() 以避免与 hazeSource/hazeEffect 冲突
                        // 每个 Screen 自己管理 hazeSource（内容）和 hazeEffect（头部/底栏）
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            //  SharedTransitionProvider 包裹导航，启用共享元素过渡
                            SharedTransitionProvider {
                                AppNavigation(
                                    navController = navController,
                                    miniPlayerManager = miniPlayerManager,
                                    isInPipMode = isInPipMode,
                                    onVideoDetailEnter = { 
                                        isInVideoDetail = true
                                        Logger.d(TAG, " 进入视频详情页")
                                    },
                                    onVideoDetailExit = { 
                                        isInVideoDetail = false
                                        Logger.d(TAG, "🔙 退出视频详情页")
                                    },
                                    mainHazeState = mainHazeState //  传递全局 Haze 状态
                                )
                            }
                            
                            //  OnboardingBottomSheet 等其他 overlay 组件

                        }
                    }
                    //  小窗全屏状态
                    var showFullscreen by remember { mutableStateOf(false) }
                    
                    //  小窗播放器覆盖层 (非 PiP 模式下显示)
                    if (!isInPipMode) {
                        MiniPlayerOverlay(
                            miniPlayerManager = miniPlayerManager,
                            onExpandClick = {
                                //  [修改] 导航回详情页，而不是只显示全屏播放器
                                miniPlayerManager.currentBvid?.let { bvid ->
                                    val cid = miniPlayerManager.currentCid
                                    navController.navigate("video/$bvid?cid=$cid&cover=") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                    
                    //  全屏播放器覆盖层（包含亮度、音量、进度调节）
                    if (showFullscreen) {
                        FullscreenPlayerOverlay(
                            miniPlayerManager = miniPlayerManager,
                            onDismiss = { 
                                showFullscreen = false
                                miniPlayerManager.enterMiniMode()
                            },
                            onNavigateToDetail = {
                                //  关闭全屏覆盖层并导航到视频详情页
                                showFullscreen = false
                                miniPlayerManager.currentBvid?.let { bvid ->
                                    //  [修复] 使用正确的 cid，而不是 0
                                    val cid = miniPlayerManager.currentCid
                                    navController.navigate("video/$bvid?cid=$cid&cover=") {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                    
                    //  护眼模式覆盖层（最顶层，应用于所有内容）
                    EyeProtectionOverlay()

                    // PiP 模式专用播放器 (只在 PiP 模式下显示，覆盖所有内容)
                    if (isInPipMode) {
                        PiPVideoPlayer(miniPlayerManager = miniPlayerManager)
                    }
                    
                    // [New] Custom Splash Wallpaper Overlay
                    var showSplash by remember { mutableStateOf(SettingsManager.isSplashEnabledSync(context)) }
                    // [Optimization] If we delayed enough in splash screen, we might want to skip custom splash or show it briefly?
                    // Logic: If user uses custom splash, system splash shows icon, then custom splash shows wallpaper.
                    // If we use setKeepOnScreenCondition, system splash (icon) stays longer.
                    // This is acceptable behavior: Icon -> Wallpaper (if enabled) -> App.
                    // Or if custom wallpaper is enabled, maybe we shouldn't delay system splash?
                    // User request: "当用户看见遮罩的时候，异步加载首页视频". Mask usually means System Splash (Icon) OR Custom Wallpaper.
                    // Implementing delay on System Splash ensures data is likely ready when ANY content shows.
                    
                    val splashUri = remember { SettingsManager.getSplashWallpaperUriSync(context) }
                    
                    LaunchedEffect(Unit) {
                        if (showSplash && splashUri.isNotEmpty()) {
                            kotlinx.coroutines.delay(2000) // Display seconds
                            showSplash = false 
                        } else {
                            showSplash = false
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = showSplash && splashUri.isNotEmpty(),
                        exit = fadeOut(animationSpec = tween(1000)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            AsyncImage(
                                model = splashUri,
                                contentDescription = "Splash Wallpaper",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Optional: Skip button or Branding?
                            // For now, simple clear image.
                        }
                    }
                }
                }  // 📐 CompositionLocalProvider 结束
            }
        }
    }
    
    //  用户按 Home 键或切换应用时触发
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        
        Logger.d(TAG, "👋 onUserLeaveHint 触发, isInVideoDetail=$isInVideoDetail, isMiniMode=${miniPlayerManager.isMiniMode}")
        
        //  [重构] 使用新的模式判断方法
        val shouldEnterPip = miniPlayerManager.shouldEnterPip()
        val currentMode = miniPlayerManager.getCurrentMode()
        val isActuallyPlaying = miniPlayerManager.isPlaying || (miniPlayerManager.player?.isPlaying == true)
        
        //  🔧 [修复] PiP 只应在视频详情页触发，小窗模式下不应触发系统 PiP
        // 原因：小窗模式意味着用户已离开视频详情页（在首页等其他页面），
        // 此时从其他页面返回桌面不应进入 PiP
        val shouldTriggerPip = isInVideoDetail 
            && shouldEnterPip 
            && isActuallyPlaying
        
        Logger.d(TAG, " miniPlayerMode=$currentMode, shouldEnterPip=$shouldEnterPip, isPlaying=$isActuallyPlaying, shouldTriggerPip=$shouldTriggerPip, API=${Build.VERSION.SDK_INT}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && shouldTriggerPip) {
            try {
                Logger.d(TAG, " 尝试进入 PiP 模式...")
                
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                
                // Android 12+: 启用自动进入和无缝调整
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    pipParams.setAutoEnterEnabled(true)
                    pipParams.setSeamlessResizeEnabled(true)
                }
                
                enterPictureInPictureMode(pipParams.build())
                Logger.d(TAG, " 成功进入 PiP 模式")
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e(TAG, " 进入 PiP 失败", e)
            }
        } else {
            Logger.d(TAG, "⏳ 未满足 PiP 条件: API>=${Build.VERSION_CODES.O}=${Build.VERSION.SDK_INT >= Build.VERSION_CODES.O}, shouldTriggerPip=$shouldTriggerPip")
        }
    }
    
    //  PiP 模式变化回调
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        Logger.d(TAG, " PiP 模式变化: $isInPictureInPictureMode")
    }
    
    //  [新增] 处理 singleTop 模式下的新 Intent
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    
    // 📺 [DLNA] 权限请求和服务绑定 - 移除自动请求，改为按需请求
    // private val dlnaPermissionLauncher = ...
    
    // private fun requestDlnaPermissionsAndBind() { ... }
    
    //  待导航的视频 ID（用于在 Compose 中触发导航）
    var pendingVideoId by mutableStateOf<String?>(null)
    var pendingRoute by mutableStateOf<String?>(null)  // 🚀 App Shortcuts: pending route
        private set
    
    /**
     *  [新增] 处理 Deep Link 和分享意图
     */
    private fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        
        Logger.d(TAG, "🔗 handleIntent: action=${intent.action}, data=${intent.data}")
        
        when (intent.action) {
            android.content.Intent.ACTION_VIEW -> {
                // 点击链接打开
                val uri = intent.data
                if (uri != null) {
                    val scheme = uri.scheme ?: ""
                    val host = uri.host ?: ""
                    
                    // 🚀 App Shortcuts: bilipai:// scheme
                    if (scheme == "bilipai") {
                        pendingRoute = host  // e.g., "search", "dynamic", "favorite", "history"
                        Logger.d(TAG, "🚀 App Shortcut detected: $host")
                    }
                    // b23.tv 短链接需要重定向
                    else if (host.contains("b23.tv")) {
                        resolveShortLinkAndNavigate(uri.toString())
                    } else {
                        // bilibili.com 直接解析
                        val result = com.android.purebilibili.core.util.BilibiliUrlParser.parseUri(uri)
                        if (result.isValid) {
                            result.getVideoId()?.let { videoId ->
                                Logger.d(TAG, "📺 从 Deep Link 提取到视频: $videoId")
                                pendingVideoId = videoId
                            }
                        }
                    }
                }
            }
            android.content.Intent.ACTION_SEND -> {
                // 分享文本到 app
                val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (text != null) {
                    Logger.d(TAG, "📤 收到分享文本: $text")
                    
                    // 检查是否包含 b23.tv 短链接
                    val urls = com.android.purebilibili.core.util.BilibiliUrlParser.extractUrls(text)
                    val shortLink = urls.find { it.contains("b23.tv") }
                    
                    if (shortLink != null) {
                        resolveShortLinkAndNavigate(shortLink)
                    } else {
                        // 直接解析
                        val result = com.android.purebilibili.core.util.BilibiliUrlParser.parse(text)
                        if (result.isValid) {
                            result.getVideoId()?.let { videoId ->
                                Logger.d(TAG, "📺 从分享文本提取到视频: $videoId")
                                pendingVideoId = videoId
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     *  解析 b23.tv 短链接并导航
     */
    private fun resolveShortLinkAndNavigate(shortUrl: String) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val fullUrl = com.android.purebilibili.core.util.BilibiliUrlParser.resolveShortUrl(shortUrl)
            if (fullUrl != null) {
                val result = com.android.purebilibili.core.util.BilibiliUrlParser.parse(fullUrl)
                if (result.isValid) {
                    result.getVideoId()?.let { videoId ->
                        Logger.d(TAG, "📺 从短链接解析到视频: $videoId")
                        pendingVideoId = videoId
                    }
                }
            } else {
                Logger.w(TAG, "⚠️ 无法解析短链接: $shortUrl")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        com.android.purebilibili.feature.cast.DlnaManager.unbindService(this)
    }
}

/**
 * PiP 模式专用播放器 Composable
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PiPVideoPlayer(miniPlayerManager: MiniPlayerManager) {
    val player = miniPlayerManager.player
    
    if (player != null) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false // 隐藏控制器，由系统 PiP 窗口接管
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // 确保视频填充窗口
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                // 每次重组确保 player 是最新的
                if (view.player != player) {
                    view.player = player
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    } else {
        // 如果没有播放器，显示黑屏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }
}

/**
 *  首次启动欢迎弹窗 - 精美设计版
 */
