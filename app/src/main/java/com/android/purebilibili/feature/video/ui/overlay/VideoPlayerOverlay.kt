// 文件路径: feature/video/VideoPlayerOverlay.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.core.util.FormatUtils
// Import reusable components from standalone files
import com.android.purebilibili.feature.video.ui.components.QualitySelectionMenu
import com.android.purebilibili.feature.video.ui.components.SpeedSelectionMenuDialog
import com.android.purebilibili.feature.video.ui.components.DanmakuSettingsPanel
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.feature.video.ui.components.AspectRatioMenu
import com.android.purebilibili.feature.video.ui.components.VideoSettingsPanel
import com.android.purebilibili.feature.video.ui.components.ChapterListPanel
import com.android.purebilibili.data.model.response.ViewPoint
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.util.ShareUtils
import com.android.purebilibili.core.util.WindowWidthSizeClass

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import io.github.alexzhirkevich.cupertino.icons.filled.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.HandThumbsup
import com.android.purebilibili.core.ui.AppIcons
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.ui.animation.tvFocusableJiggle
import com.android.purebilibili.feature.cast.DeviceListDialog
import com.android.purebilibili.feature.cast.DlnaManager
import com.android.purebilibili.feature.cast.LocalProxyServer
import com.android.purebilibili.feature.cast.SsdpCastClient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import android.view.KeyEvent
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.core.util.shouldHandleTvMenuKey
import com.android.purebilibili.core.util.shouldHandleTvSelectKey
import com.android.purebilibili.feature.video.danmaku.FaceOcclusionModuleState


@Composable
fun VideoPlayerOverlay(
    player: Player,
    title: String,
    isVisible: Boolean,
    onToggleVisible: () -> Unit,
    isFullscreen: Boolean,
    currentQualityLabel: String,
    qualityLabels: List<String>,
    qualityIds: List<Int> = emptyList(),
    isLoggedIn: Boolean = false,
    onQualitySelected: (Int) -> Unit,

    onBack: () -> Unit,
    onToggleFullscreen: () -> Unit,
    // [New] Player Data for Download
    bvid: String = "",
    cid: Long = 0L,
    videoOwnerName: String = "",
    videoOwnerFace: String = "",
    videoDuration: Int = 0,
    videoTitle: String = "",
    currentVideoUrl: String = "",
    currentAudioUrl: String = "", 
    // 🔒 [新增] 屏幕锁定
    isScreenLocked: Boolean = false,
    onLockToggle: () -> Unit = {},
    showStats: Boolean = false,
    realResolution: String = "",
    isQualitySwitching: Boolean = false,
    isBuffering: Boolean = false,  // 缓冲状态
    isVip: Boolean = false,
    //  [新增] 弹幕开关和设置
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    danmakuOpacity: Float = 0.85f,
    danmakuFontScale: Float = 1.0f,
    danmakuSpeed: Float = 1.0f,
    danmakuDisplayArea: Float = 0.5f,
    danmakuMergeDuplicates: Boolean = true,
    danmakuAllowScroll: Boolean = true,
    danmakuAllowTop: Boolean = true,
    danmakuAllowBottom: Boolean = true,
    danmakuAllowColorful: Boolean = true,
    danmakuAllowSpecial: Boolean = true,
    danmakuSmartOcclusion: Boolean = true,
    onDanmakuOpacityChange: (Float) -> Unit = {},
    onDanmakuFontScaleChange: (Float) -> Unit = {},
    onDanmakuSpeedChange: (Float) -> Unit = {},
    onDanmakuDisplayAreaChange: (Float) -> Unit = {},
    onDanmakuMergeDuplicatesChange: (Boolean) -> Unit = {},
    onDanmakuAllowScrollChange: (Boolean) -> Unit = {},
    onDanmakuAllowTopChange: (Boolean) -> Unit = {},
    onDanmakuAllowBottomChange: (Boolean) -> Unit = {},
    onDanmakuAllowColorfulChange: (Boolean) -> Unit = {},
    onDanmakuAllowSpecialChange: (Boolean) -> Unit = {},
    onDanmakuSmartOcclusionChange: (Boolean) -> Unit = {},
    smartOcclusionModuleState: FaceOcclusionModuleState = FaceOcclusionModuleState.Checking,
    smartOcclusionDownloadProgress: Int? = null,
    onDanmakuSmartOcclusionDownloadClick: () -> Unit = {},
    //  [实验性功能] 双击点赞
    doubleTapLikeEnabled: Boolean = true,
    onDoubleTapLike: () -> Unit = {},
    //  视频比例调节
    currentAspectRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    onAspectRatioChange: (VideoAspectRatio) -> Unit = {},
    // 🔗 [新增] 分享功能 (Moved bvid to top)
    onShare: (() -> Unit)? = null,
    // [New] Cover URL for Download
    coverUrl: String = "",
    //  [新增] 视频设置面板回调
    onReloadVideo: () -> Unit = {},
    sleepTimerMinutes: Int? = null,
    onSleepTimerChange: (Int?) -> Unit = {},
    isFlippedHorizontal: Boolean = false,
    isFlippedVertical: Boolean = false,
    onFlipHorizontal: () -> Unit = {},
    onFlipVertical: () -> Unit = {},
    isAudioOnly: Boolean = false,
    onAudioOnlyToggle: () -> Unit = {},
    //  [新增] 画质列表和回调
    onQualityChange: (Int, Long) -> Unit = { _, _ -> },
    //  [新增] CDN 线路切换
    currentCdnIndex: Int = 0,
    cdnCount: Int = 1,
    onSwitchCdn: () -> Unit = {},
    onSwitchCdnTo: (Int) -> Unit = {},
    // 🖼️ [新增] 视频预览图数据
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    // 📖 [新增] 视频章节数据
    viewPoints: List<ViewPoint> = emptyList(),
    // 📱 [新增] 竖屏全屏模式
    isVerticalVideo: Boolean = false,
    onPortraitFullscreen: () -> Unit = {},
    // 📲 [新增] 小窗模式
    onPipClick: () -> Unit = {},
    //  [新增] 拖动进度条开始回调（用于清除弹幕）
    onSeekStart: () -> Unit = {},
    //  [新增] 外部可接管 seek 行为（用于同步弹幕等）
    onSeekTo: ((Long) -> Unit)? = null,
    // [New] Codec & Audio Params
    currentCodec: String = "hev1",
    onCodecChange: (String) -> Unit = {},
    currentAudioQuality: Int = -1,
    onAudioQualityChange: (Int) -> Unit = {},
    // [New] AI Audio Translation
    aiAudioInfo: com.android.purebilibili.data.model.response.AiAudioInfo? = null,
    currentAudioLang: String? = null,
    onAudioLangChange: (String) -> Unit = {},
    // 👀 [新增] 在线观看人数
    onlineCount: String = "",
    // [New Actions]
    onSaveCover: () -> Unit = {},
    onCaptureScreenshot: () -> Unit = {},
    onDownloadAudio: () -> Unit = {},
    // 🔁 [新增] 播放模式
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode = com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {},
    
    // [新增] 侧边栏抽屉数据与交互
    relatedVideos: List<com.android.purebilibili.data.model.response.RelatedVideo> = emptyList(),
    ugcSeason: com.android.purebilibili.data.model.response.UgcSeason? = null,
    isFollowed: Boolean = false,
    isLiked: Boolean = false,
    isCoined: Boolean = false,
    isFavorited: Boolean = false,
    onToggleFollow: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onCoin: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onTriple: () -> Unit = {},  // [新增] 一键三连回调
    // 复用 onRelatedVideoClick 或 onVideoClick
    onDrawerVideoClick: (String) -> Unit = {},
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showRatioMenu by remember { mutableStateOf(false) }
    // [新增] 侧边栏显示状态
    var showEndDrawer by remember { mutableStateOf(false) }
    var showDanmakuSettings by remember { mutableStateOf(false) }
    var showVideoSettings by remember { mutableStateOf(false) }  //  新增
    var showChapterList by remember { mutableStateOf(false) }  // 📖 章节列表
    var showCastDialog by remember { mutableStateOf(false) }   // 📺 投屏对话框
    var showPlaybackOrderSheet by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    //  使用传入的比例状态
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val playbackCompletionBehavior by SettingsManager
        .getPlaybackCompletionBehavior(context)
        .collectAsState(initial = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC)
    

    //  双击检测状态
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showLikeAnimation by remember { mutableStateOf(false) }
    val isTvDevice = rememberIsTvDevice()
    val overlayVisualPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveVideoPlayerOverlayVisualPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }
    val tvOverlayFocusRequester = remember { FocusRequester() }
    var tvFocusZone by remember { mutableStateOf(VideoOverlayTvFocusZone.CENTER) }

    // 📺 [DLNA] 按需权限请求
    val dlnaPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            // 权限授予，绑定服务并显示对话框
            DlnaManager.bindService(context)
            DlnaManager.refresh() // 刷新列表
            showCastDialog = true
        } else {
            // 权限被拒绝，提示用户（可选）
             com.android.purebilibili.core.util.Logger.d("VideoPlayerOverlay", "DLNA permissions denied")
        }
    }

    val onCastClickAction = {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: NEARBY_WIFI_DEVICES
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.NEARBY_WIFI_DEVICES) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 DlnaManager.bindService(context)
                 DlnaManager.refresh()
                 showCastDialog = true
            } else {
                dlnaPermissionLauncher.launch(arrayOf(android.Manifest.permission.NEARBY_WIFI_DEVICES))
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12: ACCESS_FINE_LOCATION
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 DlnaManager.bindService(context)
                 DlnaManager.refresh()
                 showCastDialog = true
            } else {
                dlnaPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION))
            }
        } else {
            // Android 11-: 无需运行时权限（除了 Internet/WifiState）
            DlnaManager.bindService(context)
            DlnaManager.refresh()
            showCastDialog = true
        }
    }

    val progressState by produceState(initialValue = PlayerProgress(), key1 = player, key2 = isVisible) {
        while (isActive) {
            //  [修复] 始终更新进度，不仅在播放时
            // 这样横竖屏切换后也能显示正确的进度
            val duration = if (player.duration < 0) 0L else player.duration
            value = PlayerProgress(
                current = player.currentPosition,
                duration = duration,
                buffered = player.bufferedPosition
            )
            isPlaying = player.isPlaying
            val delayMs = if (isVisible && player.isPlaying) 200L else 500L
            delay(delayMs)
        }
    }
    
    // 📖 计算当前章节（必须在 progressState 之后定义）
    val currentChapter = remember(progressState.current, viewPoints) {
        if (viewPoints.isEmpty()) null
        else viewPoints.lastOrNull { progressState.current >= it.fromMs }?.content
    }

    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            delay(4000)
            if (isVisible) {
                onToggleVisible()
            }
        }
    }
    
    //  双击点赞动画自动消失
    LaunchedEffect(showLikeAnimation) {
        if (showLikeAnimation) {
            delay(800)
            showLikeAnimation = false
        }
    }

    LaunchedEffect(isTvDevice, isVisible) {
        if (isTvDevice && isVisible) {
            tvOverlayFocusRequester.requestFocus()
        }
        tvFocusZone = resolveInitialVideoOverlayTvFocusZone(
            isTv = isTvDevice,
            overlayVisible = isVisible
        ) ?: VideoOverlayTvFocusZone.CENTER
    }

    fun togglePlayPause() {
        if (player.playbackState == Player.STATE_ENDED) {
            onSeekTo?.invoke(0L) ?: player.seekTo(0L)
            player.play()
            isPlaying = true
        } else if (isPlaying) {
            player.pause()
            isPlaying = false
        } else {
            player.play()
            isPlaying = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isTvDevice) {
                    Modifier
                        .focusRequester(tvOverlayFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            val keyCode = event.nativeKeyEvent.keyCode
                            val action = event.nativeKeyEvent.action

                            if (shouldHandleTvSelectKey(keyCode, action)) {
                                return@onPreviewKeyEvent when (
                                    resolveVideoOverlayTvSelectAction(tvFocusZone)
                                ) {
                                    VideoOverlayTvSelectAction.BACK -> {
                                        onBack()
                                        true
                                    }
                                    VideoOverlayTvSelectAction.TOGGLE_PLAY_PAUSE -> {
                                        togglePlayPause()
                                        true
                                    }
                                    VideoOverlayTvSelectAction.TOGGLE_DRAWER -> {
                                        showEndDrawer = !showEndDrawer
                                        true
                                    }
                                    VideoOverlayTvSelectAction.NOOP -> false
                                }
                            }

                            if (shouldHandleTvMenuKey(keyCode, action)) {
                                showEndDrawer = !showEndDrawer
                                return@onPreviewKeyEvent true
                            }

                            when (
                                resolveVideoOverlayTvBackAction(
                                    keyCode = keyCode,
                                    action = action,
                                    drawerVisible = showEndDrawer
                                )
                            ) {
                                VideoOverlayTvBackAction.DISMISS_DRAWER -> {
                                    showEndDrawer = false
                                    return@onPreviewKeyEvent true
                                }

                                VideoOverlayTvBackAction.NAVIGATE_BACK -> {
                                    onBack()
                                    return@onPreviewKeyEvent true
                                }

                                VideoOverlayTvBackAction.NOOP -> Unit
                            }

                            val nextFocusZone = resolveVideoOverlayTvFocusZone(
                                current = tvFocusZone,
                                keyCode = keyCode,
                                action = action
                            )
                            if (nextFocusZone != tvFocusZone) {
                                tvFocusZone = nextFocusZone
                                true
                            } else {
                                false
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        // --- 1. 顶部渐变遮罩 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            //  [修复] align 必须在 AnimatedVisibility 的 modifier 上，而不是内部 Box 上
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(overlayVisualPolicy.topScrimHeightDp.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // --- 2. 底部渐变遮罩 ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(overlayVisualPolicy.bottomScrimHeightDp.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }

        // --- 3. 控制栏内容 (锁定时隐藏) ---
        AnimatedVisibility(
            visible = isVisible && !isScreenLocked,  // 🔒 锁定时隐藏控制栏
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            //  [修复] 确保 AnimatedVisibility 填充整个父容器
            modifier = Modifier.fillMaxSize()
        ) {
            //  [修复] 使用 Box 分别定位顶部和底部控制栏
            Box(modifier = Modifier.fillMaxSize()) {
                //  顶部控制栏 - 仅在横屏（全屏）模式显示标题和清晰度
                if (isFullscreen) {
                    TopControlBar(
                        title = title,
                        onlineCount = onlineCount,
                        isFullscreen = isFullscreen,
                        onBack = onBack,
                        // Interactions
                        isLiked = isLiked,
                        isCoined = isCoined,
                        onLikeClick = onToggleLike,
                        onDislikeClick = {}, // TODO: Implement dislike
                        onCoinClick = onCoin,
                        onShareClick = {
                            if (bvid.isNotEmpty()) {
                                ShareUtils.shareVideo(context, title, bvid)
                            }
                        },
                        onCastClick = onCastClickAction,
                        onMoreClick = { showEndDrawer = true },
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                } else {
                    //  [新增] 竖屏模式顶部栏（返回 + 画质 + 设置 + 分享按钮）
                    val context = LocalContext.current
                    PortraitTopBar(
                        onlineCount = onlineCount,
                        onBack = onBack,
                        onSettings = { showVideoSettings = true },
                        onShare = onShare ?: {
                            if (bvid.isNotEmpty()) {
                                ShareUtils.shareVideo(context, title, bvid)
                            }
                        },
                        onAudioMode = onAudioOnlyToggle,
                        isAudioOnly = isAudioOnly,
                        //  [新增] 投屏按钮
                        onCastClick = onCastClickAction,
                        // 📱 左上角改为弹幕开关，画质只保留底部入口
                        danmakuEnabled = danmakuEnabled,
                        onDanmakuToggle = onDanmakuToggle,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
                
                //  [修复] 底部控制栏 - 固定在底部
                BottomControlBar(
                    isPlaying = isPlaying,
                    progress = progressState,
                    isFullscreen = isFullscreen,
                    currentSpeed = currentSpeed,
                    currentRatio = currentAspectRatio,
                    onPlayPauseClick = {
                        togglePlayPause()
                    },
                    onSeek = { position -> onSeekTo?.invoke(position) ?: player.seekTo(position) },
                    onSeekStart = onSeekStart,  //  拖动进度条开始时清除弹幕
                    onSpeedClick = { showSpeedMenu = true },
                    onRatioClick = { showRatioMenu = true },
                    onToggleFullscreen = onToggleFullscreen,
                    //  [新增] 竖屏模式弹幕和清晰度控制
                    danmakuEnabled = danmakuEnabled,
                    onDanmakuToggle = onDanmakuToggle,
                    onDanmakuSettingsClick = { showDanmakuSettings = true },
                    currentQualityLabel = currentQualityLabel,
                    onQualityClick = { showQualityMenu = true },
                    // 🖼️ [新增] 视频预览图数据
                    videoshotData = videoshotData,
                    // 📖 [新增] 视频章节数据
                    viewPoints = viewPoints,
                    currentChapter = currentChapter,
                    onChapterClick = { showChapterList = true },
                    // 📱 [新增] 竖屏全屏模式
                    isVerticalVideo = isVerticalVideo,
                    onPortraitFullscreen = onPortraitFullscreen,
                    // 📲 [新增] 小窗模式
                    onPipClick = onPipClick,
                    // 🔁 [新增] 播放模式
                    currentPlayMode = currentPlayMode,
                    onPlayModeClick = onPlayModeClick,
                    playbackOrderLabel = resolvePlaybackOrderDisplayLabel(
                        behavior = playbackCompletionBehavior,
                        compact = !isFullscreen
                    ),
                    onPlaybackOrderClick = { showPlaybackOrderSheet = true },
                    //  [修复] 传入 modifier 确保在底部
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
        
        // --- 3.5 🔒 [新增] 屏幕锁定按钮 (仅全屏模式) ---
        if (isFullscreen) {
            AnimatedVisibility(
                visible = isVisible || isScreenLocked,  // 锁定时始终显示解锁按钮
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = overlayVisualPolicy.lockButtonEndPaddingDp.dp)
            ) {
                Surface(
                    onClick = onLockToggle,
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(overlayVisualPolicy.lockButtonCornerRadiusDp.dp),
                    modifier = Modifier.size(overlayVisualPolicy.lockButtonSizeDp.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            if (isScreenLocked) CupertinoIcons.Default.LockOpen else CupertinoIcons.Default.Lock,
                            contentDescription = if (isScreenLocked) "解锁" else "锁定",
                            tint = if (isScreenLocked) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(overlayVisualPolicy.lockIconSizeDp.dp)
                        )
                    }
                }
            }
        }

        // --- 4.  [新增] 真实分辨率统计信息 (仅在设置开启时显示) ---
        if (showStats && realResolution.isNotEmpty() && isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = overlayVisualPolicy.statsTopPaddingDp.dp,
                        end = overlayVisualPolicy.statsEndPaddingDp.dp
                    )
                    .background(
                        Color.Black.copy(0.6f),
                        RoundedCornerShape(overlayVisualPolicy.statsCornerRadiusDp.dp)
                    )
                    .padding(
                        horizontal = overlayVisualPolicy.statsHorizontalPaddingDp.dp,
                        vertical = overlayVisualPolicy.statsVerticalPaddingDp.dp
                    )
            ) {
                Text(
                    text = "Resolution: $realResolution",
                    color = Color.Green,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = overlayVisualPolicy.statsFontSp.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // --- 5. 中央播放/暂停大图标 (仅全屏模式显示) ---
        AnimatedVisibility(
            visible = isVisible && !isPlaying && !isQualitySwitching && isFullscreen,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(tween(250)) + fadeIn(tween(200)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200))
        ) {
            Surface(
                onClick = { player.play(); isPlaying = true },
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(overlayVisualPolicy.centerPlayButtonSizeDp.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        CupertinoIcons.Default.Play,
                        contentDescription = "播放",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(overlayVisualPolicy.centerPlayIconSizeDp.dp)
                    )
                }
            }
        }

        // --- 5.4  缓冲加载指示器 ---
        AnimatedVisibility(
            visible = isBuffering && !isQualitySwitching && !isVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            CupertinoActivityIndicator()
        }

        // --- 5.5  清晰度切换中 Loading 指示器 ---
        AnimatedVisibility(
            visible = isQualitySwitching,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(overlayVisualPolicy.qualitySwitchCornerRadiusDp.dp),
                modifier = Modifier.padding(overlayVisualPolicy.qualitySwitchOuterPaddingDp.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(
                        horizontal = overlayVisualPolicy.qualitySwitchContentHorizontalPaddingDp.dp,
                        vertical = overlayVisualPolicy.qualitySwitchContentVerticalPaddingDp.dp
                    )
                ) {
                    //  iOS 风格加载器
                    CupertinoActivityIndicator()
                    Spacer(modifier = Modifier.height(overlayVisualPolicy.qualitySwitchContentSpacingDp.dp))
                    Text(
                        text = "正在切换清晰度...",
                        color = Color.White,
                        fontSize = overlayVisualPolicy.qualitySwitchMessageFontSp.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // --- 6. 清晰度菜单 ---
        if (showQualityMenu) {
            QualitySelectionMenu(
                qualities = qualityLabels,
                qualityIds = qualityIds,
                currentQuality = currentQualityLabel,
                isLoggedIn = isLoggedIn,
                isVip = isVip,
                onQualitySelected = { index ->
                    onQualitySelected(index)
                    showQualityMenu = false
                },
                onDismiss = { showQualityMenu = false },
                useDialog = true
            )
        }
        
        // --- 7.  [新增] 倍速选择菜单 ---
        if (showSpeedMenu) {
            SpeedSelectionMenuDialog(
                currentSpeed = currentSpeed,
                onSpeedSelected = { speed ->
                    currentSpeed = speed
                    player.setPlaybackSpeed(speed)
                    showSpeedMenu = false
                },
                onDismiss = { showSpeedMenu = false }
            )
        }
        
        // --- 7.5  [新增] 视频比例选择菜单 ---
        if (showRatioMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showRatioMenu = false },
                contentAlignment = Alignment.Center
            ) {
                AspectRatioMenu(
                    currentRatio = currentAspectRatio,
                    onRatioSelected = { ratio ->
                        onAspectRatioChange(ratio)
                        showRatioMenu = false
                    },
                    onDismiss = { showRatioMenu = false }
                )
            }
        }
        
        // --- 8.  [新增] 弹幕设置面板 ---
        if (showDanmakuSettings) {
            DanmakuSettingsPanel(
                opacity = danmakuOpacity,
                fontScale = danmakuFontScale,
                speed = danmakuSpeed,
                displayArea = danmakuDisplayArea,
                mergeDuplicates = danmakuMergeDuplicates,
                allowScroll = danmakuAllowScroll,
                allowTop = danmakuAllowTop,
                allowBottom = danmakuAllowBottom,
                allowColorful = danmakuAllowColorful,
                allowSpecial = danmakuAllowSpecial,
                smartOcclusion = danmakuSmartOcclusion,
                smartOcclusionModuleState = smartOcclusionModuleState,
                smartOcclusionDownloadProgress = smartOcclusionDownloadProgress,
                onOpacityChange = onDanmakuOpacityChange,
                onFontScaleChange = onDanmakuFontScaleChange,
                onSpeedChange = onDanmakuSpeedChange,
                onDisplayAreaChange = onDanmakuDisplayAreaChange,
                onMergeDuplicatesChange = onDanmakuMergeDuplicatesChange,
                onAllowScrollChange = onDanmakuAllowScrollChange,
                onAllowTopChange = onDanmakuAllowTopChange,
                onAllowBottomChange = onDanmakuAllowBottomChange,
                onAllowColorfulChange = onDanmakuAllowColorfulChange,
                onAllowSpecialChange = onDanmakuAllowSpecialChange,
                onSmartOcclusionChange = onDanmakuSmartOcclusionChange,
                onSmartOcclusionDownloadClick = onDanmakuSmartOcclusionDownloadClick,
                onDismiss = { showDanmakuSettings = false }
            )
        }
        
        // --- 9.  [新增] 视频设置面板 ---
        if (showVideoSettings) {
            VideoSettingsPanel(
                sleepTimerMinutes = sleepTimerMinutes,
                onSleepTimerChange = onSleepTimerChange,
                onReload = onReloadVideo,
                currentQualityLabel = currentQualityLabel,
                qualityLabels = qualityLabels,
                qualityIds = qualityIds,
                onQualitySelected = { index ->
                    val id = qualityIds.getOrNull(index) ?: 0
                    onQualityChange(id, 0L)  // 位置由上层处理
                    showVideoSettings = false
                },
                currentSpeed = currentSpeed,
                onSpeedChange = { speed ->
                    currentSpeed = speed
                    player.setPlaybackSpeed(speed)
                },
                isFlippedHorizontal = isFlippedHorizontal,
                isFlippedVertical = isFlippedVertical,
                onFlipHorizontal = onFlipHorizontal,
                onFlipVertical = onFlipVertical,
                isAudioOnly = isAudioOnly,
                onAudioOnlyToggle = onAudioOnlyToggle,
                //  CDN 线路切换
                currentCdnIndex = currentCdnIndex,
                cdnCount = cdnCount,
                onSwitchCdn = onSwitchCdn,
                onSwitchCdnTo = { index ->
                    onSwitchCdnTo(index)
                    showVideoSettings = false
                },
                // [New] Codec & Audio
                currentCodec = currentCodec,
                onCodecChange = { codec ->
                    onCodecChange(codec)
                    showVideoSettings = false
                },
                currentAudioQuality = currentAudioQuality,
                onAudioQualityChange = { quality ->
                    onAudioQualityChange(quality)
                    showVideoSettings = false
                },
                // [New] AI Audio
                aiAudioInfo = aiAudioInfo,
                currentAudioLang = currentAudioLang,
                onAudioLangChange = { lang ->
                    onAudioLangChange(lang)
                    showVideoSettings = false
                },

                onSaveCover = {
                    onSaveCover()
                    // Disimss moved to VideoSettingsPanel internal or caller responsibility?
                    // VideoSettingsPanel calls onSaveCover then onDismiss.
                    // We just invoke the callback.
                },
                onCaptureScreenshot = {
                    onCaptureScreenshot()
                },
                onDownloadAudio = {
                    onDownloadAudio()
                },
                onDismiss = { showVideoSettings = false }
            )
        }

        if (showPlaybackOrderSheet) {
            PlaybackOrderSelectionSheet(
                currentBehavior = playbackCompletionBehavior,
                onSelect = { behavior ->
                    scope.launch {
                        SettingsManager.setPlaybackCompletionBehavior(context, behavior)
                    }
                    showPlaybackOrderSheet = false
                },
                onDismiss = { showPlaybackOrderSheet = false }
            )
        }
        
        // --- 10. 📖 [新增] 章节列表面板 ---
        if (showChapterList && viewPoints.isNotEmpty()) {
            ChapterListPanel(
                viewPoints = viewPoints,
                currentPositionMs = progressState.current,
                onSeek = { position -> onSeekTo?.invoke(position) ?: player.seekTo(position) },
                onDismiss = { showChapterList = false }
            )
        }
        
        // --- 11. [新增] 侧边栏抽屉 ---
        LandscapeEndDrawer(
            visible = showEndDrawer,
            onDismiss = { showEndDrawer = false },
            relatedVideos = relatedVideos,
            ugcSeason = ugcSeason,
            currentBvid = bvid,
            ownerName = videoOwnerName,
            ownerFace = videoOwnerFace,
            isFollowed = isFollowed,
            isLiked = isLiked,
            isCoined = isCoined,
            isFavorited = isFavorited,
            onToggleFollow = onToggleFollow,
            onToggleLike = onToggleLike,
            onCoin = onCoin,
            onToggleFavorite = onToggleFavorite,
            onTripleLike = onTriple,
            onVideoClick = { vid ->
                onDrawerVideoClick(vid)
                showEndDrawer = false
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        
        // --- 12. 📺 投屏对话框 ---
        if (showCastDialog) {
            DeviceListDialog(
                onDismissRequest = { showCastDialog = false },
                onDeviceSelected = { device ->
                    showCastDialog = false
                    // Generate Proxy URL
                    val proxyUrl = LocalProxyServer.getProxyUrl(context, currentVideoUrl)
                    // Cast!
                    DlnaManager.cast(device, proxyUrl, videoTitle, videoOwnerName)
                },
                onSsdpDeviceSelected = { ssdpDevice ->
                    showCastDialog = false
                    val proxyUrl = LocalProxyServer.getProxyUrl(context, currentVideoUrl)
                    scope.launch {
                        val result = SsdpCastClient.cast(
                            device = ssdpDevice,
                            mediaUrl = proxyUrl,
                            title = videoTitle,
                            creator = videoOwnerName
                        )
                        val message = if (result.isSuccess) {
                            "已发送投屏指令"
                        } else {
                            "投屏失败：${result.exceptionOrNull()?.message ?: "未知错误"}"
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

/**
 *  竖屏模式顶部控制栏
 * 
 * 包含返回首页按钮、弹幕开关、设置按钮和分享按钮
 */
@Composable
private fun PortraitTopBar(
    onlineCount: String = "",
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onShare: () -> Unit,
    onAudioMode: () -> Unit,
    isAudioOnly: Boolean,
    // 📺 [新增] 投屏
    onCastClick: () -> Unit = {},
    // 📱 左上角快捷入口：弹幕开关
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTvDevice = rememberIsTvDevice()
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolvePortraitTopBarLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = layoutPolicy.horizontalPaddingDp.dp,
                vertical = layoutPolicy.verticalPaddingDp.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：返回按钮 + 弹幕开关
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(layoutPolicy.leftSectionSpacingDp.dp)
        ) {
            // 返回按钮 - 简洁无背景
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.ChevronBackward,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                )
            }
            
            Surface(
                onClick = onDanmakuToggle,
                color = if (danmakuEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (danmakuEnabled) "弹幕开" else "弹幕关",
                    color = Color.White,
                    fontSize = layoutPolicy.chipFontSp.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(
                        horizontal = layoutPolicy.chipHorizontalPaddingDp.dp,
                        vertical = layoutPolicy.chipVerticalPaddingDp.dp
                    )
                )
            }
            
            // 👀 在线人数
            if (onlineCount.isNotEmpty()) {
                Text(
                    text = onlineCount,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = layoutPolicy.onlineCountFontSp.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // 右侧按钮组
        Row(
            horizontalArrangement = Arrangement.spacedBy(layoutPolicy.rightSectionSpacingDp.dp)
        ) {
            //  听视频模式按钮 - 激活时保留背景色
            IconButton(
                onClick = onAudioMode,
                modifier = Modifier
                    .size(layoutPolicy.buttonSizeDp.dp)
                    .then(
                        if (isAudioOnly) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.Headphones,
                    contentDescription = "听视频",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                )
            }

            // 📺 投屏按钮 - 无背景
            IconButton(
                onClick = onCastClick,
                modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
            ) {
                Icon(
                    imageVector = io.github.alexzhirkevich.cupertino.icons.CupertinoIcons.Default.Tv,
                    contentDescription = "投屏",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                )
            }
            
            //  设置按钮 - 无背景
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.Ellipsis,
                    contentDescription = "设置",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                )
            }
            
            // 分享按钮 - 无背景
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.SquareAndArrowUp,
                    contentDescription = "分享",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                )
            }
        }
    }
}

// --- 11. 侧边栏抽屉 (Landscape End Drawer) ---
@Composable
fun LandscapeEndDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    // Data
    relatedVideos: List<com.android.purebilibili.data.model.response.RelatedVideo>,
    ugcSeason: com.android.purebilibili.data.model.response.UgcSeason?,
    currentBvid: String,
    // UP Info
    ownerName: String,
    ownerFace: String,
    // Interaction States
    isFollowed: Boolean,
    isLiked: Boolean,
    isCoined: Boolean,
    isFavorited: Boolean,
    // Callbacks
    onToggleFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onCoin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTripleLike: () -> Unit = {},  // [新增] 一键三连回调
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTvDevice = rememberIsTvDevice()
    val context = LocalContext.current
    val tvPerformanceProfileEnabled by SettingsManager
        .getTvPerformanceProfileEnabled(context)
        .collectAsState(initial = isTvDevice)
    val configuration = LocalConfiguration.current
    val widthSizeClass = remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp < 600 -> WindowWidthSizeClass.Compact
            configuration.screenWidthDp < 840 -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
    }
    val deviceUiProfile = remember(isTvDevice, widthSizeClass, tvPerformanceProfileEnabled) {
        resolveDeviceUiProfile(
            isTv = isTvDevice,
            widthSizeClass = widthSizeClass,
            tvPerformanceProfileEnabled = tvPerformanceProfileEnabled
        )
    }
    val cardAnimationEnabled by SettingsManager
        .getCardAnimationEnabled(context)
        .collectAsState(initial = true)
    val overlayMotionTier = resolveEffectiveMotionTier(
        baseTier = deviceUiProfile.motionTier,
        animationEnabled = cardAnimationEnabled
    )
    val layoutPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveLandscapeEndDrawerLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }
    val overlayVisualPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveVideoPlayerOverlayVisualPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 点击空白处关闭
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
            
            // 抽屉内容
            Surface(
                modifier = Modifier
                    .width(layoutPolicy.drawerWidthDp.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. 顶部交互区 (UP主信息 + 一键三连)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(layoutPolicy.headerPaddingDp.dp)
                    ) {
                        // Row 1: UP主头像、名字、关注按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 头像
                            coil.compose.AsyncImage(
                                model = ownerFace,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(layoutPolicy.avatarSizeDp.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray),
                                contentScale = ContentScale.Crop
                            )
                            
                            Spacer(modifier = Modifier.width(layoutPolicy.headerSpacingDp.dp))
                            
                            // 名字
                            Text(
                                text = ownerName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = layoutPolicy.titleFontSp.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Spacer(modifier = Modifier.width(layoutPolicy.headerSpacingDp.dp))
                            
                            // 关注按钮 (放在右上角)
                            Button(
                                onClick = onToggleFollow,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowed) MaterialTheme.colorScheme.onSurface.copy(0.2f) else MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(
                                    horizontal = layoutPolicy.followButtonHorizontalPaddingDp.dp,
                                    vertical = 0.dp
                                ),
                                modifier = Modifier.height(layoutPolicy.followButtonHeightDp.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    if (isFollowed) "已关注" else "+ 关注",
                                    fontSize = layoutPolicy.followButtonFontSp.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(layoutPolicy.sectionSpacingDp.dp))
                        
                        // Row 2: 一键三连按钮 (SpaceAround)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 点赞 - 支持长按三连
                            TripleLikeInteractionButton(
                                isLiked = isLiked,
                                isCoined = isCoined,
                                isFavorited = isFavorited,
                                onLikeClick = onToggleLike,
                                onCoinClick = onCoin,
                                onFavoriteClick = onToggleFavorite,
                                onTripleComplete = onTripleLike,
                                layoutPolicy = overlayVisualPolicy
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    // 2. Tab Row
                    var selectedTab by remember { mutableIntStateOf(0) } // 0: 推荐, 1: 合集
                    val hasSeason = ugcSeason != null && ugcSeason.sections.isNotEmpty()
                    
                    if (hasSeason) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("推荐视频", fontSize = layoutPolicy.followButtonFontSp.sp) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("合集列表", fontSize = layoutPolicy.followButtonFontSp.sp) }
                            )
                        }
                    } else {
                        // 只有推荐，显示标题
                        Text(
                            text = "推荐视频",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(layoutPolicy.listContentPaddingDp.dp)
                        )
                    }
                    
                    // 3. 列表内容
                    Box(modifier = Modifier.weight(1f)) {
                        if (selectedTab == 0) {
                            // 推荐视频列表
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(layoutPolicy.listContentPaddingDp.dp),
                                verticalArrangement = Arrangement.spacedBy(layoutPolicy.listItemSpacingDp.dp)
                            ) {
                                items(relatedVideos) { video ->
                                    LandscapeVideoItem(
                                        video = video,
                                        layoutPolicy = layoutPolicy,
                                        isTvDevice = isTvDevice,
                                        screenWidthDp = configuration.screenWidthDp,
                                        motionTier = overlayMotionTier,
                                        isCurrent = video.bvid == currentBvid,
                                        onClick = { onVideoClick(video.bvid) }
                                    )
                                }
                            }
                        } else if (hasSeason && ugcSeason != null) {
                            // 合集列表
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(layoutPolicy.listContentPaddingDp.dp),
                                verticalArrangement = Arrangement.spacedBy(layoutPolicy.listItemSpacingDp.dp)
                            ) {
                                ugcSeason.sections.forEach { section ->
                                    item {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    items(section.episodes) { episode ->
                                        LandscapeEpisodeItem(
                                            episode = episode,
                                            layoutPolicy = layoutPolicy,
                                            isTvDevice = isTvDevice,
                                            screenWidthDp = configuration.screenWidthDp,
                                            motionTier = overlayMotionTier,
                                            isCurrent = episode.bvid == currentBvid,
                                            onClick = { onVideoClick(episode.bvid) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

/**
 * 一键三连长按按钮 (横屏版) - 长按显示点赞、投币、收藏三个图标的圆形进度条
 */
@Composable
private fun TripleLikeInteractionButton(
    isLiked: Boolean,
    isCoined: Boolean,
    isFavorited: Boolean,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onTripleComplete: () -> Unit,
    layoutPolicy: VideoPlayerOverlayVisualPolicy,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressProgress by remember { mutableFloatStateOf(0f) }
    val progressDuration = 1500
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (isLongPressing) 1f else 0f,
        animationSpec = if (isLongPressing) {
            androidx.compose.animation.core.tween(durationMillis = progressDuration, easing = LinearEasing)
        } else {
            androidx.compose.animation.core.tween(durationMillis = 200, easing = FastOutSlowInEasing)
        },
        label = "tripleLikeProgress",
        finishedListener = { progress ->
            if (progress >= 1f && isLongPressing) {
                haptic(HapticType.MEDIUM)
                onTripleComplete()
                isLongPressing = false
            }
        }
    )
    
    LaunchedEffect(animatedProgress) {
        longPressProgress = animatedProgress
    }
    
    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            haptic(HapticType.LIGHT)
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(layoutPolicy.tripleActionSpacingDp.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // 点赞
        LandscapeProgressIcon(
            icon = if (isLiked) CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Outlined.HandThumbsup,
            label = "点赞",
            progress = longPressProgress,
            progressColor = MaterialTheme.colorScheme.primary,
            isActive = isLiked,
            onClick = onLikeClick,
            onLongPress = { isLongPressing = true },
            onRelease = { 
                if (longPressProgress < 0.1f) onLikeClick()
                isLongPressing = false 
            },
            layoutPolicy = layoutPolicy
        )
        
        // 投币 (显示时带进度)
        LandscapeProgressIcon(
            icon = AppIcons.BiliCoin,
            label = "投币",
            progress = longPressProgress,
            progressColor = Color(0xFFFFB300),
            isActive = isCoined,
            onClick = onCoinClick,
            showProgress = longPressProgress > 0.05f,
            layoutPolicy = layoutPolicy
        )
        
        // 收藏 (显示时带进度)
        LandscapeProgressIcon(
            icon = if (isFavorited) CupertinoIcons.Filled.Star else CupertinoIcons.Default.Star,
            label = "收藏",
            progress = longPressProgress,
            progressColor = Color(0xFFFFC107),
            isActive = isFavorited,
            onClick = onFavoriteClick,
            showProgress = longPressProgress > 0.05f,
            layoutPolicy = layoutPolicy
        )
    }
}

/**
 * 横屏带进度环的交互图标
 */
@Composable
private fun LandscapeProgressIcon(
    icon: ImageVector,
    label: String,
    progress: Float,
    progressColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onRelease: (() -> Unit)? = null,
    showProgress: Boolean = true,
    layoutPolicy: VideoPlayerOverlayVisualPolicy,
    modifier: Modifier = Modifier
) {
    val iconSize = layoutPolicy.interactionIconSizeDp.dp
    val ringSize = iconSize + layoutPolicy.tripleRingExtraSizeDp.dp
    val strokeWidth = 2.5.dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(
                if (onLongPress != null && onRelease != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onLongPress()
                                tryAwaitRelease()
                                onRelease()
                            }
                        )
                    }
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                }
            )
    ) {
        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            if (showProgress && progress > 0f) {
                Canvas(modifier = Modifier.size(ringSize)) {
                    val stroke = strokeWidth.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    
                    drawArc(
                        color = progressColor.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) progressColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(iconSize)
            )
        }
        
        Spacer(modifier = Modifier.height(layoutPolicy.interactionLabelTopSpacingDp.dp))
        Text(
            text = label,
            color = if (isActive) progressColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = layoutPolicy.interactionLabelFontSp.sp
        )
    }
}

@Composable
private fun LandscapeVideoItem(
    video: com.android.purebilibili.data.model.response.RelatedVideo,
    layoutPolicy: LandscapeEndDrawerLayoutPolicy,
    isTvDevice: Boolean,
    screenWidthDp: Int,
    motionTier: MotionTier,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(layoutPolicy.videoItemHeightDp.dp)
            .tvFocusableJiggle(
                isTv = isTvDevice,
                screenWidthDp = screenWidthDp,
                reducedMotion = false,
                motionTier = motionTier
            )
            .clickable(onClick = onClick)
            .background(if (isCurrent) MaterialTheme.colorScheme.onSurface.copy(0.1f) else Color.Transparent, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .aspectRatio(16f / 9f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
        ) {
             coil.compose.AsyncImage(
                model = video.pic,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // 时长
            Text(
                text = FormatUtils.formatDuration(video.duration),
                color = Color.White,
                fontSize = layoutPolicy.itemDurationFontSp.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 标题和UP
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = video.title,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = layoutPolicy.itemTitleFontSp.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = (layoutPolicy.itemTitleFontSp + 3).sp
            )
            Text(
                text = video.owner.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = layoutPolicy.itemMetaFontSp.sp
            )
        }
    }
}

@Composable
private fun LandscapeEpisodeItem(
    episode: com.android.purebilibili.data.model.response.UgcEpisode,
    layoutPolicy: LandscapeEndDrawerLayoutPolicy,
    isTvDevice: Boolean,
    screenWidthDp: Int,
    motionTier: MotionTier,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(layoutPolicy.episodeItemHeightDp.dp)
            .tvFocusableJiggle(
                isTv = isTvDevice,
                screenWidthDp = screenWidthDp,
                reducedMotion = false,
                motionTier = motionTier
            )
            .clickable(onClick = onClick)
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(4.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 封面 (如果有 arc 信息)
        if (episode.arc != null && episode.arc.pic.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .aspectRatio(16f / 9f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                coil.compose.AsyncImage(
                    model = episode.arc.pic,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 时长
                Text(
                    text = FormatUtils.formatDuration(episode.arc.duration),
                    color = Color.White,
                    fontSize = layoutPolicy.itemDurationFontSp.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(0.6f), RoundedCornerShape(topStart = 4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            // 无封面时的占位 (或纯文本模式)
             if (isCurrent) {
                Icon(
                    imageVector = CupertinoIcons.Default.Play,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(layoutPolicy.metaIconSizeDp.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                 Box(
                    modifier = Modifier
                        .size((layoutPolicy.metaIconSizeDp / 2).dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(0.3f), CircleShape)
                )
                 Spacer(modifier = Modifier.width(18.dp))
            }
        }
        
        // 2. 信息列
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround // 分散对齐
        ) {
            // 标题
            Text(
                text = episode.title,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = layoutPolicy.itemTitleFontSp.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                lineHeight = (layoutPolicy.itemTitleFontSp + 3).sp
            )
            
            // 底部元数据 (弹幕/观看等，如果有)
            // 目前 UgcEpisodeArc -> Stat (view, danmaku)
            // 我们暂且假设 stat 存在且包含 view
            /* 
               注意：data.model.response.Stat 通常包含 view, danmaku
               这里我们需要安全访问
            */
            if (episode.arc?.stat != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 播放量
                    Icon(
                        imageVector = CupertinoIcons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(layoutPolicy.metaIconSizeDp.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = FormatUtils.formatStat(episode.arc.stat.view.toLong()), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = layoutPolicy.itemMetaFontSp.sp
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 弹幕
                    Icon(
                        imageVector = Icons.Filled.ChatBubble, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(layoutPolicy.metaIconSizeDp.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = FormatUtils.formatStat(episode.arc.stat.danmaku.toLong()), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = layoutPolicy.itemMetaFontSp.sp
                    )
                }
            } else if (episode.arc != null) {
                // 如果没有 stat 但有 arc，显示 "P<Index>" 或其他信息?
                // 暂时只显示时长 (上面已经显示在封面上了) 或保持空白
            }
        }
    }
}
