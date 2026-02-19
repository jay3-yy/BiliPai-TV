package com.android.purebilibili.feature.video.danmaku

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FaceOcclusionPolicyTest {

    @Test
    fun noFaces_keepsDefaultDisplayBand() {
        val defaultBand = DanmakuDisplayBand(topRatio = 0f, bottomRatio = 0.5f)

        val resolved = resolveFaceAwareDisplayBand(
            faceRegions = emptyList(),
            defaultBand = defaultBand
        )

        assertEquals(defaultBand, resolved)
    }

    @Test
    fun largeTopFace_prefersLowerSafeBand() {
        val resolved = resolveFaceAwareDisplayBand(
            faceRegions = listOf(FaceOcclusionRegion(topRatio = 0.06f, bottomRatio = 0.62f)),
            defaultBand = DanmakuDisplayBand(topRatio = 0f, bottomRatio = 0.5f),
            minHeightRatio = 0.2f,
            facePaddingRatio = 0.02f
        )

        assertTrue(resolved.topRatio >= 0.60f, "Expected band below face, got $resolved")
        assertTrue(resolved.bottomRatio <= 1f)
    }

    @Test
    fun tinySafeGap_fallsBackToDefaultBand() {
        val defaultBand = DanmakuDisplayBand(topRatio = 0f, bottomRatio = 0.5f)
        val resolved = resolveFaceAwareDisplayBand(
            faceRegions = listOf(FaceOcclusionRegion(topRatio = 0.02f, bottomRatio = 0.98f)),
            defaultBand = defaultBand,
            minHeightRatio = 0.2f,
            facePaddingRatio = 0f
        )

        assertEquals(defaultBand, resolved)
    }

    @Test
    fun smoothing_withLargeJump_movesIncrementally() {
        val smoothed = smoothDisplayBand(
            previousBand = DanmakuDisplayBand(topRatio = 0f, bottomRatio = 0.5f),
            targetBand = DanmakuDisplayBand(topRatio = 0.6f, bottomRatio = 1f),
            lerpFactor = 0.5f
        )

        assertTrue(abs(smoothed.topRatio - 0.3f) < 0.0001f)
        assertTrue(abs(smoothed.bottomRatio - 0.75f) < 0.0001f)
    }

    @Test
    fun smoothing_withSmallDelta_snapsToTarget() {
        val target = DanmakuDisplayBand(topRatio = 0.11f, bottomRatio = 0.58f)
        val smoothed = smoothDisplayBand(
            previousBand = DanmakuDisplayBand(topRatio = 0.1f, bottomRatio = 0.57f),
            targetBand = target,
            lerpFactor = 0.35f,
            snapThreshold = 0.02f
        )

        assertEquals(target, smoothed)
    }

    @Test
    fun maskRects_areExpandedAndClamped() {
        val masks = resolveFaceOcclusionMasks(
            rawRects = listOf(
                FaceOcclusionMaskRect(
                    leftRatio = -0.05f,
                    topRatio = 0.05f,
                    rightRatio = 0.25f,
                    bottomRatio = 0.35f
                )
            ),
            expansionRatio = 0.05f
        )

        val first = masks.first()
        assertTrue(first.leftRatio >= 0f)
        assertTrue(first.topRatio >= 0f)
        assertTrue(first.rightRatio <= 1f)
        assertTrue(first.bottomRatio <= 1f)
        assertTrue(first.bottomRatio - first.topRatio > 0.3f)
    }

    @Test
    fun overlappingMaskRects_areMerged() {
        val masks = resolveFaceOcclusionMasks(
            rawRects = listOf(
                FaceOcclusionMaskRect(0.2f, 0.2f, 0.42f, 0.55f),
                FaceOcclusionMaskRect(0.35f, 0.3f, 0.6f, 0.62f)
            ),
            expansionRatio = 0f
        )

        assertEquals(1, masks.size)
        val merged = masks.first()
        assertTrue(merged.leftRatio <= 0.2f)
        assertTrue(merged.rightRatio >= 0.6f)
        assertTrue(merged.bottomRatio >= 0.62f)
    }

    @Test
    fun polygonExpansion_keepsShapeAndClampsIntoViewport() {
        val polygon = listOf(
            NormalizedPoint(0.02f, 0.10f),
            NormalizedPoint(0.22f, 0.08f),
            NormalizedPoint(0.25f, 0.28f),
            NormalizedPoint(0.05f, 0.30f)
        )

        val expanded = expandNormalizedPolygon(polygon, expansionRatio = 0.08f)

        assertEquals(polygon.size, expanded.size)
        assertTrue(expanded.all { it.xRatio in 0f..1f && it.yRatio in 0f..1f })
        assertTrue(expanded.first().xRatio <= polygon.first().xRatio)
    }

    @Test
    fun visualMaskPrefersPolygon_whenEnoughPoints() {
        val polygon = listOf(
            NormalizedPoint(0.3f, 0.2f),
            NormalizedPoint(0.4f, 0.18f),
            NormalizedPoint(0.5f, 0.22f),
            NormalizedPoint(0.52f, 0.32f),
            NormalizedPoint(0.45f, 0.4f),
            NormalizedPoint(0.34f, 0.36f)
        )

        val mask = buildVisualMask(
            rect = FaceOcclusionMaskRect(0.3f, 0.18f, 0.52f, 0.4f),
            polygon = polygon
        )

        assertTrue(mask.polygonPoints.isNotEmpty())
    }

    @Test
    fun stabilizer_requiresStableFrames_beforeApplyingBandUpdate() {
        val stabilizer = FaceOcclusionBandStabilizer(
            config = FaceOcclusionBandStabilizerConfig(
                requiredStableFrames = 2,
                minUpdateIntervalMs = 0L,
                smoothingLerpFactor = 1f,
                minUpdateDelta = 0.01f
            )
        )
        val defaultBand = DanmakuDisplayBand(0f, 0.5f)
        val faceBand = DanmakuDisplayBand(0.58f, 1f)

        val initial = stabilizer.step(defaultBand, hasFace = false, nowRealtimeMs = 0L)
        val firstAttempt = stabilizer.step(faceBand, hasFace = true, nowRealtimeMs = 100L)
        val secondAttempt = stabilizer.step(faceBand, hasFace = true, nowRealtimeMs = 200L)

        assertEquals(defaultBand, initial)
        assertNull(firstAttempt)
        assertEquals(faceBand, secondAttempt)
    }

    @Test
    fun stabilizer_holdsBandWhenFaceTemporarilyMissing() {
        val stabilizer = FaceOcclusionBandStabilizer(
            config = FaceOcclusionBandStabilizerConfig(
                requiredStableFrames = 1,
                noFaceHoldFrames = 2,
                noFaceExtraStableFrames = 0,
                minUpdateIntervalMs = 0L,
                smoothingLerpFactor = 1f,
                minUpdateDelta = 0.01f
            )
        )
        val defaultBand = DanmakuDisplayBand(0f, 0.5f)
        val faceBand = DanmakuDisplayBand(0.6f, 1f)

        stabilizer.step(defaultBand, hasFace = false, nowRealtimeMs = 0L)
        stabilizer.step(faceBand, hasFace = true, nowRealtimeMs = 100L)

        val missing1 = stabilizer.step(defaultBand, hasFace = false, nowRealtimeMs = 200L)
        val missing2 = stabilizer.step(defaultBand, hasFace = false, nowRealtimeMs = 300L)
        val missing3 = stabilizer.step(defaultBand, hasFace = false, nowRealtimeMs = 400L)

        assertNull(missing1)
        assertNull(missing2)
        assertEquals(defaultBand, missing3)
    }

    @Test
    fun stabilizer_respectsMinIntervalUnlessJumpIsLarge() {
        val stabilizer = FaceOcclusionBandStabilizer(
            config = FaceOcclusionBandStabilizerConfig(
                requiredStableFrames = 1,
                minUpdateIntervalMs = 2_000L,
                largeJumpDelta = 0.2f,
                smoothingLerpFactor = 1f,
                minUpdateDelta = 0.01f
            )
        )
        val defaultBand = DanmakuDisplayBand(0f, 0.5f)
        val mediumShift = DanmakuDisplayBand(0.1f, 0.6f)
        val largeShift = DanmakuDisplayBand(0.65f, 1f)

        stabilizer.step(defaultBand, hasFace = false, nowRealtimeMs = 0L)
        val blocked = stabilizer.step(mediumShift, hasFace = true, nowRealtimeMs = 200L)
        val allowed = stabilizer.step(largeShift, hasFace = true, nowRealtimeMs = 400L)

        assertNull(blocked)
        assertEquals(largeShift, allowed)
    }
}
