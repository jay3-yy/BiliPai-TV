package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TopTabMotionVelocityTest {

    @Test
    fun `horizontal only when liquid glass disabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1200f
        )

        assertEquals(1200f, velocity, 0.001f)
    }

    @Test
    fun `vertical does not contribute when liquid glass enabled`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 1000f
        )

        assertEquals(1000f, velocity, 0.001f)
    }

    @Test
    fun `result is clamped to avoid excessive distortion`() {
        val velocity = resolveTopTabIndicatorVelocity(
            horizontalVelocityPxPerSecond = 5000f
        )

        assertEquals(4200f, velocity, 0.001f)
    }

    @Test
    fun `vertical motion alone does not mark interacting when liquid glass enabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            liquidGlassEnabled = true
        )

        assertEquals(false, interacting)
    }

    @Test
    fun `vertical motion ignored when liquid glass disabled`() {
        val interacting = shouldTopTabIndicatorBeInteracting(
            pagerIsScrolling = false,
            combinedVelocityPxPerSecond = 10f,
            liquidGlassEnabled = false
        )

        assertEquals(false, interacting)
    }

    @Test
    fun `tiny pager jitter is ignored by horizontal delta resolver`() {
        val delta = resolveTopTabHorizontalDeltaPx(
            positionDeltaPages = 0.0008f,
            tabWidthPx = 92f
        )

        assertEquals(0f, delta, 0.0001f)
    }

    @Test
    fun `meaningful page movement produces horizontal delta`() {
        val delta = resolveTopTabHorizontalDeltaPx(
            positionDeltaPages = 0.25f,
            tabWidthPx = 100f
        )

        assertEquals(25f, delta, 0.0001f)
    }

    @Test
    fun `viewport shift uses first visible item index and offset`() {
        val shift = resolveTopTabIndicatorViewportShiftPx(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffsetPx = 24,
            tabWidthPx = 92f
        )

        assertEquals(208f, shift, 0.0001f)
    }

    @Test
    fun `viewport shift returns zero for invalid width`() {
        val shift = resolveTopTabIndicatorViewportShiftPx(
            firstVisibleItemIndex = 2,
            firstVisibleItemScrollOffsetPx = 24,
            tabWidthPx = 0f
        )

        assertEquals(0f, shift, 0.0001f)
    }
}
