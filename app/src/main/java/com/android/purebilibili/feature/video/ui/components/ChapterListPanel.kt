// File: feature/video/ui/components/ChapterListPanel.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ViewPoint
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Xmark

/**
 * 📖 视频章节列表面板
 * 
 * 浮动小卡片设计（参考B站官方横屏样式）
 * 点击章节可跳转到对应时间
 */
@Composable
fun ChapterListPanel(
    viewPoints: List<ViewPoint>,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 计算当前章节索引
    val currentChapterIndex = remember(currentPositionMs, viewPoints) {
        viewPoints.indexOfLast { currentPositionMs >= it.fromMs }
            .coerceAtLeast(0)
    }
    
    // 点击背景关闭
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    ) {
        // 浮动卡片 - 左下角位置，固定宽度
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 60.dp)  // 避开进度条
                .width(280.dp)  // 固定宽度
                .heightIn(max = 200.dp)  // 最大高度
                .clickable(enabled = false) {},  // 阻止点击穿透
            shape = RoundedCornerShape(12.dp),
            color = Color(0xE6222222),  // 深色半透明
            shadowElevation = 8.dp
        ) {
            Column {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "视频章节",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Icon(
                        CupertinoIcons.Outlined.Xmark,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onDismiss)
                    )
                }
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                // 章节列表 - 简洁设计
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(viewPoints.size) { index ->
                        val point = viewPoints[index]
                        val isCurrentChapter = index == currentChapterIndex
                        
                        ChapterListItem(
                            chapter = point,
                            isCurrentChapter = isCurrentChapter,
                            onClick = {
                                onSeek(point.fromMs)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: ViewPoint,
    isCurrentChapter: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrentChapter) primaryColor.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 章节信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chapter.content,
                color = if (isCurrentChapter) primaryColor else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isCurrentChapter) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = FormatUtils.formatDuration(chapter.from) + " - " + FormatUtils.formatDuration(chapter.to),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp
            )
        }
        
        // 当前播放指示
        if (isCurrentChapter) {
            Text(
                text = "正在播放",
                color = primaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
