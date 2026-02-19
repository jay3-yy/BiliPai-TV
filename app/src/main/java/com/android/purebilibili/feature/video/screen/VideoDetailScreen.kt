// 文件路径: feature/video/screen/VideoDetailScreen.kt
package com.android.purebilibili.feature.video.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.KeyEvent
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.core.view.WindowCompat
import com.android.purebilibili.data.model.response.BgmInfo
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
//  已改用 MaterialTheme.colorScheme.primary

import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.VideoTag
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.model.response.ViewPoint
// Refactored UI components
import com.android.purebilibili.feature.video.ui.section.VideoTitleSection
import com.android.purebilibili.feature.video.ui.section.VideoTitleWithDesc
import com.android.purebilibili.feature.video.ui.section.UpInfoSection
import com.android.purebilibili.feature.video.ui.section.DescriptionSection
import com.android.purebilibili.feature.video.ui.section.ActionButtonsRow
import com.android.purebilibili.feature.video.ui.section.ActionButton
import com.android.purebilibili.feature.video.ui.components.RelatedVideosHeader
import com.android.purebilibili.feature.video.ui.components.RelatedVideoItem
import com.android.purebilibili.feature.video.ui.components.CoinDialog
import com.android.purebilibili.feature.video.ui.components.CollectionRow
import com.android.purebilibili.feature.video.ui.components.CollectionSheet
import com.android.purebilibili.feature.video.ui.components.PagesSelector
// Imports for moved classes
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.state.rememberVideoPlayerState
import com.android.purebilibili.feature.video.ui.section.VideoPlayerSection
import com.android.purebilibili.feature.video.ui.components.SubReplySheet
import com.android.purebilibili.feature.video.ui.components.ReplyHeader
import com.android.purebilibili.feature.video.ui.components.ReplyItemView

import com.android.purebilibili.feature.video.viewmodel.CommentSortMode  //  新增
import com.android.purebilibili.feature.video.ui.components.LikeBurstAnimation
import com.android.purebilibili.feature.video.ui.components.TripleSuccessAnimation
import com.android.purebilibili.feature.video.ui.components.VideoDetailSkeleton
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog  //  评论图片预览
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//  共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.feature.video.player.MiniPlayerManager
// 📱 [新增] 竖屏全屏
import com.android.purebilibili.feature.video.ui.overlay.PortraitFullscreenOverlay
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.ui.components.BottomInputBar // [New] Bottom Input Bar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.feature.video.ui.components.DanmakuContextMenu
import kotlin.math.abs

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun VideoDetailScreen(
    bvid: String,
    cid: Long = 0L,
    coverUrl: String = "",
    startInFullscreen: Boolean = false,
    transitionEnabled: Boolean = false,
    onBack: () -> Unit,
    onNavigateToAudioMode: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onVideoClick: (String, android.os.Bundle?) -> Unit,
    onUpClick: (Long) -> Unit = {},
    miniPlayerManager: MiniPlayerManager? = null,
    isInPipMode: Boolean = false,
    isVisible: Boolean = true,
    viewModel: PlayerViewModel = viewModel(),
    commentViewModel: VideoCommentViewModel = viewModel(),
    onBgmClick: (BgmInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val isTvDevice = rememberIsTvDevice()
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    // 🎭 [性能优化] 转场动画完成状态
    // 延迟加载重型组件（如评论区），确保转场动画流畅无卡顿
    var isTransitionFinished by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // 等待转场动画结束 (350ms + small buffer)
        // 配合 AppNavigation 中的 enterTransition duration
        kotlinx.coroutines.delay(400) 
        isTransitionFinished = true
    }
    
    // 🔄 [Seamless Playback] Internal BVID state to support seamless switching in portrait mode
    var currentBvid by remember(bvid) { mutableStateOf(bvid) }
    
    //  监听评论状态
    val commentState by commentViewModel.commentState.collectAsState()
    val subReplyState by commentViewModel.subReplyState.collectAsState()
    val commentDefaultSortMode by com.android.purebilibili.core.store.SettingsManager
        .getCommentDefaultSortMode(context)
        .collectAsState(
            initial = com.android.purebilibili.core.store.SettingsManager.getCommentDefaultSortModeSync(context)
        )
    val preferredCommentSortMode = remember(commentDefaultSortMode) {
        CommentSortMode.fromApiMode(commentDefaultSortMode)
    }
    val sortPreferenceScope = rememberCoroutineScope()
    val showFavoriteFolderDialog by viewModel.favoriteFolderDialogVisible.collectAsState()
    val favoriteFolders by viewModel.favoriteFolders.collectAsState()
    val isFavoriteFoldersLoading by viewModel.isFavoriteFoldersLoading.collectAsState()
    
    // [Blur] Haze State
    val hazeState = remember { HazeState() }
    
    //  空降助手 - 已由插件系统自动处理
    // val sponsorSegment by viewModel.currentSponsorSegment.collectAsState()
    // val showSponsorSkipButton by viewModel.showSkipButton.collectAsState()
    // val sponsorBlockEnabled by com.android.purebilibili.core.store.SettingsManager
    //     .getSponsorBlockEnabled(context)
    //     .collectAsState(initial = false)

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val tvDetailPlayerFocusRequester = remember { FocusRequester() }
    val tvDetailInfoFocusRequester = remember { FocusRequester() }
    val tvDetailRelatedFocusRequester = remember { FocusRequester() }
    var tvDetailFocusTarget by remember { mutableStateOf(VideoDetailTvFocusTarget.PLAYER) }
    val requestTvDetailFocusTarget: (VideoDetailTvFocusTarget) -> Unit = remember(
        tvDetailPlayerFocusRequester,
        tvDetailInfoFocusRequester,
        tvDetailRelatedFocusRequester
    ) {
        { target ->
            when (target) {
                VideoDetailTvFocusTarget.PLAYER -> tvDetailPlayerFocusRequester.requestFocus()
                VideoDetailTvFocusTarget.INFO -> tvDetailInfoFocusRequester.requestFocus()
                VideoDetailTvFocusTarget.RELATED -> tvDetailRelatedFocusRequester.requestFocus()
            }
        }
    }
    
    // 📐 [大屏适配] 仅 Expanded 才启用平板分栏布局
    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
    val useTabletLayout = shouldUseTabletVideoLayout(
        isExpandedScreen = windowSizeClass.isExpandedScreen,
        isTvDevice = isTvDevice
    )
    
    // 🔧 [修复] 追踪用户是否主动请求全屏（点击全屏按钮）
    // 使用 rememberSaveable 确保状态在横竖屏切换时保持
    var userRequestedFullscreen by rememberSaveable { mutableStateOf(false) }
    
    // 📐 全屏模式逻辑：
    // - 手机：横屏时自动进入全屏
    // - 大屏（Expanded）：只有用户主动点击全屏按钮后才进入全屏
    val isFullscreenMode = if (useTabletLayout) {
        userRequestedFullscreen
    } else {
        isLandscape
    }

    LaunchedEffect(isTvDevice, isFullscreenMode, useTabletLayout) {
        if (isTvDevice && !isFullscreenMode && !useTabletLayout) {
            tvDetailFocusTarget = resolveInitialVideoDetailTvFocusTarget(isTv = true)
                ?: VideoDetailTvFocusTarget.PLAYER
            requestTvDetailFocusTarget(tvDetailFocusTarget)
        }
    }

    var isPipMode by remember { mutableStateOf(isInPipMode) }
    LaunchedEffect(isInPipMode) { isPipMode = isInPipMode }
    
    //  [新增] 监听定时关闭状态
    val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
    
    // 📖 [新增] 监听视频章节数据
    // 📖 [新增] 监听视频章节数据
    val viewPoints by viewModel.viewPoints.collectAsState()
    
    // [New] Codec & Audio Preferences
    val codecPreference by viewModel.videoCodecPreference.collectAsState(initial = "hev1")
    val audioQualityPreference by viewModel.audioQualityPreference.collectAsState(initial = -1)
    
    //  [PiP修复] 记录视频播放器在屏幕上的位置，用于PiP窗口只显示视频区域
    var videoPlayerBounds by remember { mutableStateOf<android.graphics.Rect?>(null) }
    
    // 📱 [优化] isPortraitFullscreen 和 isVerticalVideo 现在从 playerState 获取（见 playerState 定义后）
    
    // 🔁 [新增] 播放模式状态
    val currentPlayMode by com.android.purebilibili.feature.video.player.PlaylistManager.playMode.collectAsState()
    
    //  从小窗展开时自动进入全屏
    LaunchedEffect(startInFullscreen) {
        if (startInFullscreen) {
            if (useTabletLayout) {
                userRequestedFullscreen = true
            } else if (!isLandscape) {
                context.findActivity()?.let { activity ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
        }
    }

    //  用于跟踪组件是否正在退出，防止 SideEffect 覆盖恢复操作
    var isScreenActive by remember { mutableStateOf(true) }
    
    //  [关键] 保存进入前的状态栏配置（在 DisposableEffect 外部定义以便复用）
    val activity = remember { context.findActivity() }
    val window = remember { activity?.window }
    val insetsController = remember {
        if (window != null && activity != null) {
            WindowCompat.getInsetsController(window, window.decorView)
        } else null
    }
    val originalStatusBarColor = remember { window?.statusBarColor ?: android.graphics.Color.TRANSPARENT }
    val originalLightStatusBars = remember { insetsController?.isAppearanceLightStatusBars ?: true }
    
    //  [新增] 恢复状态栏的函数（可复用）
    val restoreStatusBar = remember {
        {
            if (window != null && insetsController != null) {
                insetsController.isAppearanceLightStatusBars = originalLightStatusBars
                window.statusBarColor = originalStatusBarColor
            }
        }
    }
    
    //  [修复] 包装的 onBack，在导航之前立即恢复状态栏并通知小窗管理器
    var isActuallyLeaving by remember { mutableStateOf(false) }

    val handleBack = remember(onBack, miniPlayerManager) {
        {
            isActuallyLeaving = true // 标记确实是用户通过点击或返回键离开
            isScreenActive = false  // 标记页面正在退出
            // 🎯 通知小窗管理器这是用户主动导航离开（用于控制后台音频）
            miniPlayerManager?.markLeavingByNavigation()
            
            restoreStatusBar()      //  立即恢复状态栏（动画开始前）
            onBack()                // 执行实际的返回导航
        }
    }
    
    // 🔄 [新增] 自动横竖屏切换 - 跟随手机传感器方向
    val autoRotateEnabled by com.android.purebilibili.core.store.SettingsManager
        .getAutoRotateEnabled(context).collectAsState(initial = false)
    
    LaunchedEffect(autoRotateEnabled) {
        if (!useTabletLayout) {  // 只对手机生效
            activity?.requestedOrientation = if (autoRotateEnabled) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR  // 传感器控制，跟随手机方向
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT  // 锁定竖屏
            }
            com.android.purebilibili.core.util.Logger.d(
                "VideoDetailScreen", 
                "🔄 Auto-rotate: enabled=$autoRotateEnabled, orientation=${if (autoRotateEnabled) "SENSOR" else "PORTRAIT"}"
            )
        }
    }
    
    // 退出重置亮度 +  屏幕常亮管理 + 状态栏恢复（作为安全网）
    // 追踪是否正在导航到音频模式（防止取消通知）
    var isNavigatingToAudioMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        //  [沉浸式] 启用边到边显示，让内容延伸到状态栏下方
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        
        //  [修复] 进入视频页时保持屏幕常亮，防止自动熄屏
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            //  [关键] 标记页面正在退出，防止 SideEffect 覆盖
            isScreenActive = false
            
            // 🎯 [修复] 通知小窗管理器这是导航离开（用于控制后台音频）
            // 移动到这里以支持预测性返回手势（原来在 BackHandler 中会阻止手势动画）
            // [修复] 如果是导航到音频模式，不要标记为离开（否则会触发自动暂停）
            // ⚠️ [MOVED] Logic moved to a later DisposableEffect to ensure it runs BEFORE playerState disposal
            // if (!isNavigatingToAudioMode) {
            //    miniPlayerManager?.markLeavingByNavigation()
            // }
            
            // 🎯 [新增] 标记正在返回，跳过首页卡片入场动画
            // 这确保共享元素返回动画正常播放（不被卡片入场动画干扰）
            com.android.purebilibili.core.util.CardPositionManager.markReturning()
            
            val layoutParams = window?.attributes
            layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = layoutParams
            
            //  [修复] 离开视频页时取消屏幕常亮
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            //  [安全网] 确保状态栏被恢复（以防 handleBack 未被调用，如系统返回）
            restoreStatusBar()

            // 🔧 [修复] 退出视频页时重置 PiP 参数，防止其他页面自动进入 PiP
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                activity?.let { act ->
                    try {
                        val pipParams = android.app.PictureInPictureParams.Builder()
                            .setAutoEnterEnabled(false)  // 关闭自动进入 PiP
                            .build()
                        act.setPictureInPictureParams(pipParams)
                        com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", 
                            "🔧 退出页面：重置 PiP autoEnterEnabled=false")
                    } catch (e: Exception) {
                        com.android.purebilibili.core.util.Logger.e("VideoDetailScreen", 
                            "重置 PiP 参数失败", e)
                    }
                }
            }
            
            // 🔕 [修复] 退出视频页时取消媒体通知（防止状态不同步）
            //  [关键修复] 如果是导航到音频模式，则保留通知！
            if (!isNavigatingToAudioMode) {
                val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) 
                    as android.app.NotificationManager
                notificationManager.cancel(1001)  // NOTIFICATION_ID from VideoPlayerState
            }
            
            // 恢复屏幕方向
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    
    //  新增：监听消息事件（关注/收藏反馈）- 使用居中弹窗
    var popupMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            popupMessage = message
            // 2秒后自动隐藏
            kotlinx.coroutines.delay(2000)
            popupMessage = null
        }
    }
    
    //  [新增] 监听弹幕发送事件 - 将发送的弹幕显示在屏幕上
    val danmakuManager = rememberDanmakuManager()
    LaunchedEffect(Unit) {
        viewModel.danmakuSentEvent.collect { danmakuData ->
            android.util.Log.d("VideoDetailScreen", "📺 Displaying sent danmaku: ${danmakuData.text}")
            danmakuManager.addLocalDanmaku(
                text = danmakuData.text,
                color = danmakuData.color,
                mode = danmakuData.mode,
                fontSize = danmakuData.fontSize
            )
        }
    }
    
    //  初始化进度持久化存储
    LaunchedEffect(Unit) {
        viewModel.initWithContext(context)
        //  [埋点] 页面浏览追踪
        com.android.purebilibili.core.util.AnalyticsHelper.logScreenView("VideoDetailScreen")
    }
    
    //  [PiP修复] 当视频播放器位置更新时，同步更新PiP参数
    //  [修复] 只有 SYSTEM_PIP 模式才启用自动进入PiP
    val pipModeEnabled = remember { 
        com.android.purebilibili.core.store.SettingsManager.getMiniPlayerModeSync(context) == 
            com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.SYSTEM_PIP
    }
    
    // 🔧 [性能优化] 记录上次设置的 PiP bounds，避免重复设置
    var lastPipBounds by remember { mutableStateOf<android.graphics.Rect?>(null) }
    var lastPipModeEnabled by remember { mutableStateOf<Boolean?>(null) }
    var lastPipUpdateElapsedMs by remember { mutableStateOf(0L) }
    val hasMeaningfulBoundsChange = remember {
        { oldBounds: android.graphics.Rect?, newBounds: android.graphics.Rect? ->
            when {
                oldBounds == null && newBounds == null -> false
                oldBounds == null || newBounds == null -> true
                else -> {
                    abs(oldBounds.left - newBounds.left) > 3 ||
                        abs(oldBounds.top - newBounds.top) > 3 ||
                        abs(oldBounds.right - newBounds.right) > 3 ||
                        abs(oldBounds.bottom - newBounds.bottom) > 3
                }
            }
        }
    }
    
    LaunchedEffect(videoPlayerBounds, pipModeEnabled) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val modeChanged = lastPipModeEnabled == null || lastPipModeEnabled != pipModeEnabled
            val boundsChanged = hasMeaningfulBoundsChange(lastPipBounds, videoPlayerBounds)
            if (!modeChanged) {
                // OFF 模式只在切换当下更新一次，不跟随布局抖动反复设置
                if (!pipModeEnabled) return@LaunchedEffect
                if (!boundsChanged) return@LaunchedEffect

                // 对布局连续变化节流，避免每帧 setPictureInPictureParams
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastPipUpdateElapsedMs < 400L) return@LaunchedEffect
                lastPipUpdateElapsedMs = now
            } else {
                lastPipUpdateElapsedMs = android.os.SystemClock.elapsedRealtime()
            }

            lastPipBounds = videoPlayerBounds?.let { android.graphics.Rect(it) }
            lastPipModeEnabled = pipModeEnabled
            
            activity?.let { act ->
                val pipParamsBuilder = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                
                //  设置源矩形区域 - PiP只显示视频播放器区域
                videoPlayerBounds?.let { bounds ->
                    pipParamsBuilder.setSourceRectHint(bounds)
                }
                
                // Android 12+ 支持手势自动进入 PiP -  只有 SYSTEM_PIP 模式才启用
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    pipParamsBuilder.setAutoEnterEnabled(pipModeEnabled)  //  受设置控制
                    pipParamsBuilder.setSeamlessResizeEnabled(pipModeEnabled)
                }
                
                act.setPictureInPictureParams(pipParamsBuilder.build())
                com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", 
                    " PiP参数更新: autoEnterEnabled=$pipModeEnabled, modeChanged=$modeChanged, boundsChanged=$boundsChanged")
            }
        }
    }

    // 📱 [修复] 提升竖屏全屏状态到 Screen 级别，防止 VideoPlayerState 重建时状态丢失
    var isPortraitFullscreen by rememberSaveable { mutableStateOf(false) }
    val useSharedPortraitPlayer = true
    var portraitSyncSnapshotBvid by remember { mutableStateOf<String?>(null) }
    var portraitSyncSnapshotPositionMs by remember { mutableLongStateOf(0L) }
    var hasPendingPortraitSync by remember { mutableStateOf(false) }
    var pendingMainReloadBvidAfterPortrait by remember { mutableStateOf<String?>(null) }

    // 初始化播放器状态
    val playerState = rememberVideoPlayerState(
        context = context,
        viewModel = viewModel,
        bvid = currentBvid,
        startPaused = isPortraitFullscreen && !useSharedPortraitPlayer
    )

    var hasAppliedInitialPageSwitch by remember(currentBvid, cid) { mutableStateOf(false) }
    LaunchedEffect(uiState, currentBvid, cid, hasAppliedInitialPageSwitch) {
        if (hasAppliedInitialPageSwitch) return@LaunchedEffect
        val success = uiState as? PlayerUiState.Success ?: return@LaunchedEffect
        if (success.info.bvid != currentBvid) return@LaunchedEffect

        val targetPageIndex = resolveInitialPageIndex(
            requestedCid = cid,
            currentCid = success.info.cid,
            pages = success.info.pages
        )
        hasAppliedInitialPageSwitch = true
        if (targetPageIndex != null) {
            viewModel.switchPage(targetPageIndex)
        }
    }

    // 🎯 [修复] 确保在 VideoPlayerState 销毁之前通知 MiniPlayerManager 页面退出
    // 必须在 playerState 之后声明此 Effect，这样它会在 playerState.onDispose 之前执行（LIFO 顺序）
    DisposableEffect(playerState) {
        onDispose {
            // 标记页面正在退出
            // [修复] 仅在明确用户离开时标记导航退出。
            // 系统返回手势的兜底由 AppNavigation 处理，避免“视频A->视频B”切换时误标记离开。
            // 配置切换时不标记离开，避免旋转屏幕误暂停。
            val isChangingConfigurations = activity?.isChangingConfigurations == true
            if (isActuallyLeaving && !isNavigatingToAudioMode && !isChangingConfigurations) {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoDetailScreen",
                    "🛑 Disposing screen as navigation exit, notifying MiniPlayerManager"
                )
                miniPlayerManager?.markLeavingByNavigation()
            } else {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoDetailScreen",
                    "💤 Screen disposed without navigation-exit mark (isActuallyLeaving=$isActuallyLeaving, audioMode=$isNavigatingToAudioMode, changingConfig=$isChangingConfigurations)"
                )
            }
        }
    }
    
    //  [性能优化] 生命周期感知：进入后台时暂停播放，返回前台时继续
    //  [修复] 此处逻辑已移至 VideoPlayerState.kt 统一处理
    // 删除冗余的暂停逻辑，避免与 VideoPlayerState 中的生命周期处理冲突
    // VideoPlayerState 会检查 PiP/小窗模式来决定是否暂停
    
    // 📱 [优化] 竖屏视频检测已移至 VideoPlayerState 集中管理
    val isVerticalVideo by playerState.isVerticalVideo.collectAsState()
    val enterPortraitFullscreen = {
        portraitSyncSnapshotBvid = (uiState as? PlayerUiState.Success)?.info?.bvid
        portraitSyncSnapshotPositionMs = playerState.player.currentPosition.coerceAtLeast(0L)
        hasPendingPortraitSync = false
        isPortraitFullscreen = true
    }
    val shouldMirrorPortraitProgressToMainPlayer = com.android.purebilibili.feature.video.ui.pager
        .shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = useSharedPortraitPlayer)

    val tryApplyPortraitProgressSync = remember(playerState, viewModel) {
        { snapshotBvid: String?, snapshotPositionMs: Long ->
            val currentBvid = (viewModel.uiState.value as? PlayerUiState.Success)?.info?.bvid
            if (!com.android.purebilibili.feature.video.ui.pager.shouldApplyPortraitProgressSync(
                    snapshotBvid = snapshotBvid,
                    currentBvid = currentBvid
                )
            ) {
                false
            } else {
                playerState.player.seekTo(snapshotPositionMs.coerceAtLeast(0L))
                true
            }
        }
    }

    
    
    // 同步状态到 playerState (可选，用于日志或内部逻辑)
    LaunchedEffect(isPortraitFullscreen) {
        playerState.setPortraitFullscreen(isPortraitFullscreen)
        viewModel.setPortraitPlaybackSessionActive(isPortraitFullscreen)
        val shouldPauseMainPlayer = com.android.purebilibili.feature.video.ui.pager
            .shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = useSharedPortraitPlayer)
        if (isPortraitFullscreen) {
            if (shouldPauseMainPlayer) {
                playerState.player.pause()
                playerState.player.volume = 0f
                playerState.player.playWhenReady = false
            }
            portraitSyncSnapshotBvid = (uiState as? PlayerUiState.Success)?.info?.bvid
            portraitSyncSnapshotPositionMs = playerState.player.currentPosition.coerceAtLeast(0L)
            hasPendingPortraitSync = shouldPauseMainPlayer
        } else {
             if (shouldPauseMainPlayer) {
                 // 退出时恢复音量 (不自动播放，等待用户操作或 onResume)
                 playerState.player.volume = 1f
             }
            val currentUiBvid = (viewModel.uiState.value as? PlayerUiState.Success)?.info?.bvid
            val targetBvid = pendingMainReloadBvidAfterPortrait ?: portraitSyncSnapshotBvid
            if (com.android.purebilibili.feature.video.ui.pager.shouldReloadMainPlayerAfterPortraitExit(
                    snapshotBvid = targetBvid,
                    currentBvid = currentUiBvid
                )
            ) {
                viewModel.loadVideo(targetBvid!!, autoPlay = true)
            }
            pendingMainReloadBvidAfterPortrait = null
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.setPortraitPlaybackSessionActive(false)
        }
    }

    LaunchedEffect(
        uiState,
        hasPendingPortraitSync,
        portraitSyncSnapshotBvid,
        portraitSyncSnapshotPositionMs
    ) {
        if (hasPendingPortraitSync && tryApplyPortraitProgressSync(
                portraitSyncSnapshotBvid,
                portraitSyncSnapshotPositionMs
            )
        ) {
            hasPendingPortraitSync = false
        }
    }

    // 📲 小窗模式（手机/平板统一逻辑）
    val handlePipClick = {
        // 使用 MiniPlayerManager 进入应用内小窗模式
        miniPlayerManager?.let { manager ->
            val stopPlaybackOnExit = com.android.purebilibili.core.store.SettingsManager
                .getStopPlaybackOnExitSync(context)
            if (stopPlaybackOnExit) {
                com.android.purebilibili.core.util.Logger.d(
                    "VideoDetailScreen",
                    "Stop-on-exit enabled, skip mini mode and leave page directly"
                )
                manager.markLeavingByNavigation()
                onBack()
                return@let
            }

            //  [埋点] PiP 进入事件
            com.android.purebilibili.core.util.AnalyticsHelper.logPictureInPicture(
                videoId = currentBvid,
                action = "enter_mini"
            )

            // 1. 将当前播放器信息传递给小窗管理器
            val info = uiState as? PlayerUiState.Success
            manager.setVideoInfo(
                bvid = currentBvid,
                title = info?.info?.title ?: "",
                cover = info?.info?.pic ?: "",
                owner = info?.info?.owner?.name ?: "",
                cid = info?.info?.cid ?: 0L,
                aid = info?.info?.aid ?: 0L,
                externalPlayer = playerState.player
            )

            // 2. 进入小窗模式（强制，不管当前模式设置）
            manager.enterMiniMode(forced = true)

            // 3. 返回上一页（首页）
            onBack()
        } ?: run {
            // 如果 miniPlayerManager 不存在，直接返回
            com.android.purebilibili.core.util.Logger.w("VideoDetailScreen", "⚠️ miniPlayerManager 为 null，无法进入小窗")
            onBack()
        }
    }

    // 🔧 [性能优化] 记录上次缓存的 bvid，避免重复缓存 MiniPlayer 信息
    var lastCachedMiniPlayerBvid by remember { mutableStateOf<String?>(null) }
    
    //  核心修改：初始化评论 & 媒体中心信息
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.Success) {
            val info = (uiState as PlayerUiState.Success).info
            val success = uiState as PlayerUiState.Success
            
            // 初始化评论（传入 UP 主 mid 用于筛选）- 保持在主线程
            commentViewModel.init(info.aid, info.owner.mid, preferredCommentSortMode)
            
            playerState.updateMediaMetadata(
                title = info.title,
                artist = info.owner.name,
                coverUrl = info.pic
            )
            
            // 📱 [双重验证] 从 API dimension 字段设置预判断值
            info.dimension?.let { dim ->
                playerState.setApiDimension(dim.width, dim.height)
            }
            
            //  同步视频信息到小窗管理器（为小窗模式做准备）
            //  🚀 [性能优化] 将繁重的序列化和缓存操作移至后台线程，防止主线程卡顿
            // 🔧 [性能优化] 只有首次加载或视频切换时才缓存 MiniPlayer 信息
            val shouldCacheMiniPlayer = lastCachedMiniPlayerBvid != currentBvid
            
            if (miniPlayerManager != null && shouldCacheMiniPlayer) {
                lastCachedMiniPlayerBvid = currentBvid
                
                launch(Dispatchers.Default) {
                    com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", "🔄 [Background] Preparing MiniPlayer info...")
                    
                    // 准备数据
                    // 注意：这里访问外部变量需要确保线程安全，但在 Compose 中读取 State 是安全的
                    // setVideoInfo 只是设置数据，通常是线程安全的或者内部做了处理
                    // cacheUiState 涉及序列化，必须在后台
                    
                    withContext(Dispatchers.Main) {
                        miniPlayerManager.setVideoInfo(
                            bvid = currentBvid,
                            title = info.title,
                            cover = info.pic,
                            owner = info.owner.name,
                            cid = info.cid,  //  传递 cid 用于弹幕加载
                            aid = info.aid,
                            externalPlayer = playerState.player,
                            fromLeft = com.android.purebilibili.core.util.CardPositionManager.isCardOnLeft  //  传递入场方向
                        )
                    }
                    
                    // 序列化缓存 (Heavy Operation)
                    miniPlayerManager.cacheUiState(success)
                    com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", "✅ [Background] MiniPlayer info cached")
                }
            } else if (miniPlayerManager == null) {
                android.util.Log.w("VideoDetailScreen", " miniPlayerManager 是 null!")
            }
        } else if (uiState is PlayerUiState.Loading) {
            playerState.updateMediaMetadata(
                title = "加载中...",
                artist = "",
                coverUrl = coverUrl
            )
        }
    }
    
    //  弹幕加载逻辑已移至 VideoPlayerState 内部处理
    // 避免在此处重复消耗 InputStream

    // 辅助函数：切换全屏状态
    val toggleFullscreen = {
        val activity = context.findActivity()
        if (activity != null) {
            if (useTabletLayout) {
                // 🖥️ 平板：仅切换 UI 状态，不改变屏幕方向
                // [修复] 如果退出全屏且是手机（sw < 600），强制转回竖屏
                val wasFullscreen = userRequestedFullscreen
                userRequestedFullscreen = !userRequestedFullscreen
                
                if (wasFullscreen && !userRequestedFullscreen) {
                    // check if it is a phone
                    if (configuration.smallestScreenWidthDp < 600 && !isTvDevice) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
            } else {
                // 📱 手机：通过旋转屏幕触发全屏
                if (isLandscape) {
                    userRequestedFullscreen = false
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    userRequestedFullscreen = true
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
        }
    }

    //  拦截系统返回键：如果是全屏模式，则先退出全屏
    BackHandler(enabled = isFullscreenMode) {
        toggleFullscreen()
    }
    
    // 📱 拦截系统返回键：如果是竖屏全屏模式，则先退出竖屏全屏
    BackHandler(enabled = isPortraitFullscreen) {
        isPortraitFullscreen = false
    }
    
    // 📱 [新增] 拦截系统返回键：手机横屏进入了平板分栏模式，应切换回竖屏而非退出
    val isPhoneInLandscapeSplitView = shouldRotateToPortraitOnSplitBack(
        useTabletLayout = useTabletLayout,
        smallestScreenWidthDp = configuration.smallestScreenWidthDp,
        orientation = configuration.orientation,
        isTvDevice = isTvDevice
    )
    
    BackHandler(enabled = isPhoneInLandscapeSplitView && !isFullscreenMode && !isPortraitFullscreen) {
        com.android.purebilibili.core.util.Logger.d(
            "VideoDetailScreen", 
            "📱 System back pressed in phone landscape split-view, rotating to PORTRAIT"
        )
        val activity = context.findActivity()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // 🎯 [移除] 以下 BackHandler 会阻止 Compose Navigation 的预测性返回手势动画
    // CardPositionManager.markReturning() 已在 onDispose 中处理（见下方修改）
    // BackHandler(enabled = !isFullscreenMode && !isPortraitFullscreen, onBack = handleBack)
    
    
    // 清理逻辑（markLeavingByNavigation、restoreStatusBar）已移至 DisposableEffect.onDispose

    // 沉浸式状态栏控制
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = remember(backgroundColor) { backgroundColor.luminance() > 0.5f }

    //  iOS风格：竖屏时状态栏黑色背景（与播放器融为一体）
    //  只在页面活跃时修改状态栏，避免退出时覆盖恢复操作
    if (!view.isInEditMode && isScreenActive) {
        SideEffect {
            val window = (view.context.findActivity())?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)

            if (isFullscreenMode) {
                // 📱 手机全屏隐藏状态栏
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = Color.Black.toArgb()
            } else {
                //  [沉浸式] 非全屏模式：状态栏透明，让视频延伸到状态栏下方
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                insetsController.isAppearanceLightStatusBars = false  // 白色图标（视频区域是深色的）
                window.statusBarColor = Color.Transparent.toArgb()  // 透明状态栏
                window.navigationBarColor = Color.Transparent.toArgb()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isFullscreenMode) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // 📐 [平板适配] 全屏模式过渡动画（只有手机横屏才进入全屏）
        if (isFullscreenMode) {
            VideoPlayerSection(
                playerState = playerState,
                uiState = uiState,
                isFullscreen = true,
                isInPipMode = isPipMode,
                onToggleFullscreen = { toggleFullscreen() },
                onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                onBack = { toggleFullscreen() },
                // 🔗 [新增] 分享功能
                bvid = bvid,
                coverUrl = coverUrl,
                //  实验性功能：双击点赞
                onDoubleTapLike = { viewModel.toggleLike() },
                //  [新增] 重载视频
                onReloadVideo = { viewModel.reloadVideo() },
                //  [新增] CDN 线路切换
                cdnCount = (uiState as? PlayerUiState.Success)?.cdnCount ?: 1,
                onSwitchCdn = { viewModel.switchCdn() },
                onSwitchCdnTo = { viewModel.switchCdnTo(it) },

                // [New] Codec & Audio (Fullscreen)
                currentCodec = codecPreference,
                onCodecChange = { viewModel.setVideoCodec(it) },
                currentAudioQuality = audioQualityPreference,
                onAudioQualityChange = { viewModel.setAudioQuality(it) },
                // [New] Audio Language
                onAudioLangChange = { viewModel.changeAudioLanguage(it) },
                
                //  [新增] 音频模式
                isAudioOnly = false, // 全屏模式只有视频
                onAudioOnlyToggle = { 
                    viewModel.setAudioMode(true)
                    isNavigatingToAudioMode = true // [Fix] Set flag to prevent notification cancellation
                    onNavigateToAudioMode()
                },
                
                //  [新增] 定时关闭
                sleepTimerMinutes = sleepTimerMinutes,
                onSleepTimerChange = { viewModel.setSleepTimer(it) },
                
                // 🖼️ [新增] 视频预览图数据
                    videoshotData = (uiState as? PlayerUiState.Success)?.videoshotData,
                    
                    // 📖 [新增] 视频章节数据
                    viewPoints = viewPoints,
                // 📱 [新增] 竖屏全屏模式
                isVerticalVideo = isVerticalVideo,
                isPortraitFullscreen = isPortraitFullscreen,
                onPortraitFullscreen = {
                    if (!isPortraitFullscreen) {
                        if (isFullscreenMode) {
                            toggleFullscreen()
                        }
                        enterPortraitFullscreen()
                    } else {
                        isPortraitFullscreen = false
                    }
                },
                // 🔁 [新增] 播放模式
                currentPlayMode = currentPlayMode,
                onPlayModeClick = { com.android.purebilibili.feature.video.player.PlaylistManager.togglePlayMode() },

                // [New Actions]
                onSaveCover = { viewModel.saveCover(context) },
                onDownloadAudio = { viewModel.downloadAudio(context) },
                
                // [新增] 侧边栏抽屉数据与交互
                relatedVideos = (uiState as? PlayerUiState.Success)?.related ?: emptyList(),
                ugcSeason = (uiState as? PlayerUiState.Success)?.info?.ugc_season,
                isFollowed = (uiState as? PlayerUiState.Success)?.isFollowing ?: false,
                isLiked = (uiState as? PlayerUiState.Success)?.isLiked ?: false,
                isCoined = (uiState as? PlayerUiState.Success)?.coinCount?.let { it > 0 } ?: false,
                isFavorited = (uiState as? PlayerUiState.Success)?.isFavorited ?: false,
                onToggleFollow = { viewModel.toggleFollow() },
                onToggleLike = { viewModel.toggleLike() },
                onCoin = { viewModel.showCoinDialog() },
                onToggleFavorite = { viewModel.showFavoriteFolderDialog() },
                onTriple = { viewModel.doTripleAction() },
                onRelatedVideoClick = onVideoClick,
                tvFocusRequester = tvDetailPlayerFocusRequester
            )
        } else {
                //  沉浸式布局：视频延伸到状态栏 + 内容区域
                //  📐 [大屏适配] 仅 Expanded 使用分栏布局
                
                //  📐 [大屏适配] 根据设备类型选择布局
                if (useTabletLayout) {
                    // 🖥️ 平板：左右分栏布局（视频+信息 | 评论/推荐）
                    TabletCinemaLayout(
                        playerState = playerState,
                        uiState = uiState,
                        commentState = commentState,
                        viewModel = viewModel,
                        commentViewModel = commentViewModel,
                        configuration = configuration,
                        isVerticalVideo = isVerticalVideo,
                        sleepTimerMinutes = sleepTimerMinutes,

                        viewPoints = viewPoints,
                        bvid = bvid,
                        coverUrl = coverUrl,
                        onBack = {
                            // 📱 手机误入平板模式（如横屏宽度触发 Expanded），点击返回应切换回竖屏
                            // 🔧 [修复] 检查 smallestScreenWidthDp 确保这不是真正的平板
                            val smallestWidth = configuration.smallestScreenWidthDp
                            val currentOrientation = configuration.orientation
                            val shouldRotatePortrait = shouldRotateToPortraitOnSplitBack(
                                useTabletLayout = true,
                                smallestScreenWidthDp = smallestWidth,
                                orientation = currentOrientation,
                                isTvDevice = isTvDevice
                            )
                            
                            com.android.purebilibili.core.util.Logger.d(
                                "VideoDetailScreen", 
                                "📱 onBack clicked: smallestWidth=$smallestWidth, shouldRotatePortrait=$shouldRotatePortrait, " +
                                "orientation=$currentOrientation, " +
                                "activity=${activity != null}"
                            )
                            
                            if (shouldRotatePortrait) {
                                com.android.purebilibili.core.util.Logger.d(
                                    "VideoDetailScreen", 
                                    "📱 Rotating to PORTRAIT"
                                )
                                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                com.android.purebilibili.core.util.Logger.d(
                                    "VideoDetailScreen", 
                                    "📱 Calling handleBack()"
                                )
                                handleBack()
                            }
                        },
                        onUpClick = onUpClick,
                        onNavigateToAudioMode = {
                            isNavigatingToAudioMode = true // [Fix] Set flag to prevent notification cancellation
                            onNavigateToAudioMode()
                        },
                        onToggleFullscreen = { toggleFullscreen() },  // 📺 平板全屏切换
                        isInPipMode = isPipMode,
                        onPipClick = handlePipClick,
                        isPortraitFullscreen = isPortraitFullscreen,

                        transitionEnabled = transitionEnabled,  //  传递过渡动画开关
                        // [New] Codec & Audio
                        currentCodec = codecPreference,
                        onCodecChange = { viewModel.setVideoCodec(it) },
                        currentAudioQuality = audioQualityPreference,
                        onAudioQualityChange = { viewModel.setAudioQuality(it) },
                        onRelatedVideoClick = onVideoClick,
                        // 🔁 [新增] 播放模式
                        currentPlayMode = currentPlayMode,
                        onPlayModeClick = { com.android.purebilibili.feature.video.player.PlaylistManager.togglePlayMode() }
                    )
                } else {
                    // 📱 手机竖屏：原有单列布局
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val screenWidthDp = configuration.screenWidthDp.dp
                    val videoHeight = screenWidthDp * 9f / 16f  // 16:9 比例

                    //  读取上滑隐藏播放器设置
                    val swipeHidePlayerEnabled by com.android.purebilibili.core.store.SettingsManager
                        .getSwipeHidePlayerEnabled(context).collectAsState(initial = false)
                    
                    // 📏 [Collapsing Player] 上滑隐藏播放器逻辑
                    val videoHeightPx = with(LocalDensity.current) { videoHeight.toPx() }
                    var playerHeightOffsetPx by remember { mutableFloatStateOf(0f) }
                    
                    // 当设置关闭时，重置高度
                    LaunchedEffect(swipeHidePlayerEnabled) {
                        if (!swipeHidePlayerEnabled) playerHeightOffsetPx = 0f
                    }

                    val nestedScrollConnection = remember(swipeHidePlayerEnabled, isPortraitFullscreen) {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                if (!swipeHidePlayerEnabled || isPortraitFullscreen) return Offset.Zero
                                
                                val delta = available.y
                                // 上滑 (delta < 0)：隐藏播放器，消费滚动
                                if (delta < 0) {
                                    val newOffset = playerHeightOffsetPx + delta
                                    val coercedOffset = newOffset.coerceIn(-videoHeightPx, 0f)
                                    val consumed = coercedOffset - playerHeightOffsetPx
                                    playerHeightOffsetPx = coercedOffset
                                    return Offset(0f, consumed)
                                }
                                return Offset.Zero
                            }

                            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                                if (!swipeHidePlayerEnabled || isPortraitFullscreen) return Offset.Zero
                                
                                val delta = available.y
                                // 下滑 (delta > 0)：显示播放器 (且 available > 0 说明内容已滚到顶)
                                if (delta > 0) {
                                     val newOffset = playerHeightOffsetPx + delta
                                     val coercedOffset = newOffset.coerceIn(-videoHeightPx, 0f)
                                     val consumedDelta = coercedOffset - playerHeightOffsetPx
                                     playerHeightOffsetPx = coercedOffset
                                     return Offset(0f, consumedDelta)
                                }
                                return Offset.Zero
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection)
                    ) {
                    
                    //  播放器隐藏状态（用于动画）
                    //  播放器隐藏状态（用于动画）
                    //  当 playerHeightOffsetPx 为 -videoHeightPx 时，高度只剩 statusBarHeight
                    //  [Fix] 竖屏全屏模式下强制高度不受偏移影响
                    val playerHeightOffset = if (isPortraitFullscreen) 0f else playerHeightOffsetPx
                    val animatedPlayerHeight = videoHeight + statusBarHeight + with(LocalDensity.current) { playerHeightOffset.toDp() }
                    
                    //  注意：移除了状态栏黑色 Spacer
                    // 播放器将延伸到状态栏下方，共享元素过渡更流畅
                    
                    //  注意：移除了状态栏黑色 Spacer
                    // 播放器将延伸到状态栏下方，共享元素过渡更流畅
                    
                    //  视频播放器区域 - 包含状态栏高度
                    //  尝试获取共享元素作用域
                    val sharedTransitionScope = LocalSharedTransitionScope.current
                    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                    
                    //  为播放器容器添加共享元素标记（受开关控制）
                    val playerContainerModifier = if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier
                                .sharedBounds(
                                    sharedContentState = rememberSharedContentState(key = "video_cover_$bvid"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    //  添加回弹效果的 spring 动画
                                    boundsTransform = { _, _ ->
                                        spring(
                                            dampingRatio = 0.8f,   // [Hero] 高阻尼
                                            stiffness = 200f       // [Hero] 低刚度，与卡片保持一致
                                        )
                                    },
                                    clipInOverlayDuringTransition = OverlayClip(
                                        RoundedCornerShape(0.dp)  //  播放器无圆角
                                    )
                                )
                        }
                    } else {
                        Modifier
                    }
                    
                    //  播放器容器包含状态栏高度，让视频延伸到顶部
                    //  [修复] 始终保持播放器在 Composition 中，避免隐藏时重新创建导致重载
                    Box(
                        modifier = playerContainerModifier
                            .fillMaxWidth()
                            .height(animatedPlayerHeight)  //  使用动画高度（包含0高度）
                            .background(Color.Black)  // 黑色背景
                            .clipToBounds()
                            //  [PiP修复] 捕获视频播放器在屏幕上的位置
                            .onGloballyPositioned { layoutCoordinates ->
                                val position = layoutCoordinates.positionInWindow()
                                val size = layoutCoordinates.size
                                videoPlayerBounds = android.graphics.Rect(
                                    position.x.toInt(),
                                    position.y.toInt(),
                                    position.x.toInt() + size.width,
                                    position.y.toInt() + size.height
                                )
                            }
                    ) {
                        //  播放器内部使用 padding 避开状态栏
                        //  [关键] 即使高度为0也保持播放器渲染，避免重载
                        //  [修复] 高度需要包含statusBarHeight，扣除padding后视频内容才是完整的16:9
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(videoHeight + statusBarHeight)  //  修复：包含状态栏高度
                                .padding(top = statusBarHeight)  //  顶部 padding 避开状态栏
                                // [Fix] 竖屏全屏时隐藏底层播放器，防止 UI (如 00:00 进度条) 透出
                                .alpha(if (isPortraitFullscreen) 0f else 1f)
                        ) {
                            VideoPlayerSection(
                                playerState = playerState,
                                uiState = uiState,
                                isFullscreen = false,
                                isInPipMode = isPipMode,
                                onToggleFullscreen = { toggleFullscreen() },
                                onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                                onBack = handleBack,
                                // 🔗 [新增] 分享功能
                                bvid = bvid,
                                coverUrl = coverUrl,
                                onDoubleTapLike = { viewModel.toggleLike() },
                                //  [新增] 重载视频
                                onReloadVideo = { viewModel.reloadVideo() },
                                //  [新增] CDN 线路切换
                                currentCdnIndex = (uiState as? PlayerUiState.Success)?.currentCdnIndex ?: 0,
                                cdnCount = (uiState as? PlayerUiState.Success)?.cdnCount ?: 1,
                                onSwitchCdn = { viewModel.switchCdn() },
                                onSwitchCdnTo = { viewModel.switchCdnTo(it) },
                                
                                //  [新增] 音频模式
                                isAudioOnly = false,
                                onAudioOnlyToggle = { 
                                    viewModel.setAudioMode(true)
                                    isNavigatingToAudioMode = true // [Fix] Set flag to prevent notification cancellation
                                    onNavigateToAudioMode()
                                },
                                
                                //  [新增] 定时关闭
                                sleepTimerMinutes = sleepTimerMinutes,
                                onSleepTimerChange = { viewModel.setSleepTimer(it) },
                                
                                // 🖼️ [新增] 视频预览图数据
                                videoshotData = (uiState as? PlayerUiState.Success)?.videoshotData,
                                
                                // 📖 [新增] 视频章节数据
                        viewPoints = viewPoints,
                        
                        // 📱 [新增] 竖屏全屏模式
                        isVerticalVideo = isVerticalVideo,
                        onPortraitFullscreen = enterPortraitFullscreen,
                        isPortraitFullscreen = isPortraitFullscreen,

                                // 📲 [修复] 小窗模式 - 转移到应用内小窗而非直接进入系统 PiP
                                onPipClick = handlePipClick,
                                // [New] Codec & Audio
                                currentCodec = codecPreference,
                                onCodecChange = { viewModel.setVideoCodec(it) },
                                currentAudioQuality = audioQualityPreference,
                                onAudioQualityChange = { viewModel.setAudioQuality(it) },
                                // [New] Audio Language
                                onAudioLangChange = { viewModel.changeAudioLanguage(it) },
                                // [New Actions]
                                onSaveCover = { viewModel.saveCover(context) },
                                onDownloadAudio = { viewModel.downloadAudio(context) },
                                tvFocusRequester = tvDetailPlayerFocusRequester,
                                onTvMoveFocusDown = {
                                    val hasRelatedContent =
                                        (uiState as? PlayerUiState.Success)?.related?.isNotEmpty() == true
                                    val nextTarget = resolveVideoDetailTvFocusTarget(
                                        current = tvDetailFocusTarget,
                                        keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
                                        action = KeyEvent.ACTION_DOWN,
                                        hasRelatedContent = hasRelatedContent
                                    )
                                    if (nextTarget != tvDetailFocusTarget) {
                                        tvDetailFocusTarget = nextTarget
                                        requestTvDetailFocusTarget(nextTarget)
                                    }
                                }
                                //  空降助手 - 已由插件系统自动处理
                                // sponsorSegment = sponsorSegment,
                                // showSponsorSkipButton = showSponsorSkipButton,
                                // onSponsorSkip = { viewModel.skipCurrentSponsorSegment() },
                                // onSponsorDismiss = { viewModel.dismissSponsorSkipButton() }
                            )
                        }
                    }
                    
                    val handleTvContentKeyEvent: (KeyEvent) -> Boolean = { nativeEvent ->
                        val hasRelatedContent =
                            (uiState as? PlayerUiState.Success)?.related?.isNotEmpty() == true
                        val nextTarget = resolveVideoDetailTvFocusTarget(
                            current = tvDetailFocusTarget,
                            keyCode = nativeEvent.keyCode,
                            action = nativeEvent.action,
                            hasRelatedContent = hasRelatedContent
                        )
                        if (nextTarget != tvDetailFocusTarget) {
                            tvDetailFocusTarget = nextTarget
                            requestTvDetailFocusTarget(nextTarget)
                            true
                        } else {
                            false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .onPreviewKeyEvent { event ->
                                if (!isTvDevice) return@onPreviewKeyEvent false
                                handleTvContentKeyEvent(event.nativeKeyEvent)
                            }
                            // .nestedScroll(nestedScrollConnection) // [Remove] 移除嵌套滚动，确保 Tabs 正常滑动
                    ) {
                        when (uiState) {
                            is PlayerUiState.Loading -> {
                                val loadingState = uiState as PlayerUiState.Loading
                                //  显示重试进度
                                if (loadingState.retryAttempt > 0) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            //  iOS 风格加载
                                            CupertinoActivityIndicator()
                                            Spacer(Modifier.height(16.dp))
                                            Text(
                                                text = "正在重试 ${loadingState.retryAttempt}/${loadingState.maxAttempts}...",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    VideoDetailSkeleton()
                                }
                            }

                            is PlayerUiState.Success -> {
                                val success = uiState as PlayerUiState.Success
                                //  计算当前分P索引
                                val currentPageIndex = success.info.pages.indexOfFirst { it.cid == success.info.cid }.coerceAtLeast(0)
                                
                                //  下载进度
                                val downloadProgress by viewModel.downloadProgress.collectAsState()
                                
                                // 📱 [优化] 视频切换过渡动画
                                AnimatedContent(
                                    targetState = success.info.bvid,
                                    transitionSpec = {
                                        // 左右滑动 + 淡入淡出过渡动画
                                        (slideInHorizontally { width -> width / 4 } + fadeIn(animationSpec = tween(300)))
                                            .togetherWith(
                                                slideOutHorizontally { width -> -width / 4 } + fadeOut(animationSpec = tween(300))
                                            )
                                    },
                                    label = "video_content_transition"
                                ) { currentBvid ->
                                    // 使用 currentBvid 确保动画正确触发，并使用 key 显式消耗该参数以解决 unused parameter 报错
                                    key(currentBvid) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            // [Blur] Source: 只将内容区域标记为模糊源
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .hazeSource(hazeState)
                                        ) {
                                            // [性能优化] 延迟显示下方内容，优先保证进场动画流畅
                                            // 配合 isTransitionFinished 状态
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = isTransitionFinished,
                                                enter = fadeIn(tween(300))
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    // [新增] 计算播放器是否已折叠 (容差 10px)
                                                    val isPlayerCollapsed = if (swipeHidePlayerEnabled) {
                                                        playerHeightOffsetPx <= -videoHeightPx + 10f
                                                    } else {
                                                        false
                                                    }

                                                    VideoContentSection(
                                                        info = success.info,
                                                        relatedVideos = success.related,
                                                        replies = commentState.replies,
                                                        replyCount = commentState.replyCount,
                                                        emoteMap = success.emoteMap,
                                                        isRepliesLoading = commentState.isRepliesLoading,
                                                        isRepliesEnd = commentState.isRepliesEnd,
                                                        // [新增] 传递删除相关参数
                                                        currentMid = commentState.currentMid,
                                                        dissolvingIds = commentState.dissolvingIds,
                                                        // [新增] 删除评论
                                                        onDeleteComment = { rpid ->
                                                            commentViewModel.deleteComment(rpid)
                                                        },
                                                        onDissolveStart = { rpid ->
                                                            commentViewModel.startDissolve(rpid)
                                                        },
                                                        // [新增] 点赞
                                                        onCommentLike = commentViewModel::likeComment,
                                                        likedComments = commentState.likedComments,
                                                        isFollowing = success.isFollowing,
                                                        isFavorited = success.isFavorited,
                                                        isLiked = success.isLiked,
                                                        coinCount = success.coinCount,
                                                        currentPageIndex = currentPageIndex,
                                                        downloadProgress = downloadProgress,
                                                        isInWatchLater = success.isInWatchLater,
                                                        followingMids = success.followingMids,
                                                        videoTags = success.videoTags,
                                                        //  [新增] 评论排序/筛选参数
                                                        sortMode = commentState.sortMode,
                                                        upOnlyFilter = commentState.upOnlyFilter,
                                                        onSortModeChange = { mode ->
                                                            commentViewModel.setSortMode(mode)
                                                            sortPreferenceScope.launch {
                                                                com.android.purebilibili.core.store.SettingsManager
                                                                    .setCommentDefaultSortMode(context, mode.apiMode)
                                                            }
                                                        },
                                                        onUpOnlyToggle = { commentViewModel.toggleUpOnly() },
                                                        onFollowClick = { viewModel.toggleFollow() },
                                                        onFavoriteClick = { viewModel.showFavoriteFolderDialog() }, // [修改] 单击直接打开收藏夹选择
                                                        onLikeClick = { viewModel.toggleLike() },
                                                        onCoinClick = { viewModel.openCoinDialog() },
                                                        onTripleClick = { viewModel.doTripleAction() },
                                                        onPageSelect = { viewModel.switchPage(it) },
                                                        onUpClick = onUpClick,
                                                        onRelatedVideoClick = onVideoClick,
                                                        onSubReplyClick = { commentViewModel.openSubReply(it) },
                                                        onLoadMoreReplies = { commentViewModel.loadComments() },
                                                        onDownloadClick = { viewModel.openDownloadDialog() },
                                                        onWatchLaterClick = { viewModel.toggleWatchLater() },
                                                        //  [新增] 时间戳点击跳转
                                                        onTimestampClick = { positionMs ->
                                                            playerState.player.seekTo(positionMs)
                                                            playerState.player.play()
                                                        },
                                                        //  [新增] 弹幕发送
                                                        onDanmakuSendClick = {
                                                            android.util.Log.d("VideoDetailScreen", "📤 Danmaku send clicked!")
                                                            viewModel.showDanmakuSendDialog()
                                                        },
                                                        // 🔗 [新增] 传递共享元素过渡开关
                                                        transitionEnabled = transitionEnabled,
                                                        
                                                        // [新增] 收藏夹相关
                                                        favoriteFolderDialogVisible = showFavoriteFolderDialog,
                                                        favoriteFolders = favoriteFolders,
                                                        isFavoriteFoldersLoading = isFavoriteFoldersLoading,
                                                        onFavoriteLongClick = { viewModel.showFavoriteFolderDialog() },
                                                        onFavoriteFolderClick = { folder -> viewModel.addToFavoriteFolder(folder) },
                                                        onDismissFavoriteFolderDialog = { viewModel.dismissFavoriteFolderDialog() },
                                                        onCreateFavoriteFolder = { title, intro, isPrivate -> 
                                                            viewModel.createFavoriteFolder(title, intro, isPrivate) 
                                                        },
                                                        // [新增] 恢复播放器 (音频模式 -> 视频模式)
                                                        isPlayerCollapsed = isPlayerCollapsed,
                                                        onRestorePlayer = { playerHeightOffsetPx = 0f },
                                                        tvInfoFocusRequester = if (isTvDevice) tvDetailInfoFocusRequester else null,
                                                        tvRelatedFocusRequester = if (isTvDevice) tvDetailRelatedFocusRequester else null,
                                                        isTvInfoFocused = isTvDevice && tvDetailFocusTarget == VideoDetailTvFocusTarget.INFO,
                                                        isTvRelatedFocused = isTvDevice && tvDetailFocusTarget == VideoDetailTvFocusTarget.RELATED,
                                                        onTvContentPreviewKeyEvent = { keyEvent ->
                                                            handleTvContentKeyEvent(keyEvent)
                                                        },
                                                        // [新增] AI Summary & BGM
                                                        aiSummary = success.aiSummary,
                                                        bgmInfo = success.bgmInfo,
                                                        onBgmClick = onBgmClick
                                                    )

                                                    // 底部输入栏 (覆盖在内容之上)
                                                    BottomInputBar(
                                                        modifier = Modifier.align(Alignment.BottomCenter),
                                                        isLiked = success.isLiked,
                                                        isFavorited = success.isFavorited,
                                                        isCoined = success.coinCount > 0,
                                                        onLikeClick = { viewModel.toggleLike() },
                                                        onFavoriteClick = { viewModel.toggleFavorite() },
                                                        onCoinClick = { viewModel.openCoinDialog() },
                                                        onShareClick = {
                                                            val shareText = "【${success.info.title}】\nhttps://www.bilibili.com/video/${success.info.bvid}"
                                                            val sendIntent = android.content.Intent().apply {
                                                                action = android.content.Intent.ACTION_SEND
                                                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                                type = "text/plain"
                                                            }
                                                            val shareIntent = android.content.Intent.createChooser(sendIntent, "分享视频到")
                                                            context.startActivity(shareIntent)
                                                        },
                                                        onCommentClick = { 
                                                            android.util.Log.d("VideoDetailScreen", "📝 Comment input clicked!")
                                                            viewModel.showCommentInputDialog()
                                                        },
                                                        hazeState = hazeState
                                                    )
                                                }
                                            }
                                    }
                                }
                            }
                            } // End of AnimatedContent
                        } // End of Success block

                            is PlayerUiState.Error -> {
                                val errorState = uiState as PlayerUiState.Error
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        //  根据错误类型显示不同图标
                                        Text(
                                            text = when (errorState.error) {
                                                is com.android.purebilibili.data.model.VideoLoadError.NetworkError -> "📡"
                                                is com.android.purebilibili.data.model.VideoLoadError.VideoNotFound -> "🔍"
                                                is com.android.purebilibili.data.model.VideoLoadError.RegionRestricted -> "🌐"
                                                is com.android.purebilibili.data.model.VideoLoadError.RateLimited -> "⏳"
                                                is com.android.purebilibili.data.model.VideoLoadError.GlobalCooldown -> ""
                                                is com.android.purebilibili.data.model.VideoLoadError.PlayUrlEmpty -> "⚡"
                                                else -> ""
                                            },
                                            fontSize = 48.sp
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = errorState.msg,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        
                                        //  针对风控错误显示额外建议
                                        when (errorState.error) {
                                            is com.android.purebilibili.data.model.VideoLoadError.GlobalCooldown,
                                            is com.android.purebilibili.data.model.VideoLoadError.PlayUrlEmpty -> {
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = " 建议：切换 WiFi/移动数据 或 清除缓存后重试",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                            is com.android.purebilibili.data.model.VideoLoadError.RateLimited -> {
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = " 该视频可能暂时不可用，请尝试其他视频",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                            else -> {}
                                        }
                                        
                                        //  只有可重试的错误才显示重试按钮（或者风控错误允许强制重试）
                                        val showRetryButton = errorState.canRetry || 
                                            errorState.error is com.android.purebilibili.data.model.VideoLoadError.RateLimited ||
                                            errorState.error is com.android.purebilibili.data.model.VideoLoadError.PlayUrlEmpty
                                        if (showRetryButton) {
                                            Spacer(Modifier.height(24.dp))
                                            Button(
                                                onClick = { viewModel.retry() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text(
                                                    text = when (errorState.error) {
                                                        is com.android.purebilibili.data.model.VideoLoadError.RateLimited -> "强制重试"
                                                        is com.android.purebilibili.data.model.VideoLoadError.GlobalCooldown -> "清除冷却并重试"
                                                        else -> "重试"
                                                    }
                                                )
                                            }
                                        }
                                }
                            }
                        }
                }
                }  // 📱 手机竖屏布局结束（Column）
                }  // Box with nested scroll
            }  // else shouldUseSplitLayout
        }  // else targetIsLandscape
        // 📱 [新增] 竖屏全屏覆盖层
        // [修复] 在 Loading 状态时也保持竖屏全屏，使用上一个成功状态的数据
        // [修复] 移除 !isLandscape 限制，允许用户强制进入（例如在平板或特殊设备上）
        val showPortraitFullscreen = isPortraitFullscreen && 
            (uiState is PlayerUiState.Success || uiState is PlayerUiState.Loading)
        
        // 缓存上一个成功状态以在 Loading 时使用
        var cachedSuccess by remember { mutableStateOf<PlayerUiState.Success?>(null) }
        LaunchedEffect(uiState) {
            if (uiState is PlayerUiState.Success) {
                cachedSuccess = uiState as PlayerUiState.Success
            }
        }
        

        
        // 获取当前或缓存的成功状态
        val success = when {
            uiState is PlayerUiState.Success -> uiState as PlayerUiState.Success
            uiState is PlayerUiState.Loading && cachedSuccess != null -> cachedSuccess!!
            else -> null
        }
        
        val isLoadingNewVideo = uiState is PlayerUiState.Loading

        // Diagnostic Log
        LaunchedEffect(isPortraitFullscreen, showPortraitFullscreen, success) {
            com.android.purebilibili.core.util.Logger.d("VideoDetailScreen", 
                "Portrait Mode Check: requested=$isPortraitFullscreen, shown=$showPortraitFullscreen, " + 
                "success=${success != null}, isLandscape=$isLandscape")
        }
        
        if (showPortraitFullscreen && success != null) {
            val portraitInitialBvid = pendingMainReloadBvidAfterPortrait ?: success.info.bvid
            // 竖屏全屏模式：使用 Pager 实现无缝滑动 (TikTok Style)
            com.android.purebilibili.feature.video.ui.pager.PortraitVideoPager(
                initialBvid = portraitInitialBvid,
                initialInfo = success.info,
                recommendations = success.related,
                onBack = { isPortraitFullscreen = false },
                onVideoChange = { newBvid ->
                    // 高频滑动期间不重载主播放器，避免与竖屏播放器抢焦点导致暂停。
                    // 退出竖屏时再同步到主播放器。
                    pendingMainReloadBvidAfterPortrait = newBvid
                },
                viewModel = viewModel,
                commentViewModel = commentViewModel,
                sharedPlayer = if (useSharedPortraitPlayer) playerState.player else null,
                // [新增] 进度同步
                initialStartPositionMs = portraitSyncSnapshotPositionMs,
                onProgressUpdate = { bvid, pos ->
                    portraitSyncSnapshotBvid = bvid
                    portraitSyncSnapshotPositionMs = pos.coerceAtLeast(0L)
                    if (shouldMirrorPortraitProgressToMainPlayer) {
                        hasPendingPortraitSync = true
                        if (tryApplyPortraitProgressSync(bvid, portraitSyncSnapshotPositionMs)) {
                            hasPendingPortraitSync = false
                        }
                    }
                },
                onExitSnapshot = { bvid, pos ->
                    portraitSyncSnapshotBvid = bvid
                    portraitSyncSnapshotPositionMs = pos.coerceAtLeast(0L)
                    pendingMainReloadBvidAfterPortrait = bvid
                    if (shouldMirrorPortraitProgressToMainPlayer) {
                        hasPendingPortraitSync = true
                        if (tryApplyPortraitProgressSync(bvid, portraitSyncSnapshotPositionMs)) {
                            hasPendingPortraitSync = false
                        }
                    }
                },
                onSearchClick = {
                    if (com.android.purebilibili.feature.video.ui.pager
                            .shouldExitPortraitForExternalNavigation(isPortraitFullscreen)
                    ) {
                        isPortraitFullscreen = false
                    }
                    onNavigateToSearch()
                },
                onUserClick = { mid ->
                    if (com.android.purebilibili.feature.video.ui.pager
                            .shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen)
                    ) {
                        isPortraitFullscreen = false
                    }
                    onUpClick(mid)
                },
                onRotateToLandscape = {
                    isPortraitFullscreen = false
                    toggleFullscreen()
                }
            )
        }
        //  [新增] 投币对话框
        val coinDialogVisible by viewModel.coinDialogVisible.collectAsState()
        val currentCoinCount = (uiState as? PlayerUiState.Success)?.coinCount ?: 0
        val userBalance by viewModel.userCoinBalance.collectAsState()
        CoinDialog(
            visible = coinDialogVisible,
            currentCoinCount = currentCoinCount,
            userBalance = userBalance,
            onDismiss = { viewModel.closeCoinDialog() },
            onConfirm = { count, alsoLike -> viewModel.doCoin(count, alsoLike) }
        )
        
        // [新增] 播放完成选择对话框
        val showPlaybackEndedDialog by viewModel.showPlaybackEndedDialog.collectAsState()
        if (showPlaybackEndedDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { viewModel.dismissPlaybackEndedDialog() }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "播放完成",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "选择接下来的操作",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // 重播按钮
                        Button(
                            onClick = {
                                viewModel.dismissPlaybackEndedDialog()
                                playerState.player.seekTo(0)
                                playerState.player.play()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("🔄 重播当前视频")
                        }
                        
                        // 播放下一个按钮
                        Button(
                            onClick = {
                                viewModel.dismissPlaybackEndedDialog()
                                viewModel.playNextRecommended()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("▶️ 播放下一个视频")
                        }
                        
                        // 关闭按钮
                        TextButton(
                            onClick = { viewModel.dismissPlaybackEndedDialog() }
                        ) {
                            Text("暂不操作")
                        }
                    }
                }
            }
        }
        
        //  [新增] 弹幕发送对话框
        val showDanmakuDialog by viewModel.showDanmakuDialog.collectAsState()
        val isSendingDanmaku by viewModel.isSendingDanmaku.collectAsState()
        com.android.purebilibili.feature.video.ui.components.DanmakuSendDialog(
            visible = showDanmakuDialog,
            onDismiss = { viewModel.hideDanmakuSendDialog() },
            onSend = { message, color, mode, fontSize ->
                android.util.Log.d("VideoDetailScreen", "📤 Sending danmaku: $message")
                viewModel.sendDanmaku(message, color, mode, fontSize)
            },
            isSending = isSendingDanmaku
        )
        
        //  [新增] 评论输入对话框
        val showCommentInput by viewModel.showCommentDialog.collectAsState()
        val isSendingComment by viewModel.isSendingComment.collectAsState() // 暂时复用 ViewModel 状态?
        val replyingToComment by viewModel.replyingToComment.collectAsState()
        val emotePackages by viewModel.emotePackages.collectAsState() // [新增]
        
        com.android.purebilibili.feature.video.ui.components.CommentInputDialog(
            visible = showCommentInput,
            onDismiss = { viewModel.hideCommentInputDialog() },
            isSending = isSendingComment,
            replyToName = replyingToComment?.member?.uname,
            inputHint = if (replyingToComment != null) commentState.childInputHint else commentState.rootInputHint,
            canUploadImage = commentState.canUploadImage,
            canInputComment = commentState.canInputComment,
            emotePackages = emotePackages, // [新增]
            onSend = { message, imageUris ->
                viewModel.sendComment(message, imageUris)
                viewModel.hideCommentInputDialog()
            }
        )
        
        //  [新增] 下载选项菜单 & 画质选择
        val showDownloadDialog by viewModel.showDownloadDialog.collectAsState()
        val successForDownload = uiState as? PlayerUiState.Success
        
        // 本地状态控制画质选择弹窗
        var showQualitySelection by remember { mutableStateOf(false) }

        if (showDownloadDialog && successForDownload != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closeDownloadDialog() },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "下载选项",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    
                    // 1. 缓存视频
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // 检查任务状态
                                val existingTask = com.android.purebilibili.feature.download.DownloadManager.getTask(successForDownload.info.bvid, successForDownload.info.cid)
                                if (existingTask != null && !existingTask.isFailed) {
                                    if (existingTask.isComplete) viewModel.toast("视频已缓存")
                                    else viewModel.toast("正在下载中...")
                                    viewModel.closeDownloadDialog()
                                } else {
                                    // 打开画质选择
                                    showQualitySelection = true
                                    viewModel.closeDownloadDialog()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.ArrowDown, // 假设已有此图标或使用 Icons.Rounded.Download
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "缓存视频",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "选择画质缓存当前视频",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 2. 下载音频
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val task = com.android.purebilibili.feature.download.DownloadTask(
                                    bvid = successForDownload.info.bvid,
                                    cid = successForDownload.info.cid,
                                    title = successForDownload.info.title,
                                    cover = successForDownload.info.pic,
                                    ownerName = successForDownload.info.owner.name,
                                    ownerFace = successForDownload.info.owner.face,
                                    duration = 0, // 音频不需要 duration?
                                    quality = 0,
                                    qualityDesc = "音频",
                                    videoUrl = "",
                                    audioUrl = successForDownload.audioUrl ?: "",
                                    isAudioOnly = true
                                )
                                if (task.audioUrl.isNotEmpty()) {
                                    val started = com.android.purebilibili.feature.download.DownloadManager.addTask(task)
                                    if (started) viewModel.toast("已开始下载音频")
                                    else viewModel.toast("该任务已在下载中或已完成")
                                } else {
                                    viewModel.toast("无法获取音频地址")
                                }
                                viewModel.closeDownloadDialog()
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "下载音频",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "仅保存音频文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 3. 保存封面
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current // 获取 Context
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val coverUrl = successForDownload.info.pic
                                val title = successForDownload.info.title
                                if (coverUrl.isNotEmpty()) {
                                    scope.launch {
                                        val success = com.android.purebilibili.feature.download.DownloadManager.saveImageToGallery(
                                            context, 
                                            coverUrl, 
                                            title
                                        )
                                        // Toast 已经在 saveImageToGallery 内部或者需要外部调用? 
                                        // VideoPlayerOverlay 是自己调用的。
                                        // context 是必要的。
                                        if (success) viewModel.toast("封面已保存到相册")
                                        else viewModel.toast("保存失败")
                                    }
                                } else {
                                    viewModel.toast("无法获取封面地址")
                                }
                                viewModel.closeDownloadDialog()
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Photo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "保存封面",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "保存当前视频封面到相册",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // 缓存视频 - 画质选择弹窗 (当 showQualitySelection 为 true 时显示)
        if (showQualitySelection && successForDownload != null) {
            val sortedQualityOptions = successForDownload.qualityIds
                .zip(successForDownload.qualityLabels)
                .sortedByDescending { it.first }
            val highestQuality = sortedQualityOptions.firstOrNull()?.first ?: successForDownload.currentQuality
            val defaultPath = remember { com.android.purebilibili.feature.download.DownloadManager.getDownloadDir().absolutePath }
            
            com.android.purebilibili.feature.download.DownloadQualityDialog(
                title = successForDownload.info.title,
                qualityOptions = sortedQualityOptions,
                currentQuality = highestQuality,
                defaultPath = defaultPath,
                onQualitySelected = { quality, path -> 
                    viewModel.downloadWithQuality(quality, path) 
                    showQualitySelection = false
                },
                onDismiss = { showQualitySelection = false }
            )
        }
        
        //  评论二级弹窗
        // [#14修复] 添加图片预览状态
        var subReplyShowImagePreview by remember { mutableStateOf(false) }
        var subReplyPreviewImages by remember { mutableStateOf<List<String>>(emptyList()) }
        var subReplyPreviewIndex by remember { mutableIntStateOf(0) }
        var subReplySourceRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        
        // [#14修复] 评论详情图片预览对话框
        if (subReplyShowImagePreview && subReplyPreviewImages.isNotEmpty()) {
            ImagePreviewDialog(
                images = subReplyPreviewImages,
                initialIndex = subReplyPreviewIndex,
                sourceRect = subReplySourceRect,
                onDismiss = { subReplyShowImagePreview = false }
            )
        }
        
        if (subReplyState.visible) {
            BackHandler {
                commentViewModel.closeSubReply()
            }
            val successState = uiState as? PlayerUiState.Success
            SubReplySheet(
                state = subReplyState,
                emoteMap = successState?.emoteMap ?: emptyMap(),
                onDismiss = { commentViewModel.closeSubReply() },
                onLoadMore = { commentViewModel.loadMoreSubReplies() },
                //  [新增] 时间戳点击跳转
                onTimestampClick = { positionMs ->
                    playerState.player.seekTo(positionMs)
                    playerState.player.play()
                    commentViewModel.closeSubReply()  // 关闭弹窗以便看视频
                },
                // [#14修复] 图片预览回调
                onImagePreview = { images, index, rect ->
                    subReplyPreviewImages = images
                    subReplyPreviewIndex = index
                    subReplySourceRect = rect
                    subReplyShowImagePreview = true
                },
                //  [修复] 点击评论回复
                onReplyClick = { replyItem ->
                    android.util.Log.d("VideoDetailScreen", "📝 Reply to: ${replyItem.member.uname}")
                    viewModel.setReplyingTo(replyItem)  // 设置回复目标
                    viewModel.showCommentInputDialog()  // 显示评论输入对话框
                },
                // [新增] 删除评论（消散动画）
                currentMid = commentState.currentMid,
                onDissolveStart = { rpid ->
                    commentViewModel.startSubDissolve(rpid)
                },
                onDeleteComment = { rpid ->
                    commentViewModel.deleteSubComment(rpid)
                },
                onCommentLike = commentViewModel::likeComment,
                likedComments = commentState.likedComments,
                onUrlClick = { url ->
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onAvatarClick = { mid -> mid.toLongOrNull()?.let { onUpClick(it) } }
            )
        }

        // 📁 收藏夹选择弹窗
        if (showFavoriteFolderDialog) {
            com.android.purebilibili.feature.video.ui.components.FavoriteFolderSheet(
                folders = favoriteFolders,
                isLoading = isFavoriteFoldersLoading,
                onFolderClick = { folder -> 
                    viewModel.addToFavoriteFolder(folder)
                },
                onDismissRequest = { viewModel.dismissFavoriteFolderDialog() },
                onCreateFolder = { title, intro, isPrivate ->
                    viewModel.createFavoriteFolder(title, intro, isPrivate)
                }
            )
        }
        
        // 🎉 点赞成功爆裂动画
        val likeBurstVisible by viewModel.likeBurstVisible.collectAsState()
        if (likeBurstVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-50).dp)
            ) {
                LikeBurstAnimation(
                    visible = true,
                    onAnimationEnd = { viewModel.dismissLikeBurst() }
                )
            }
        }
        
        // 🎉 三连成功庆祝动画
        val tripleCelebrationVisible by viewModel.tripleCelebrationVisible.collectAsState()
        if (tripleCelebrationVisible) {
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                TripleSuccessAnimation(
                    visible = true,
                    onAnimationEnd = { viewModel.dismissTripleCelebration() }
                )
            }
        }
        
        //  居中弹窗提示（关注/收藏反馈）
        androidx.compose.animation.AnimatedVisibility(
            visible = popupMessage != null,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                tonalElevation = 8.dp
            ) {
                Text(
                    text = popupMessage ?: "",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )
            }
        }

        if (isTvDevice && !isFullscreenMode && !useTabletLayout) {
            TvDetailFocusZoneOverlay(
                focusTarget = tvDetailFocusTarget,
                onTargetSelected = { target ->
                    val hasRelatedContent =
                        (uiState as? PlayerUiState.Success)?.related?.isNotEmpty() == true
                    val normalizedTarget = normalizeVideoDetailTvFocusTarget(
                        target = target,
                        hasRelatedContent = hasRelatedContent
                    )
                    if (normalizedTarget != tvDetailFocusTarget) {
                        tvDetailFocusTarget = normalizedTarget
                        requestTvDetailFocusTarget(normalizedTarget)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            )
        }
        
        // 💬 弹幕上下文菜单
        val danmakuMenuState by viewModel.danmakuMenuState.collectAsState()
        
        if (danmakuMenuState.visible) {
            DanmakuContextMenu(
                text = danmakuMenuState.text,
                onDismiss = { viewModel.hideDanmakuMenu() },
                onLike = { viewModel.likeDanmaku(danmakuMenuState.dmid) },
                onRecall = { viewModel.recallDanmaku(danmakuMenuState.dmid) },
                onReport = { reason -> 
                    viewModel.reportDanmaku(danmakuMenuState.dmid, reason)
                },
                voteCount = danmakuMenuState.voteCount,
                hasLiked = danmakuMenuState.hasLiked,
                voteLoading = danmakuMenuState.voteLoading,
                canVote = danmakuMenuState.canVote,
                onBlockUser = {
                    viewModel.toast("暂不支持屏蔽用户")
                }
            )
        }
        
        // 🔗 绑定弹幕点击监听器
        LaunchedEffect(danmakuManager) {
            danmakuManager.setOnDanmakuClickListener { text, dmid, uid, isSelf ->
                android.util.Log.d("VideoDetailScreen", "👆 Danmaku clicked: $text")
                viewModel.showDanmakuMenu(dmid, text, uid, isSelf)
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
private fun TvDetailFocusZoneOverlay(
    focusTarget: VideoDetailTvFocusTarget,
    onTargetSelected: (VideoDetailTvFocusTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusTargets = remember {
        listOf(
            VideoDetailTvFocusTarget.PLAYER,
            VideoDetailTvFocusTarget.INFO,
            VideoDetailTvFocusTarget.RELATED
        )
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            focusTargets.forEach { target ->
                FilterChip(
                    selected = target == focusTarget,
                    onClick = { onTargetSelected(target) },
                    label = { Text(resolveVideoDetailTvFocusLabel(target), fontSize = 11.sp) }
                )
            }
        }
    }
}

internal fun shouldRotateToPortraitOnSplitBack(
    useTabletLayout: Boolean,
    smallestScreenWidthDp: Int,
    orientation: Int,
    isTvDevice: Boolean
): Boolean {
    return useTabletLayout &&
        !isTvDevice &&
        smallestScreenWidthDp < 600 &&
        orientation == Configuration.ORIENTATION_LANDSCAPE
}

internal fun shouldUseTabletVideoLayout(
    isExpandedScreen: Boolean,
    isTvDevice: Boolean
): Boolean {
    return isExpandedScreen && !isTvDevice
}

// VideoContentSection 已提取到 VideoContentSection.kt
// VideoTagsRow 和 VideoTagChip 也已提取到 VideoContentSection.kt
