package com.android.purebilibili.feature.download

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.android.purebilibili.core.util.FormatUtils
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.abs

/**
 * 手势模式枚举
 */
private enum class GestureMode { None, Brightness, Volume, Seek }

/**
 * 🔧 [重构] 离线视频播放器
 * 支持完整手势功能：亮度、音量、进度调节、双击快进/后退、长按倍速
 */
@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun OfflineVideoPlayerScreen(
    taskId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    val tasks by DownloadManager.tasks.collectAsState()
    val task = tasks[taskId]
    
    // === 状态管理 ===
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    
    // 手势状态
    var gestureMode by remember { mutableStateOf(GestureMode.None) }
    var gestureIcon by remember { mutableStateOf<ImageVector?>(null) }
    var gesturePercent by remember { mutableFloatStateOf(0f) }
    var isGestureVisible by remember { mutableStateOf(false) }
    
    // 进度拖动状态
    var seekTargetTime by remember { mutableLongStateOf(0L) }
    var startPosition by remember { mutableLongStateOf(0L) }
    var totalDragDistanceX by remember { mutableFloatStateOf(0f) }
    var totalDragDistanceY by remember { mutableFloatStateOf(0f) }
    var startVolume by remember { mutableIntStateOf(0) }
    var startBrightness by remember { mutableFloatStateOf(0.5f) }
    
    // 双击跳转反馈
    var seekFeedbackText by remember { mutableStateOf<String?>(null) }
    var seekFeedbackVisible by remember { mutableStateOf(false) }
    
    // 长按倍速状态
    var isLongPressing by remember { mutableStateOf(false) }
    var originalSpeed by remember { mutableFloatStateOf(1.0f) }
    var longPressSpeedVisible by remember { mutableStateOf(false) }
    val longPressSpeed = 2.0f
    
    // 双击跳转秒数
    val seekForwardSeconds = 10
    val seekBackwardSeconds = 10
    
    if (task == null || task.filePath == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("视频文件不存在", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("返回") }
            }
        }
        return
    }
    
    val file = File(task.filePath!!)
    if (!file.exists()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("视频文件已被删除", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("返回") }
            }
        }
        return
    }
    
    // 创建播放器
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    // 进度状态
    val progressState by produceState(
        initialValue = ProgressInfo(0L, 0L, 0L),
        key1 = player,
        key2 = showControls
    ) {
        while (isActive) {
            val duration = if (player.duration < 0) 0L else player.duration
            value = ProgressInfo(
                current = player.currentPosition,
                duration = duration,
                buffered = player.bufferedPosition
            )
            isPlaying = player.isPlaying
            delay(if (showControls) 200L else 500L)
        }
    }
    
    // 自动隐藏控制栏
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }
    
    // 双击反馈自动消失
    LaunchedEffect(seekFeedbackVisible) {
        if (seekFeedbackVisible) {
            delay(800)
            seekFeedbackVisible = false
        }
    }
    
    // 长按倍速提示自动消失
    LaunchedEffect(longPressSpeedVisible) {
        if (longPressSpeedVisible) {
            delay(1000)
            longPressSpeedVisible = false
        }
    }
    
    // 获取 Activity
    fun getActivity(): Activity? = activity
    
    // 全屏切换函数
    fun toggleFullscreen() {
        val act = getActivity() ?: return
        isFullscreen = !isFullscreen
        
        if (isFullscreen) {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    
    // 返回键处理
    BackHandler(enabled = isFullscreen) { toggleFullscreen() }
    
    DisposableEffect(Unit) {
        onDispose {
            player.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.let { act ->
                val windowInsetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 🎛️ 拖拽手势：亮度/音量/进度
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // 边缘防误触
                        val density = context.resources.displayMetrics.density
                        val safeZonePx = 48 * density
                        val screenHeight = size.height
                        val isEdgeGesture = offset.y < safeZonePx || offset.y > (screenHeight - safeZonePx)
                        
                        if (isEdgeGesture) {
                            isGestureVisible = false
                            gestureMode = GestureMode.None
                        } else {
                            isGestureVisible = true
                            gestureMode = GestureMode.None
                            totalDragDistanceY = 0f
                            totalDragDistanceX = 0f
                            
                            startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            startPosition = player.currentPosition
                            
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
                        if (gestureMode == GestureMode.Seek) {
                            player.seekTo(seekTargetTime)
                            player.play()
                        }
                        isGestureVisible = false
                        gestureMode = GestureMode.None
                    },
                    onDragCancel = {
                        isGestureVisible = false
                        gestureMode = GestureMode.None
                    },
                    onDrag = { change, dragAmount ->
                        if (!isGestureVisible && gestureMode == GestureMode.None) {
                            // 在 safe zone 中启动被忽略
                        } else {
                            // 确定手势类型
                            if (gestureMode == GestureMode.None) {
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    gestureMode = GestureMode.Seek
                                } else {
                                    val screenWidth = context.resources.displayMetrics.widthPixels
                                    gestureMode = if (change.position.x < screenWidth / 2) {
                                        GestureMode.Brightness
                                    } else {
                                        GestureMode.Volume
                                    }
                                }
                            }
                            
                            when (gestureMode) {
                                GestureMode.Seek -> {
                                    totalDragDistanceX += dragAmount.x
                                    val duration = player.duration.coerceAtLeast(0L)
                                    val seekDelta = (totalDragDistanceX * 200).toLong()
                                    seekTargetTime = (startPosition + seekDelta).coerceIn(0L, duration)
                                }
                                GestureMode.Brightness -> {
                                    totalDragDistanceY -= dragAmount.y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    val deltaPercent = totalDragDistanceY / screenHeight
                                    val newBrightness = (startBrightness + deltaPercent).coerceIn(0f, 1f)
                                    
                                    if (abs(newBrightness - gesturePercent) > 0.02f) {
                                        getActivity()?.window?.attributes = getActivity()?.window?.attributes?.apply {
                                            screenBrightness = newBrightness
                                        }
                                        gesturePercent = newBrightness
                                    }
                                    gestureIcon = CupertinoIcons.Default.SunMax
                                }
                                GestureMode.Volume -> {
                                    totalDragDistanceY -= dragAmount.y
                                    val screenHeight = context.resources.displayMetrics.heightPixels
                                    val deltaPercent = totalDragDistanceY / screenHeight
                                    val newVolPercent = ((startVolume.toFloat() / maxVolume) + deltaPercent).coerceIn(0f, 1f)
                                    val targetVol = (newVolPercent * maxVolume).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    gesturePercent = newVolPercent
                                    
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
            // 🖱️ 点击/双击/长按手势
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onLongPress = {
                        // 长按倍速
                        originalSpeed = player.playbackParameters.speed
                        player.setPlaybackSpeed(longPressSpeed)
                        isLongPressing = true
                        longPressSpeedVisible = true
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        when {
                            // 右侧 1/3：快进
                            offset.x > screenWidth * 2 / 3 -> {
                                val seekMs = seekForwardSeconds * 1000L
                                val newPos = (player.currentPosition + seekMs).coerceAtMost(player.duration.coerceAtLeast(0L))
                                player.seekTo(newPos)
                                seekFeedbackText = "+${seekForwardSeconds}s"
                                seekFeedbackVisible = true
                            }
                            // 左侧 1/3：后退
                            offset.x < screenWidth / 3 -> {
                                val seekMs = seekBackwardSeconds * 1000L
                                val newPos = (player.currentPosition - seekMs).coerceAtLeast(0L)
                                player.seekTo(newPos)
                                seekFeedbackText = "-${seekBackwardSeconds}s"
                                seekFeedbackVisible = true
                            }
                            // 中间：暂停/播放
                            else -> {
                                player.playWhenReady = !player.playWhenReady
                            }
                        }
                    },
                    onPress = {
                        tryAwaitRelease()
                        // 松开时恢复原速度
                        if (isLongPressing) {
                            player.setPlaybackSpeed(originalSpeed)
                            isLongPressing = false
                            longPressSpeedVisible = false
                        }
                    }
                )
            }
    ) {
        // 1. PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 2. 封面图（播放前显示，或是纯音频模式常驻显示）
        val showCover = (!player.isPlaying && player.playbackState == Player.STATE_IDLE) || task.isAudioOnly
        AnimatedVisibility(visible = showCover, enter = fadeIn(), exit = fadeOut()) {
            val localCoverFile = task.localCoverPath?.let { File(it) }
            AsyncImage(
                model = if (localCoverFile?.exists() == true) localCoverFile else task.cover,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().background(Color.Black)
            )
        }
        
        // 3. 手势指示器（亮度/音量/进度）
        AnimatedVisibility(
            visible = isGestureVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (gestureMode == GestureMode.Seek) {
                        val durationSeconds = (player.duration / 1000).coerceAtLeast(1)
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
                                color = if (deltaSeconds > 0) Color.Green else Color.Red,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Icon(
                            imageVector = gestureIcon ?: CupertinoIcons.Default.SunMax,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
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
        
        // 4. 双击跳转反馈
        AnimatedVisibility(
            visible = seekFeedbackVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Black.copy(0.75f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seekFeedbackText ?: "",
                    color = if (seekFeedbackText?.startsWith("+") == true) Color.Green else Color.Red,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
        
        // 5. 长按倍速提示
        AnimatedVisibility(
            visible = longPressSpeedVisible,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        CupertinoIcons.Default.Forward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${longPressSpeed}x 倍速播放中",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 6. 顶部渐变遮罩
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )
        }
        
        // 7. 底部渐变遮罩
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
        }
        
        // 8. 顶部控制栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (isFullscreen) toggleFullscreen() else onBack() }) {
                    Icon(
                        CupertinoIcons.Default.ChevronBackward,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = task.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 9. 底部控制栏
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // 进度条
                OfflineProgressBar(
                    currentPosition = progressState.current,
                    duration = progressState.duration,
                    bufferedPosition = progressState.buffered,
                    onSeek = { player.seekTo(it) }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 控制按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 播放/暂停按钮
                    IconButton(
                        onClick = {
                            if (player.playbackState == Player.STATE_ENDED) {
                                player.seekTo(0)
                                player.play()
                            } else if (player.isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isPlaying) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // 时间显示
                    Text(
                        text = "${FormatUtils.formatDuration((progressState.current / 1000).toInt())} / ${FormatUtils.formatDuration((progressState.duration / 1000).toInt())}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 📺 全屏按钮
                    Surface(
                        onClick = { toggleFullscreen() },
                        color = if (!isFullscreen) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                                contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // 10. 中央播放按钮（暂停时显示）
        AnimatedVisibility(
            visible = showControls && !isPlaying,
            modifier = Modifier.align(Alignment.Center),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Surface(
                onClick = { player.play() },
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        CupertinoIcons.Default.Play,
                        contentDescription = "播放",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }
    }
}

/**
 * 进度信息
 */
private data class ProgressInfo(
    val current: Long,
    val duration: Long,
    val buffered: Long
)

/**
 * 简化版进度条
 */
@Composable
private fun OfflineProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f
    var tempProgress by remember { mutableFloatStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    
    LaunchedEffect(progress) {
        if (!isDragging) {
            tempProgress = progress
        }
    }
    
    val displayProgress = if (isDragging) tempProgress else progress
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((newProgress * duration).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 背景轨道
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.5.dp))
        )
        
        // 缓冲进度
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp))
        )
        
        // 当前进度
        Box(
            modifier = Modifier
                .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(primaryColor, RoundedCornerShape(1.5.dp))
        )
        
        // 滑块（圆点）
        Box(modifier = Modifier.fillMaxWidth(displayProgress.coerceIn(0f, 1f))) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(12.dp)
                    .offset(x = 6.dp)
                    .background(primaryColor, CircleShape)
            )
        }
    }
}
