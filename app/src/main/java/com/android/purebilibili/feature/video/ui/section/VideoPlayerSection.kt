// 文件路径: feature/video/VideoPlayerSection.kt
package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.feature.video.danmaku.DanmakuManager
import com.android.purebilibili.feature.video.danmaku.FaceOcclusionDanmakuContainer
import com.android.purebilibili.feature.video.danmaku.FaceOcclusionMaskStabilizer
import com.android.purebilibili.feature.video.danmaku.FaceOcclusionModuleState
import com.android.purebilibili.feature.video.danmaku.FaceOcclusionVisualMask
import com.android.purebilibili.feature.video.danmaku.checkFaceOcclusionModuleState
import com.android.purebilibili.feature.video.danmaku.createFaceOcclusionDetector
import com.android.purebilibili.feature.video.danmaku.detectFaceOcclusionRegions
import com.android.purebilibili.feature.video.danmaku.installFaceOcclusionModule
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.ui.overlay.VideoPlayerOverlay
import com.android.purebilibili.feature.video.ui.components.SponsorSkipButton
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import com.android.purebilibili.data.model.response.ViewPoint

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
// 🌈 Material Icons Extended - 亮度图标
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.core.util.shouldHandleTvBackKey
import com.android.purebilibili.core.util.shouldHandleTvMenuKey
import com.android.purebilibili.core.util.shouldHandleTvMoveFocusDownKey
import com.android.purebilibili.core.util.shouldHandleTvPlayPauseKey
import com.android.purebilibili.core.util.shouldHandleTvSelectKey
import com.android.purebilibili.feature.video.util.captureAndSaveVideoScreenshot
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

enum class VideoGestureMode { None, Brightness, Volume, Seek, SwipeToFullscreen }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerSection(
    playerState: VideoPlayerState,
    uiState: PlayerUiState,
    isFullscreen: Boolean,
    isInPipMode: Boolean,
    onToggleFullscreen: () -> Unit,
    onQualityChange: (Int, Long) -> Unit,
    onBack: () -> Unit,
    // 🔗 [新增] 分享功能
    bvid: String = "",
    coverUrl: String = "",
    //  实验性功能：双击点赞
    onDoubleTapLike: () -> Unit = {},
    //  空降助手
    sponsorSegment: com.android.purebilibili.data.model.response.SponsorSegment? = null,
    showSponsorSkipButton: Boolean = false,
    onSponsorSkip: () -> Unit = {},
    onSponsorDismiss: () -> Unit = {},
    //  [新增] 重载视频回调
    onReloadVideo: () -> Unit = {},
    //  [新增] CDN 线路切换
    currentCdnIndex: Int = 0,
    cdnCount: Int = 1,
    onSwitchCdn: () -> Unit = {},
    onSwitchCdnTo: (Int) -> Unit = {},
    
    //  [新增] 音频模式
    isAudioOnly: Boolean = false,
    onAudioOnlyToggle: () -> Unit = {},
    
    //  [新增] 定时关闭
    sleepTimerMinutes: Int? = null,
    onSleepTimerChange: (Int?) -> Unit = {},
    
    // 🖼️ [新增] 视频预览图数据
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    
    // 📖 [新增] 视频章节数据
    viewPoints: List<ViewPoint> = emptyList(),
    
    // 📱 [新增] 竖屏全屏模式
    isVerticalVideo: Boolean = false,
    onPortraitFullscreen: () -> Unit = {},
    isPortraitFullscreen: Boolean = false,
    // 📲 [新增] 小窗模式
    // 📲 [新增] 小窗模式
    onPipClick: () -> Unit = {},
    // [New] Codec & Audio Params
    currentCodec: String = "hev1", 
    onCodecChange: (String) -> Unit = {},
    currentAudioQuality: Int = -1,
    onAudioQualityChange: (Int) -> Unit = {},
    // [New] Audio Language
    onAudioLangChange: (String) -> Unit = {},
    // 👀 [新增] 在线观看人数
    onlineCount: String = "",
    // [New Actions]
    onSaveCover: () -> Unit = {},
    onDownloadAudio: () -> Unit = {},
    // 🔁 [新增] 播放模式
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode = com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {},

    // [新增] 侧边栏抽屉数据与交互
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit = {_,_ -> },
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
    tvFocusRequester: FocusRequester? = null,
    onTvMoveFocusDown: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isTvDevice = rememberIsTvDevice()
    val configuration = LocalConfiguration.current
    val uiLayoutPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveVideoPlayerUiLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // --- 新增：读取设置中的"详细统计信息"开关 ---
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    // 使用 rememberUpdatedState 确保重组时获取最新值（虽然在单一 Activity 生命周期内可能需要重启生效，但简单场景够用）
    val showStats by remember { mutableStateOf(prefs.getBoolean("show_stats", false)) }
    
    //  [新增] 读取手势灵敏度设置
    val gestureSensitivity by com.android.purebilibili.core.store.SettingsManager
        .getGestureSensitivity(context)
        .collectAsState(initial = 1.0f)

    // 📱 [优化] realResolution 现在从 playerState.videoSize 计算（见下方）
    
    //  读取双击点赞设置 (从 DataStore 读取)
    val doubleTapLikeEnabled by com.android.purebilibili.core.store.SettingsManager
        .getDoubleTapLike(context)
        .collectAsState(initial = true)
    
    //  [新增] 读取双击跳转秒数设置
    val doubleTapSeekEnabled by com.android.purebilibili.core.store.SettingsManager
        .getDoubleTapSeekEnabled(context)
        .collectAsState(initial = true)

    val seekForwardSeconds by com.android.purebilibili.core.store.SettingsManager
        .getSeekForwardSeconds(context)
        .collectAsState(initial = 10)
    val seekBackwardSeconds by com.android.purebilibili.core.store.SettingsManager
        .getSeekBackwardSeconds(context)
        .collectAsState(initial = 10)
    
    //  [新增] 双击跳转视觉反馈状态
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var seekFeedbackVisible by remember { mutableStateOf(false) }
    
    //  [新增] 长按倍速设置和状态
    val longPressSpeed by com.android.purebilibili.core.store.SettingsManager
        .getLongPressSpeed(context)
        .collectAsState(initial = 2.0f)
    var isLongPressing by remember { mutableStateOf(false) }
    var originalSpeed by remember { mutableFloatStateOf(1.0f) }
    var longPressSpeedFeedbackVisible by remember { mutableStateOf(false) }
    
    //  [新增] 缓冲状态监听
    var isBuffering by remember { mutableStateOf(false) }
    DisposableEffect(playerState.player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        playerState.player.addListener(listener)
        // 初始化状态
        isBuffering = playerState.player.playbackState == Player.STATE_BUFFERING
        onDispose {
            playerState.player.removeListener(listener)
        }
    }

    // 📱 [优化] 复用 VideoPlayerState 中的视频尺寸状态，避免重复监听
    val videoSizeState by playerState.videoSize.collectAsState()
    val realResolution = if (videoSizeState.first > 0 && videoSizeState.second > 0) {
        "${videoSizeState.first} x ${videoSizeState.second}"
    } else {
        ""
    }

    // 控制器显示状态
    var showControls by remember { mutableStateOf(true) }
    val tvPlayerFocusRequester = tvFocusRequester ?: remember { FocusRequester() }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var faceVisualMasks by remember { mutableStateOf(emptyList<FaceOcclusionVisualMask>()) }
    val faceMaskStabilizer = remember { FaceOcclusionMaskStabilizer() }
    var smartOcclusionModuleState by remember { mutableStateOf(FaceOcclusionModuleState.Checking) }
    var smartOcclusionDownloadProgress by remember { mutableStateOf<Int?>(null) }
    
    // 🔒 [新增] 屏幕锁定状态（全屏时防误触）
    var isScreenLocked by remember { mutableStateOf(false) }

    var gestureMode by remember { mutableStateOf<VideoGestureMode>(VideoGestureMode.None) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gesturePercent by remember { mutableFloatStateOf(0f) }

    // 进度手势相关状态
    var seekTargetTime by remember { mutableLongStateOf(0L) }
    var startPosition by remember { mutableLongStateOf(0L) }
    var isGestureVisible by remember { mutableStateOf(false) }
    
    //  视频比例状态
    var currentAspectRatio by remember { mutableStateOf(VideoAspectRatio.FIT) }
    
    //  [新增] 视频翻转状态
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var isFlippedVertical by remember { mutableStateOf(false) }

    // 记录手势开始时的初始值
    var startVolume by remember { mutableIntStateOf(0) }
    var startBrightness by remember { mutableFloatStateOf(0f) }

    // 记录累计拖动距离
    var totalDragDistanceY by remember { mutableFloatStateOf(0f) }
    var totalDragDistanceX by remember { mutableFloatStateOf(0f) }
    // 记录手势起点 X（用于锁定分区，避免拖动过程横向漂移导致误判）
    var dragStartX by remember { mutableFloatStateOf(-1f) }

    fun getActivity(): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> context.baseContext as? Activity
        else -> null
    }

    //  [新增] 缩放和平移状态
    var scale by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        onDispose { playerViewRef = null }
    }

    // [新增] 共享元素过渡支持
    val sharedTransitionScope = com.android.purebilibili.core.ui.LocalSharedTransitionScope.current
    val animatedVisibilityScope = com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    //  共享弹幕管理器（用于所有 seek 路径的一致同步）
    val danmakuManager = rememberDanmakuManager()

    LaunchedEffect(isTvDevice) {
        if (isTvDevice) {
            tvPlayerFocusRequester.requestFocus()
        }
    }
    
    var rootModifier = Modifier
        .fillMaxSize()
        .clipToBounds()
        .background(Color.Black)

    // 应用共享元素
    if (bvid.isNotEmpty() && sharedTransitionScope != null && animatedVisibilityScope != null) {
         with(sharedTransitionScope) {
             rootModifier = rootModifier.sharedElement(
                 sharedContentState = rememberSharedContentState(key = "video-$bvid"),
                 animatedVisibilityScope = animatedVisibilityScope,
                 boundsTransform = { _, _ ->
                     com.android.purebilibili.core.theme.AnimationSpecs.BiliPaiSpringSpec
                 }
             )
         }
    }

    Box(
        modifier = rootModifier
            .then(
                if (isTvDevice) {
                    Modifier
                        .focusRequester(tvPlayerFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            val keyCode = event.nativeKeyEvent.keyCode
                            val action = event.nativeKeyEvent.action
                            when {
                                shouldHandleTvPlayPauseKey(keyCode, action) -> {
                                    val player = playerState.player
                                    player.playWhenReady = !player.playWhenReady
                                    showControls = true
                                    true
                                }
                                shouldHandleTvMenuKey(keyCode, action) -> {
                                    if (!showControls) {
                                        showControls = true
                                        true
                                    } else {
                                        false
                                    }
                                }
                                shouldHandleTvSelectKey(keyCode, action) -> {
                                    if (!showControls) {
                                        showControls = true
                                        true
                                    } else {
                                        false
                                    }
                                }
                                shouldHandleTvBackKey(keyCode, action) && isFullscreen && !isScreenLocked -> {
                                    onToggleFullscreen()
                                    true
                                }
                                shouldHandleTvMoveFocusDownKey(keyCode, action) &&
                                    onTvMoveFocusDown != null -> {
                                    onTvMoveFocusDown.invoke()
                                    true
                                }
                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT &&
                                    action == KeyEvent.ACTION_DOWN &&
                                    onTvMoveFocusDown != null -> {
                                    onTvMoveFocusDown.invoke()
                                    true
                                }
                                else -> false
                            }
                        }
                } else {
                    Modifier
                }
            )
            //  [新增] 处理双指缩放和平移
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    
                    if (scale > 1f) {
                        // 缩放状态下，允许平移
                        val maxPanX = (size.width * scale - size.width) / 2
                        val maxPanY = (size.height * scale - size.height) / 2
                        panX = (panX + pan.x * scale).coerceIn(-maxPanX, maxPanX)
                        panY = (panY + pan.y * scale).coerceIn(-maxPanY, maxPanY)
                        
                        // 如果正在缩放/平移，隐藏手势图标和控制栏
                        isGestureVisible = false
                        showControls = false
                    } else {
                        // 恢复原始比例时，重置平移
                        panX = 0f
                        panY = 0f
                    }
                }
            }
            //  先处理拖拽手势 (音量/亮度/进度)
            .pointerInput(isInPipMode, isScreenLocked) {
                if (!isInPipMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // [新增] 如果处于缩放状态，禁用常规拖拽手势，优先处理平移
                            if (scale > 1.01f) {  // 留一点浮点数buffer
                                return@detectDragGestures
                            }
                            
                            // 🔒 锁定时禁用拖拽手势
                            if (isScreenLocked) {
                                return@detectDragGestures
                            }                
                            //  [新增] 边缘防误触检测
                            //  如果在屏幕顶部或底部区域开始滑动，则视为系统手势（如下拉通知栏），不触发播放器手势
                            val density = context.resources.displayMetrics.density
                            val safeZonePx = 48 * density  //  48dp 安全区域
                            val screenHeight = size.height

                            // 检查是否在安全区域内 (顶部或底部)
                            val isEdgeGesture = offset.y < safeZonePx || offset.y > (screenHeight - safeZonePx)
                            
                            if (isEdgeGesture) {
                                isGestureVisible = false
                                gestureMode = VideoGestureMode.None
                                dragStartX = -1f
                                // 不需要 return，直接不执行下面的初始化逻辑即可
                            } else {
                                isGestureVisible = true
                                gestureMode = VideoGestureMode.None
                                dragStartX = offset.x
                                totalDragDistanceY = 0f
                                totalDragDistanceX = 0f

                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                startPosition = playerState.player.currentPosition

                                val attributes = getActivity()?.window?.attributes
                                val currentWindowBrightness = attributes?.screenBrightness ?: -1f

                                if (currentWindowBrightness < 0) {
                                    try {
                                        val sysBrightness = Settings.System.getInt(
                                            context.contentResolver,
                                            Settings.System.SCREEN_BRIGHTNESS
                                        )
                                        startBrightness = sysBrightness / 255f
                                    } catch (e: Exception) {
                                        startBrightness = 0.5f
                                    }
                                } else {
                                    startBrightness = currentWindowBrightness
                                }
                            }
                        },
                        onDragEnd = {
                            if (gestureMode == VideoGestureMode.Seek) {
                                playerState.player.seekTo(seekTargetTime)
                                danmakuManager.seekTo(seekTargetTime)
                                playerState.player.play()
                            } else if (gestureMode == VideoGestureMode.SwipeToFullscreen) {
                                //  阈值判定：上滑超过一定距离触发全屏
                                val swipeThreshold = 50.dp.toPx()
                                if (totalDragDistanceY < -swipeThreshold && !isFullscreen) {
                                    onToggleFullscreen()
                                    // 震动反馈 (可选)
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "👆 Swipe up to fullscreen triggered")
                                }
                            }
                            isGestureVisible = false
                            gestureMode = VideoGestureMode.None
                            dragStartX = -1f
                        },
                        onDragCancel = {
                            isGestureVisible = false
                            gestureMode = VideoGestureMode.None
                            dragStartX = -1f
                        },
                        //  [修复点] 使用 dragAmount 而不是 change.positionChange()
                        onDrag = { change, dragAmount ->
                            // 如果手势不可见（即在 safe zone 中启动被忽略），则停止处理
                            if (!isGestureVisible && gestureMode == VideoGestureMode.None) {
                                // do nothing
                            } else {
                            
                            // [修复] 累积拖动距离，用于更准确的方向判断
                            totalDragDistanceX += dragAmount.x
                            totalDragDistanceY += dragAmount.y
                            
                            // [修复] 等待累积一定距离后再确定手势类型，避免初始噪声导致误判
                            val minDragThreshold = 20.dp.toPx()
                            val totalDrag = kotlin.math.hypot(totalDragDistanceX, totalDragDistanceY)

                            if (gestureMode == VideoGestureMode.None && totalDrag >= minDragThreshold) {
                                // [修复] 使用累积距离判断方向，而非单帧增量
                                if (abs(totalDragDistanceX) > abs(totalDragDistanceY)) {
                                    gestureMode = VideoGestureMode.Seek
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "🎯 Gesture: Seek (cumDx=$totalDragDistanceX, cumDy=$totalDragDistanceY)")
                                } else {
                                    // 根据起始 X 坐标判断区域 (左1/3=亮度, 右1/3=音量, 中间1/3=功能区)
                                    val width = size.width.toFloat()
                                    // 使用 onDragStart 锁定的起点 X，避免拖动中横向偏移导致误触
                                    val startX = if (dragStartX >= 0f) dragStartX else change.position.x
                                    // 分区边界增加缓冲，避免中间区域在边界附近被误判
                                    val boundaryPadding = uiLayoutPolicy.gestureBoundaryPaddingDp.dp.toPx()
                                    val leftZoneEnd = (width / 3f - boundaryPadding).coerceAtLeast(0f)
                                    val rightZoneStart = (width * 2f / 3f + boundaryPadding).coerceAtMost(width)
                                    val isSwipeUp = totalDragDistanceY < -minDragThreshold
                                    
                                    gestureMode = if (!isFullscreen) {
                                        // 竖屏模式：上滑优先进入全屏，避免误触发亮度/音量浮层
                                        if (isSwipeUp) {
                                            VideoGestureMode.SwipeToFullscreen
                                        } else {
                                            when {
                                                startX < leftZoneEnd -> VideoGestureMode.Brightness
                                                startX > rightZoneStart -> VideoGestureMode.Volume
                                                else -> VideoGestureMode.SwipeToFullscreen
                                            }
                                        }
                                    } else {
                                        // 横屏模式
                                        // 左侧 1/3: 亮度
                                        // 右侧 1/3: 音量
                                        // 中间 1/3: 忽略垂直手势，避免误触
                                        when {
                                            startX < leftZoneEnd -> VideoGestureMode.Brightness
                                            startX > rightZoneStart -> VideoGestureMode.Volume
                                            else -> VideoGestureMode.None
                                        }
                                    }

                                    // 横屏中间 1/3 的垂直手势直接忽略，避免误触亮度/音量
                                    if (isFullscreen && gestureMode == VideoGestureMode.None) {
                                        isGestureVisible = false
                                        com.android.purebilibili.core.util.Logger.d(
                                            "VideoPlayerSection",
                                            "🎯 Gesture ignored in center zone (fullscreen, startX=$startX, width=$width)"
                                        )
                                        return@detectDragGestures
                                    }

                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "🎯 Gesture: $gestureMode (startX=$startX, width=$width, isFullscreen=$isFullscreen)")
                                }
                            }

                            when (gestureMode) {
                                VideoGestureMode.SwipeToFullscreen -> {
                                    // 累积 Y 轴距离已在上方处理
                                }
                                VideoGestureMode.Seek -> {
                                    // 距离已在上方累积，直接计算目标位置
                                    val duration = playerState.player.duration.coerceAtLeast(0L)
                                    //  应用灵敏度
                                    val seekDelta = (totalDragDistanceX * 200 * gestureSensitivity).toLong()
                                    seekTargetTime = (startPosition + seekDelta).coerceIn(0L, duration)
                                }
                                VideoGestureMode.Brightness -> {
                                    // 距离已在上方累积，使用负值因为上滑是负 Y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    //  应用灵敏度
                                    val deltaPercent = -totalDragDistanceY / screenHeight * gestureSensitivity
                                    val newBrightness = (startBrightness + deltaPercent).coerceIn(0f, 1f)
                                    
                                    //  优化：仅在变化超过阈值时更新（减少 WindowManager 调用）
                                    if (kotlin.math.abs(newBrightness - gesturePercent) > 0.02f) {
                                        getActivity()?.window?.attributes = getActivity()?.window?.attributes?.apply {
                                            screenBrightness = newBrightness
                                        }
                                        gesturePercent = newBrightness
                                    }
                                    //  亮度图标：CupertinoIcons SunMax (iOS SF Symbols 风格)
                                    gestureIcon = CupertinoIcons.Default.SunMax
                                }
                                VideoGestureMode.Volume -> {
                                    // 距离已在上方累积，使用负值因为上滑是负 Y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    //  应用灵敏度
                                    val deltaPercent = -totalDragDistanceY / screenHeight * gestureSensitivity
                                    val newVolPercent = ((startVolume.toFloat() / maxVolume) + deltaPercent).coerceIn(0f, 1f)
                                    val targetVol = (newVolPercent * maxVolume).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    gesturePercent = newVolPercent
                                    //  动态音量图标：3 级
                                    gestureIcon = when {
                                        gesturePercent < 0.01f -> CupertinoIcons.Default.SpeakerSlash
                                        gesturePercent < 0.5f -> CupertinoIcons.Default.Speaker
                                        else -> CupertinoIcons.Default.SpeakerWave2
                                    }
                                }
                                else -> {}
                            }
                            }
                        }
                    )
                }
            }
            //  点击/双击/长按手势在拖拽之后处理
            .pointerInput(seekForwardSeconds, seekBackwardSeconds, longPressSpeed, isScreenLocked) {
                detectTapGestures(
                    onTap = { 
                        // 🔒 锁定时点击只显示解锁按钮
                        if (isScreenLocked) {
                            showControls = !showControls  // 显示/隐藏解锁按钮
                        } else {
                            showControls = !showControls
                        }
                    },
                    onLongPress = {
                        // 🔒 锁定时禁用长按倍速
                        if (isScreenLocked) return@detectTapGestures
                        //  长按开始：保存原速度并应用长按倍速
                        val player = playerState.player
                        originalSpeed = player.playbackParameters.speed
                        player.setPlaybackSpeed(longPressSpeed)
                        isLongPressing = true
                        longPressSpeedFeedbackVisible = true
                        com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏩ LongPress: speed ${longPressSpeed}x")
                    },
                    onDoubleTap = { offset ->
                        // 🔒 锁定时禁用双击
                        if (isScreenLocked) return@detectTapGestures
                        
                        val screenWidth = size.width
                        val player = playerState.player
                        
                        //  [新增] 读取双击跳转开关
                        // 注意：这里 directly accessing the state value captured in the closure
                        // We need to ensure we have access to the latest value. 
                        // Since `doubleTapSeekEnabled` is a state, we can read it here.
                        
                        // 逻辑：如果开启跳转 -> 以前的逻辑 (两侧跳转，中间暂停)
                        //      如果关闭跳转 -> 全屏双击均为暂停/播放 (解决长屏按不到暂停的问题)
                        
                        if (doubleTapSeekEnabled) {
                            when {
                                // 右侧 1/3：快进
                                offset.x > screenWidth * 2 / 3 -> {
                                    val seekMs = seekForwardSeconds * 1000L
                                    val newPos = (player.currentPosition + seekMs).coerceAtMost(player.duration.coerceAtLeast(0L))
                                    player.seekTo(newPos)
                                    danmakuManager.seekTo(newPos)
                                    seekFeedbackText = "+${seekForwardSeconds}s"
                                    seekFeedbackVisible = true
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏩ DoubleTap right: +${seekForwardSeconds}s")
                                }
                                // 左侧 1/3：后退
                                offset.x < screenWidth / 3 -> {
                                    val seekMs = seekBackwardSeconds * 1000L
                                    val newPos = (player.currentPosition - seekMs).coerceAtLeast(0L)
                                    player.seekTo(newPos)
                                    danmakuManager.seekTo(newPos)
                                    seekFeedbackText = "-${seekBackwardSeconds}s"
                                    seekFeedbackVisible = true
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏪ DoubleTap left: -${seekBackwardSeconds}s")
                                }
                                // 中间：暂停/播放
                                else -> {
                                    player.playWhenReady = !player.playWhenReady
                                    com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏯️ DoubleTap center: toggle play/pause")
                                }
                            }
                        } else {
                            // 关闭跳转时，全屏双击暂停/播放
                            player.playWhenReady = !player.playWhenReady
                            com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏯️ DoubleTap (Seek Disabled): toggle play/pause")
                        }
                    },
                    onPress = { offset ->
                        //  等待手指抬起
                        tryAwaitRelease()
                        //  如果之前是长按状态，松开时恢复原速度
                        if (isLongPressing) {
                            playerState.player.setPlaybackSpeed(originalSpeed)
                            isLongPressing = false
                            longPressSpeedFeedbackVisible = false
                            com.android.purebilibili.core.util.Logger.d("VideoPlayerSection", "⏹️ LongPress released: speed ${originalSpeed}x")
                        }
                    }
                )
            }
    ) {
        val scope = rememberCoroutineScope()  //  用于设置弹幕开关
        
        //  弹幕开关设置
        val danmakuEnabled by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuEnabled(context)
            .collectAsState(initial = true)
        
        //  弹幕设置（全局持久化）
        val danmakuOpacity by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuOpacity(context)
            .collectAsState(initial = 0.85f)
        val danmakuFontScale by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuFontScale(context)
            .collectAsState(initial = 1.0f)
        val danmakuSpeed by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuSpeed(context)
            .collectAsState(initial = 1.0f)
        val danmakuDisplayArea by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuArea(context)
            .collectAsState(initial = 0.5f)
        val danmakuMergeDuplicates by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuMergeDuplicates(context)
            .collectAsState(initial = true)
        val danmakuAllowScroll by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuAllowScroll(context)
            .collectAsState(initial = true)
        val danmakuAllowTop by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuAllowTop(context)
            .collectAsState(initial = true)
        val danmakuAllowBottom by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuAllowBottom(context)
            .collectAsState(initial = true)
        val danmakuAllowColorful by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuAllowColorful(context)
            .collectAsState(initial = true)
        val danmakuAllowSpecial by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuAllowSpecial(context)
            .collectAsState(initial = true)
        val danmakuSmartOcclusion by com.android.purebilibili.core.store.SettingsManager
            .getDanmakuSmartOcclusion(context)
            .collectAsState(initial = false)
        val faceDetector = remember { createFaceOcclusionDetector() }
        DisposableEffect(faceDetector) {
            onDispose { faceDetector.close() }
        }

        LaunchedEffect(faceDetector) {
            smartOcclusionModuleState = FaceOcclusionModuleState.Checking
            smartOcclusionDownloadProgress = null
            smartOcclusionModuleState = checkFaceOcclusionModuleState(context, faceDetector)
        }
        val canSyncDanmakuCloud = (uiState as? PlayerUiState.Success)?.isLoggedIn == true
        var pendingDanmakuCloudSync by remember {
            mutableStateOf<com.android.purebilibili.data.repository.DanmakuCloudSyncSettings?>(null)
        }

        fun queueDanmakuCloudSync(
            enabled: Boolean = danmakuEnabled,
            allowScroll: Boolean = danmakuAllowScroll,
            allowTop: Boolean = danmakuAllowTop,
            allowBottom: Boolean = danmakuAllowBottom,
            allowColorful: Boolean = danmakuAllowColorful,
            allowSpecial: Boolean = danmakuAllowSpecial,
            opacity: Float = danmakuOpacity,
            displayAreaRatio: Float = danmakuDisplayArea,
            speed: Float = danmakuSpeed,
            fontScale: Float = danmakuFontScale
        ) {
            pendingDanmakuCloudSync = com.android.purebilibili.data.repository.DanmakuCloudSyncSettings(
                enabled = enabled,
                allowScroll = allowScroll,
                allowTop = allowTop,
                allowBottom = allowBottom,
                allowColorful = allowColorful,
                allowSpecial = allowSpecial,
                opacity = opacity,
                displayAreaRatio = displayAreaRatio,
                speed = speed,
                fontScale = fontScale
            )
        }
        
        //  当视频加载成功时加载弹幕（不再依赖 isFullscreen，单例会保持弹幕）
        val cid = (uiState as? PlayerUiState.Success)?.info?.cid ?: 0L
        val aid = (uiState as? PlayerUiState.Success)?.info?.aid ?: 0L
        //  监听 player 状态，等待 duration 可用后加载弹幕
        LaunchedEffect(cid) {
            if (cid > 0) {
                danmakuManager.isEnabled = danmakuEnabled
                
                //  [修复] 等待播放器准备好并获取 duration (最多等待 5 秒)
                var durationMs = 0L
                var retries = 0
                while (durationMs <= 0 && retries < 50) {
                    durationMs = playerState.player.duration.takeIf { it > 0 } ?: 0L
                    if (durationMs <= 0) {
                        kotlinx.coroutines.delay(100)
                        retries++
                    }
                }
                
                android.util.Log.d("VideoPlayerSection", "🎯 Loading danmaku for cid=$cid, aid=$aid, duration=${durationMs}ms (after $retries retries)")
                danmakuManager.loadDanmaku(cid, aid, durationMs)  //  传入时长启用 Protobuf API
            }
        }
        
        //  弹幕开关变化时更新
        LaunchedEffect(danmakuEnabled) {
            danmakuManager.isEnabled = danmakuEnabled
        }

        //  横竖屏/小窗切换后，若应当播放但未播放，主动恢复
        LaunchedEffect(isFullscreen, isInPipMode) {
            val player = playerState.player
            if (player.playWhenReady && !player.isPlaying && player.playbackState == Player.STATE_READY) {
                player.play()
            }
        }
        
        //  弹幕设置变化时实时应用
        LaunchedEffect(
            danmakuOpacity,
            danmakuFontScale,
            danmakuSpeed,
            danmakuDisplayArea,
            danmakuMergeDuplicates,
            danmakuAllowScroll,
            danmakuAllowTop,
            danmakuAllowBottom,
            danmakuAllowColorful,
            danmakuAllowSpecial,
            danmakuSmartOcclusion
        ) {
            danmakuManager.updateSettings(
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
                // Mask-only mode: keep lane layout fixed, do not move danmaku tracks.
                smartOcclusion = false
            )
        }

        LaunchedEffect(
            playerViewRef,
            faceDetector,
            danmakuEnabled,
            danmakuSmartOcclusion,
            smartOcclusionModuleState,
            isInPipMode,
            isPortraitFullscreen
        ) {
            if (
                !danmakuEnabled ||
                !danmakuSmartOcclusion ||
                smartOcclusionModuleState != FaceOcclusionModuleState.Ready ||
                isInPipMode ||
                isPortraitFullscreen
            ) {
                faceMaskStabilizer.reset()
                faceVisualMasks = emptyList()
                return@LaunchedEffect
            }
            faceMaskStabilizer.reset()

            while (isActive) {
                val view = playerViewRef
                val player = playerState.player
                if (view == null || view.width <= 0 || view.height <= 0 || !player.isPlaying) {
                    kotlinx.coroutines.delay(1200L)
                    continue
                }

                val videoWidth = player.videoSize.width
                val videoHeight = player.videoSize.height
                val sampleWidth = 480
                val sampleHeight = when {
                    videoWidth > 0 && videoHeight > 0 -> (sampleWidth * videoHeight / videoWidth).coerceIn(270, 960)
                    else -> 270
                }

                val detection = withTimeoutOrNull(1_500L) {
                    detectFaceOcclusionRegions(
                        playerView = view,
                        sampleWidth = sampleWidth,
                        sampleHeight = sampleHeight,
                        detector = faceDetector
                    )
                } ?: com.android.purebilibili.feature.video.danmaku.FaceOcclusionDetectionResult(
                    verticalRegions = emptyList(),
                    maskRects = emptyList(),
                    visualMasks = emptyList()
                )
                faceVisualMasks = faceMaskStabilizer.step(detection.visualMasks)
                kotlinx.coroutines.delay(if (detection.visualMasks.isEmpty()) 1300L else 900L)
            }
        }

        // 账号云同步：用户修改弹幕设置后防抖上云，避免滑杆拖动时高频请求
        LaunchedEffect(pendingDanmakuCloudSync, canSyncDanmakuCloud) {
            val settings = pendingDanmakuCloudSync ?: return@LaunchedEffect
            if (!canSyncDanmakuCloud) return@LaunchedEffect

            kotlinx.coroutines.delay(700)
            val result = com.android.purebilibili.data.repository.DanmakuRepository
                .syncDanmakuCloudConfig(settings)
            if (result.isFailure) {
                android.util.Log.w(
                    "VideoPlayerSection",
                    "Danmaku cloud sync failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
        
        //  绑定 Player（不在 onDispose 中释放，单例保持状态）
        DisposableEffect(playerState.player) {
            android.util.Log.d("VideoPlayerSection", " attachPlayer, isFullscreen=$isFullscreen")
            danmakuManager.attachPlayer(playerState.player)
            onDispose {
                // 单例模式不需要释放
            }
        }
        
        //  [修复] 使用 LifecycleOwner 监听真正的 Activity 生命周期
        // DisposableEffect(Unit) 会在横竖屏切换时触发，导致 player 引用被清除
        //  [关键修复] 添加 ON_RESUME 事件，确保从其他视频返回后重新绑定弹幕播放器
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, playerState.player) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                        //  [关键修复] 返回页面时重新绑定弹幕播放器
                        // 解决导航到其他视频后返回，弹幕暂停失效的问题
                        android.util.Log.d("VideoPlayerSection", " ON_RESUME: Re-attaching danmaku player")
                        danmakuManager.attachPlayer(playerState.player)
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                        android.util.Log.d("VideoPlayerSection", " ON_DESTROY: Clearing danmaku references")
                        danmakuManager.clearViewReference()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        
        // 1. PlayerView (底层) - key 触发 graphicsLayer 强制更新
        //  [修复] 添加 isPortraitFullscreen 到 key，确保从全屏返回时重建 PlayerView 并重新绑定 Surface (解决黑屏问题)
        key(isFlippedHorizontal, isFlippedVertical, isPortraitFullscreen) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        playerViewRef = this
                        player = if (isPortraitFullscreen) null else playerState.player
                        setKeepContentOnPlayerReset(true)
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)  // 禁用系统缓冲指示器，使用自定义iOS风格加载动画
                        useController = false
                        keepScreenOn = true
                        resizeMode = currentAspectRatio.resizeMode
                    }
                },
                update = { playerView ->
                    playerViewRef = playerView
                    playerView.player = if (isPortraitFullscreen) null else playerState.player
                    playerView.resizeMode = currentAspectRatio.resizeMode
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        //  [新增] 应用缩放和平移
                        scaleX = if (isFlippedHorizontal) -scale else scale
                        scaleY = if (isFlippedVertical) -scale else scale
                        translationX = panX
                        translationY = panY
                    }
            )
        }
        

        
    // --- [优化] 视频封面逻辑 ---
    // 使用 isFirstFrameRendered 确保只有在第一帧真正渲染后才隐藏封面，防止黑屏
    var isFirstFrameRendered by remember { mutableStateOf(false) }

    DisposableEffect(playerState.player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                android.util.Log.d("VideoPlayerCover", "🎬 onRenderedFirstFrame triggered")
                isFirstFrameRendered = true
            }
            
            // 兼容性：同时也监听 Events
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
                    android.util.Log.d("VideoPlayerCover", "🎬 EVENT_RENDERED_FIRST_FRAME triggered")
                    isFirstFrameRendered = true
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // 播放结束显示重播状态（通常由上层逻辑处理，这里不复位封面以免闪烁）
                    // isFirstFrameRendered = false 
                }
            }
        }
        
        playerState.player.addListener(listener)
        
        // 初始化检查：如果播放器已经开始播放且有进度，可能错过了事件
        // [Debug] Log initial check
        if (playerState.player.isPlaying && playerState.player.currentPosition > 0) {
             android.util.Log.d("VideoPlayerCover", "⚠️ Initial check: Already playing at ${playerState.player.currentPosition}, hiding cover. (Might be previous video?)")
             isFirstFrameRendered = true
        } else {
             android.util.Log.d("VideoPlayerCover", "✅ Initial check: Not playing or at start. Keeping cover.")
        }

        onDispose {
            playerState.player.removeListener(listener)
        }
    }
    
    // 如果 bvid 改变，重置状态
    LaunchedEffect(bvid) {
        isFirstFrameRendered = false
    }

    // 4. 封面图 (Cover Image) - 始终在第一帧渲染前显示
    // 优先使用 PlayerUiState.Success 中的高清封面 (pic)，否则使用传入的 coverUrl
    var rawCoverUrl = if (uiState is PlayerUiState.Success) uiState.info.pic else coverUrl
    
    // [Fix] 使用 FormatUtils 统一处理 URL (支持无协议头 URL)
    val currentCoverUrl = FormatUtils.fixImageUrl(rawCoverUrl)
    
    // [修改] 只要第一帧未渲染，就显示封面
    // 增加额外检查：如果 buffering 且位置 > 1000ms，说明是中途缓冲，不需要显示封面(保持最后一帧)
    val isInitialBuffering = isBuffering && playerState.player.currentPosition < 1000
    val showCover = !isFirstFrameRendered || (isInitialBuffering && !isFirstFrameRendered)
    
    // [Debug] Logging
    LaunchedEffect(showCover, currentCoverUrl, isFirstFrameRendered, uiState) {
        android.util.Log.d("VideoPlayerCover", "🔍 Check: bvid=$bvid, showCover=$showCover, isFirstFrame=$isFirstFrameRendered, coverUrl=$coverUrl, finalUrl=$currentCoverUrl")
    }

    AnimatedVisibility(
        visible = showCover && currentCoverUrl.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(300)), // 稍微慢一点消失，平滑过渡
        modifier = Modifier.zIndex(10f) // [Fix] 强制提升层级，确保在 VideoSurface 之上
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(currentCoverUrl)
                // [关键] 尝试使用首页卡片的缓存 Key 作为占位，实现无缝过渡
                // 假设首页卡片使用的是普通模式 ("n")
                .placeholderMemoryCacheKey("cover_${bvid}_n")
                .listener(
                    onStart = { android.util.Log.d("VideoPlayerCover", "🖼️ Image loading started: $currentCoverUrl") },
                    onSuccess = { _, _ -> android.util.Log.d("VideoPlayerCover", "🖼️ Image loaded successfully") },
                    onError = { _, result -> android.util.Log.e("VideoPlayerCover", "❌ Image load failed: ${result.throwable.message}", result.throwable) }
                )
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop, // [修改] 使用 Crop 填满屏幕
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    }

    // 2. DanmakuView (使用 ByteDance DanmakuRenderEngine - 覆盖在 PlayerView 上方)
    android.util.Log.d("VideoPlayerSection", "🔍 DanmakuView check: isInPipMode=$isInPipMode, danmakuEnabled=$danmakuEnabled")
        if (!isInPipMode && danmakuEnabled && !isPortraitFullscreen) {
            android.util.Log.d("VideoPlayerSection", " Conditions met, creating DanmakuView...")
            //  计算状态栏高度
            val statusBarHeightPx = remember(context) {
                val resourceId = context.resources.getIdentifier(
                    "status_bar_height", "dimen", "android"
                )
                if (resourceId > 0) {
                    context.resources.getDimensionPixelSize(resourceId)
                } else {
                    (24 * context.resources.displayMetrics.density).toInt()
                }
            }
            
            //  非全屏时的顶部偏移量
            val topOffset = if (isFullscreen) 0 else statusBarHeightPx + 20
            
            //  [修复] 移除 key(isFullscreen)，避免横竖屏切换时重建 DanmakuView 导致弹幕消失
            // 使用 remember 保存 DanmakuView 引用，在 update 回调中处理尺寸变化
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isFullscreen) {
                            Modifier.padding(top = with(LocalContext.current.resources.displayMetrics) {
                                (topOffset / density).dp
                            })
                        } else Modifier
                    )
                    .clipToBounds()
            ) {
                AndroidView(
                    factory = { ctx ->
                        FaceOcclusionDanmakuContainer(ctx).apply {
                            setMasks(faceVisualMasks)
                            setVideoViewport(
                                videoWidth = playerState.player.videoSize.width,
                                videoHeight = playerState.player.videoSize.height,
                                resizeMode = currentAspectRatio.resizeMode
                            )
                            danmakuManager.attachView(danmakuView())
                            android.util.Log.d("VideoPlayerSection", " DanmakuView (RenderEngine) created, isFullscreen=$isFullscreen")
                        }
                    },
                    update = { container ->
                        container.setMasks(faceVisualMasks)
                        container.setVideoViewport(
                            videoWidth = playerState.player.videoSize.width,
                            videoHeight = playerState.player.videoSize.height,
                            resizeMode = currentAspectRatio.resizeMode
                        )
                        val view = container.danmakuView()
                        //  [关键] 横竖屏切换后视图尺寸变化时，重新 attachView 确保弹幕正确显示
                        android.util.Log.d("VideoPlayerSection", " DanmakuView update: size=${view.width}x${view.height}, isFullscreen=$isFullscreen")
                        // 只有当视图有有效尺寸时才 re-attach
                        if (view.width > 0 && view.height > 0) {
                            val sizeTag = "${view.width}x${view.height}"
                            if (view.tag != sizeTag) {
                                view.tag = sizeTag
                                danmakuManager.attachView(view)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // 3. 高级弹幕层 (Mode 7) - 覆盖在标准弹幕上方
        val advancedDanmakuList by danmakuManager.advancedDanmakuFlow.collectAsState()
        
        if (!isInPipMode && danmakuEnabled && advancedDanmakuList.isNotEmpty()) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                com.android.purebilibili.feature.video.ui.overlay.AdvancedDanmakuOverlay(
                    danmakuList = advancedDanmakuList,
                    player = playerState.player,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 🖼️ [修复] 手势指示器：仅在亮度/音量/Seek 模式显示，避免上滑全屏时误显示亮度图标
        val shouldShowGestureIndicator = isGestureVisible &&
            !isInPipMode &&
            (gestureMode == VideoGestureMode.Seek ||
                gestureMode == VideoGestureMode.Brightness ||
                gestureMode == VideoGestureMode.Volume)

        if (shouldShowGestureIndicator) {
            if (gestureMode == VideoGestureMode.Seek) {
                // 🖼️ Seek 模式：显示带缩略图的预览气泡
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoshotData != null && videoshotData.isValid) {
                        // 🖼️ 有缩略图：显示完整预览
                        com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble(
                            videoshotData = videoshotData,
                            targetPositionMs = seekTargetTime,
                            currentPositionMs = startPosition,
                            durationMs = playerState.player.duration,
                            offsetX = 80f,  // 居中偏移（气泡宽度的一半）
                            containerWidth = 160f  // 与气泡宽度匹配
                        )
                    } else {
                        // 无缩略图：使用原有样式
                        Box(
                            modifier = Modifier
                                .size(uiLayoutPolicy.gestureOverlaySizeDp.dp)
                                .background(Color.Black.copy(0.7f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val durationSeconds = (playerState.player.duration / 1000).coerceAtLeast(1)
                                val targetSeconds = (seekTargetTime / 1000).toInt()

                                Text(
                                    text = "${FormatUtils.formatDuration(targetSeconds)} / ${FormatUtils.formatDuration(durationSeconds.toInt())}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                val deltaSeconds = (seekTargetTime - startPosition) / 1000
                                val sign = if (deltaSeconds > 0) "+" else ""
                                if (deltaSeconds != 0L) {
                                    Text(
                                        text = "($sign${deltaSeconds}s)",
                                        color = if (deltaSeconds > 0) com.android.purebilibili.core.theme.iOSGreen else com.android.purebilibili.core.theme.iOSRed,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 亮度/音量模式：保持原有样式
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(uiLayoutPolicy.gestureOverlaySizeDp.dp)
                        .background(Color.Black.copy(0.7f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = gestureIcon ?: CupertinoIcons.Default.SunMax,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(uiLayoutPolicy.gestureIconSizeDp.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(gesturePercent * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }
        }
        
        //  [新增] 双击跳转视觉反馈 (±Ns 提示)
        LaunchedEffect(seekFeedbackVisible) {
            if (seekFeedbackVisible) {
                kotlinx.coroutines.delay(800)
                seekFeedbackVisible = false
            }
        }
        
        AnimatedVisibility(
            visible = seekFeedbackVisible && !isInPipMode,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(uiLayoutPolicy.seekFeedbackSizeDp.dp)
                    .background(Color.Black.copy(0.75f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seekFeedbackText ?: "",
                    color = if (seekFeedbackText?.startsWith("+") == true) com.android.purebilibili.core.theme.iOSGreen else com.android.purebilibili.core.theme.iOSRed,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        //  [新增] 缩放还原按钮 (仅在放大时显示)
        AnimatedVisibility(
            visible = scale > 1.05f && !isInPipMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = uiLayoutPolicy.restoreButtonBottomOffsetDp.dp), // 避开底部进度条位置
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Button(
                onClick = {
                    scale = 1f
                    panX = 0f
                    panY = 0f
                    // showControls = true // 可选：还原后显示控制栏
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(
                    horizontal = uiLayoutPolicy.restoreButtonHorizontalPaddingDp.dp,
                    vertical = uiLayoutPolicy.restoreButtonVerticalPaddingDp.dp
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "还原画面",
                    modifier = Modifier.size(uiLayoutPolicy.restoreButtonIconSizeDp.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "还原画面",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        //  长按倍速提示（简洁版，1秒后消失）
        LaunchedEffect(longPressSpeedFeedbackVisible) {
            if (longPressSpeedFeedbackVisible) {
                kotlinx.coroutines.delay(1000)
                longPressSpeedFeedbackVisible = false
            }
        }
        
        AnimatedVisibility(
            visible = longPressSpeedFeedbackVisible && !isInPipMode,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(0.75f), RoundedCornerShape(16.dp))
                    .padding(
                        horizontal = uiLayoutPolicy.longPressBadgeHorizontalPaddingDp.dp,
                        vertical = uiLayoutPolicy.longPressBadgeVerticalPaddingDp.dp
                    )
            ) {
                Text(
                    text = "${longPressSpeed}x",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (uiState is PlayerUiState.Success && !isInPipMode) {
            VideoPlayerOverlay(
                player = playerState.player,
                title = uiState.info.title,
                // [修复] 竖屏全屏模式下隐藏底部 Overlay，避免进度状态冲突
                isVisible = showControls && !isPortraitFullscreen,
                onToggleVisible = { showControls = !showControls },
                isFullscreen = isFullscreen,
                currentQualityLabel = uiState.qualityLabels.getOrNull(uiState.qualityIds.indexOf(uiState.currentQuality)) ?: "自动",
                qualityLabels = uiState.qualityLabels,
                qualityIds = uiState.qualityIds,
                onQualitySelected = { index ->
                    val id = uiState.qualityIds.getOrNull(index) ?: 0
                    onQualityChange(id, playerState.player.currentPosition)
                },
                onBack = onBack,
                onToggleFullscreen = onToggleFullscreen,
                
                // 🔒 [新增] 屏幕锁定
                isScreenLocked = isScreenLocked,
                onLockToggle = { isScreenLocked = !isScreenLocked },
                //  [关键] 传入设置状态和真实分辨率字符串
                showStats = showStats,
                realResolution = realResolution,
                //  [新增] 传入清晰度切换状态和会员状态
                isQualitySwitching = uiState.isQualitySwitching,
                isBuffering = isBuffering,  // 缓冲状态
                isLoggedIn = uiState.isLoggedIn,
                isVip = uiState.isVip,
                //  [新增] 弹幕开关和设置
                danmakuEnabled = danmakuEnabled,
                onDanmakuToggle = {
                    val newState = !danmakuEnabled
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuEnabled(context, newState)
                    }
                    queueDanmakuCloudSync(enabled = newState)
                    //  记录弹幕开关事件
                    com.android.purebilibili.core.util.AnalyticsHelper.logDanmakuToggle(newState)
                },
                danmakuOpacity = danmakuOpacity,
                danmakuFontScale = danmakuFontScale,
                danmakuSpeed = danmakuSpeed,
                danmakuDisplayArea = danmakuDisplayArea,
                danmakuMergeDuplicates = danmakuMergeDuplicates,
                danmakuAllowScroll = danmakuAllowScroll,
                danmakuAllowTop = danmakuAllowTop,
                danmakuAllowBottom = danmakuAllowBottom,
                danmakuAllowColorful = danmakuAllowColorful,
                danmakuAllowSpecial = danmakuAllowSpecial,
                danmakuSmartOcclusion = danmakuSmartOcclusion,
                onDanmakuOpacityChange = { value ->
                    danmakuManager.opacity = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuOpacity(context, value)
                    }
                    queueDanmakuCloudSync(opacity = value)
                },
                onDanmakuFontScaleChange = { value ->
                    danmakuManager.fontScale = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuFontScale(context, value)
                    }
                    queueDanmakuCloudSync(fontScale = value)
                },
                onDanmakuSpeedChange = { value ->
                    danmakuManager.speedFactor = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuSpeed(context, value)
                    }
                    queueDanmakuCloudSync(speed = value)
                },
                onDanmakuDisplayAreaChange = { value ->
                    danmakuManager.displayArea = value
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuArea(context, value)
                    }
                    queueDanmakuCloudSync(displayAreaRatio = value)
                },
                onDanmakuMergeDuplicatesChange = { value ->
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuMergeDuplicates(context, value)
                    }
                },
                onDanmakuAllowScrollChange = { value ->
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuAllowScroll(context, value)
                    }
                    queueDanmakuCloudSync(allowScroll = value)
                },
                onDanmakuAllowTopChange = { value ->
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuAllowTop(context, value)
                    }
                    queueDanmakuCloudSync(allowTop = value)
                },
                onDanmakuAllowBottomChange = { value ->
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuAllowBottom(context, value)
                    }
                    queueDanmakuCloudSync(allowBottom = value)
                },
                onDanmakuAllowColorfulChange = { value ->
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuAllowColorful(context, value)
                    }
                    queueDanmakuCloudSync(allowColorful = value)
                },
                onDanmakuAllowSpecialChange = { value ->
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuAllowSpecial(context, value)
                    }
                    queueDanmakuCloudSync(allowSpecial = value)
                },
                onDanmakuSmartOcclusionChange = { value ->
                    scope.launch {
                        com.android.purebilibili.core.store.SettingsManager.setDanmakuSmartOcclusion(context, value)
                    }
                },
                smartOcclusionModuleState = smartOcclusionModuleState,
                smartOcclusionDownloadProgress = smartOcclusionDownloadProgress,
                onDanmakuSmartOcclusionDownloadClick = {
                    if (smartOcclusionModuleState != FaceOcclusionModuleState.Downloading) {
                        scope.launch {
                            smartOcclusionModuleState = FaceOcclusionModuleState.Downloading
                            smartOcclusionDownloadProgress = 0
                            smartOcclusionModuleState = installFaceOcclusionModule(
                                context = context,
                                detector = faceDetector,
                                onProgress = { progress ->
                                    smartOcclusionDownloadProgress = progress
                                }
                            )
                            if (smartOcclusionModuleState != FaceOcclusionModuleState.Downloading) {
                                smartOcclusionDownloadProgress = null
                            }
                        }
                    }
                },
                //  视频比例调节

                currentAspectRatio = currentAspectRatio,
                onAspectRatioChange = { currentAspectRatio = it },
                // 🕺 [新增] 分享功能
                bvid = bvid,
                cid = uiState.info.cid,
                videoOwnerName = uiState.info.owner.name,
                videoOwnerFace = uiState.info.owner.face,
                videoDuration = playerState.player.duration.toInt().coerceAtLeast(0),
                videoTitle = uiState.info.title,
                currentVideoUrl = uiState.playUrl,
                currentAudioUrl = uiState.audioUrl ?: "",
                coverUrl = uiState.info.pic,
                //  [新增] 视频设置面板回调
                onReloadVideo = onReloadVideo,
                isFlippedHorizontal = isFlippedHorizontal,
                isFlippedVertical = isFlippedVertical,
                onFlipHorizontal = { isFlippedHorizontal = !isFlippedHorizontal },
                onFlipVertical = { isFlippedVertical = !isFlippedVertical },
                //  [新增] 画质切换（用于设置面板）
                onQualityChange = { qid, pos ->
                    onQualityChange(qid, playerState.player.currentPosition)
                },
                //  [新增] CDN 线路切换
                currentCdnIndex = currentCdnIndex,
                cdnCount = cdnCount,
                onSwitchCdn = onSwitchCdn,
                onSwitchCdnTo = onSwitchCdnTo,
                
                //  [新增] 音频模式
                isAudioOnly = isAudioOnly,
                onAudioOnlyToggle = onAudioOnlyToggle,
                
                //  [新增] 定时关闭
                sleepTimerMinutes = sleepTimerMinutes,
                onSleepTimerChange = onSleepTimerChange,
                
                // 🖼️ [新增] 视频预览图数据
                videoshotData = videoshotData,
                
                // 📖 [新增] 视频章节数据
                viewPoints = viewPoints,
                
                // 📱 [新增] 竖屏全屏模式
                isVerticalVideo = isVerticalVideo,
                onPortraitFullscreen = onPortraitFullscreen,
                // 📲 [新增] 小窗模式
                // 📲 [新增] 小窗模式
                onPipClick = onPipClick,
                //  [新增] 拖动进度条开始时清除弹幕
                onSeekStart = { danmakuManager.clear() },
                //  [加固] 显式同步弹幕到新进度，避免某些设备 seek 回调时机差导致短暂不同步
                onSeekTo = { position ->
                    playerState.player.seekTo(position)
                    danmakuManager.seekTo(position)
                },
                // [New] Codec & Audio
                currentCodec = currentCodec,
                onCodecChange = onCodecChange,
                currentAudioQuality = currentAudioQuality,
                onAudioQualityChange = onAudioQualityChange,
                // [New] AI Audio
                aiAudioInfo = uiState.aiAudio,
                currentAudioLang = uiState.currentAudioLang,
                onAudioLangChange = onAudioLangChange,
                // 👀 [新增] 在线观看人数
                onlineCount = uiState.onlineCount,
                // [New]
                onSaveCover = onSaveCover,
                onCaptureScreenshot = {
                    val playerView = playerViewRef
                    if (playerView == null) {
                        Toast.makeText(context, "截图失败：播放器未就绪", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            val success = captureAndSaveVideoScreenshot(
                                context = context,
                                playerView = playerView,
                                videoWidth = videoSizeState.first,
                                videoHeight = videoSizeState.second,
                                videoTitle = uiState.info.title,
                            )
                            Toast.makeText(
                                context,
                                if (success) "截图已保存到相册（PNG）" else "截图失败，请稍后重试",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onDownloadAudio = onDownloadAudio,
                // 🔁 [新增] 播放模式
                currentPlayMode = currentPlayMode,
                onPlayModeClick = onPlayModeClick,
                
                // [新增] 侧边栏抽屉数据与交互
                relatedVideos = relatedVideos,
                ugcSeason = ugcSeason,
                isFollowed = isFollowed,
                isLiked = isLiked,
                isCoined = isCoined,
                isFavorited = isFavorited,
                onToggleFollow = onToggleFollow,
                onToggleLike = onToggleLike,
                onCoin = onCoin,
                onToggleFavorite = onToggleFavorite,
                onTriple = onTriple,
                onDrawerVideoClick = { vid ->
                    onRelatedVideoClick(vid, null) 
                }
            )
    }



    // [新增] 返回时的触感反馈
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticScope = rememberCoroutineScope()

    // 拦截系统返回事件 (仅在全屏时拦截以处理退出全屏，否则交给系统处理预测性返回)
    BackHandler(enabled = !isScreenLocked && isFullscreen) {
        onToggleFullscreen()
    }
    }
}
