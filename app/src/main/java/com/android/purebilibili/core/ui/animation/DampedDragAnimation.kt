// 文件路径: core/ui/animation/DampedDragAnimation.kt
package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * 🌊 阻尼拖拽动画状态
 * 
 * 实现类似 LiquidBottomTabs 的手势跟随效果：
 * - 拖拽时平滑跟随手指
 * - 释放后弹回吸附到最近选项
 * - 支持速度感知的弹性形变
 */
class DampedDragAnimationState(
    initialIndex: Int,
    private val itemCount: Int,
    private val scope: CoroutineScope,
    private val onIndexChanged: (Int) -> Unit
) {
    /** 当前动画值（浮点索引，用于平滑过渡） */
    private val animatable = Animatable(initialIndex.toFloat())
    
    /** 当前动画位置 */
    val value: Float get() = animatable.value
    
    /** 当前速度（用于形变效果） */
    val velocity: Float get() = animatable.velocity
    
    /** 是否正在拖拽 */
    var isDragging by mutableStateOf(false)
        private set
    
    /** 拖拽时的缩放比例 */
    val scale: Float get() = if (isDragging) 1.1f else 1f
    
    /** 目标索引（释放后吸附的目标） */
    var targetIndex = initialIndex
        private set
    
    /** 动画是否正在运行 */
    val isRunning: Boolean get() = animatable.isRunning

    /**
     * 处理拖拽事件
     * @param dragAmountPx 拖拽像素距离
     * @param itemWidthPx 单个项目宽度（像素）
     */
    fun onDrag(dragAmountPx: Float, itemWidthPx: Float) {
        if (!isDragging) isDragging = true
        
        // [优化] 橡皮筋阻尼物理：
        val currentValue = animatable.value
        val isOverscrolling = currentValue < 0f || currentValue > (itemCount - 1).toFloat()
        
        // [调整] 提升灵敏度系数 (0.6 -> 1.0) 确保完全跟手
        val baseResistance = 1.0f 
        val dragResistance = if (isOverscrolling) 0.3f else baseResistance
        
        val deltaIndex = (dragAmountPx / itemWidthPx) * dragResistance
        
        // 允许边缘回弹：放宽限制范围
        val newValue = (animatable.value + deltaIndex).fastCoerceIn(-0.5f, (itemCount - 0.5f))
        
        scope.launch {
            animatable.snapTo(newValue)
        }
    }
    
    /**
     * 立即跳转到指定位置（无动画）
     */
    fun snapTo(targetValue: Float) {
        // 更新目标索引以防止 offset 累积误差
        targetIndex = targetValue.roundToInt().coerceIn(0, itemCount - 1)
        scope.launch {
            animatable.snapTo(targetValue)
        }
    }

    /**
     * 处理拖拽结束 (带速度感知)
     * @param velocityX 水平速度 (px/s)
     * @param itemWidthPx 项目宽度 (px)
     */
    fun onDragEnd(velocityX: Float, itemWidthPx: Float) {
        isDragging = false
        
        val currentValue = animatable.value
        
        // [核心优化] 基于速度的意图判断 (Fling Logic)
        // 1. 计算这一帧的归一化速度 (items/sec)
        val velocityItems = velocityX / itemWidthPx
        
        // 2. 预测终点 (Projected End Point)
        // 简单的投射：当前位置 + 速度 * 时间常数 (模拟滑行)
        // 使用 0.2s 作为预测时间窗
        val projectedValue = currentValue + velocityItems * 0.2f
        
        // 3. 确定目标索引
        // 如果速度很快 (> 1 item/s)，则强制切换到下一个/上一个
        // 否则回弹到最近的整数
        var nextIndex = projectedValue.roundToInt()
        
        // 4. 限制跳跃范围：一次只允许跳一格 (防止飞太远)
        // 获取当前基础索引 (int part)
        val baseIndex = currentValue.roundToInt()
        
        // 强制约束 nextIndex 在 baseIndex ± 1 范围内 (前提是确实发生了显著移动)
        if (abs(nextIndex - baseIndex) > 1) {
            nextIndex = baseIndex + (nextIndex - baseIndex).sign
        }
        
        targetIndex = nextIndex.coerceIn(0, itemCount - 1)
        
        // 如果虽然有速度，但没有跨过阈值且距离目标太远，可能需要额外逻辑 (Optional)
        // 目前 roundToInt 已经处理了 >0.5 的情况，加上速度投射处理了 <0.5 但快速的情况
        
        scope.launch {
            animatable.animateTo(
                targetValue = targetIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.7f,   // [调整] 增加阻尼减少回弹
                    stiffness = 500f       // [调整] 增加刚度提升响应速度
                ),
                initialVelocity = velocityItems // 传递同向速度
                // Animatable velocity 是 float/unit. input 是 px/s. 
                // 我们操作的是 unit. 所以 velocity 应该是 items/s.
                // 注意方向：PointerInput 中 dragAmount 方向与 scroll 相反?
                // 通常 pointer delta > 0 是向右。Item index 增加。
                // 所以 velocityX > 0 -> index 增加。
                // animatable 在 index 空间。
                // 传入 initialVelocity 可能会导致过冲，暂时不传，让 spring 自己处理
            )
            onIndexChanged(targetIndex)
        }
    }
    
    /**
     * 外部更新选中索引（点击选择时）
     */
    fun updateIndex(index: Int) {
        // [修复] 拖拽过程中忽略外部更新，防止动画中断
        if (isDragging) return
        
        // [Fix] Check actual value distance. 
        // If targetIndex matches but we are stuck at an offset (e.g. 2.8 vs 3.0 via snapTo), 
        // we MUST force restart the animation.
        if (index == targetIndex && (isRunning || abs(value - index.toFloat()) < 0.005f)) return
        targetIndex = index
        scope.launch {
            animatable.animateTo(
                targetValue = index.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.7f, 
                    stiffness = 500f
                )
            )
        }
    }
}

/**
 * 创建并记住阻尼拖拽动画状态
 */
@Composable
fun rememberDampedDragAnimationState(
    initialIndex: Int,
    itemCount: Int,
    onIndexChanged: (Int) -> Unit
): DampedDragAnimationState {
    val scope = rememberCoroutineScope()
    val currentOnIndexChanged by rememberUpdatedState(onIndexChanged)
    
    return remember(itemCount) {
        DampedDragAnimationState(
            initialIndex = initialIndex,
            itemCount = itemCount,
            scope = scope,
            onIndexChanged = { currentOnIndexChanged(it) }
        )
    }
}

/**
 * 水平拖拽手势 Modifier (带速度追踪)
 */
fun Modifier.horizontalDragGesture(
    dragState: DampedDragAnimationState,
    itemWidthPx: Float
): Modifier = this.pointerInput(dragState, itemWidthPx) {
    val velocityTracker = VelocityTracker()
    
    awaitPointerEventScope {
        while (true) {
            // [Fix] Allow gesture to start even if child (clickable) consumed the DOWN event
            val down = awaitFirstDown(requireUnconsumed = false)
            velocityTracker.resetTracking()
            velocityTracker.addPosition(down.uptimeMillis, down.position)
            
            // [Fix] Wait for touch slop before claiming the gesture (to distinguish from tap)
            val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                dragState.onDrag(over, itemWidthPx)
            }

            if (dragStart != null) {
                // Drag confirmed
                velocityTracker.addPosition(dragStart.uptimeMillis, dragStart.position)
                
                var isCancelled = false
                
                // Continue handling drag events
                try {
                     horizontalDrag(dragStart.id) { change ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        
                        val dragAmount = change.position.x - change.previousPosition.x
                        dragState.onDrag(dragAmount, itemWidthPx)
                    }
                } catch (e: Exception) {
                    isCancelled = true
                }
                
                // Drag ended
                if (!isCancelled) {
                    val velocity = velocityTracker.calculateVelocity()
                    dragState.onDragEnd(velocity.x, itemWidthPx)
                } else {
                    // Cancelled
                    dragState.onDragEnd(0f, itemWidthPx)
                }
            }
        }
    }
}
