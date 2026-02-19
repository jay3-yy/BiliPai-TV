// File: feature/video/ui/gesture/PlayerGestureHandler.kt
package com.android.purebilibili.feature.video.ui.gesture

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.FormatUtils
import kotlin.math.abs

/**
 * Player Gesture Handler
 * 
 * Unified gesture handling for video player:
 * - Brightness adjustment (left side drag)
 * - Volume adjustment (right side drag)
 * - Seek (middle drag)
 * - Tap to show/hide controls
 * - Double tap to play/pause
 * 
 * Requirement Reference: AC6.1 - Unified PlayerGestureHandler
 */

/**
 * Gesture mode enum
 */
enum class GestureMode {
    None,
    Brightness,
    Volume,
    Seek
}

/**
 * Player gesture state
 */
@Stable
class PlayerGestureState(
    private val context: Context,
    private val audioManager: AudioManager,
    private val maxVolume: Int
) {
    var gestureMode by mutableStateOf(GestureMode.None)
    var gestureValue by mutableFloatStateOf(0f)
    var dragDelta by mutableFloatStateOf(0f)
    var seekPreviewPosition by mutableLongStateOf(0L)
    var currentBrightness by mutableFloatStateOf(
        try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (e: Exception) { 0.5f }
    )
    
    /**
     * Start drag gesture
     */
    fun onDragStart(
        offsetX: Float,
        screenWidth: Float,
        currentPosition: Long
    ) {
        dragDelta = 0f
        
        gestureMode = when {
            offsetX < screenWidth * 0.3f -> {
                gestureValue = currentBrightness
                GestureMode.Brightness
            }
            offsetX > screenWidth * 0.7f -> {
                gestureValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                GestureMode.Volume
            }
            else -> {
                seekPreviewPosition = currentPosition
                GestureMode.Seek
            }
        }
    }
    
    /**
     * Handle drag
     */
    fun onDrag(
        dragAmountX: Float,
        dragAmountY: Float,
        screenWidth: Float,
        screenHeight: Float,
        currentPosition: Long,
        duration: Long
    ): Float {
        when (gestureMode) {
            GestureMode.Brightness -> {
                gestureValue = (gestureValue - dragAmountY / screenHeight).coerceIn(0f, 1f)
                currentBrightness = gestureValue
                (context as? Activity)?.window?.let { window ->
                    val params = window.attributes
                    params.screenBrightness = gestureValue
                    window.attributes = params
                }
                return gestureValue
            }
            GestureMode.Volume -> {
                gestureValue = (gestureValue - dragAmountY / screenHeight).coerceIn(0f, 1f)
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    (gestureValue * maxVolume).toInt(),
                    0
                )
                return gestureValue
            }
            GestureMode.Seek -> {
                dragDelta += dragAmountX
                val seekDelta = (dragDelta / screenWidth * duration).toLong()
                seekPreviewPosition = (currentPosition + seekDelta).coerceIn(0L, duration)
                return (seekPreviewPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
            else -> return 0f
        }
    }
    
    /**
     * End drag gesture
     */
    fun onDragEnd(onSeek: (Long) -> Unit) {
        if (gestureMode == GestureMode.Seek && abs(dragDelta) > 20f) {
            onSeek(seekPreviewPosition)
        }
        gestureMode = GestureMode.None
    }
    
    /**
     * Cancel drag gesture
     */
    fun onDragCancel() {
        gestureMode = GestureMode.None
    }
    
    /**
     * Get display value for gesture indicator
     */
    fun getDisplayValue(): Float {
        return when (gestureMode) {
            GestureMode.Brightness -> currentBrightness
            GestureMode.Volume -> gestureValue
            GestureMode.Seek -> (seekPreviewPosition.toFloat() / 1000f) // Will be formatted
            else -> 0f
        }
    }
}

/**
 * Remember player gesture state
 */
@Composable
fun rememberPlayerGestureState(): PlayerGestureState {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    return remember(context, audioManager, maxVolume) {
        PlayerGestureState(context, audioManager, maxVolume)
    }
}

/**
 * Gesture Indicator
 * 
 * Displays current gesture mode and value
 * 🖼️ [修复] 添加 videoshotData 支持，在 Seek 模式下显示视频缩略图
 */
@Composable
fun GestureIndicator(
    mode: GestureMode,
    value: Float,
    seekTime: Long?,
    duration: Long,
    modifier: Modifier = Modifier,
    // 🖼️ [新增] 视频预览数据
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    currentPositionMs: Long = 0L
) {
    if (mode == GestureMode.None) return
    
    when (mode) {
        GestureMode.Seek -> {
            // 🖼️ Seek 模式：显示带缩略图的预览气泡（居中显示）
            val targetPositionMs = seekTime ?: 0L
            
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                if (videoshotData != null && videoshotData.isValid) {
                    // 🖼️ 有缩略图：显示完整预览
                    com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble(
                        videoshotData = videoshotData,
                        targetPositionMs = targetPositionMs,
                        currentPositionMs = currentPositionMs,
                        durationMs = duration,
                        offsetX = 80f,  // 居中偏移（气泡宽度的一半）
                        containerWidth = 160f  // 与气泡宽度匹配
                    )
                } else {
                    // 无缩略图：显示简化版
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.8f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                "${FormatUtils.formatDuration((targetPositionMs / 1000).toInt())} / ${FormatUtils.formatDuration((duration / 1000).toInt())}",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // 显示时间差
                            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000
                            if (deltaSeconds != 0L) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (deltaSeconds > 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                                    color = if (deltaSeconds > 0) Color(0xFF81C784) else Color(0xFFE57373),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        GestureMode.Brightness -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // 亮度图标：CupertinoIcons SunMax (iOS SF Symbols 风格)
                    Icon(CupertinoIcons.Default.SunMax, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("亮度", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        GestureMode.Volume -> {
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // 动态音量图标：3 级
                    val volumeIcon = when {
                        value < 0.01f -> CupertinoIcons.Default.SpeakerSlash
                        value < 0.5f -> CupertinoIcons.Default.Speaker
                        else -> CupertinoIcons.Default.SpeakerWave2
                    }
                    Icon(volumeIcon, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("音量", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        else -> {}
    }
}

/**
 * Create tap gesture detector
 */
suspend fun PointerInputScope.detectPlayerTapGestures(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
) {
    detectTapGestures(
        onTap = { onTap() },
        onDoubleTap = { onDoubleTap() }
    )
}

/**
 * Create drag gesture detector for player
 */
suspend fun PointerInputScope.detectPlayerDragGestures(
    gestureState: PlayerGestureState,
    currentPosition: Long,
    duration: Long,
    onShowControls: () -> Unit,
    onSeek: (Long) -> Unit,
    onProgressUpdate: (Float) -> Unit
) {
    val screenWidth = size.width.toFloat()
    val screenHeight = size.height.toFloat()
    
    detectDragGestures(
        onDragStart = { offset ->
            onShowControls()
            gestureState.onDragStart(offset.x, screenWidth, currentPosition)
        },
        onDragEnd = {
            gestureState.onDragEnd(onSeek)
        },
        onDragCancel = {
            gestureState.onDragCancel()
        },
        onDrag = { change, dragAmount ->
            change.consume()
            val progress = gestureState.onDrag(
                dragAmount.x,
                dragAmount.y,
                screenWidth,
                screenHeight,
                currentPosition,
                duration
            )
            if (gestureState.gestureMode == GestureMode.Seek) {
                onProgressUpdate(progress)
            }
        }
    )
}
