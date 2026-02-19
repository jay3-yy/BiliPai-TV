package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidIndicatorTranslationTest {

    @Test
    fun `translation keeps center alignment when clamp disabled`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 0f,
            itemWidthPx = 100f,
            indicatorWidthPx = 136f,
            startPaddingPx = 0f,
            containerWidthPx = 500f,
            clampToBounds = false,
            edgeInsetPx = 0f
        )

        assertEquals(-18f, translation, 0.001f)
    }

    @Test
    fun `translation clamps left edge when enabled`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 0f,
            itemWidthPx = 100f,
            indicatorWidthPx = 136f,
            startPaddingPx = 0f,
            containerWidthPx = 500f,
            clampToBounds = true,
            edgeInsetPx = 0f
        )

        assertEquals(0f, translation, 0.001f)
    }

    @Test
    fun `translation clamps right edge when enabled`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 4f,
            itemWidthPx = 100f,
            indicatorWidthPx = 136f,
            startPaddingPx = 0f,
            containerWidthPx = 500f,
            clampToBounds = true,
            edgeInsetPx = 0f
        )

        assertEquals(364f, translation, 0.001f)
    }

    @Test
    fun `translation clamps with viewport shift to keep visible after parent translation`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 1f,
            itemWidthPx = 100f,
            indicatorWidthPx = 120f,
            startPaddingPx = 11f,
            containerWidthPx = 500f,
            clampToBounds = true,
            edgeInsetPx = 1f,
            viewportShiftPx = 111f
        )

        assertEquals(112f, translation, 0.001f)
    }
}
