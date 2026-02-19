package com.android.purebilibili.feature.video.danmaku

/**
 * 高级弹幕数据模型 (Mode 7)
 * 
 * 用于描述 Bilibili 高级弹幕 (BAS - Bilibili Animation Script 的简化版 Mode 7)
 * 此类弹幕不通过 DanmakuRenderEngine 渲染，而是由 Compose Overlay 独立渲染。
 */
data class AdvancedDanmakuData(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val startTimeMs: Long,
    val durationMs: Long,
    
    // 初始位置 (0.0 ~ 1.0, 相对于视频画面)
    val startX: Float,
    val startY: Float,
    
    // 目标位置 (如果只是定位弹幕，endX/Y 可能等于 startX/Y)
    val endX: Float = startX,
    val endY: Float = startY,
    
    // 样式属性
    val fontSize: Float = 25f,
    val color: Int = 0xFFFFFF, // RGB, alpha will be handled separately if needed
    val alpha: Float = 1.0f,
    
    // 动画曲线 (Linear, EaseIn, EaseOut 等)
    // 简化处理：目前默认为 Linear
    val motionType: String = "Linear",
    
    // 旋转 (角度 0~360)
    val rotateZ: Float = 0f,
    val rotateY: Float = 0f,

    // [新增] 高能弹幕计数动画属性
    // 如果 maxCount > 1，渲染时会根据时间动态显示 "x1" -> "x{maxCount}" 的增长过程
    val maxCount: Int = 0,
    // 计数增长的持续时间 (毫秒)，在这段时间内数字从 1 涨到 maxCount
    // 剩余的 durationMs - accumulationDurationMs 时间用于展示最终结果
    val accumulationDurationMs: Long = 0L
) {
    /**
     * 判断当前时间是否应该显示此弹幕
     */
    fun isActive(currentPos: Long): Boolean {
        return currentPos >= startTimeMs && currentPos <= startTimeMs + durationMs
    }
    
    /**
     * 计算当前进度的插值 (0.0 ~ 1.0)
     */
    fun getProgress(currentPos: Long): Float {
        if (currentPos < startTimeMs) return 0f
        if (currentPos > startTimeMs + durationMs) return 1f
        return (currentPos - startTimeMs).toFloat() / durationMs
    }
}

/**
 * 弹幕解析结果
 * 包含标准引擎弹幕和高级弹幕
 */
data class ParsedDanmaku(
    val standardList: List<com.bytedance.danmaku.render.engine.data.DanmakuData>,
    val advancedList: List<AdvancedDanmakuData>
)
