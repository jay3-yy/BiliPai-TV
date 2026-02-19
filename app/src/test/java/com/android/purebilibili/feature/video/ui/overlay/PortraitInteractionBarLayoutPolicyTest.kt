package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitInteractionBarLayoutPolicyTest {

    @Test
    fun compactPhone_usesDenseInteractionRail() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(8, policy.endPaddingDp)
        assertEquals(180, policy.bottomPaddingDp)
        assertEquals(20, policy.itemSpacingDp)
        assertEquals(34, policy.iconSizeDp)
        assertEquals(12, policy.labelFontSp)
    }

    @Test
    fun mediumTablet_improvesRailSpacingWithoutOverstretch() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(10, policy.endPaddingDp)
        assertEquals(188, policy.bottomPaddingDp)
        assertEquals(22, policy.itemSpacingDp)
        assertEquals(37, policy.iconSizeDp)
        assertEquals(12, policy.labelFontSp)
    }

    @Test
    fun tablet_expandsIconAndSpacing() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 1024,
            isTv = false
        )

        assertEquals(12, policy.endPaddingDp)
        assertEquals(196, policy.bottomPaddingDp)
        assertEquals(24, policy.itemSpacingDp)
        assertEquals(40, policy.iconSizeDp)
        assertEquals(13, policy.labelFontSp)
    }

    @Test
    fun tv_forcesLargestInteractionRailScale() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(18, policy.endPaddingDp)
        assertEquals(220, policy.bottomPaddingDp)
        assertEquals(28, policy.itemSpacingDp)
        assertEquals(46, policy.iconSizeDp)
        assertEquals(15, policy.labelFontSp)
    }
}
