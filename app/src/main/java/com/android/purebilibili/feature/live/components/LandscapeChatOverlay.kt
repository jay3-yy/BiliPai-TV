package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import com.android.purebilibili.feature.live.LiveDanmakuItem
import kotlinx.coroutines.flow.SharedFlow
import coil.compose.AsyncImage

/**
 * 横屏模式专用 - 透明弹幕覆盖层
 * 左下角浮动显示，透明渐变背景
 */
@Composable
fun LandscapeChatOverlay(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    modifier: Modifier = Modifier
) {
    val messages = remember { mutableStateListOf<LiveDanmakuItem>() }
    val listState = rememberLazyListState()
    
    LaunchedEffect(danmakuFlow) {
        danmakuFlow.collect { item ->
            // 确保列表操作在主线程执行 (Compose 状态修改必须在主线程)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                try {
                    messages.add(item)
                    if (messages.size > 50) messages.removeFirst() // 横屏模式只保留最近50条
                    if (!listState.isScrollInProgress && messages.isNotEmpty()) {
                        listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LandscapeChatOverlay", "❌ Message add error: ${e.message}")
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.3f),
                        Color.Black.copy(alpha = 0.5f)
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .padding(start = 12.dp, bottom = 8.dp, end = 8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            reverseLayout = false // 正常方向，新消息在底部
        ) {
            items(messages) { item ->
                LandscapeChatItem(item)
            }
        }
    }
}



@Composable
private fun LandscapeChatItem(item: LiveDanmakuItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        // [新增] 粉丝牌 (横屏版，稍微小一点)
        if (item.medalLevel > 0) {
            val color = if (item.medalColor != 0) Color(item.medalColor) else Color(0xFFFF6699)
            Surface(
                color = color.copy(alpha = 0.8f), // 稍微透明一点
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                 Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = item.medalName,
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${item.medalLevel}",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 用户名 + 消息 (带描边确保可读)
        val textStyle = TextStyle(
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(1f, 1f),
                blurRadius = 2f
            )
        )

        if (item.emoticonUrl != null) {
            Text(
                text = "${item.uname}: ",
                style = textStyle
            )
            AsyncImage(
                model = item.emoticonUrl,
                contentDescription = item.text,
                modifier = Modifier.size(32.dp)
            )
        } else {
             // 简单的描边效果实现比较麻烦，这里使用 Shadow 增强对比度
             // 并区分用户名为黄色或根据权限变色
             val nameColor = if (item.isAdmin) Color(0xFFFF6699) else Color(0xFFE0E0E0)
             
             Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = nameColor, fontWeight = FontWeight.Bold)) {
                        append(item.uname)
                    }
                    append(": ")
                    append(item.text)
                },
                style = textStyle,
                maxLines = 3 // 横屏允许稍微多一点行数
            )
        }
    }
}
