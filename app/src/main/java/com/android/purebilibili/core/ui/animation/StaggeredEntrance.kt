package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 瀑布流/交错式入场动画
 * Staggered entrance animation modifier.
 *
 * @param index The index of the item in the list.
 * @param visible Whether the item should be visible.
 * @param offsetDistance The initial Y offset distance.
 */
data class StaggeredEntranceMotionPolicy(
    val delayStepMs: Int,
    val maxDelayMs: Int,
    val alphaDurationMs: Int,
    val translationDurationMs: Int,
    val scaleDurationMs: Int,
    val initialScale: Float,
    val offsetFactor: Float
)

fun resolveStaggeredEntranceMotionPolicy(
    motionTier: MotionTier
): StaggeredEntranceMotionPolicy {
    return when (motionTier) {
        MotionTier.Reduced -> StaggeredEntranceMotionPolicy(
            delayStepMs = 10,
            maxDelayMs = 80,
            alphaDurationMs = 180,
            translationDurationMs = 230,
            scaleDurationMs = 200,
            initialScale = 0.98f,
            offsetFactor = 0.35f
        )

        MotionTier.Enhanced -> StaggeredEntranceMotionPolicy(
            delayStepMs = 28,
            maxDelayMs = 240,
            alphaDurationMs = 340,
            translationDurationMs = 520,
            scaleDurationMs = 460,
            initialScale = 0.9f,
            offsetFactor = 1.15f
        )

        MotionTier.Normal -> StaggeredEntranceMotionPolicy(
            delayStepMs = 35,
            maxDelayMs = 260,
            alphaDurationMs = 300,
            translationDurationMs = 450,
            scaleDurationMs = 400,
            initialScale = 0.94f,
            offsetFactor = 1f
        )
    }
}

fun Modifier.staggeredEntrance(
    index: Int,
    visible: Boolean,
    offsetDistance: Float = 50f,
    motionTier: MotionTier = MotionTier.Normal
): Modifier = composed {
    val policy = remember(motionTier) { resolveStaggeredEntranceMotionPolicy(motionTier) }
    val alpha = remember { Animatable(0f) }
    val translationY = remember(offsetDistance, policy.offsetFactor) {
        Animatable(offsetDistance * policy.offsetFactor)
    }
    val scale = remember(policy.initialScale) { Animatable(policy.initialScale) }

    LaunchedEffect(visible) {
        if (visible) {
            // Delay based on index for the staggered effect
            val delayMs = (index * policy.delayStepMs).coerceAtMost(policy.maxDelayMs).toLong()
            delay(delayMs)

            // Parallel animations
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = policy.alphaDurationMs,
                        easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f) // Ease-out
                    )
                )
            }
            launch {
                translationY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = policy.translationDurationMs,
                        easing = CubicBezierEasing(0.18f, 0.8f, 0.2f, 1.0f) // Fast-out, slow-in
                    )
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = policy.scaleDurationMs,
                        easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)
                    )
                )
            }
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value * density // Convert dp-like float to pixels if needed, but here we treat input as px or user dp
        this.scaleX = scale.value
        this.scaleY = scale.value
    }
}
