package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTabletLayoutPolicyTest {

    @Test
    fun compactWidth_usesPhoneDefaults() {
        val policy = resolveSearchLayoutPolicy(widthDp = 393, isTv = false)

        assertEquals(160, policy.resultGridMinItemWidthDp)
        assertEquals(8, policy.resultGridSpacingDp)
        assertEquals(8, policy.resultHorizontalPaddingDp)
        assertEquals(2, policy.hotSearchColumns)
    }

    @Test
    fun mediumTablet_keepsReadableDensity() {
        val policy = resolveSearchLayoutPolicy(widthDp = 720, isTv = false)

        assertEquals(200, policy.resultGridMinItemWidthDp)
        assertEquals(12, policy.resultGridSpacingDp)
        assertEquals(16, policy.resultHorizontalPaddingDp)
        assertEquals(2, policy.hotSearchColumns)
        assertEquals(1f, policy.leftPaneWeight)
        assertEquals(1f, policy.rightPaneWeight)
    }

    @Test
    fun expandedTablet_increasesColumnsForDiscovery() {
        val policy = resolveSearchLayoutPolicy(widthDp = 1280, isTv = false)

        assertEquals(220, policy.resultGridMinItemWidthDp)
        assertEquals(20, policy.resultHorizontalPaddingDp)
        assertEquals(3, policy.hotSearchColumns)
        assertEquals(1.05f, policy.leftPaneWeight)
        assertEquals(0.95f, policy.rightPaneWeight)
    }

    @Test
    fun ultraWideTablet_expandsColumnsAndCardWidth() {
        val policy = resolveSearchLayoutPolicy(widthDp = 1920, isTv = false)

        assertEquals(260, policy.resultGridMinItemWidthDp)
        assertEquals(24, policy.resultHorizontalPaddingDp)
        assertEquals(4, policy.hotSearchColumns)
        assertEquals(1.15f, policy.leftPaneWeight)
        assertEquals(0.85f, policy.rightPaneWeight)
    }

    @Test
    fun tv_prefersLowerColumnCount() {
        val policy = resolveSearchLayoutPolicy(widthDp = 1920, isTv = true)

        assertTrue(policy.resultGridMinItemWidthDp >= 220)
        assertEquals(2, policy.hotSearchColumns)
    }

    @Test
    fun splitLayout_threshold_isExpandedOrTv_only() {
        assertEquals(false, shouldUseSearchSplitLayout(widthDp = 720, isTv = false))
        assertEquals(true, shouldUseSearchSplitLayout(widthDp = 1024, isTv = false))
        assertEquals(true, shouldUseSearchSplitLayout(widthDp = 720, isTv = true))
    }
}
