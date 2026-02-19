package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class BottomRightControlsLayoutPolicyTest {

    @Test
    fun compactPhone_usesDenseControlChipStyle() {
        val policy = resolveBottomRightControlsLayoutPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(8, policy.rowSpacingDp)
        assertEquals(-10, policy.menuOffsetYDp)
        assertEquals(6, policy.chipCornerRadiusDp)
        assertEquals(12, policy.chipFontSp)
    }

    @Test
    fun mediumTablet_balancesSpacingAndReadability() {
        val policy = resolveBottomRightControlsLayoutPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(9, policy.rowSpacingDp)
        assertEquals(-10, policy.menuOffsetYDp)
        assertEquals(6, policy.chipCornerRadiusDp)
        assertEquals(12, policy.chipFontSp)
    }

    @Test
    fun expandedTablet_increasesChipDensity() {
        val policy = resolveBottomRightControlsLayoutPolicy(
            widthDp = 1024,
            isTv = false
        )

        assertEquals(10, policy.rowSpacingDp)
        assertEquals(-11, policy.menuOffsetYDp)
        assertEquals(7, policy.chipCornerRadiusDp)
        assertEquals(13, policy.chipFontSp)
    }

    @Test
    fun tv_usesLargestTenFootControlDensity() {
        val policy = resolveBottomRightControlsLayoutPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(12, policy.rowSpacingDp)
        assertEquals(-12, policy.menuOffsetYDp)
        assertEquals(8, policy.chipCornerRadiusDp)
        assertEquals(14, policy.chipFontSp)
    }
}
