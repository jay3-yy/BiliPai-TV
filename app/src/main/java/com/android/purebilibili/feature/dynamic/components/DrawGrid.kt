// 文件路径: feature/dynamic/components/DrawGrid.kt
package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.android.purebilibili.data.model.response.DrawItem
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp

/**
 *  图片九宫格V2（支持GIF + 点击预览）
 *  🎨 [优化] 更大圆角、单图大尺寸、多图角标
 *  📍 [新增] 支持返回图片位置用于展开动画
 */
@Composable
fun DrawGridV2(
    items: List<DrawItem>,
    gifImageLoader: ImageLoader,
    onImageClick: (Int, Rect?) -> Unit = { _, _ -> }  //  [修改] 图片点击回调，新增 Rect 参数
) {
    if (items.isEmpty()) return
    
    val context = LocalContext.current
    val totalCount = items.size  //  保存总图片数
    val displayItems = items.take(9)
    val columns = when {
        displayItems.size == 1 -> 1
        displayItems.size <= 4 -> 2
        else -> 3
    }
    
    //  [优化] 单图时保持原始比例，但限制最大高度
    val singleImageRatio = if (displayItems.size == 1 && displayItems[0].width > 0 && displayItems[0].height > 0) {
        (displayItems[0].width.toFloat() / displayItems[0].height.toFloat()).coerceIn(0.6f, 2f)
    } else {
        1.33f  //  默认 4:3 比例
    }
    
    var globalIndex = 0
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {  //  [优化] 增加间距
        displayItems.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)  //  [优化] 增加间距
            ) {
                row.forEach { item ->
                    val currentIndex = globalIndex++
                    val imageUrl = remember(item.src) {
                        val rawSrc = item.src.trim()
                        when {
                            rawSrc.startsWith("https://") -> rawSrc
                            rawSrc.startsWith("http://") -> rawSrc.replace("http://", "https://")
                            rawSrc.startsWith("//") -> "https:$rawSrc"
                            rawSrc.isNotEmpty() -> "https://$rawSrc"
                            else -> ""
                        }
                    }
                    
                    //  [优化] 单图使用原始比例，多图使用正方形
                    val aspectRatio = if (displayItems.size == 1) singleImageRatio else 1f
                    val isGif = imageUrl.endsWith(".gif", ignoreCase = true)
                    //  [优化] 单图占满宽度，多图均分
                    val imageModifier = if (displayItems.size == 1) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.weight(1f)
                    }
                    
                    //  [新增] 存储图片位置
                    var imageRect by remember { mutableStateOf<Rect?>(null) }
                    
                    Box(
                        modifier = imageModifier
                            .aspectRatio(aspectRatio)
                            .clip(RoundedCornerShape(12.dp))  //  [优化] 更大圆角 8dp → 12dp
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .onGloballyPositioned { coordinates ->
                                imageRect = coordinates.boundsInWindow()
                            }
                            .clickable { onImageClick(currentIndex, imageRect) },  //  [修改] 传递位置信息
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(imageUrl)
                                    .addHeader("Referer", "https://www.bilibili.com/")
                                    .crossfade(!isGif)
                                    .build(),
                                imageLoader = if (isGif) gifImageLoader else ImageLoader(context),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                CupertinoIcons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = Color.Gray.copy(0.5f)
                            )
                        }
                        
                        //  [新增] 最后一张图片显示多图角标（如 +3）
                        if (currentIndex == displayItems.size - 1 && totalCount > 9) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${totalCount - 9}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


