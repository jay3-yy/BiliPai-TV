package com.android.purebilibili.feature.bangumi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiUiPolicyTest {

    @Test
    fun `bangumi navigation title font should be reduced on phone`() {
        assertEquals(22f, resolveBangumiNavigationTitleFontSizeSp(screenWidthDp = 393), 0.01f)
        assertEquals(20f, resolveBangumiNavigationTitleFontSizeSp(screenWidthDp = 320), 0.01f)
    }

    @Test
    fun `bangumi type tab font should avoid oversized labels`() {
        assertEquals(16f, resolveBangumiTypeTabFontSizeSp(screenWidthDp = 393), 0.01f)
        assertEquals(14f, resolveBangumiTypeTabFontSizeSp(screenWidthDp = 320), 0.01f)
    }

    @Test
    fun `portrait player controls should clear status bar and danmaku should start below controls`() {
        val statusInsetDp = 28f
        val topControlsPadding = resolveBangumiPlayerTopControlsPaddingTopDp(
            isFullscreen = false,
            statusBarsInsetDp = statusInsetDp
        )
        val danmakuTopInset = resolveBangumiDanmakuTopInsetDp(
            isFullscreen = false,
            statusBarsInsetDp = statusInsetDp
        )

        assertTrue(topControlsPadding >= statusInsetDp)
        assertTrue(danmakuTopInset > topControlsPadding)
    }

    @Test
    fun `fullscreen player should not crop danmaku top area`() {
        assertEquals(
            0f,
            resolveBangumiDanmakuTopInsetDp(isFullscreen = true, statusBarsInsetDp = 28f),
            0.01f
        )
    }
}

