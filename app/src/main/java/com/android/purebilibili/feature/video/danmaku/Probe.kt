package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.android.purebilibili.feature.live.components.DanmakuEmoticonMapper
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.bitmap.BitmapData
import java.util.regex.Pattern

/**
 * 创建支持表情的 BitmapDanmaku
 * 
 * 修复记录:
 * - 添加 Bitmap 最大尺寸限制 (2048px) 防止 OOM
 * - 添加 try-catch 异常处理
 */
fun createBitmapDanmaku(
    context: Context,
    text: String,
    textColor: Int,
    textSize: Float,
    layerType: Int,
    showAtTime: Long,
    onUpdate: () -> Unit
): BitmapData {
    // 1. 解析文本
    val segments = parseSegments(text)
    
    // 2. 测量尺寸
    val paint = TextPaint().apply {
        this.textSize = textSize
        this.color = textColor
        this.isAntiAlias = true
    }
    
    val fontMetrics = paint.fontMetrics
    val textHeight = fontMetrics.descent - fontMetrics.ascent
    // 图片高度稍微大一点 (1.3倍)
    val iconSize = (textSize * 1.3f).toInt()
    val contentHeight = maxOf(textHeight, iconSize.toFloat())
    
    var totalWidth = 0f
    segments.forEach { seg ->
        if (seg.isEmoticon) {
            totalWidth += iconSize + 4 // 图片 + 间距
        } else {
            totalWidth += paint.measureText(seg.content)
        }
    }
    
    // 3. 创建 Bitmap (添加尺寸限制防止 OOM)
    val maxWidth = 2048 // 最大宽度限制
    val widthInt = totalWidth.toInt().coerceIn(1, maxWidth)
    val heightInt = (contentHeight * 1.2f).toInt().coerceIn(1, 256) // 高度也加限制
    
    // 如果文本被截断，记录日志
    if (totalWidth > maxWidth) {
        android.util.Log.w("Probe", "⚠️ Danmaku bitmap truncated: ${totalWidth.toInt()} -> $maxWidth")
    }
    
    val bitmap: Bitmap = try {
        Bitmap.createBitmap(widthInt, heightInt, Bitmap.Config.ARGB_8888)
    } catch (e: OutOfMemoryError) {
        android.util.Log.e("Probe", "❌ OOM creating bitmap, using fallback")
        // 返回一个极小的占位 Bitmap
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
    
    val canvas = Canvas(bitmap)
    
    // 4. 绘制内容
    try {
        drawContent(context, canvas, segments, paint, iconSize, heightInt, onUpdate, bitmap)
    } catch (e: Exception) {
        android.util.Log.e("Probe", "❌ Draw content failed: ${e.message}")
    }
    
    // 5. 创建 BitmapData
    return BitmapData().apply {
        this.bitmap = bitmap
        this.width = widthInt.toFloat()
        this.height = heightInt.toFloat()
        this.showAtTime = showAtTime
        this.layerType = layerType
    }
}

private data class Segment(
    val content: String,
    val isEmoticon: Boolean
)

private val emoticonPattern = Pattern.compile("\\[(.*?)\\]")

private fun parseSegments(text: String): List<Segment> {
    val list = mutableListOf<Segment>()
    val matcher = emoticonPattern.matcher(text)
    var lastIndex = 0
    
    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()
        val key = matcher.group(1) ?: ""
        
        if (start > lastIndex) {
            list.add(Segment(text.substring(lastIndex, start), false))
        }
        
        // Check map
        val currentMap = DanmakuEmoticonMapper.emoticonMap.value
        val mapKey = if (currentMap.containsKey(key)) key else if (currentMap.containsKey("[$key]")) "[$key]" else null
        
        if (mapKey != null) {
            // Store the key to look up URL later
            list.add(Segment(mapKey, true))
        } else {
            list.add(Segment(text.substring(start, end), false))
        }
        
        lastIndex = end
    }
    
    if (lastIndex < text.length) {
        list.add(Segment(text.substring(lastIndex), false))
    }
    return list
}

private fun drawContent(
    context: Context,
    canvas: Canvas,
    segments: List<Segment>,
    paint: TextPaint,
    iconSize: Int,
    canvasHeight: Int,
    onUpdate: () -> Unit,
    targetBitmap: Bitmap // 用于重绘
) {
    // 清除画布 (如果重绘)
    // 但 BitmapData 可能持有引用，所以我们最好重绘到同一个 Bitmap 上
    // 注意: 在 Coil 回调中，我们会在同一个 Canvas 上绘制吗？ No, create new Canvas for the bitmap
    
    val fontMetrics = paint.fontMetrics
    // 基线计算: 垂直居中
    // Icon 在 centerY, Text 基线在 centerY + (descent-ascent)/2 - descent ?
    // 简单点: 
    val centerY = canvasHeight / 2f
    val textBaseLine = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2f
    
    var xOffset = 0f
    
    segments.forEach { seg ->
        if (seg.isEmoticon) {
            val url = DanmakuEmoticonMapper.emoticonMap.value[seg.content]
            if (url != null) {
                // 尝试加载
                val startX = xOffset
                
                // 加载图片
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .target { result ->
                        val loadedBitmap = result.toBitmap()
                        // 重绘！
                        // 注意：这里是异步回调。我们需要重新获取 Canvas (因为 Bitmap 是同一个)
                        val asyncCanvas = Canvas(targetBitmap)
                        // 擦除这块区域？比较难。
                        // 简单策略：直接覆盖绘制。
                        // 计算位置
                        val top = centerY - iconSize / 2f
                        val dstRect = Rect(startX.toInt(), top.toInt(), (startX + iconSize).toInt(), (top + iconSize).toInt())
                        asyncCanvas.drawBitmap(loadedBitmap, null, dstRect, null)
                        
                        // 通知 UI 刷新
                        onUpdate()
                    }
                    .build()
                imageLoader.enqueue(request)
                
                xOffset += iconSize + 4
            } else {
                 canvas.drawText("[${seg.content}]", xOffset, textBaseLine, paint)
                 xOffset += paint.measureText("[${seg.content}]")
            }
        } else {
            canvas.drawText(seg.content, xOffset, textBaseLine, paint)
            xOffset += paint.measureText(seg.content)
        }
    }
}
