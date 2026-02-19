// 聊天详情页面
package com.android.purebilibili.feature.message

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.EmoteInfo
import com.android.purebilibili.data.model.response.PrivateMessageItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    talkerId: Long,
    sessionType: Int,
    userName: String,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(talkerId, sessionType))
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // 滚动到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text(userName) }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                isSending = uiState.isSending
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.error ?: "加载失败")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadMessages() }) {
                            Text("重试")
                        }
                    }
                }
                uiState.messages.isEmpty() -> {
                    Text(
                        text = "暂无消息",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 加载更多按钮
                        if (uiState.hasMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        TextButton(onClick = { viewModel.loadMoreMessages() }) {
                                            Text("加载更多")
                                        }
                                    }
                                }
                            }
                        }
                        
                        items(
                            items = uiState.messages,
                            key = { it.msg_key }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.sender_uid == viewModel.currentUserMid,
                                emoteInfos = uiState.emoteInfos,
                                videoPreviews = uiState.videoPreviews,
                                onVideoClick = { bvid ->
                                    onNavigateToVideo(bvid)
                                }
                            )
                        }
                    }
                }
            }
            
            // 发送错误提示
            uiState.sendError?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearSendError() }) {
                            Text("知道了")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: PrivateMessageItem,
    isOwnMessage: Boolean,
    emoteInfos: List<EmoteInfo> = emptyList(),
    videoPreviews: Map<String, VideoPreviewInfo> = emptyMap(),
    onVideoClick: ((String) -> Unit)? = null
) {
    val bubbleColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // BV号正则匹配
    val bvPattern = remember { Regex("BV[a-zA-Z0-9]{10}") }
    
    // 从消息内容中提取BV号
    val detectedBvids = remember(message.content) {
        if (message.msg_type == 1) {
            val content = parseTextContent(message.content)
            bvPattern.findAll(content).map { it.value }.toList()
        } else {
            emptyList()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        // 消息气泡
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                        bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when {
                message.msg_status == 1 -> {
                    // 已撤回消息
                    Text(
                        text = "[消息已撤回]",
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
                message.msg_type == 1 -> {
                    // 文字消息 - 支持表情渲染
                    val content = parseTextContent(message.content)
                    EmoteText(
                        text = content,
                        emoteInfos = emoteInfos,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }
                message.msg_type == 2 -> {
                    // 图片消息
                    val imageUrl = parseImageUrl(message.content)
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "图片",
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "[图片]",
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                }
                message.msg_type == 6 -> {
                    // 表情消息 (大表情)
                    val emoteUrl = parseEmoteUrl(message.content)
                    if (emoteUrl.isNotEmpty()) {
                        AsyncImage(
                            model = emoteUrl,
                            contentDescription = "表情",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "[表情]",
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                }
                message.msg_type == 10 -> {
                    // 通知消息
                    Text(
                        text = parseNotificationContent(message.content),
                        color = textColor,
                        fontSize = 14.sp
                    )
                }
                else -> {
                    Text(
                        text = "[${getMessageTypeName(message.msg_type)}]",
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // 视频链接预览卡片
        detectedBvids.forEach { bvid ->
            videoPreviews[bvid]?.let { preview ->
                Spacer(modifier = Modifier.height(4.dp))
                VideoLinkPreviewCard(
                    preview = preview,
                    onClick = { onVideoClick?.invoke(bvid) }
                )
            }
        }
        
        // 时间
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatMessageTime(message.timestamp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * 视频链接预览卡片
 */
@Composable
fun VideoLinkPreviewCard(
    preview: VideoPreviewInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 封面图
            Box {
                AsyncImage(
                    model = preview.cover,
                    contentDescription = preview.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentScale = ContentScale.Crop
                )
                
                // 播放图标
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▶",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
                
                // 时长
                if (preview.duration > 0) {
                    Text(
                        text = formatDuration(preview.duration),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 信息
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                // 标题
                Text(
                    text = preview.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // UP主 + 播放量
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preview.ownerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = " · ${formatViewCount(preview.viewCount)}播放",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 格式化时长
 */
private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%d:%02d", m, s)
    }
}

/**
 * 格式化播放量
 */
private fun formatViewCount(count: Long): String {
    return when {
        count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
        count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
        else -> count.toString()
    }
}

/**
 * 支持表情和链接渲染的富文本组件
 */
@Composable
fun RichMessageText(
    text: String,
    emoteInfos: List<EmoteInfo>,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    linkColor: Color = MaterialTheme.colorScheme.primary,  // 使用主题色
    onLinkClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // 构建表情映射 (text -> EmoteInfo)
    val emoteMap = remember(emoteInfos) {
        emoteInfos.associateBy { it.text }
    }
    
    // 匹配模式
    val emotePattern = remember { "\\[([^\\[\\]]+)\\]".toRegex() }
    val urlPattern = remember { 
        "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex() 
    }
    
    // 扫描所有特殊内容 (表情和URL)
    data class ContentMatch(val range: IntRange, val type: String, val value: String)
    
    val allMatches = remember(text) {
        val matches = mutableListOf<ContentMatch>()
        
        // 收集表情
        emotePattern.findAll(text).forEach { match ->
            matches.add(ContentMatch(match.range, "emote", match.value))
        }
        
        // 收集 URL
        urlPattern.findAll(text).forEach { match ->
            matches.add(ContentMatch(match.range, "url", match.value))
        }
        
        // 按位置排序
        matches.sortedBy { it.range.first }
    }
    
    // 如果没有特殊内容，直接显示文本
    if (allMatches.isEmpty()) {
        Text(text = text, color = color, fontSize = fontSize)
        return
    }
    
    // 构建 AnnotatedString
    val annotatedString = buildAnnotatedString {
        var lastEnd = 0
        
        allMatches.forEach { match ->
            // 添加前面的普通文本
            if (match.range.first > lastEnd) {
                append(text.substring(lastEnd, match.range.first))
            }
            
            when (match.type) {
                "emote" -> {
                    val emote = emoteMap[match.value]
                    if (emote != null && emote.url.isNotEmpty()) {
                        appendInlineContent(match.value, match.value)
                    } else {
                        append(match.value)
                    }
                }
                "url" -> {
                    // 添加链接样式和注解
                    pushStringAnnotation(tag = "URL", annotation = match.value)
                    withStyle(SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append(match.value)
                    }
                    pop()
                }
            }
            
            lastEnd = match.range.last + 1
        }
        
        // 添加剩余文本
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
    
    // 构建 InlineContent 映射 (表情图片)
    val inlineContentMap = remember(emoteInfos) {
        emoteInfos.filter { it.url.isNotEmpty() }.associate { emote ->
            emote.text to InlineTextContent(
                placeholder = Placeholder(
                    width = 20.sp,
                    height = 20.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                AsyncImage(
                    model = emote.url,
                    contentDescription = emote.text,
                    modifier = Modifier.size(20.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
    
    // 用于检测点击位置
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Text(
        text = annotatedString,
        color = color,
        fontSize = fontSize,
        inlineContent = inlineContentMap,
        onTextLayout = { layoutResult = it },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                layoutResult?.let { layout ->
                    val position = layout.getOffsetForPosition(offset)
                    annotatedString.getStringAnnotations(
                        tag = "URL",
                        start = position,
                        end = position
                    ).firstOrNull()?.let { annotation ->
                        val url = annotation.item
                        if (onLinkClick != null) {
                            onLinkClick(url)
                        } else {
                            // 默认: 用浏览器打开
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("RichMessageText", "Failed to open URL: $url", e)
                            }
                        }
                    }
                }
            }
        }
    )
}

// 保留旧函数名的兼容性别名
@Composable
fun EmoteText(
    text: String,
    emoteInfos: List<EmoteInfo>,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    RichMessageText(
        text = text,
        emoteInfos = emoteInfos,
        color = color,
        fontSize = fontSize
    )
}


/**
 * 解析文字消息内容
 */
private fun parseTextContent(content: String): String {
    if (content.isBlank()) return ""
    if (!content.trim().startsWith("{")) return content

    return try {
        val json = Json.parseToJsonElement(content)
        json.jsonObject["content"]?.jsonPrimitive?.content ?: content
    } catch (e: Exception) {
        content
    }
}

/**
 * 解析图片URL
 */
private fun parseImageUrl(content: String): String {
    return try {
        val json = Json.parseToJsonElement(content)
        json.jsonObject["url"]?.jsonPrimitive?.content ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * 解析表情URL
 */
private fun parseEmoteUrl(content: String): String {
    return try {
        val json = Json.parseToJsonElement(content)
        json.jsonObject["url"]?.jsonPrimitive?.content ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * 解析通知消息
 */
private fun parseNotificationContent(content: String): String {
    return try {
        val json = Json.parseToJsonElement(content)
        val title = json.jsonObject["title"]?.jsonPrimitive?.content ?: ""
        val text = json.jsonObject["text"]?.jsonPrimitive?.content ?: ""
        if (title.isNotEmpty()) "$title\n$text" else text
    } catch (e: Exception) {
        "[通知]"
    }
}

/**
 * 获取消息类型名称
 */
private fun getMessageTypeName(msgType: Int): String {
    return when (msgType) {
        1 -> "文字"
        2 -> "图片"
        5 -> "撤回"
        6 -> "表情"
        7 -> "分享"
        10 -> "通知"
        11 -> "视频"
        12 -> "专栏"
        else -> "消息"
    }
}

/**
 * 格式化消息时间
 */
private fun formatMessageTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp * 1000 }
    
    val sameDay = now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
    
    val pattern = if (sameDay) "HH:mm" else "MM-dd HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp * 1000))
}
