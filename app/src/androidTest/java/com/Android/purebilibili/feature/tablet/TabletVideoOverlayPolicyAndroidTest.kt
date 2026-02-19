package com.android.purebilibili.feature.tablet

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.overlay.resolveBottomControlBarLayoutPolicy
import com.android.purebilibili.feature.video.ui.overlay.resolvePortraitFullscreenOverlayLayoutPolicy
import com.android.purebilibili.feature.video.ui.overlay.resolvePortraitProgressBarLayoutPolicy
import com.android.purebilibili.feature.video.ui.overlay.resolveVideoPlayerOverlayVisualPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabletVideoOverlayPolicyAndroidTest {

    @Test
    fun mediumTablet_usesDedicatedOverlayTier() {
        val visual = resolveVideoPlayerOverlayVisualPolicy(widthDp = 720, isTv = false)
        val bottom = resolveBottomControlBarLayoutPolicy(widthDp = 720, isTv = false)
        val fullscreen = resolvePortraitFullscreenOverlayLayoutPolicy(widthDp = 720, isTv = false)
        val progress = resolvePortraitProgressBarLayoutPolicy(widthDp = 720, isTv = false)

        assertEquals(152, visual.topScrimHeightDp)
        assertEquals(36, bottom.playButtonSizeDp)
        assertEquals(16, fullscreen.topHorizontalPaddingDp)
        assertEquals(52, progress.touchAreaHeightDp)
    }

    @Test
    fun mediumTablet_staysBetweenPhoneAndExpandedDensity() {
        val phone = resolveBottomControlBarLayoutPolicy(widthDp = 393, isTv = false)
        val medium = resolveBottomControlBarLayoutPolicy(widthDp = 720, isTv = false)
        val expanded = resolveBottomControlBarLayoutPolicy(widthDp = 1024, isTv = false)

        assertTrue(medium.playButtonSizeDp > phone.playButtonSizeDp)
        assertTrue(medium.playButtonSizeDp < expanded.playButtonSizeDp)
        assertTrue(medium.horizontalPaddingDp > phone.horizontalPaddingDp)
        assertTrue(medium.horizontalPaddingDp < expanded.horizontalPaddingDp)
    }
}
