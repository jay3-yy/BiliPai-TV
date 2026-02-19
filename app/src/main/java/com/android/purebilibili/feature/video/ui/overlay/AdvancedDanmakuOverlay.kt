package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.feature.video.danmaku.AdvancedDanmakuData
import kotlin.math.roundToInt

/**
 * 高级弹幕渲染层 (Compose 实现)
 * 
 * 负责渲染 Mode 7 (高级定位弹幕)。
 * 使用 BoxWithConstraints 获取屏幕尺寸，并根据弹幕的 startX/Y 和 progress 进行定位。
 * 
 * @param danmakuList 所有高级弹幕数据
 * @param currentPosition 当前视频播放进度 (毫秒)
 */
@Composable
fun AdvancedDanmakuOverlay(
    danmakuList: List<AdvancedDanmakuData>,
    player: androidx.media3.common.Player,
    modifier: Modifier = Modifier
) {
    // 使用 produceState 每一帧更新播放进度
    // 并处理暂停/播放状态
    val currentPosition by androidx.compose.runtime.produceState(initialValue = player.currentPosition, key1 = player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                value = player.currentPosition
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                value = player.currentPosition
            }
            override fun onPositionDiscontinuity(oldPosition: androidx.media3.common.Player.PositionInfo, newPosition: androidx.media3.common.Player.PositionInfo, reason: Int) {
                value = newPosition.positionMs
            }
        }
        player.addListener(listener)
        
        while (true) {
            if (player.isPlaying) {
                value = player.currentPosition
            }
            // 约 60fps 更新
            kotlinx.coroutines.delay(16)
        }
        
        awaitDispose {
            player.removeListener(listener)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidthPx = constraints.maxWidth
        val maxHeightPx = constraints.maxHeight
        
        // 筛选当前时间应该显示的弹幕
        // 为了性能，只处理当前时间窗口内的弹幕
        val activeDanmakus by remember(danmakuList, currentPosition) {
            derivedStateOf {
                // 预留一些 buffer，避免刚好在边界闪烁
                danmakuList.filter { 
                    currentPosition >= it.startTimeMs - 100 && currentPosition <= it.startTimeMs + it.durationMs + 100
                }
            }
        }
        
        activeDanmakus.forEach { danmaku ->
            key(danmaku.id) {
                RenderSingleAdvancedDanmaku(
                    danmaku = danmaku,
                    currentPosition = currentPosition,
                    maxWidth = maxWidthPx,
                    maxHeight = maxHeightPx
                )
            }
        }
    }
}

@Composable
private fun RenderSingleAdvancedDanmaku(
    danmaku: AdvancedDanmakuData,
    currentPosition: Long,
    maxWidth: Int,
    maxHeight: Int
) {
    // 计算进度 (0.0 ~ 1.0)
    val progress = danmaku.getProgress(currentPosition)
    
    // 如果是高能弹幕 (maxCount > 1)，需要动态计算显示的文字
    val displayText = if (danmaku.maxCount > 1) {
        // 计算积累阶段的进度
        val elapsed = currentPosition - danmaku.startTimeMs
        if (elapsed < danmaku.accumulationDurationMs) {
            // 增长阶段：根据时间比例计算当前数字
            // 至少显示 1
            val currentCount = (1 + (danmaku.maxCount - 1) * (elapsed.toFloat() / danmaku.accumulationDurationMs.toFloat())).toInt()
            "${danmaku.content} ×$currentCount"
        } else {
            // 积累完成，显示最大值
            "${danmaku.content} ×${danmaku.maxCount}"
        }
    } else {
        danmaku.content
    }

    // 简单的线性插值计算位置
    val currentX = danmaku.startX + (danmaku.endX - danmaku.startX) * progress
    val currentY = danmaku.startY + (danmaku.endY - danmaku.startY) * progress

    // 转换为像素坐标
    // AdvancedDanmakuData 中存储的是 0.0~1.0
    // 高能弹幕居中时需减去文字宽度的一半 -> Compose 会自动处理居中 (通过 offset 并不是全部，需要 alignment)
    // 为了简单起见，我们假设 startX/Y 是中心点。
    // 在 Compose 中，offset 是偏移量。如果不做额外处理，offset(x, y) 是将组件左上角移动到 (x, y)。
    // 这里我们先计算左上角位置。
    val xPx = (currentX * maxWidth).roundToInt()
    val yPx = (currentY * maxHeight).roundToInt()

    // 颜色转换
    val color = Color(danmaku.color or 0xFF000000.toInt())
    
    // [视觉优化] 高能弹幕使用更强烈的动画效果
    val isAccumulating = currentPosition < danmaku.startTimeMs + danmaku.accumulationDurationMs
    
    // 心跳动画
    // 使用 infiniteTransition 或者简单的根据时间取余计算 scale
    val scale = if (danmaku.maxCount > 1 && isAccumulating) {
        // 简单的模拟心跳: 每 300ms 跳动一次
        val pulsePhase = (currentPosition % 300) / 300f
        // 1.0 -> 1.3 -> 1.0
        if (pulsePhase < 0.5f) {
            1.0f + 0.3f * (pulsePhase * 2)
        } else {
            1.3f - 0.3f * ((pulsePhase - 0.5f) * 2)
        }
    } else 1.0f

    // [视觉优化] 增加文字描边/阴影以提高清晰度
    // Compose Text 原生不支持描边，使用 Shadow 或者 Box 叠加
    Box(
        modifier = Modifier
            .offset { IntOffset(xPx, yPx) }
            // 使得 (x,y) 成为中心点，而不是左上角
            .offset(x = (-50).sp.value.dp.run { -this }, y = (-20).sp.value.dp.run { -this }) // 粗略修正，或者使用 alignment
            // 由于我们不知道具体 Text 大小，无法完美居中，除非使用 onGloballyPositioned
            // 简化处理：对于 mode 7 和高能弹幕，通常文本较长，我们这里假设其锚点就是左上角，或者我们改用 Alignment
            // 但 AdvancedDanmakuOverlay 使用的是 BoxWithConstraints + absolute offset
            // FIXME: 暂时保持左上角锚点，避免复杂布局变动
            .alpha(danmaku.alpha)
            .rotate(danmaku.rotateZ)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // 阴影层 (多次绘制模拟描边，或者简单的 Shadow)
        // 简单方案：绘制黑色背景文字在下方
        Text(
            text = displayText,
            color = Color.Black.copy(alpha = 0.8f),
            fontSize = danmaku.fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.offset(2.dp, 2.dp),
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    blurRadius = 4f
                )
            )
        )
        
        // 主文字
        Text(
            text = displayText,
            color = color,
            fontSize = danmaku.fontSize.sp,
            fontWeight = FontWeight.Bold,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 2f
                )
            )
        )
    }
}
