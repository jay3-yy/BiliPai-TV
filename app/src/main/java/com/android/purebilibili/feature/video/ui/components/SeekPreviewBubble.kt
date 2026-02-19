// File: feature/video/ui/components/SeekPreviewBubble.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoshotData

/**
 * 进度条拖动预览气泡
 * 
 * 显示视频缩略图和目标时间，类似 B 站网页版效果
 */
@Composable
fun SeekPreviewBubble(
    videoshotData: VideoshotData?,
    targetPositionMs: Long,
    currentPositionMs: Long,
    durationMs: Long,
    offsetX: Float,            // 水平偏移量 (相对于进度条左端)
    containerWidth: Float,      // 进度条容器宽度
    modifier: Modifier = Modifier
) {
    // 计算气泡位置（限制在容器边界内）
    val bubbleWidth = 160.dp
    val bubbleHeight = 90.dp
    val bubbleWidthPx = with(LocalDensity.current) { bubbleWidth.toPx() }
    val halfBubble = bubbleWidthPx / 2
    
    // 限制气泡水平位置在容器内
    // [修复] 当 containerWidth 小于 bubbleWidth 时（居中显示场景），跳过位置限制
    val clampedOffsetX = if (containerWidth > bubbleWidthPx) {
        offsetX.coerceIn(halfBubble, containerWidth - halfBubble)
    } else {
        halfBubble // 居中显示时，直接使用半宽偏移
    }
    
    val context = LocalContext.current
    
    // 🔧 [修复] 计算当前帧的预览信息
    // 这个值会随着拖动更新，但我们只在图片URL或偏移变化时才重新加载图片
    val currentPreviewInfo = remember(videoshotData, targetPositionMs, durationMs) {
        videoshotData?.getPreviewInfo(targetPositionMs, durationMs)
    }
    
    // 使用 previewInfo 的内容（URL+偏移）作为稳定 key
    // 这样相同的帧不会重复触发图片加载
    val stableImageKey = remember(currentPreviewInfo) {
        currentPreviewInfo?.let { (url, x, y) ->
            "$url-$x-$y"
        }
    }
    

    Box(
        modifier = modifier
            .offset { IntOffset((clampedOffsetX - halfBubble).toInt(), 0) }
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .width(bubbleWidth)
            .height(bubbleHeight)
            .background(Color.Black)
    ) {
        // 1. 视频缩略图 (底层)
        if (currentPreviewInfo != null && videoshotData != null) {
            val (rawImageUrl, spriteOffsetX, spriteOffsetY) = currentPreviewInfo
            
            // 🔧 修复：B站 URL 可能以 // 开头，需要补全 https:
            val imageUrl = if (rawImageUrl.startsWith("//")) {
                "https:$rawImageUrl"
            } else {
                rawImageUrl
            }
            
            val thumbWidthPx = videoshotData.img_x_size
            val thumbHeightPx = videoshotData.img_y_size
            
            // 🔧 [关键修复] 使用 rememberAsyncImagePainter
            // 这个 painter 会在 stableImageKey 变化时才重新加载
            // 🔧 [最终修复] 性能优化方案
            // 1. Coil 只负责加载整张雪碧图 (只加载一次，缓存 key 只跟 URL 有关)
            val painter = coil.compose.rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(coil.size.Size.ORIGINAL) // 加载原图
                    .crossfade(false)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build()
            )
            
            // 2. 加载状态处理
            val painterState = painter.state
            if (painterState is coil.compose.AsyncImagePainter.State.Loading) {
                 Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                    Text("...", color = Color.White, fontSize = 12.sp)
                }
            } else if (painterState is coil.compose.AsyncImagePainter.State.Error) {
                Box(Modifier.fillMaxSize().background(Color.Red), contentAlignment = Alignment.Center) {
                    Text("×", color = Color.White, fontSize = 16.sp)
                }
            } else if (painterState is coil.compose.AsyncImagePainter.State.Success) {
                // 3. 使用 drawWithContent 手动裁剪绘制
                // 这样即使 offset 变化，也不需要重新加载图片，只是重绘 Canvas
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val drawable = painterState.result.drawable
                    val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    
                    if (bitmap != null) {
                        val inputWidth = bitmap.width
                        val inputHeight = bitmap.height
                        
                         // 预期总宽高
                        val expectedWidth = thumbWidthPx * videoshotData.img_x_len
                        val expectedHeight = thumbHeightPx * videoshotData.img_y_len
                        
                        // 计算缩放比例 (实际 / 预期)
                        val scaleX = inputWidth.toFloat() / expectedWidth.toFloat()
                        val scaleY = inputHeight.toFloat() / expectedHeight.toFloat()
                        
                        // 计算实际裁剪区域
                        val realOffsetX = (spriteOffsetX * scaleX).toInt()
                        val realOffsetY = (spriteOffsetY * scaleY).toInt()
                        val realCropWidth = (thumbWidthPx * scaleX).toInt()
                        val realCropHeight = (thumbHeightPx * scaleY).toInt()
                        
                        // 源矩形 (裁剪区域)
                        val srcRect = android.graphics.Rect(
                            realOffsetX, 
                            realOffsetY, 
                            realOffsetX + realCropWidth, 
                            realOffsetY + realCropHeight
                        )
                        
                        // 目标矩形 (View 大小)
                        val dstOffset = IntOffset.Zero
                        val dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        
                        // 绘制
                        drawImage(
                            image = bitmap.asImageBitmap(), // 需要 import androidx.compose.ui.graphics.asImageBitmap
                            srcOffset = IntOffset(realOffsetX, realOffsetY),
                            srcSize = IntSize(realCropWidth, realCropHeight),
                            dstOffset = dstOffset,
                            dstSize = dstSize
                        )
                    }
                }
            }
        } else {
            // Loading 状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "预览加载中...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
        
        // 2. 底部渐变遮罩 (中间层) - 仅在文字区域
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // 3. 时间标签 (顶层)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 目标时间
            Text(
                text = FormatUtils.formatDuration((targetPositionMs / 1000).toInt()),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 4f
                    )
                )
            )
            
            // 时间差
            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000
            if (deltaSeconds != 0L) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (deltaSeconds > 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                    color = if (deltaSeconds > 0) Color(0xFF81C784) else Color(0xFFE57373), // 稍微调亮一点颜色以在黑底上更清晰
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

/**
 * 简化版预览气泡（仅显示时间，无缩略图）
 * 
 * 用于无 videoshot 数据时的降级显示
 */
@Composable
fun SeekPreviewBubbleSimple(
    targetPositionMs: Long,
    currentPositionMs: Long,
    offsetX: Float,
    containerWidth: Float,
    modifier: Modifier = Modifier
) {
    val bubbleWidth = 100.dp
    val bubbleWidthPx = with(LocalDensity.current) { bubbleWidth.toPx() }
    val halfBubble = bubbleWidthPx / 2
    val clampedOffsetX = offsetX.coerceIn(halfBubble, containerWidth - halfBubble)
    
    Box(
        modifier = modifier
            .offset { IntOffset((clampedOffsetX - halfBubble).toInt(), 0) }
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 目标时间
            Text(
                text = FormatUtils.formatDuration((targetPositionMs / 1000).toInt()),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            // 时间差
            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000
            if (deltaSeconds != 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (deltaSeconds > 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                    color = if (deltaSeconds > 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    fontSize = 12.sp
                )
            }
        }
    }
}
