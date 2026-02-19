package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.media3.ui.AspectRatioFrameLayout
import com.bytedance.danmaku.render.engine.DanmakuView

internal class FaceOcclusionDanmakuContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val danmakuView = DanmakuView(context).apply {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private val edgeErasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        alpha = 132
    }
    private val coreErasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        alpha = 255
    }
    private val innerPath = Path()
    private val outerPath = Path()
    private val tmpRect = RectF()
    private val tmpOuterRect = RectF()
    private var masks: List<FaceOcclusionVisualMask> = emptyList()
    private var sourceVideoWidth: Int = 0
    private var sourceVideoHeight: Int = 0
    private var sourceResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT

    init {
        setWillNotDraw(false)
        addView(danmakuView)
    }

    fun danmakuView(): DanmakuView = danmakuView

    fun setMasks(nextMasks: List<FaceOcclusionVisualMask>) {
        if (masks == nextMasks) return
        masks = nextMasks
        invalidate()
    }

    fun setVideoViewport(
        videoWidth: Int,
        videoHeight: Int,
        resizeMode: Int
    ) {
        if (
            sourceVideoWidth == videoWidth &&
                sourceVideoHeight == videoHeight &&
                sourceResizeMode == resizeMode
        ) {
            return
        }
        sourceVideoWidth = videoWidth
        sourceVideoHeight = videoHeight
        sourceResizeMode = resizeMode
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (masks.isEmpty() || width <= 0 || height <= 0) {
            super.dispatchDraw(canvas)
            return
        }

        val checkpoint = canvas.saveLayer(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            null
        )
        super.dispatchDraw(canvas)
        drawFaceMasks(canvas)
        canvas.restoreToCount(checkpoint)
    }

    private fun drawFaceMasks(canvas: Canvas) {
        val viewport = resolveVideoContentRect(
            containerWidth = width,
            containerHeight = height,
            videoWidth = sourceVideoWidth,
            videoHeight = sourceVideoHeight,
            resizeMode = sourceResizeMode
        )
        val viewportWidth = viewport.width.coerceAtLeast(1f)
        val viewportHeight = viewport.height.coerceAtLeast(1f)
        val edgeExpansionRatio = resolveMaskEdgeExpansionRatio(
            viewportWidthPx = viewportWidth,
            viewportHeightPx = viewportHeight,
            featherPx = 8f
        )

        masks.forEach { mask ->
            if (mask.polygonPoints.size >= 5) {
                val expandedPoints = expandNormalizedPolygon(mask.polygonPoints, edgeExpansionRatio)
                buildPolygonPath(outerPath, expandedPoints, viewport.left, viewport.top, viewportWidth, viewportHeight)
                buildPolygonPath(innerPath, mask.polygonPoints, viewport.left, viewport.top, viewportWidth, viewportHeight)
                canvas.drawPath(outerPath, edgeErasePaint)
                canvas.drawPath(innerPath, coreErasePaint)
                return@forEach
            }

            val rect = mask.fallbackRect
            tmpRect.set(
                viewport.left + rect.leftRatio * viewportWidth,
                viewport.top + rect.topRatio * viewportHeight,
                viewport.left + rect.rightRatio * viewportWidth,
                viewport.top + rect.bottomRatio * viewportHeight
            )
            if (tmpRect.width() <= 0f || tmpRect.height() <= 0f) return@forEach

            val outerRect = rect.expanded(edgeExpansionRatio)
            tmpOuterRect.set(
                viewport.left + outerRect.leftRatio * viewportWidth,
                viewport.top + outerRect.topRatio * viewportHeight,
                viewport.left + outerRect.rightRatio * viewportWidth,
                viewport.top + outerRect.bottomRatio * viewportHeight
            )
            if (tmpOuterRect.width() <= 0f || tmpOuterRect.height() <= 0f) return@forEach

            val innerCornerRadiusX = tmpRect.width() * 0.46f
            val innerCornerRadiusY = tmpRect.height() * 0.52f
            val outerCornerRadiusX = tmpOuterRect.width() * 0.48f
            val outerCornerRadiusY = tmpOuterRect.height() * 0.54f

            outerPath.reset()
            outerPath.addRoundRect(
                tmpOuterRect,
                outerCornerRadiusX,
                outerCornerRadiusY,
                Path.Direction.CW
            )
            innerPath.reset()
            innerPath.addRoundRect(
                tmpRect,
                innerCornerRadiusX,
                innerCornerRadiusY,
                Path.Direction.CW
            )
            canvas.drawPath(outerPath, edgeErasePaint)
            canvas.drawPath(innerPath, coreErasePaint)
        }
    }

    private fun buildPolygonPath(
        path: Path,
        points: List<NormalizedPoint>,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        path.reset()
        points.forEachIndexed { index, point ->
            val px = left + point.xRatio * width
            val py = top + point.yRatio * height
            if (index == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, py)
            }
        }
        path.close()
    }
}
