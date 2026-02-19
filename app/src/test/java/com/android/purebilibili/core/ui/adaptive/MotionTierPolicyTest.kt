package com.android.purebilibili.core.ui.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals

class MotionTierPolicyTest {

    @Test
    fun animationDisabled_forcesReducedMotionTier() {
        val tier = resolveEffectiveMotionTier(
            baseTier = MotionTier.Enhanced,
            animationEnabled = false
        )

        assertEquals(MotionTier.Reduced, tier)
    }

    @Test
    fun animationEnabled_keepsBaseMotionTier() {
        val tier = resolveEffectiveMotionTier(
            baseTier = MotionTier.Normal,
            animationEnabled = true
        )

        assertEquals(MotionTier.Normal, tier)
    }
}
