package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class TabletSecondaryPanePolicyTest {

    @Test
    fun expandedSidebar_keepsBaseRatio() {
        val ratio = resolveTabletPrimaryRatio(
            basePrimaryRatio = 0.72f,
            secondaryPaneMode = TabletSecondaryPaneMode.EXPANDED
        )

        assertEquals(0.72f, ratio)
    }

    @Test
    fun compactSidebar_moderatelyBoostsPrimaryRatio() {
        val ratio = resolveTabletPrimaryRatio(
            basePrimaryRatio = 0.72f,
            secondaryPaneMode = TabletSecondaryPaneMode.COMPACT
        )

        assertEquals(0.8f, ratio)
    }

    @Test
    fun collapsedSidebar_boostsPrimaryRatio_withUpperCap() {
        val ratio = resolveTabletPrimaryRatio(
            basePrimaryRatio = 0.66f,
            secondaryPaneMode = TabletSecondaryPaneMode.COLLAPSED
        )

        assertEquals(0.80f, ratio)
    }

    @Test
    fun cycleMode_rotatesExpandedCompactCollapsed() {
        assertEquals(
            TabletSecondaryPaneMode.COMPACT,
            nextTabletSecondaryPaneMode(TabletSecondaryPaneMode.EXPANDED)
        )
        assertEquals(
            TabletSecondaryPaneMode.COLLAPSED,
            nextTabletSecondaryPaneMode(TabletSecondaryPaneMode.COMPACT)
        )
        assertEquals(
            TabletSecondaryPaneMode.EXPANDED,
            nextTabletSecondaryPaneMode(TabletSecondaryPaneMode.COLLAPSED)
        )
    }
}
