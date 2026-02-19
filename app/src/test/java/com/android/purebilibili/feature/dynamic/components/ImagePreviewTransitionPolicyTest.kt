package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ImagePreviewTransitionPolicyTest {

    @Test
    fun resolveImagePreviewTransitionFrame_clampsVisualProgressButKeepsLayoutOvershoot() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = -0.2f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(-0.08f, frame.layoutProgress)
        assertEquals(0f, frame.visualProgress)
        assertEquals(12f, frame.cornerRadiusDp)
    }

    @Test
    fun resolveImagePreviewTransitionFrame_keepsCornerRadiusConstantDuringTransition() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = 0.5f,
            hasSourceRect = true,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(12f, frame.cornerRadiusDp)
    }

    @Test
    fun resolveImagePreviewTransitionFrame_usesZeroCornerWhenNoSourceRect() {
        val frame = resolveImagePreviewTransitionFrame(
            rawProgress = 0.5f,
            hasSourceRect = false,
            sourceCornerRadiusDp = 12f
        )

        assertEquals(0f, frame.cornerRadiusDp)
    }

    @Test
    fun imagePreviewDismissMotion_returnsNoOvershootTargets() {
        val motion = imagePreviewDismissMotion()

        assertEquals(0f, motion.overshootTarget)
        assertEquals(0f, motion.settleTarget)
    }

    @Test
    fun resolvePredictiveBackAnimationProgress_isInverseOfGestureProgress() {
        assertEquals(1f, resolvePredictiveBackAnimationProgress(0f))
        assertEquals(0.5f, resolvePredictiveBackAnimationProgress(0.5f))
        assertEquals(0f, resolvePredictiveBackAnimationProgress(1f))
    }

    @Test
    fun resolvePredictiveBackAnimationProgress_clampsOutOfRangeInput() {
        assertEquals(1f, resolvePredictiveBackAnimationProgress(-0.3f))
        assertEquals(0f, resolvePredictiveBackAnimationProgress(1.6f))
    }
}
