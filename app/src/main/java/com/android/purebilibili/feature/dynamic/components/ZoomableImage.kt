package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 可缩放的图片组件
 * - 支持双指缩放
 * - 支持双击放大
 * - 支持长图滑动
 * - 自动处理边界限制
 */
@Composable
fun ZoomableImage(
    model: Any?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onZoomChange: (Float) -> Unit = {},
    onClick: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // 图片原始尺寸
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    // 容器尺寸
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 是否正在加载
    var isLoading by remember { mutableStateOf(true) }
    var isLongImage by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    
    // 双击放大逻辑
    fun onDoubleTap(tapOffset: Offset) {
        if (scale > 1f) {
            // 恢复原大小
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            onZoomChange(1f)
        } else {
            // 放大 2.5 倍
            scale = 2.5f
            
            // 计算偏移量，使点击点居中
            if (containerSize != IntSize.Zero) {
                val centerX = containerSize.width / 2f
                val centerY = containerSize.height / 2f
                
                offsetX = (centerX - tapOffset.x) * (scale - 1)
                offsetY = (centerY - tapOffset.y) * (scale - 1)
                
                // 边界限制
                val fitScale = min(
                    containerSize.width.toFloat() / imageSize.width,
                    containerSize.height.toFloat() / imageSize.height
                )
                val displayWidth = imageSize.width * fitScale * scale
                val displayHeight = imageSize.height * fitScale * scale
                
                val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
                val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)
                
                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
            }
            onZoomChange(scale)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap(it) },
                    onTap = { onClick() }
                )
            }
            .pointerInput(Unit) {
                // 手势监听：缩放 + 拖拽
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    var isMultiTouch = false  // 🔧 [新增] 标记是否为多指手势
                    
                    awaitFirstDown(requireUnconsumed = false)
                    
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        // 🔧 [修复] 检测是否为多指手势
                        if (event.changes.size > 1) {
                            isMultiTouch = true
                        }

                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()

                        if (!pastTouchSlop) {
                            zoom *= zoomChange
                            pan += panChange

                            val centroidSize = event.calculateCentroidSize(useCurrent = false)
                            val zoomMotion = abs(1 - zoom) * centroidSize
                            val panMotion = pan.getDistance()

                            if (zoomMotion > touchSlop ||
                                panMotion > touchSlop
                            ) {
                                pastTouchSlop = true
                            }
                        }

                        if (pastTouchSlop) {
                            val centroid = event.calculateCentroid(useCurrent = false)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                // 处理缩放
                                val oldScale = scale
                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                
                                // 根据缩放中心点调整偏移，保持视觉连贯
                                if (oldScale != scale) {
                                     val zoomFactor = scale / oldScale
                                     val dx = (1 - zoomFactor) * (centroid.x - containerSize.width / 2f - offsetX)
                                     val dy = (1 - zoomFactor) * (centroid.y - containerSize.height / 2f - offsetY)
                                     offsetX += dx
                                     offsetY += dy
                                }
                                
                                // 处理平移
                                offsetX += panChange.x
                                offsetY += panChange.y
                                
                                // 边界限制逻辑
                                if (containerSize != IntSize.Zero && imageSize != IntSize.Zero) {
                                    // 计算图片当前显示的实际尺寸
                                    // 基础缩放比例 (Fit Center)
                                    val fitScale = min(
                                        containerSize.width.toFloat() / imageSize.width,
                                        containerSize.height.toFloat() / imageSize.height
                                    )
                                    
                                    val displayWidth = imageSize.width * fitScale * scale
                                    val displayHeight = imageSize.height * fitScale * scale
                                    
                                    // 计算允许的最大偏移量
                                    // 如果图片显示的尺寸大于容器，允许偏移；否则居中（偏移为0）
                                    val maxOffsetX = max(0f, (displayWidth - containerSize.width) / 2f)
                                    val maxOffsetY = max(0f, (displayHeight - containerSize.height) / 2f)
                                    
                                    offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                }
                                
                                onZoomChange(scale)
                            }
                            
                            // 🔧 [关键修复] 只在以下情况消费事件：
                            // 1. 多指手势（缩放操作）
                            // 2. 图片已缩放（scale > 1）时的拖拽
                            // 这样 scale=1 时的水平滑动可以传递给 HorizontalPager
                            if (isMultiTouch || scale > 1.01f) {
                                event.changes.forEach {
                                    if (it.position != it.previousPosition) {
                                        it.consume()
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                },
            onSuccess = { state ->
                isLoading = false
                val originalSize = state.painter.intrinsicSize
                if (originalSize.width > 0 && originalSize.height > 0) {
                    imageSize = IntSize(originalSize.width.toInt(), originalSize.height.toInt())
                    
                    // 判断是否为长图 (高宽比 > 3 且高度 > 容器高度)
                    isLongImage = originalSize.height / originalSize.width > 3
                    
                    // 初始长图处理：如果不需要双击就直接看长图细节，可以把 scale 设置为 fillWidth 对应的 scale
                    // 但标准的图片预览通常还是先看全图，再放大
                }
            },
            // 使用 Fit 模式确保初始完整显示
            contentScale = ContentScale.Fit
        )
    }
}
