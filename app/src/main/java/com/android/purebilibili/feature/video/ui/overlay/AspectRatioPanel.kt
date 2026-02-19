// 文件路径: feature/video/ui/overlay/AspectRatioPanel.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.feature.video.ui.components.VideoAspectRatio

/**
 *  画面比例选择面板（官方 B 站样式）
 * 
 * 从左侧滑入的面板，支持：
 * - 点击选择比例
 * - 上下滑动快速切换
 * 
 * 选项：适应 | 填充 | 16:9 | 4:3
 */
@Composable
fun AspectRatioPanel(
    visible: Boolean,
    currentRatio: VideoAspectRatio,
    onRatioChange: (VideoAspectRatio) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class RatioOption(val ratio: VideoAspectRatio, val label: String)
    
    val ratioOptions = listOf(
        RatioOption(VideoAspectRatio.FIT, "适应"),
        RatioOption(VideoAspectRatio.FILL, "填充"),
        RatioOption(VideoAspectRatio.RATIO_16_9, "16:9"),
        RatioOption(VideoAspectRatio.RATIO_4_3, "4:3")
    )
    
    // 滑动检测状态
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally { -it },
        exit = fadeOut() + slideOutHorizontally { -it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() }
        ) {
            // 左侧面板
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .width(80.dp)
                    .pointerInput(currentRatio) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                accumulatedDrag = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                accumulatedDrag += dragAmount
                                val threshold = 50f
                                
                                if (accumulatedDrag > threshold) {
                                    // 向下滑动 -> 下一个选项
                                    val currentIndex = ratioOptions.indexOfFirst { it.ratio == currentRatio }
                                    if (currentIndex < ratioOptions.size - 1) {
                                        onRatioChange(ratioOptions[currentIndex + 1].ratio)
                                    }
                                    accumulatedDrag = 0f
                                } else if (accumulatedDrag < -threshold) {
                                    // 向上滑动 -> 上一个选项
                                    val currentIndex = ratioOptions.indexOfFirst { it.ratio == currentRatio }
                                    if (currentIndex > 0) {
                                        onRatioChange(ratioOptions[currentIndex - 1].ratio)
                                    }
                                    accumulatedDrag = 0f
                                }
                            }
                        )
                    }
                    .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { /* 阻止点击穿透 */ },
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 标题
                    Text(
                        text = "画面比例",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 选项列表
                    ratioOptions.forEach { option ->
                        val isSelected = currentRatio == option.ratio
                        
                        Surface(
                            onClick = { 
                                onRatioChange(option.ratio)
                                onDismiss()
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = option.label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

