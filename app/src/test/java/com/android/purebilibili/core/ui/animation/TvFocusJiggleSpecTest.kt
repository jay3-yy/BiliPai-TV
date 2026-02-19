package com.android.purebilibili.core.ui.animation

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TvFocusJiggleSpecTest {

    @Test
    fun nonTv_disablesJiggle() {
        val spec = resolveTvFocusJiggleSpec(
            isTv = false,
            screenWidthDp = 1920,
            reducedMotion = false
        )

        assertEquals(1f, spec.focusScale)
        assertEquals(0f, spec.rotationAmplitude)
        assertEquals(0, spec.translationAmplitudeDp)
    }

    @Test
    fun reducedMotion_keepsLightScale_withoutJiggle() {
        val spec = resolveTvFocusJiggleSpec(
            isTv = true,
            screenWidthDp = 1920,
            reducedMotion = true
        )

        assertEquals(1.03f, spec.focusScale)
        assertEquals(0f, spec.rotationAmplitude)
        assertEquals(0, spec.translationAmplitudeDp)
    }

    @Test
    fun tvNormal_usesMildJiggle() {
        val spec = resolveTvFocusJiggleSpec(
            isTv = true,
            screenWidthDp = 1280,
            reducedMotion = false
        )

        assertEquals(1.05f, spec.focusScale)
        assertTrue(spec.focusBumpScale > spec.focusScale)
        assertEquals(1.2f, spec.rotationAmplitude)
        assertEquals(2, spec.translationAmplitudeDp)
    }

    @Test
    fun tvUltraWide_boostsScaleAndAmplitude() {
        val spec = resolveTvFocusJiggleSpec(
            isTv = true,
            screenWidthDp = 1920,
            reducedMotion = false
        )

        assertEquals(1.06f, spec.focusScale)
        assertTrue(spec.rotationAmplitude > 1.2f)
        assertEquals(3, spec.translationAmplitudeDp)
    }

    @Test
    fun reducedMotionTier_forcesNoJiggle() {
        val spec = resolveTvFocusJiggleSpec(
            isTv = true,
            screenWidthDp = 1920,
            reducedMotion = false,
            motionTier = MotionTier.Reduced
        )

        assertEquals(1.03f, spec.focusScale)
        assertEquals(spec.focusScale, spec.focusBumpScale)
        assertEquals(0f, spec.rotationAmplitude)
        assertEquals(0, spec.translationAmplitudeDp)
    }

    @Test
    fun largeCard_usesSofterJiggleThanStandard() {
        val standard = resolveTvFocusJiggleSpec(
            isTv = true,
            screenWidthDp = 1920,
            reducedMotion = false,
            cardEmphasis = TvFocusCardEmphasis.Standard
        )
        val large = resolveTvFocusJiggleSpec(
            isTv = true,
            screenWidthDp = 1920,
            reducedMotion = false,
            cardEmphasis = TvFocusCardEmphasis.Large
        )

        assertTrue(large.rotationAmplitude < standard.rotationAmplitude)
        assertTrue(large.translationAmplitudeDp <= standard.translationAmplitudeDp)
        assertTrue(large.cycleMillis > standard.cycleMillis)
    }
}
