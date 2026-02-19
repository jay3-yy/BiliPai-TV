package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.store.LiquidGlassStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidLensProfileTest {

    @Test
    fun `stationary state disables refraction`() {
        val profile = resolveLiquidLensProfile(
            isDragging = false,
            velocityPxPerSecond = 0f
        )

        assertFalse(profile.shouldRefract)
        assertEquals(0f, profile.motionFraction, 0.0001f)
        assertEquals(0f, profile.aberrationStrength, 0.0001f)
    }

    @Test
    fun `drag mode uses lower speed threshold for refraction`() {
        val normal = resolveLiquidLensProfile(
            isDragging = false,
            velocityPxPerSecond = 70f
        )
        val dragging = resolveLiquidLensProfile(
            isDragging = true,
            velocityPxPerSecond = 70f
        )

        assertFalse(normal.shouldRefract)
        assertTrue(dragging.shouldRefract)
    }

    @Test
    fun `dragging with near zero velocity still refracts`() {
        val profile = resolveLiquidLensProfile(
            isDragging = true,
            velocityPxPerSecond = 0f
        )

        assertTrue(profile.shouldRefract)
        assertTrue(profile.motionFraction > 0f)
    }

    @Test
    fun `higher speed increases spherical lens intensity`() {
        val slow = resolveLiquidLensProfile(
            isDragging = false,
            velocityPxPerSecond = 300f
        )
        val fast = resolveLiquidLensProfile(
            isDragging = false,
            velocityPxPerSecond = 2200f
        )

        assertTrue(slow.shouldRefract)
        assertTrue(fast.shouldRefract)
        assertTrue(fast.refractionAmount > slow.refractionAmount)
        assertTrue(fast.refractionHeight > slow.refractionHeight)
        assertTrue(fast.centerHighlightAlpha > slow.centerHighlightAlpha)
        assertTrue(fast.edgeCompressionAlpha > slow.edgeCompressionAlpha)
    }

    @Test
    fun `bottom boost increases spacing distortion strength`() {
        val normal = resolveLiquidLensProfile(
            isDragging = true,
            velocityPxPerSecond = 1200f
        )
        val boosted = resolveLiquidLensProfile(
            isDragging = true,
            velocityPxPerSecond = 1200f,
            lensIntensityBoost = 1.38f,
            edgeWarpBoost = 1.58f,
            chromaticBoost = 1.45f
        )

        assertTrue(boosted.refractionAmount > normal.refractionAmount)
        assertTrue(boosted.edgeCompressionAlpha > normal.edgeCompressionAlpha)
        assertTrue(boosted.aberrationStrength > normal.aberrationStrength)
    }

    @Test
    fun `higher drag motion floor increases distortion at low speed`() {
        val base = resolveLiquidLensProfile(
            isDragging = true,
            velocityPxPerSecond = 0f
        )
        val stronger = resolveLiquidLensProfile(
            isDragging = true,
            velocityPxPerSecond = 0f,
            dragMotionFloor = 0.38f
        )

        assertTrue(stronger.motionFraction > base.motionFraction)
        assertTrue(stronger.refractionAmount > base.refractionAmount)
    }

    @Test
    fun `ios26 tuning keeps dynamic lens while controlling color split`() {
        val classic = resolveLiquidStyleTuning(LiquidGlassStyle.CLASSIC)
        val ios26 = resolveLiquidStyleTuning(LiquidGlassStyle.IOS26)

        assertTrue(ios26.idleThresholdPxPerSecond >= classic.idleThresholdPxPerSecond)
        assertTrue(ios26.lensIntensityMultiplier > classic.lensIntensityMultiplier)
        assertTrue(ios26.edgeWarpMultiplier > 1f)
        assertTrue(ios26.chromaticMultiplier <= classic.chromaticMultiplier)
        assertTrue(ios26.chromaticMultiplier > 0f)
        assertTrue(ios26.dragMotionFloor < classic.dragMotionFloor)
        assertTrue(ios26.deformationMultiplier <= classic.deformationMultiplier)
        assertTrue(ios26.depthEffectEnabled)
        assertTrue(ios26.allowChromaticAberration)
    }

    @Test
    fun `idle state remains non refractive`() {
        val idle = resolveLiquidLensProfile(
            isDragging = false,
            velocityPxPerSecond = 0f
        )

        assertFalse(idle.shouldRefract)
        assertEquals(0f, idle.refractionAmount, 0.0001f)
    }
}
