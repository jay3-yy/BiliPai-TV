package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPlayerUiLayoutPolicyTest {

    @Test
    fun phoneProfile_usesCompactControlTargets() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 393, isTv = false)

        assertEquals(120, policy.gestureOverlaySizeDp)
        assertEquals(48, policy.gestureIconSizeDp)
        assertEquals(100, policy.seekFeedbackSizeDp)
        assertEquals(24, policy.gestureBoundaryPaddingDp)
    }

    @Test
    fun expandedTablet_enlargesControlTargetsAndSpacing() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 1280, isTv = false)

        assertEquals(132, policy.gestureOverlaySizeDp)
        assertEquals(52, policy.gestureIconSizeDp)
        assertEquals(108, policy.seekFeedbackSizeDp)
        assertEquals(112, policy.restoreButtonBottomOffsetDp)
    }

    @Test
    fun ultraWideTablet_furtherImprovesReadability() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 1920, isTv = false)

        assertEquals(140, policy.gestureOverlaySizeDp)
        assertEquals(56, policy.gestureIconSizeDp)
        assertTrue(policy.restoreButtonHorizontalPaddingDp >= 20)
        assertTrue(policy.longPressBadgeHorizontalPaddingDp >= 24)
    }

    @Test
    fun tv_prefersStableLargeTouchTargets() {
        val policy = resolveVideoPlayerUiLayoutPolicy(widthDp = 1920, isTv = true)

        assertEquals(140, policy.gestureOverlaySizeDp)
        assertEquals(56, policy.gestureIconSizeDp)
        assertEquals(112, policy.seekFeedbackSizeDp)
        assertEquals(28, policy.gestureBoundaryPaddingDp)
    }
}
