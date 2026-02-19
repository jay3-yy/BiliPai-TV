// 文件路径: feature/video/danmaku/DanmakuConfig.kt
package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import com.bytedance.danmaku.render.engine.control.DanmakuConfig as EngineConfig

/**
 * 弹幕配置管理
 * 
 * 管理弹幕的样式、速度、透明度等设置
 * 适配 ByteDance DanmakuRenderEngine
 */
class DanmakuConfig {
    
    // 弹幕开关
    var isEnabled = true
    
    // 透明度 (0.0 - 1.0)
    var opacity = 0.85f
    
    // 字体缩放 (0.5 - 2.0)
    var fontScale = 1.0f
    
    // 滚动速度因子 (数值越大弹幕越慢)
    var speedFactor = 1.0f
    
    // 显示区域比例 (0.25, 0.5, 0.75, 1.0)
    var displayAreaRatio = 0.5f
    
    // [问题9修复] 描边设置
    var strokeEnabled = true  // 默认开启描边
    var strokeWidth = 3f  // 描边宽度（像素）
    
    // [新增] 合并重复弹幕
    var mergeDuplicates = true

    // [新增] 类型屏蔽（与 B 站 blockxxx 语义对齐，true=显示/不屏蔽）
    var allowScroll = true
    var allowTop = true
    var allowBottom = true
    var allowColorful = true
    var allowSpecial = true

    // [新增] 智能避脸：根据检测到的人脸动态调整弹幕可显示带
    var smartOcclusionEnabled = false
    var safeBandTopRatio = 0f
    var safeBandBottomRatio = 1f
    
    // 顶部边距（像素）
    var topMarginPx = 0
    
    /**
     * 应用配置到 DanmakuRenderEngine 的 DanmakuConfig
     * 
     * DanmakuRenderEngine 的配置结构:
     * - config.text: TextConfig (size, color, strokeWidth, strokeColor)
     * - config.scroll: ScrollLayerConfig (moveTime, lineHeight, lineMargin, margin)
     * - config.common: CommonConfig (alpha, bufferSize, bufferDiscardRule)
     */
    fun applyTo(engineConfig: EngineConfig, viewHeight: Int = 0) {
        engineConfig.apply {
            // 通用配置 - 透明度 (0-255 Int)
            common.alpha = (opacity * 255).toInt()
            
            // 文字配置 - 字体大小 (增大基准值以提高可见性)
            text.size = 42f * fontScale
            
            // [问题9修复] 描边配置 - 提高弹幕可见性
            if (strokeEnabled) {
                text.strokeWidth = strokeWidth
                text.strokeColor = android.graphics.Color.BLACK  // 黑色描边
            } else {
                text.strokeWidth = 0f
            }
            
            // 滚动层配置
            // moveTime: 弹幕滚过屏幕的时间（毫秒），越大越慢
            // speedFactor > 1 表示更慢（更长的 moveTime）
            // 基准值 5000ms，speedFactor=1 时 5000ms，speedFactor=2 时 10000ms
            val baseTime = 5000L
            scroll.moveTime = (baseTime * speedFactor).toLong().coerceIn(2000L, 10000L)

            val activeBand = resolveActiveDisplayBand(displayAreaRatio)
            val visibleHeightPx = if (viewHeight > 0) {
                (viewHeight * activeBand.heightRatio).coerceAtLeast(0f)
            } else {
                0f
            }

            // [修复] 显示区域控制：通过 lineCount + marginTop 约束弹幕轨道
            val maxLines = getMaxLines(
                visibleHeightPx = visibleHeightPx,
                areaRatioHint = activeBand.heightRatio,
                fontSize = text.size,
                strokeWidth = text.strokeWidth
            )
            scroll.lineCount = maxLines

            val topMargin = if (viewHeight > 0) (viewHeight * activeBand.topRatio) else 0f
            val bottomInset = if (viewHeight > 0) (viewHeight * (1f - activeBand.bottomRatio)) else 0f
            topMarginPx = topMargin.toInt()
            scroll.marginTop = topMargin
            top.marginTop = topMargin
            bottom.marginBottom = bottomInset

            // 顶部/底部弹幕的轨道数量跟随可见区高度，避免挤占人脸区
            val pinnedLineCount = (maxLines / 2).coerceAtLeast(1)
            top.lineCount = pinnedLineCount
            bottom.lineCount = pinnedLineCount
            
            android.util.Log.w(
                "DanmakuConfig",
                " Applied: opacity=$opacity, fontSize=${text.size}, moveTime=${scroll.moveTime}ms, " +
                    "displayArea=$displayAreaRatio, band=${activeBand.topRatio}-${activeBand.bottomRatio}, " +
                    "maxLines=$maxLines (h=$viewHeight, visiblePx=$visibleHeightPx, marginTop=$topMargin)"
            )
        }
    }
    
    /**
     * 根据显示区域比例计算最大行数
     *  [修复] 不能返回 Int.MAX_VALUE，否则弹幕引擎会尝试为海量行分配内存导致 OOM
     */
    /**
     * 根据显示区域比例计算最大行数
     * [修复] 动态计算：基于视图高度和行高
     */
    private fun getMaxLines(
        visibleHeightPx: Float,
        areaRatioHint: Float,
        fontSize: Float,
        strokeWidth: Float
    ): Int {
        if (visibleHeightPx <= 0f) {
             // 视图高度未知时使用兜底行数，避免仅显示一行
             return resolveDanmakuFallbackMaxLines(areaRatioHint)
        }
        
        // 估算行高：字体大小 + 描边(上下各半? 通常加上 padding) + 行间距
        // 引擎内部通常会有一定的 lineMargin (默认为 0 或很小)
        // 假设行高约为 字体大小 + 描边 + 4dp 间距
        val estimatedLineHeight = fontSize + (if (strokeEnabled) strokeWidth else 0f) + 12f // 12f 为估算的间距buffer
        
        val totalLines = (visibleHeightPx / estimatedLineHeight).toInt()
        val visibleLines = totalLines
        val minLines = resolveDanmakuMinimumVisibleLines(areaRatioHint)

        // 竖屏小播放器场景下避免退化成 1 行
        return visibleLines.coerceAtLeast(minLines).also {
             android.util.Log.i("DanmakuConfig", "DisplayArea: visibleHeight=$visibleHeightPx, fontSize=$fontSize, ratio=$areaRatioHint -> total=$totalLines, visible=$it")
        }
    }

    private fun resolveActiveDisplayBand(defaultArea: Float): DanmakuDisplayBand {
        val fallback = DanmakuDisplayBand(0f, defaultArea.coerceIn(0.25f, 1f))
        if (!smartOcclusionEnabled) return fallback

        val requested = DanmakuDisplayBand(
            topRatio = safeBandTopRatio,
            bottomRatio = safeBandBottomRatio
        ).normalized()
        if (requested.heightRatio < 0.12f) return fallback
        return requested
    }
    
    companion object {
        /**
         * 获取状态栏高度（像素）
         */
        fun getStatusBarHeight(context: Context): Int {
            val resourceId = context.resources.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
            return if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId)
            } else {
                (24 * context.resources.displayMetrics.density).toInt()
            }
        }
    }
}

internal fun resolveDanmakuMinimumVisibleLines(displayAreaRatio: Float): Int {
    return when {
        displayAreaRatio <= 0.25f -> 2
        displayAreaRatio <= 0.5f -> 3
        displayAreaRatio <= 0.75f -> 5
        else -> 6
    }
}

internal fun resolveDanmakuFallbackMaxLines(displayAreaRatio: Float): Int {
    return when {
        displayAreaRatio <= 0.25f -> 4
        displayAreaRatio <= 0.5f -> 8
        displayAreaRatio <= 0.75f -> 12
        else -> 16
    }
}
