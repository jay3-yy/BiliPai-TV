package com.android.purebilibili.core.util

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnterAnimationPolicyTest {

    @Test
    fun reducedMotion_usesMinimalStaggerAndHigherStiffness() {
        val policy = resolveEnterMotionPolicy(MotionTier.Reduced)

        assertTrue(policy.staggerStepMs <= 12)
        assertTrue(policy.maxStaggerMs <= 80)
        assertTrue(policy.stiffness >= 600f)
    }

    @Test
    fun enhancedMotion_hasMoreExpressiveScaleThanNormal() {
        val normal = resolveEnterMotionPolicy(MotionTier.Normal)
        val enhanced = resolveEnterMotionPolicy(MotionTier.Enhanced)

        assertTrue(enhanced.initialScale < normal.initialScale)
        assertTrue(enhanced.translationFactor >= normal.translationFactor)
    }

    @Test
    fun normalMotion_keepsCurrentBaselineStagger() {
        val normal = resolveEnterMotionPolicy(MotionTier.Normal)

        assertEquals(30, normal.staggerStepMs)
        assertEquals(200, normal.maxStaggerMs)
    }
}
