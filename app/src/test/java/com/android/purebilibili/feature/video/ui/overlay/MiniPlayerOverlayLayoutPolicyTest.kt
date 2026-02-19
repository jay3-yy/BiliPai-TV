package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class MiniPlayerOverlayLayoutPolicyTest {

    @Test
    fun compactPhone_usesCurrentMiniPlayerSize() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(220, policy.miniPlayerWidthDp)
        assertEquals(130, policy.miniPlayerHeightDp)
        assertEquals(28, policy.headerHeightDp)
        assertEquals(24, policy.headerButtonSizeDp)
        assertEquals(8, policy.seekHintCornerRadiusDp)
        assertEquals(8, policy.stashedSideCornerExtraDp)
    }

    @Test
    fun mediumTablet_keepsReadableMiniPlayerWithoutOverflow() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(280, policy.miniPlayerWidthDp)
        assertEquals(157, policy.miniPlayerHeightDp)
        assertEquals(28, policy.headerHeightDp)
        assertEquals(24, policy.headerButtonSizeDp)
        assertEquals(8, policy.seekHintCornerRadiusDp)
        assertEquals(8, policy.stashedSideCornerExtraDp)
    }

    @Test
    fun expandedTablet_scalesMiniPlayerAndControls() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 1280,
            isTv = false
        )

        assertEquals(300, policy.miniPlayerWidthDp)
        assertEquals(168, policy.miniPlayerHeightDp)
        assertEquals(30, policy.headerHeightDp)
        assertEquals(26, policy.headerButtonSizeDp)
        assertEquals(10, policy.seekHintCornerRadiusDp)
        assertEquals(10, policy.stashedSideCornerExtraDp)
    }

    @Test
    fun tv_usesLargestTenFootMiniPlayerTier() {
        val policy = resolveMiniPlayerOverlayLayoutPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(320, policy.miniPlayerWidthDp)
        assertEquals(180, policy.miniPlayerHeightDp)
        assertEquals(32, policy.headerHeightDp)
        assertEquals(28, policy.headerButtonSizeDp)
        assertEquals(12, policy.seekHintCornerRadiusDp)
        assertEquals(12, policy.stashedSideCornerExtraDp)
    }
}
