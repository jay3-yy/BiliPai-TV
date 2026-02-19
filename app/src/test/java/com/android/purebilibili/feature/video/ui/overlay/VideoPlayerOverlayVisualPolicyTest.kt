package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlayerOverlayVisualPolicyTest {

    @Test
    fun compactPhone_usesDefaultOverlayVisualDensity() {
        val policy = resolveVideoPlayerOverlayVisualPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(140, policy.topScrimHeightDp)
        assertEquals(200, policy.bottomScrimHeightDp)
        assertEquals(48, policy.lockButtonSizeDp)
        assertEquals(72, policy.centerPlayButtonSizeDp)
        assertEquals(12, policy.lockButtonCornerRadiusDp)
        assertEquals(12, policy.qualitySwitchCornerRadiusDp)
        assertEquals(24, policy.interactionIconSizeDp)
    }

    @Test
    fun mediumTablet_balancesScrimAndCenterControls() {
        val policy = resolveVideoPlayerOverlayVisualPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(152, policy.topScrimHeightDp)
        assertEquals(220, policy.bottomScrimHeightDp)
        assertEquals(52, policy.lockButtonSizeDp)
        assertEquals(78, policy.centerPlayButtonSizeDp)
        assertEquals(13, policy.lockButtonCornerRadiusDp)
        assertEquals(13, policy.qualitySwitchCornerRadiusDp)
        assertEquals(25, policy.interactionIconSizeDp)
    }

    @Test
    fun tablet_expandsScrimAndControlTargets() {
        val policy = resolveVideoPlayerOverlayVisualPolicy(
            widthDp = 1024,
            isTv = false
        )

        assertEquals(168, policy.topScrimHeightDp)
        assertEquals(240, policy.bottomScrimHeightDp)
        assertEquals(56, policy.lockButtonSizeDp)
        assertEquals(84, policy.centerPlayButtonSizeDp)
        assertEquals(14, policy.lockButtonCornerRadiusDp)
        assertEquals(14, policy.qualitySwitchCornerRadiusDp)
        assertEquals(26, policy.interactionIconSizeDp)
    }

    @Test
    fun tv_forcesLargestOverlayVisualScale() {
        val policy = resolveVideoPlayerOverlayVisualPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(200, policy.topScrimHeightDp)
        assertEquals(280, policy.bottomScrimHeightDp)
        assertEquals(64, policy.lockButtonSizeDp)
        assertEquals(96, policy.centerPlayButtonSizeDp)
        assertEquals(16, policy.lockButtonCornerRadiusDp)
        assertEquals(16, policy.qualitySwitchCornerRadiusDp)
        assertEquals(28, policy.interactionIconSizeDp)
    }
}
