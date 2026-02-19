package com.android.purebilibili.feature.live.components

import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.SpeakerWave2
import io.github.alexzhirkevich.cupertino.icons.filled.SunMax
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward
import io.github.alexzhirkevich.cupertino.icons.outlined.Pause
import io.github.alexzhirkevich.cupertino.icons.outlined.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowUpLeftAndArrowDownRight
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowDownRightAndArrowUpLeft
import io.github.alexzhirkevich.cupertino.icons.outlined.ArrowClockwise
import io.github.alexzhirkevich.cupertino.icons.filled.TextBubble
import io.github.alexzhirkevich.cupertino.icons.filled.BubbleLeft
import android.app.Activity
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
private fun PlayerIconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White
        )
    }
}

/**
 * 直播播放器控制层
 * 支持：
 * 1. 左侧亮度调节手势
 * 2. 右侧音量调节手势
 * 3. 单击显示/隐藏控制器
 * 4. 双击暂停/播放
 */
@Composable
fun LivePlayerControls(
    isPlaying: Boolean,
    isFullscreen: Boolean,
    title: String,
    onPlayPause: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isChatVisible: Boolean = true,
    onToggleChat: () -> Unit = {},
    showChatToggle: Boolean = false,
    // 新增：弹幕开关
    isDanmakuEnabled: Boolean = true,
    onToggleDanmaku: () -> Unit = {},
    // [新增] 刷新
    onRefresh: () -> Unit = {}
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    
    // 自动隐藏控制器
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3000)
            isControlsVisible = false
        }
    }
    
    // 手势调节状态
    var gestureIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    var gestureText by remember { mutableStateOf("") }
    var isGestureVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isControlsVisible = !isControlsVisible },
                    onDoubleTap = { onPlayPause() }
                )
            }
            .pointerInput(Unit) {
                val screenHeight = size.height.toFloat()
                val screenWidth = size.width.toFloat()
                
                // 使用 Float 累积变化量，解决"不跟手"问题
                var volumeAccumulator = 0f
                var brightnessAccumulator = 0f
                
                var maxVolume = 0
                
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < screenWidth / 2) {
                            // 左侧：亮度
                            val windowAttr = activity?.window?.attributes?.screenBrightness ?: -1f
                            brightnessAccumulator = if (windowAttr >= 0) {
                                windowAttr
                            } else {
                                try {
                                    val sysBrightness = android.provider.Settings.System.getInt(
                                        context.contentResolver,
                                        android.provider.Settings.System.SCREEN_BRIGHTNESS
                                    )
                                    sysBrightness / 255f
                                } catch (e: Exception) {
                                    0.5f
                                }
                            }
                        } else {
                            // 右侧：音量
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            volumeAccumulator = currentVol.toFloat()
                        }
                        isGestureVisible = true
                    },
                    onDragEnd = {
                        isGestureVisible = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        // 灵敏度基于屏幕高度: 拖动全屏高度 = 100% 调整
                        val sensitivity = screenHeight 
                        val delta = -dragAmount / sensitivity
                        
                        if (change.position.x < screenWidth / 2) {
                            // 调节亮度
                            // 亮度范围 0.0 ~ 1.0 (增加拖动系数使调节稍快一点，比如 1.5 倍)
                            val targetBrightness = (brightnessAccumulator + delta * 1.5f).coerceIn(0.01f, 1f)
                            brightnessAccumulator = targetBrightness // 更新累积值以保持连续性
                            
                            val lp = activity?.window?.attributes
                            lp?.screenBrightness = targetBrightness
                            activity?.window?.attributes = lp
                            
                            gestureIcon = CupertinoIcons.Filled.SunMax
                            gestureText = "${(targetBrightness * 100).toInt()}%"
                        } else {
                            // 调节音量 (maxVolume 比如 15)
                            if (maxVolume > 0) {
                                // 音量需要映射到 0~maxVolume
                                val targetVolFloat = (volumeAccumulator + delta * maxVolume * 1.2f).coerceIn(0f, maxVolume.toFloat())
                                volumeAccumulator = targetVolFloat
                                
                                val newVolInt = targetVolFloat.toInt()
                                val currentVolInt = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                
                                if (newVolInt != currentVolInt) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolInt, 0)
                                    // 注意：不要在这里重置 volumeAccumulator，否则会丢失小数部分导致卡顿
                                }
                                
                                gestureIcon = CupertinoIcons.Filled.SpeakerWave2
                                gestureText = "${(newVolInt * 100 / maxVolume)}%"
                            }
                        }
                    }
                )
            }
    ) {
        // 1. 中间手势提示
        androidx.compose.animation.AnimatedVisibility(
            visible = isGestureVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (gestureIcon != null) {
                        Icon(gestureIcon!!, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(gestureText, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        
        // 2. 顶部栏 (返回 + 标题)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerIconBtn(
                    icon = CupertinoIcons.Default.ChevronBackward,
                    onClick = onBack
                )
                Spacer(Modifier.width(16.dp))
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            }
        }
        
        // 3. 底部栏 (播放暂停 + 进度(直播无进度) + 全屏)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 播放/暂停
                PlayerIconBtn(
                    icon = if (isPlaying) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                    onClick = onPlayPause
                )
                
                Spacer(Modifier.width(16.dp))
                
                // [新增] 刷新按钮
                PlayerIconBtn(
                    icon = CupertinoIcons.Outlined.ArrowClockwise,
                    onClick = onRefresh
                )
                
                Spacer(Modifier.weight(1f))
                
                // [修改] 弹幕开关 - 始终显示 (竖屏/横屏都需要)
                // 弹幕开关胶囊按钮
                Surface(
                    onClick = onToggleDanmaku,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDanmakuEnabled) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                         // [小图标]
                        Icon(
                            imageVector = CupertinoIcons.Filled.TextBubble, 
                            contentDescription = null,
                            tint = if (isDanmakuEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isDanmakuEnabled) "弹幕 开" else "弹幕 关",
                            color = if (isDanmakuEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))

                // 侧边栏开关 (仅横屏/全屏下显示)
                if (showChatToggle) {
                    // 侧边栏开关胶囊按钮
                    Surface(
                        onClick = {
                            com.android.purebilibili.core.util.Logger.d("LivePlayerControls", "Chat toggle clicked, current visible: $isChatVisible")
                            onToggleChat()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isChatVisible) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                             Icon(
                                imageVector = CupertinoIcons.Filled.BubbleLeft, 
                                contentDescription = null,
                                tint = if (isChatVisible) Color.White else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isChatVisible) "与大家互动" else "与大家互动",
                                color = if (isChatVisible) Color.White else Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                }
                
                // 全屏
                PlayerIconBtn(
                    icon = if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                    onClick = onToggleFullscreen
                )
            }
        }
    }
}
