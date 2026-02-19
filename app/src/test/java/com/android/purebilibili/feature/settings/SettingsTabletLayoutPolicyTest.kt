package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsTabletLayoutPolicyTest {

    @Test
    fun expandedTablet_usesBalancedMasterDetailRatio() {
        val policy = resolveSettingsTabletLayoutPolicy(widthDp = 1024, isTv = false)

        assertEquals(0.35f, policy.primaryRatio)
        assertEquals(16, policy.masterPanePaddingDp)
        assertEquals(24, policy.detailPanePaddingDp)
        assertEquals(800, policy.detailMaxWidthDp)
    }

    @Test
    fun ultraWideTablet_reducesMasterRatio_andExpandsDetailWidth() {
        val policy = resolveSettingsTabletLayoutPolicy(widthDp = 1700, isTv = false)

        assertEquals(0.30f, policy.primaryRatio)
        assertEquals(20, policy.masterPanePaddingDp)
        assertEquals(28, policy.detailPanePaddingDp)
        assertEquals(920, policy.detailMaxWidthDp)
        assertTrue(policy.rootPanelMaxWidthDp >= 680)
    }

    @Test
    fun tv_keepsComfortableReadingWidth() {
        val policy = resolveSettingsTabletLayoutPolicy(widthDp = 1920, isTv = true)

        assertEquals(0.34f, policy.primaryRatio)
        assertEquals(20, policy.masterPanePaddingDp)
        assertEquals(28, policy.detailPanePaddingDp)
        assertEquals(880, policy.detailMaxWidthDp)
    }

    @Test
    fun splitLayout_threshold_isExpandedOrTv_only() {
        assertEquals(false, shouldUseSettingsSplitLayout(widthDp = 720, isTv = false))
        assertEquals(true, shouldUseSettingsSplitLayout(widthDp = 1024, isTv = false))
        assertEquals(true, shouldUseSettingsSplitLayout(widthDp = 720, isTv = true))
    }
}
