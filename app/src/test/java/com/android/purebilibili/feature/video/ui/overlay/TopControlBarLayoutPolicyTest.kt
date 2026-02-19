package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class TopControlBarLayoutPolicyTest {

    @Test
    fun compactWidth_usesDefaultCompactControlSizing() {
        val policy = resolveTopControlBarLayoutPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(32, policy.buttonSizeDp)
        assertEquals(24, policy.iconSizeDp)
        assertEquals(24, policy.actionSpacingDp)
        assertEquals(12, policy.timeFontSp)
        assertEquals(14, policy.backToTitleSpacingDp)
        assertEquals(11, policy.onlineCountFontSp)
    }

    @Test
    fun mediumTablet_balancesControlTargets() {
        val policy = resolveTopControlBarLayoutPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(36, policy.buttonSizeDp)
        assertEquals(26, policy.iconSizeDp)
        assertEquals(26, policy.actionSpacingDp)
        assertEquals(13, policy.timeFontSp)
        assertEquals(16, policy.backToTitleSpacingDp)
        assertEquals(12, policy.onlineCountFontSp)
    }

    @Test
    fun largeTablet_enlargesControlsAndSpacing() {
        val policy = resolveTopControlBarLayoutPolicy(
            widthDp = 1280,
            isTv = false
        )

        assertEquals(40, policy.buttonSizeDp)
        assertEquals(28, policy.iconSizeDp)
        assertEquals(28, policy.actionSpacingDp)
        assertEquals(14, policy.timeFontSp)
        assertEquals(18, policy.backToTitleSpacingDp)
        assertEquals(12, policy.onlineCountFontSp)
    }

    @Test
    fun tv_forcesLargestControlSizing() {
        val policy = resolveTopControlBarLayoutPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(44, policy.buttonSizeDp)
        assertEquals(30, policy.iconSizeDp)
        assertEquals(30, policy.actionSpacingDp)
        assertEquals(15, policy.timeFontSp)
        assertEquals(20, policy.backToTitleSpacingDp)
        assertEquals(13, policy.onlineCountFontSp)
    }
}
