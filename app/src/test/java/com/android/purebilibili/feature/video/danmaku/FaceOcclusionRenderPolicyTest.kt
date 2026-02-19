package com.android.purebilibili.feature.video.danmaku

import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaceOcclusionRenderPolicyTest {

    @Test
    fun contentRect_fitMode_letterboxesVertically() {
        val rect = resolveVideoContentRect(
            containerWidth = 1920,
            containerHeight = 1080,
            videoWidth = 1920,
            videoHeight = 800,
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        )

        assertNear(0f, rect.left)
        assertNear(1920f, rect.right)
        assertNear(140f, rect.top)
        assertNear(940f, rect.bottom)
    }

    @Test
    fun contentRect_zoomMode_cropsHorizontally() {
        val rect = resolveVideoContentRect(
            containerWidth = 1920,
            containerHeight = 1080,
            videoWidth = 1920,
            videoHeight = 800,
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        )

        assertNear(0f, rect.top)
        assertNear(1080f, rect.bottom)
        assertTrue(rect.left < 0f, "Expected horizontal crop, got left=${rect.left}")
        assertTrue(rect.right > 1920f, "Expected horizontal crop, got right=${rect.right}")
    }

    @Test
    fun maskStabilizer_smoothsMaskMovement() {
        val stabilizer = FaceOcclusionMaskStabilizer(
            config = FaceOcclusionMaskStabilizerConfig(
                positionLerpFactor = 0.5f,
                holdMissingFrames = 0,
                minIouForTracking = 0f
            )
        )
        val firstMask = listOf(maskRect(0.1f, 0.2f, 0.3f, 0.5f))
        val secondMask = listOf(maskRect(0.3f, 0.2f, 0.5f, 0.5f))

        val first = stabilizer.step(firstMask)
        val second = stabilizer.step(secondMask)

        assertNear(0.1f, first.first().fallbackRect.leftRatio)
        assertNear(0.2f, second.first().fallbackRect.leftRatio)
        assertNear(0.4f, second.first().fallbackRect.rightRatio)
    }

    @Test
    fun maskStabilizer_holdsPreviousMasks_forShortMisses() {
        val stabilizer = FaceOcclusionMaskStabilizer(
            config = FaceOcclusionMaskStabilizerConfig(
                holdMissingFrames = 2,
                positionLerpFactor = 1f
            )
        )
        val firstMask = listOf(maskRect(0.2f, 0.2f, 0.4f, 0.5f))

        stabilizer.step(firstMask)
        val missing1 = stabilizer.step(emptyList())
        val missing2 = stabilizer.step(emptyList())
        val missing3 = stabilizer.step(emptyList())

        assertEquals(1, missing1.size)
        assertEquals(1, missing2.size)
        assertTrue(missing3.isEmpty())
    }

    @Test
    fun edgeExpansionRatio_isBoundedAndResponsiveToViewportSize() {
        val smallViewport = resolveMaskEdgeExpansionRatio(
            viewportWidthPx = 360f,
            viewportHeightPx = 640f,
            featherPx = 8f
        )
        val largeViewport = resolveMaskEdgeExpansionRatio(
            viewportWidthPx = 2160f,
            viewportHeightPx = 3840f,
            featherPx = 8f
        )

        assertTrue(smallViewport > largeViewport)
        assertTrue(smallViewport in 0.004f..0.03f)
        assertTrue(largeViewport in 0.004f..0.03f)
    }

    private fun maskRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): FaceOcclusionVisualMask {
        return FaceOcclusionVisualMask(
            fallbackRect = FaceOcclusionMaskRect(left, top, right, bottom),
            polygonPoints = emptyList()
        )
    }

    private fun assertNear(expected: Float, actual: Float, delta: Float = 0.001f) {
        assertTrue(abs(expected - actual) <= delta, "Expected $expected, got $actual")
    }
}
