package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import android.os.Build
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ReplyPicture
import androidx.compose.ui.layout.ContentScale
import com.android.purebilibili.core.ui.common.copyOnLongPress
import androidx.compose.foundation.text.selection.SelectionContainer
import java.text.SimpleDateFormat
import java.util.*

//  优化后的颜色常量 (使用 MaterialTheme 替代硬编码)
// private val SubReplyBgColor = Color(0xFFF7F8FA)  // OLD
// private val TextSecondaryColor = Color(0xFF9499A0)  // OLD
// private val TextTertiaryColor = Color(0xFFB2B7BF)   // OLD

@Composable
fun ReplyHeader(count: Int) {
    Row(
        modifier = Modifier
        .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "评论",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = FormatUtils.formatStat(count.toLong()),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReplyItemView(
    item: ReplyItem,
    upMid: Long = 0,
    isPinned: Boolean = false,
    emoteMap: Map<String, String> = emptyMap(),
    onClick: () -> Unit,
    onSubClick: (ReplyItem) -> Unit,
    onTimestampClick: ((Long) -> Unit)? = null,
    onImagePreview: ((List<String>, Int, Rect?) -> Unit)? = null,
    isLiked: Boolean = item.action == 1,
    onLikeClick: (() -> Unit)? = null,
    onReplyClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    location: String? = item.replyControl?.location,
    onUrlClick: ((String) -> Unit)? = null,
    hideSubPreview: Boolean = false,
    onAvatarClick: (String) -> Unit
) {
    val isUpComment = upMid > 0 && item.mid == upMid
    val localEmoteMap = remember(item.content.emote, emoteMap) {
        val mergedMap = emoteMap.toMutableMap()
        item.content.emote?.forEach { (key, value) -> mergedMap[key] = value.url }
        mergedMap
    }
    
    // [优化] IP 属地显示逻辑
    val displayLocation = remember(location) {
        if (!location.isNullOrEmpty()) {
            // 移除旧的前缀（如果有），统一添加 "IP归属地："
            val cleanLocation = location.removePrefix("IP属地：").removePrefix("IP属地")
            "IP归属地：$cleanLocation"
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // White background for iOS list feel
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            // Avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(item.member.avatar))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onAvatarClick(item.member.mid) }
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // User Info Header
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.member.uname,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.member.vip?.vipStatus == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .copyOnLongPress(item.member.uname, "用户名")
                    )
                    
                    if (isUpComment) {
                        Spacer(modifier = Modifier.width(6.dp))
                        UpTag()
                    }
                    
                    if (isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        PinnedTag()
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    LevelTag(level = item.member.levelInfo.currentLevel)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Content
                RichCommentText(
                    text = item.content.message,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    emoteMap = localEmoteMap,
                    onTimestampClick = onTimestampClick,
                    onUrlClick = onUrlClick
                )

                // Images
                if (!item.content.pictures.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CommentPictures(
                        pictures = item.content.pictures,
                        onImageClick = { images, index, rect ->
                            onImagePreview?.invoke(images, index, rect)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Time & Location
                    Text(
                        text = buildString {
                            append(formatTime(item.ctime))
                            if (!displayLocation.isNullOrEmpty()) {
                                append(" · $displayLocation")
                            }
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Like
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(enabled = onLikeClick != null) { onLikeClick?.invoke() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Default.HandThumbsup,
                            contentDescription = "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        // [修复] 计算乐观更新后的点赞数
                        val displayLikeCount = remember(item.like, item.action, isLiked) {
                            var count = item.like
                            if (isLiked && item.action != 1) {
                                count++
                            } else if (!isLiked && item.action == 1) {
                                count--
                            }
                            count
                        }

                        if (displayLikeCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = FormatUtils.formatStat(displayLikeCount.toLong()),
                                fontSize = 12.sp,
                                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Dislike (Placeholder)
                    Icon(
                        imageVector = CupertinoIcons.Default.MinusCircle, // Fallback Dislike
                        contentDescription = "Dislike",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { /* TODO: Dislike */ }
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Reply Icon
                    Icon(
                        imageVector = CupertinoIcons.Default.BubbleLeft, // iOS Bubble icon
                        contentDescription = "Reply",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onReplyClick?.invoke() ?: onSubClick(item) }
                    )

                    // [新增] 删除按钮 (仅显示给本人)
                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(
                            imageVector = CupertinoIcons.Default.Trash,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDeleteClick() }
                        )
                    }
                }

                // Sub-comments (Threaded view)
                if (!hideSubPreview && (!item.replies.isNullOrEmpty() || item.rcount > 0)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // No background container, just cleaner list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubClick(item) }
                    ) {
                        item.replies?.take(3)?.forEach { subReply ->
                            val subEmoteMap = remember(subReply.content.emote, emoteMap) {
                                val map = emoteMap.toMutableMap()
                                subReply.content.emote?.forEach { (key, value) -> map[key] = value.url }
                                map
                            }
                            
                            //  [修复] 将子评论改为行内显示 (用户名: 内容)
                            val prefix = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))) {
                                    append(subReply.member.uname)
                                }
                                if (subReply.member.mid == upMid.toString()) {
                                    append(" ") 
                                }
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append(": ")
                                }
                            }
                            
                            RichCommentText(
                                text = subReply.content.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                emoteMap = subEmoteMap,
                                maxLines = 3,  // Increase max lines for inline text
                                onTimestampClick = onTimestampClick,
                                onUrlClick = onUrlClick,
                                prefix = prefix
                            )
                        }
                        
                        if (item.rcount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "查看全部 ${item.rcount} 条回复 >",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { onSubClick(item) }
                                    .padding(vertical = 4.dp) // Increase touch target
                            )
                        }
                    }
                }
            }
        }
        
        // Inset Divider (starts after avatar + spacing = 16 + 40 + 12 = 68dp)
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    }
}

/**
 *  [新增] 富文本评论组件
 * 支持：表情渲染、时间戳点击跳转
 */
@Composable
fun RichCommentText(
    text: String,
    fontSize: TextUnit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    emoteMap: Map<String, String>,
    maxLines: Int = Int.MAX_VALUE,
    onTimestampClick: ((Long) -> Unit)? = null,
    onUrlClick: ((String) -> Unit)? = null,
    prefix: AnnotatedString? = null
) {
    val timestampColor = MaterialTheme.colorScheme.primary
    
    //  时间戳正则: 支持 "1:23", "12:34", "1:23:45" 格式
    val timestampPattern = """(?<!\d)(\d{1,2}):(\d{2})(?::(\d{2}))?(?!\d)""".toRegex()
    
    val annotatedString = buildAnnotatedString {
        // [新增] 添加前缀 (如用户名)
        if (prefix != null) {
            append(prefix)
        }

        // 高亮 "回复 @某人 :"
        val replyPattern = "^回复 @(.*?) :".toRegex()
        val replyMatch = replyPattern.find(text)
        var startIndex = 0
        if (replyMatch != null) {
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) {
                append(replyMatch.value)
            }
            startIndex = replyMatch.range.last + 1
        }

        val remainingText = text.substring(startIndex)
        val emotePattern = """\[(.*?)\]""".toRegex()
        
        //  收集所有匹配（表情 + 时间戳）并按位置排序
        data class MatchInfo(val range: IntRange, val type: String, val value: String, val seconds: Long = 0)
        val allMatches = mutableListOf<MatchInfo>()
        
        // 收集表情匹配
        emotePattern.findAll(remainingText).forEach { match ->
            allMatches.add(MatchInfo(match.range, "emote", match.value))
        }
        
        // 收集时间戳匹配
        timestampPattern.findAll(remainingText).forEach { match ->
            val hours = match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            val minutes = match.groupValues[1].toIntOrNull() ?: 0
            val seconds = match.groupValues[2].toIntOrNull() ?: 0
            val totalSeconds = if (match.groupValues[3].isNotEmpty()) {
                // 格式: H:MM:SS
                match.groupValues[1].toInt() * 3600 + match.groupValues[2].toInt() * 60 + match.groupValues[3].toInt()
            } else {
                // 格式: MM:SS
                minutes * 60 + seconds
            }
            allMatches.add(MatchInfo(match.range, "timestamp", match.value, totalSeconds.toLong()))
        }
        
        // 收集 URL 匹配
        // 简单 URL 正则，匹配 http/https/ftp/file 开头
        val urlPattern = """((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])""".toRegex()
        urlPattern.findAll(remainingText).forEach { match ->
            allMatches.add(MatchInfo(match.range, "url", match.value))
        }

        // 按位置排序
        allMatches.sortBy { it.range.first }
        
        var lastIndex = 0
        allMatches.forEach { matchInfo ->
            // 添加匹配之前的普通文本
            if (lastIndex < matchInfo.range.first) {
                append(remainingText.substring(lastIndex, matchInfo.range.first))
            }
            // 避免重叠匹配（例如 URL 中的数字被识别为时间戳）
            if (matchInfo.range.first >= lastIndex) {
            
                when (matchInfo.type) {
                    "emote" -> {
                        if (emoteMap.containsKey(matchInfo.value)) {
                            appendInlineContent(id = matchInfo.value, alternateText = matchInfo.value)
                        } else {
                            append(matchInfo.value)
                        }
                    }
                    "timestamp" -> {
                        //  时间戳使用特殊样式并添加点击注解
                        pushStringAnnotation(tag = "TIMESTAMP", annotation = matchInfo.seconds.toString())
                        withStyle(SpanStyle(color = timestampColor, fontWeight = FontWeight.Medium)) {
                            append(matchInfo.value)
                        }
                        pop()
                    }
                    "url" -> {
                        // URL 高亮
                        pushStringAnnotation(tag = "URL", annotation = matchInfo.value)
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append(matchInfo.value)
                        }
                        pop()
                    }
                }
                lastIndex = matchInfo.range.last + 1
            }
        }
        
        // 添加剩余文本
        if (lastIndex < remainingText.length) {
            append(remainingText.substring(lastIndex))
        }
    }

    val inlineContent = emoteMap.mapValues { (_, url) ->
        InlineTextContent(
            Placeholder(width = 1.4.em, height = 1.4.em, placeholderVerticalAlign = PlaceholderVerticalAlign.Center)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // [新增] 使用 SelectionContainer 包裹文本以支持滑动选择复制
    SelectionContainer {
        //  使用 Text + pointerInput 实现带表情的可点击文本
        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        
        Text(
            text = annotatedString,
            inlineContent = inlineContent,
            fontSize = fontSize,
            color = color,
            lineHeight = (fontSize.value * 1.5).sp,
            maxLines = maxLines,
            onTextLayout = { textLayoutResult = it },
            //  [修复] 添加 padding 确保点击区域足够大
            modifier = Modifier.then(
                if (onTimestampClick != null || onUrlClick != null) {
                    Modifier.pointerInput(annotatedString) {
                        detectTapGestures { offset ->
                            textLayoutResult?.let { layoutResult ->
                                val position = layoutResult.getOffsetForPosition(offset)
                                //  [修复] 扩大搜索范围，允许一定的点击容差
                                val searchStart = maxOf(0, position - 1)
                                val searchEnd = minOf(annotatedString.length, position + 1)
                                
                                // 优先检测 URL 点击
                                annotatedString.getStringAnnotations(
                                    tag = "URL",
                                    start = searchStart,
                                    end = searchEnd
                                ).firstOrNull()?.let { annotation ->
                                    onUrlClick?.invoke(annotation.item)
                                    return@detectTapGestures
                                }

                                // 检测时间戳点击
                                annotatedString.getStringAnnotations(
                                    tag = "TIMESTAMP", 
                                    start = searchStart, 
                                    end = searchEnd
                                )
                                    .firstOrNull()?.let { annotation ->
                                        val seconds = annotation.item.toLongOrNull() ?: 0L
                                        onTimestampClick?.invoke(seconds * 1000)  // 转换为毫秒
                                    }
                            }
                        }
                    }
                } else Modifier
            )
            // .copyOnLongPress(text, "评论内容") // 移除自定义长按复制，使用系统原生的选择复制
        )
    }
}

/**
 *  [兼容] 旧版 EmojiText (保持向后兼容)
 */
@Composable
fun EmojiText(
    text: String,
    fontSize: TextUnit,
    color: Color = MaterialTheme.colorScheme.onSurface,
    emoteMap: Map<String, String>
) {
    RichCommentText(
        text = text,
        fontSize = fontSize,
        color = color,
        emoteMap = emoteMap,
        onTimestampClick = null
    )
}

//  [重构] 等级标签 - iOS 风格 (简约文字)
@Composable
fun LevelTag(level: Int) {
    val textColor = when {
        level >= 6 -> Color(0xFFFF6699)
        level >= 5 -> Color(0xFFFF9500)
        level >= 4 -> Color(0xFF22C3AA)
        level >= 3 -> Color(0xFF7BC549)
        level >= 2 -> Color(0xFF5EAADE)
        else -> Color(0xFF969696)
    }
    
    Text(
        text = "Lv.$level",
        fontSize = 11.sp, // Slightly larger for readability
        fontWeight = FontWeight.SemiBold, // Less heavy than Bold
        color = textColor
    )
}

fun formatTime(timestamp: Long): String {
    val date = Date(timestamp * 1000)
    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault()) // Shortened date format for iOS style
    return sdf.format(date)
}

//  UP 标签 - iOS Pill Style
@Composable
fun UpTag() {
    Surface(
        color = Color(0xFFFF6699),
        shape = CircleShape, // Capsule/Pill shape
        modifier = Modifier
            .height(16.dp)
            .wrapContentWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .fillMaxHeight()
        ) {
            Text(
                text = "UP",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 9.sp
            )
        }
    }
}

//  置顶标签 - iOS Pill Style
@Composable
fun PinnedTag() {
    Surface(
        color = Color(0xFFFFA500),
        shape = CircleShape,
        modifier = Modifier.height(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = "置顶",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

//  评论图片网格组件 - 支持 GIF 动画
//  [优化] 更新为匹配动态页面的视觉风格
@Composable
fun CommentPictures(
    pictures: List<ReplyPicture>,
    onImageClick: (List<String>, Int, Rect?) -> Unit
) {
    //  获取高质量图片URL（移除分辨率限制参数）
    val imageUrls = remember(pictures) {
        pictures.map { pic ->
            var url = pic.imgSrc
            // 修复协议
            if (url.startsWith("//")) {
                url = "https:$url"
            } else if (url.startsWith("http://")) {
                url = url.replace("http://", "https://")
            }
            //  移除尺寸参数以获取原图（避免模糊）
            if (url.contains("@")) {
                url = url.substringBefore("@")
            }
            url
        }
    }
    val context = LocalContext.current
    val totalCount = pictures.size  //  [优化] 保存总图片数用于角标显示
    
    //  GIF 图片加载器
    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }
    
    // 检测是否是 GIF
    fun isGif(url: String) = url.contains(".gif", ignoreCase = true)
    
    // 根据图片数量选择不同的布局
    when (pictures.size) {
        1 -> {
            // 单张图片：保持原始比例，限制最大尺寸
            val pic = pictures[0]
            //  [优化] 更好的比例计算
            val aspectRatio = if (pic.imgHeight > 0 && pic.imgWidth > 0) {
                (pic.imgWidth.toFloat() / pic.imgHeight.toFloat()).coerceIn(0.5f, 2f)
            } else {
                1.33f  // 默认 4:3 比例
            }
            var imageRect by remember { mutableStateOf<Rect?>(null) }
            
            Box(
                modifier = Modifier
                    .widthIn(max = 220.dp)  //  [优化] 增大最大宽度
                    .heightIn(max = 220.dp)
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(12.dp))  //  [优化] 更大圆角 8dp → 12dp
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .onGloballyPositioned { coordinates ->
                        imageRect = coordinates.boundsInWindow()
                    }
                    .clickable { onImageClick(imageUrls, 0, imageRect) }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrls[0])
                        .size(coil.size.Size.ORIGINAL)  //  强制加载原图，避免模糊
                        .addHeader("Referer", "https://www.bilibili.com/")  //  必需
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    imageLoader = gifImageLoader,  //  支持 GIF 和其他格式
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        else -> {
            // 多张图片：网格布局
            val displayItems = pictures.take(9)  //  [优化] 最多显示9张
            val columns = when {
                displayItems.size <= 4 -> 2
                else -> 3
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {  //  [优化] 增加间距 4dp → 6dp
                displayItems.chunked(columns).forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { colIndex, pic ->
                            val globalIndex = rowIndex * columns + colIndex
                            var imageRect by remember { mutableStateOf<Rect?>(null) }
                            
                            Box(
                                modifier = Modifier
                                    .size(85.dp)  //  [优化] 增大尺寸 80dp → 85dp
                                    .clip(RoundedCornerShape(10.dp))  //  [优化] 更大圆角 6dp → 10dp
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .onGloballyPositioned { coordinates ->
                                        imageRect = coordinates.boundsInWindow()
                                    }
                                    .clickable { onImageClick(imageUrls, globalIndex, imageRect) },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrls[globalIndex])
                                        .size(coil.size.Size.ORIGINAL)  //  强制加载原图，避免模糊
                                        .addHeader("Referer", "https://www.bilibili.com/")  //  必需
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    imageLoader = gifImageLoader,  //  支持 GIF
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                //  [新增] 最后一张图片显示多图角标（如 +3）
                                if (globalIndex == displayItems.size - 1 && totalCount > 9) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "+${totalCount - 9}",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
