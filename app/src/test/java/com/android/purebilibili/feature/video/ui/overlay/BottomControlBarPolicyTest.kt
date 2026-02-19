package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomControlBarPolicyTest {

    @Test
    fun aspectRatioButtonVisibleInFullscreen() {
        assertTrue(shouldShowAspectRatioButtonInControlBar(isFullscreen = true))
    }

    @Test
    fun aspectRatioButtonHiddenInPortraitMode() {
        assertFalse(shouldShowAspectRatioButtonInControlBar(isFullscreen = false))
    }

    @Test
    fun portraitSwitchButtonVisibleInFullscreen() {
        assertTrue(shouldShowPortraitSwitchButtonInControlBar(isFullscreen = true))
    }

    @Test
    fun portraitSwitchButtonHiddenInPortraitMode() {
        assertFalse(shouldShowPortraitSwitchButtonInControlBar(isFullscreen = false))
    }
}
