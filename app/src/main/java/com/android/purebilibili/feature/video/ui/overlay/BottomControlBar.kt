// File: feature/video/ui/overlay/BottomControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
//  Cupertino Icons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.draw.clip

/**
 * Bottom Control Bar Component
 * 
 * Redesigned Control Bar:
 * [Play/Pause] [Time]  [Danmaku Switch] [       Input Bar       ] [Settings]  [Quality] [Speed] [Fullscreen]
 */

data class PlayerProgress(
    val current: Long = 0L,
    val duration: Long = 0L,
    val buffered: Long = 0L
)

internal fun shouldShowAspectRatioButtonInControlBar(isFullscreen: Boolean): Boolean = isFullscreen
internal fun shouldShowPortraitSwitchButtonInControlBar(isFullscreen: Boolean): Boolean = isFullscreen

@Composable
fun BottomControlBar(
    isPlaying: Boolean,
    progress: PlayerProgress,
    isFullscreen: Boolean,
    currentSpeed: Float = 1.0f,
    currentRatio: VideoAspectRatio = VideoAspectRatio.FIT,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onRatioClick: () -> Unit = {},
    onToggleFullscreen: () -> Unit,
    
    // Danmaku
    danmakuEnabled: Boolean = true,
    onDanmakuToggle: () -> Unit = {},
    onDanmakuSettingsClick: () -> Unit = {},
    
    // Quality
    currentQualityLabel: String = "",
    onQualityClick: () -> Unit = {},
    
    // Features
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {},
    
    // Portrait controls (kept for compatibility, though less used in new design)
    isVerticalVideo: Boolean = false,
    onPortraitFullscreen: () -> Unit = {},
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode = com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {},
    playbackOrderLabel: String = "",
    onPlaybackOrderClick: () -> Unit = {},
    onPipClick: () -> Unit = {},
    
    modifier: Modifier = Modifier
) {
    val isTvDevice = rememberIsTvDevice()
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveBottomControlBarLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }
    val progressLayoutPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveVideoProgressBarLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = layoutPolicy.bottomPaddingDp.dp)
            .let { if (isFullscreen) it.navigationBarsPadding() else it }
    ) {
        // 1. Progress Bar (Top of controls)
        VideoProgressBar(
            currentPosition = progress.current,
            duration = progress.duration,
            bufferedPosition = progress.buffered,
            layoutPolicy = progressLayoutPolicy,
            onSeek = onSeek,
            onSeekStart = onSeekStart,
            videoshotData = videoshotData,
            viewPoints = viewPoints,
            currentChapter = currentChapter,
            onChapterClick = onChapterClick
        )

        Spacer(modifier = Modifier.height(layoutPolicy.progressSpacingDp.dp))

        // 2. Control Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layoutPolicy.horizontalPaddingDp.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Play/Pause
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(layoutPolicy.playButtonSizeDp.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(layoutPolicy.playIconSizeDp.dp)
                )
            }

            Spacer(modifier = Modifier.width(layoutPolicy.afterPlaySpacingDp.dp))

            // Time
            Text(
                text = "${FormatUtils.formatDuration((progress.current / 1000).toInt())} / ${FormatUtils.formatDuration((progress.duration / 1000).toInt())}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = layoutPolicy.timeFontSp.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(layoutPolicy.afterTimeSpacingDp.dp))

            // Center area: Danmaku Controls (Switch + Input) - Only visible in Fullscreen/Landscape
            if (isFullscreen) {
                // Danmaku Switch
                Icon(
                    imageVector = if (danmakuEnabled) CupertinoIcons.Default.TextBubble else CupertinoIcons.Outlined.TextBubble,
                    contentDescription = "Danmaku Toggle",
                    tint = if (danmakuEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(layoutPolicy.danmakuIconSizeDp.dp)
                        .clickable(onClick = onDanmakuToggle)
                )
                
                Spacer(modifier = Modifier.width(layoutPolicy.danmakuSwitchToInputSpacingDp.dp))
                
                // Danmaku Input Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(layoutPolicy.danmakuInputHeightDp.dp)
                        .clip(RoundedCornerShape((layoutPolicy.danmakuInputHeightDp / 2).dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { /* TODO: Open Input Dialog */ },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "发个友善的弹幕见证当下...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = layoutPolicy.danmakuInputFontSp.sp,
                        modifier = Modifier.padding(start = layoutPolicy.danmakuInputStartPaddingDp.dp)
                    )
                    
                    // Settings Icon inside input bar (right)
                    IconButton(
                        onClick = onDanmakuSettingsClick,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = layoutPolicy.danmakuSettingEndPaddingDp.dp)
                            .size(layoutPolicy.danmakuSettingButtonSizeDp.dp)
                    ) {
                        Icon(
                            imageVector = CupertinoIcons.Default.Gearshape,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(layoutPolicy.danmakuSettingIconSizeDp.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(layoutPolicy.afterInputSpacingDp.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Right: Function Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layoutPolicy.rightActionSpacingDp.dp)
            ) {
                // Quality
                if (currentQualityLabel.isNotEmpty()) {
                    Text(
                        text = currentQualityLabel,
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onQualityClick)
                    )
                }
                
                // Speed
                Text(
                    text = if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                    color = if (currentSpeed == 1.0f) Color.White else MaterialTheme.colorScheme.primary,
                    fontSize = layoutPolicy.actionTextFontSp.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onSpeedClick)
                )

                if (playbackOrderLabel.isNotBlank()) {
                    Text(
                        text = playbackOrderLabel,
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onPlaybackOrderClick)
                    )
                }

                // 📺 横屏全屏模式下显示画面比例按钮
                if (shouldShowAspectRatioButtonInControlBar(isFullscreen)) {
                    Text(
                        text = currentRatio.displayName,
                        color = if (currentRatio == VideoAspectRatio.FIT) Color.White else MaterialTheme.colorScheme.primary,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onRatioClick)
                    )
                }

                // 📱 [修复] 竖屏全屏按钮 - 仅在非全屏模式下显示
                if (!isFullscreen) {
                    Text(
                        text = "竖屏",
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onPortraitFullscreen)
                    )
                }

                if (shouldShowPortraitSwitchButtonInControlBar(isFullscreen)) {
                    Text(
                        text = "竖屏",
                        color = Color.White,
                        fontSize = layoutPolicy.actionTextFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onPortraitFullscreen)
                    )
                }

                // Fullscreen
                Icon(
                    imageVector = if (isFullscreen) CupertinoIcons.Default.ArrowDownRightAndArrowUpLeft else CupertinoIcons.Default.ArrowUpLeftAndArrowDownRight,
                    contentDescription = if (isFullscreen) "退出横屏" else "横屏",
                    tint = Color.White,
                    modifier = Modifier
                        .size(layoutPolicy.fullscreenIconSizeDp.dp)
                        .clickable(onClick = onToggleFullscreen)
                )
            }
        }
    }
}

/**
 * Reusing existing VideoProgressBar
 */
@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    layoutPolicy: VideoProgressBarLayoutPolicy,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit = {},
    videoshotData: com.android.purebilibili.data.model.response.VideoshotData? = null,
    viewPoints: List<com.android.purebilibili.data.model.response.ViewPoint> = emptyList(),
    currentChapter: String? = null,
    onChapterClick: () -> Unit = {}
) {
     val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f
    var tempProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            tempProgress = progress
        }
    }
    
    val displayProgress = if (isDragging) tempProgress else progress
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetPositionMs = (tempProgress * duration).toLong()
    val baseHeight = if (currentChapter != null) {
        layoutPolicy.baseHeightWithChapterDp.dp
    } else {
        layoutPolicy.baseHeightWithoutChapterDp.dp
    }
    val containerHeight = if (isDragging && videoshotData != null) {
        layoutPolicy.draggingContainerHeightDp.dp
    } else {
        baseHeight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .pointerInput(Unit) {
                containerWidth = size.width.toFloat()
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((newProgress * duration).toLong())
                }
            }
            .pointerInput(Unit) {
                containerWidth = size.width.toFloat()
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        tempProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragOffsetX = offset.x
                        onSeekStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        tempProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragOffsetX = change.position.x
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((tempProgress * duration).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                        tempProgress = progress
                    }
                )
            }
    ) {
         if (isDragging) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = layoutPolicy.previewBottomPaddingDp.dp)
            ) {
                if (videoshotData != null && videoshotData.isValid) {
                    com.android.purebilibili.feature.video.ui.components.SeekPreviewBubble(
                        videoshotData = videoshotData,
                        targetPositionMs = targetPositionMs,
                        currentPositionMs = currentPosition,
                        durationMs = duration,
                        offsetX = dragOffsetX,
                        containerWidth = containerWidth
                    )
                } else {
                    com.android.purebilibili.feature.video.ui.components.SeekPreviewBubbleSimple(
                        targetPositionMs = targetPositionMs,
                        currentPositionMs = currentPosition,
                        offsetX = dragOffsetX,
                        containerWidth = containerWidth
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
             if (currentChapter != null) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onChapterClick)
                        .padding(
                            bottom = layoutPolicy.chapterBottomPaddingDp.dp,
                            start = layoutPolicy.chapterStartPaddingDp.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        CupertinoIcons.Default.ListBullet,
                        contentDescription = "Chapter",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(layoutPolicy.chapterIconSizeDp.dp)
                    )
                    Spacer(modifier = Modifier.width(layoutPolicy.chapterSpacingDp.dp))
                    Text(
                        text = currentChapter,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = layoutPolicy.chapterFontSp.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(layoutPolicy.touchContainerHeightDp.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val trackCornerRadius = (layoutPolicy.trackHeightDp / 2f).dp
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layoutPolicy.trackHeightDp.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(trackCornerRadius))
                        .drawWithContent {
                            drawContent()
                            if (duration > 0 && viewPoints.isNotEmpty()) {
                                viewPoints.forEach { point ->
                                    val position = point.fromMs.toFloat() / duration
                                    if (position > 0.01f && position < 0.99f) {
                                        val x = size.width * position
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.8f),
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = 2f
                                        )
                                    }
                                }
                            }
                        }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                        .height(layoutPolicy.trackHeightDp.dp)
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(trackCornerRadius))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                        .height(layoutPolicy.trackHeightDp.dp)
                        .background(primaryColor, RoundedCornerShape(trackCornerRadius))
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(
                                if (isDragging) layoutPolicy.thumbDraggingSizeDp.dp
                                else layoutPolicy.thumbIdleSizeDp.dp
                            )
                            .offset(
                                x = if (isDragging) layoutPolicy.thumbDraggingOffsetDp.dp
                                else layoutPolicy.thumbIdleOffsetDp.dp
                            )
                            .background(primaryColor, CircleShape)
                    )
                }
            }
        }
    }
}
