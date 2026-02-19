package com.android.purebilibili.feature.dynamic.components

private const val LAYOUT_PROGRESS_MIN = -0.08f
private const val LAYOUT_PROGRESS_MAX = 1.02f
private const val FALLBACK_START_SCALE = 0.96f

internal data class ImagePreviewTransitionFrame(
    val layoutProgress: Float,
    val visualProgress: Float,
    val cornerRadiusDp: Float,
    val fallbackScale: Float
)

internal data class ImagePreviewDismissMotion(
    val overshootTarget: Float,
    val settleTarget: Float
)

internal fun resolveImagePreviewTransitionFrame(
    rawProgress: Float,
    hasSourceRect: Boolean,
    sourceCornerRadiusDp: Float
): ImagePreviewTransitionFrame {
    val layoutProgress = rawProgress.coerceIn(LAYOUT_PROGRESS_MIN, LAYOUT_PROGRESS_MAX)
    val visualProgress = rawProgress.coerceIn(0f, 1f)
    val cornerRadiusDp = if (hasSourceRect) {
        sourceCornerRadiusDp.coerceAtLeast(0f)
    } else {
        0f
    }
    val fallbackScale = lerpFloat(FALLBACK_START_SCALE, 1f, visualProgress)
    return ImagePreviewTransitionFrame(
        layoutProgress = layoutProgress,
        visualProgress = visualProgress,
        cornerRadiusDp = cornerRadiusDp,
        fallbackScale = fallbackScale
    )
}

internal fun imagePreviewDismissMotion(): ImagePreviewDismissMotion {
    return ImagePreviewDismissMotion(
        overshootTarget = 0f,
        settleTarget = 0f
    )
}

internal fun resolvePredictiveBackAnimationProgress(backGestureProgress: Float): Float {
    val clamped = backGestureProgress.coerceIn(0f, 1f)
    return 1f - clamped
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
