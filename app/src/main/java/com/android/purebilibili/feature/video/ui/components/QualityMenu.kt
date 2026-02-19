// File: feature/video/ui/components/QualityMenu.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Quality Selection Menu
 * 
 * Displays a menu for selecting video quality.
 * 
 * Requirement Reference: AC2.3 - Reusable quality menu
 */
@Composable
fun QualitySelectionMenu(
    qualities: List<String>,
    qualityIds: List<Int> = emptyList(),
    currentQuality: String,
    isLoggedIn: Boolean = false,
    isVip: Boolean = false,
    onQualitySelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    useDialog: Boolean = false
) {
    fun getQualityTag(qualityId: Int): String? {
        return when (qualityId) {
            127, 126, 125, 120 -> if (!isVip) "大会员" else null
            116, 112 -> if (!isVip) "大会员" else null
            80 -> if (!isLoggedIn) "登录" else null
            else -> null
        }
    }
    
    //  [新增] 判断用户是否有权限使用该画质
    fun isQualityAvailable(qualityId: Int): Boolean {
        return when {
            qualityId >= 112 -> isVip  // VIP 画质需要大会员
            qualityId >= 80 -> isLoggedIn  // 1080P 需要登录
            else -> true  // 720P 及以下无限制
        }
    }

    val menuContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 280.dp)
                    .heightIn(max = 400.dp)  //  [修复] 限制最大高度，允许滚动
                    .clip(RoundedCornerShape(12.dp))
                    // 显式消费点击，防止事件穿透到播放器手势层
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                color = Color(0xFF2B2B2B),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState())  //  [修复] 添加垂直滚动
                ) {
                    Text(
                        text = "画质选择",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(0.1f))
                    qualities.forEachIndexed { index, quality ->
                        val isSelected = quality == currentQuality
                        val qualityId = qualityIds.getOrNull(index) ?: 0
                        val tag = getQualityTag(qualityId)
                        val isAvailable = isQualityAvailable(qualityId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                //  [修改] 不可用画质仍可点击，由 ViewModel 处理权限提示
                                .clickable { onQualitySelected(index) }
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quality,
                                //  [修改] 不可用画质显示为灰色
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    !isAvailable -> Color.White.copy(0.4f)
                                    else -> Color.White.copy(0.9f)
                                },
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            
                            if (tag != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (tag == "大会员") MaterialTheme.colorScheme.primary else Color(0xFF666666),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            if (isSelected) {
                                Icon(CupertinoIcons.Default.Checkmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (useDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            menuContent()
        }
    } else {
        menuContent()
    }
}

/**
 * Speed Selection Menu
 * 
 * Displays a menu for selecting playback speed.
 */
@Composable
fun SpeedSelectionMenu(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    val menuContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    .heightIn(max = 400.dp)
                    .clip(RoundedCornerShape(12.dp))
                    // 显式消费点击，防止事件穿透到播放器手势层
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                color = Color(0xFF2B2B2B),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "播放速度",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(0.1f))
                    speedOptions.forEach { speed ->
                        val isSelected = speed == currentSpeed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSpeedSelected(speed) }
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (speed == 1.0f) "正常" else "${speed}x",
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(0.9f),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(CupertinoIcons.Default.Checkmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    menuContent()
}

@Composable
fun SpeedSelectionMenuDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SpeedSelectionMenu(
            currentSpeed = currentSpeed,
            onSpeedSelected = onSpeedSelected,
            onDismiss = onDismiss
        )
    }
}
