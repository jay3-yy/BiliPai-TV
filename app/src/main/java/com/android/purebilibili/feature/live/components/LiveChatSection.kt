package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.android.purebilibili.feature.live.LiveDanmakuItem
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Paperplane
import kotlinx.coroutines.flow.SharedFlow
import coil.compose.AsyncImage

/**
 * 直播聊天区域组件
 * 包含：
 * 1. 聊天列表 (LazyColumn)
 * 2. 底部输入栏
 */
@Composable
fun LiveChatSection(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    onSendDanmaku: (String) -> Unit,
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
                    if (messages.size > 200) messages.removeFirst()
                    // 只有当用户没有滚动时才自动滚动
                    if (!listState.isScrollInProgress && messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LiveChatSection", "❌ Message add error: ${e.message}")
                }
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // 适配深色模式
    ) {
        // 1. 聊天列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { item ->
                ChatMessageItem(item)
            }
        }
        
        // 2. 底部输入栏
        ChatInputBar(onSend = onSendDanmaku)
    }
}



@Composable
private fun ChatMessageItem(item: LiveDanmakuItem) {
    Row(
        verticalAlignment = Alignment.Top, // 顶部对齐，适合多行文本
        modifier = Modifier.padding(vertical = 4.dp) // 增加间距
    ) {
        // [新增] 粉丝牌徽章
        if (item.medalLevel > 0) {
            MedalBadge(
                name = item.medalName,
                level = item.medalLevel,
                colorInt = item.medalColor
            )
            Spacer(Modifier.width(4.dp))
        }

        // [新增] 用户等级徽章 (可选，B站App通常只显示粉丝牌，但这里为了丰富度可以加上缩略版)
        // 只有当没有粉丝牌或者空间足够时显示，为了清爽这里我们作为次要信息
        // 或者仅在没有粉丝牌时显示 UL ? 暂时策略：都显示，但 UL 做得很小
        if (item.userLevel > 0) {
           UserLevelBadge(level = item.userLevel)
           Spacer(Modifier.width(4.dp))
        }

        // 文本内容
        val usernameColor = if (item.isAdmin) Color(0xFFFF6699) // 房管粉色
                           else if (item.isSelf) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // 普通用户淡一点

        if (item.emoticonUrl != null) {
            Column {
                Text(
                    text = "${item.uname}: ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, // 用户名加粗
                    color = usernameColor,
                    lineHeight = 20.sp
                )
                AsyncImage(
                    model = item.emoticonUrl,
                    contentDescription = item.text,
                    modifier = Modifier.size(50.dp)
                )
            }
        } else {
             // [新增] 使用 Mapper 解析表情
            val emoticonMap by DanmakuEmoticonMapper.emoticonMap.collectAsState()
            val annotatedText = remember(item.text, item.uname, emoticonMap) {
               //以此构建完整的 AnnotatedString
               val builder = androidx.compose.ui.text.AnnotatedString.Builder()
               builder.pushStyle(androidx.compose.ui.text.SpanStyle(
                   color = usernameColor,
                   fontWeight = FontWeight.Bold,
                   fontSize = 14.sp
               ))
               builder.append(item.uname)
               builder.pop()
               
               builder.append(": ")
               
               // 解析内容部分的表情
               val contentText = DanmakuEmoticonMapper.parse(item.text, emoticonMap)
               builder.append(contentText)
               
               builder.toAnnotatedString()
            }
            
            // 动态构建 inlineContent Map (逻辑同上，可复用)
            val inlineContentMap = remember(annotatedText) {
                // ... (复用之前的逻辑)
                val usedKeys = Regex("\\[(.*?)\\]").findAll(item.text).map { it.value }.toSet()
                emoticonMap.filterKeys { it in usedKeys }.mapValues { (_, url) ->
                    androidx.compose.foundation.text.InlineTextContent(
                        androidx.compose.ui.text.Placeholder(
                            width = 1.4.em,
                            height = 1.4.em,
                            placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.Center
                        )
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            androidx.compose.foundation.text.BasicText(
                text = annotatedText,
                style = TextStyle(
                    fontSize = 15.sp, // 稍微加大字号
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                inlineContent = inlineContentMap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// [新增] 粉丝牌组件
@Composable
private fun MedalBadge(name: String, level: Int, colorInt: Int) {
    val color = if (colorInt != 0) Color(colorInt) else Color(0xFFFF6699)
    
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
        ) {
            Text(
                text = name,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(2.dp))
            // 简单的竖线分隔
            Box(Modifier.width(0.5.dp).height(8.dp).background(Color.White.copy(0.7f)))
            Spacer(Modifier.width(2.dp))
            Text(
                text = "$level",
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// [新增] 用户等级组件
@Composable
private fun UserLevelBadge(level: Int) {
    // 简单的胶囊样式
    // 颜色根据等级变化 (简化处理：低等级灰/蓝，高等级橙/红)
    val color = when {
        level >= 40 -> Color(0xFFFF3333) // 红
        level >= 20 -> Color(0xFFFFAA33) // 橙
        else -> Color(0xFF66CCFF)        // 蓝
    }
    
    Surface(
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        shape = RoundedCornerShape(3.dp),
        color = Color.Transparent, // 空心
        modifier = Modifier.padding(top = 2.dp)
    ) {
         Text(
            text = "UL$level",
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.dp)
        )
    }
}

@Composable
private fun ChatInputBar(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp, // 增加投影增加层次感
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 输入框
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant) // 适配深色模式
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = "发个弹幕和主播互动吧~",
                                color = Color(0xFF9499A0),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 发送按钮
            val isEnabled = text.isNotBlank()
            Surface(
                onClick = {
                    if (isEnabled) {
                        onSend(text)
                        text = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = isEnabled,
                shape = CircleShape,
                color = if (isEnabled) MaterialTheme.colorScheme.primary else Color(0xFFE3E5E7), // 主题色 vs 禁用灰
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = CupertinoIcons.Filled.Paperplane,
                        contentDescription = "发送",
                        tint = if (isEnabled) Color.White else Color(0xFF9499A0),
                        modifier = Modifier.size(20.dp).offset(x = (-2).dp, y = 2.dp) // 视觉居中微调
                    )
                }
            }
        }
    }
}
