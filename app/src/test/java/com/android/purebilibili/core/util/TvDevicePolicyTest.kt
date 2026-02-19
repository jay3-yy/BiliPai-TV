package com.android.purebilibili.core.util

import android.content.res.Configuration
import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvDevicePolicyTest {

    @Test
    fun leanbackFeatureForcesTvMode() {
        assertTrue(
            shouldTreatAsTvDevice(
                hasLeanbackFeature = true,
                uiModeType = Configuration.UI_MODE_TYPE_NORMAL
            )
        )
    }

    @Test
    fun televisionUiModeForcesTvMode() {
        assertTrue(
            shouldTreatAsTvDevice(
                hasLeanbackFeature = false,
                uiModeType = Configuration.UI_MODE_TYPE_TELEVISION
            )
        )
    }

    @Test
    fun normalPhoneModeIsNotTv() {
        assertFalse(
            shouldTreatAsTvDevice(
                hasLeanbackFeature = false,
                uiModeType = Configuration.UI_MODE_TYPE_NORMAL
            )
        )
    }

    @Test
    fun selectKeyHandledOnKeyUp() {
        assertTrue(shouldHandleTvSelectKey(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_UP))
        assertTrue(shouldHandleTvSelectKey(KeyEvent.KEYCODE_ENTER, KeyEvent.ACTION_UP))
        assertTrue(shouldHandleTvSelectKey(KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.ACTION_UP))
    }

    @Test
    fun selectKeyIgnoredOnKeyDown() {
        assertFalse(shouldHandleTvSelectKey(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_DOWN))
    }

    @Test
    fun nonSelectKeyIgnored() {
        assertFalse(shouldHandleTvSelectKey(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_UP))
    }

    @Test
    fun pagerRightMovesToNextPageWithinBounds() {
        val target = resolveTvPagerTargetPage(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_DOWN,
            currentPage = 1,
            pageCount = 4
        )
        assertTrue(target == 2)
    }

    @Test
    fun pagerLeftMovesToPreviousPageWithinBounds() {
        val target = resolveTvPagerTargetPage(
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_DOWN,
            currentPage = 2,
            pageCount = 4
        )
        assertTrue(target == 1)
    }

    @Test
    fun pagerNavigationIgnoredAtBounds() {
        val leftBound = resolveTvPagerTargetPage(
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_DOWN,
            currentPage = 0,
            pageCount = 4
        )
        val rightBound = resolveTvPagerTargetPage(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_DOWN,
            currentPage = 3,
            pageCount = 4
        )
        assertTrue(leftBound == null)
        assertTrue(rightBound == null)
    }

    @Test
    fun pagerNavigationIgnoredOnKeyUp() {
        val target = resolveTvPagerTargetPage(
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_UP,
            currentPage = 1,
            pageCount = 4
        )
        assertTrue(target == null)
    }

    @Test
    fun playPauseKeyHandledOnKeyUp() {
        assertTrue(shouldHandleTvPlayPauseKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.ACTION_UP))
        assertTrue(shouldHandleTvPlayPauseKey(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.ACTION_UP))
        assertTrue(shouldHandleTvPlayPauseKey(KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.ACTION_UP))
    }

    @Test
    fun backAndMenuKeysHandledOnKeyUp() {
        assertTrue(shouldHandleTvBackKey(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP))
        assertTrue(shouldHandleTvMenuKey(KeyEvent.KEYCODE_MENU, KeyEvent.ACTION_UP))
    }

    @Test
    fun moveFocusDownKeyHandledOnKeyDown() {
        assertTrue(shouldHandleTvMoveFocusDownKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN))
    }

    @Test
    fun moveFocusDownKeyIgnoredOnKeyUp() {
        assertFalse(shouldHandleTvMoveFocusDownKey(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_UP))
    }

    @Test
    fun tvPerformanceProfileForcesLowCostHomeVisuals() {
        val config = resolveTvHomePerformanceConfig(
            isTvDevice = true,
            isTvPerformanceProfileEnabled = true,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = true,
            liquidGlassEnabled = true,
            cardAnimationEnabled = true,
            cardTransitionEnabled = true,
            isDataSaverActive = false,
            normalPreloadAheadCount = 5
        )

        assertFalse(config.headerBlurEnabled)
        assertFalse(config.bottomBarBlurEnabled)
        assertFalse(config.liquidGlassEnabled)
        assertFalse(config.cardAnimationEnabled)
        assertFalse(config.cardTransitionEnabled)
        assertTrue(config.isDataSaverActive)
        assertTrue(config.preloadAheadCount == 0)
    }

    @Test
    fun tvPerformanceProfileDisabledKeepsHomeVisualSettings() {
        val config = resolveTvHomePerformanceConfig(
            isTvDevice = true,
            isTvPerformanceProfileEnabled = false,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = false,
            liquidGlassEnabled = true,
            cardAnimationEnabled = false,
            cardTransitionEnabled = true,
            isDataSaverActive = false,
            normalPreloadAheadCount = 5
        )

        assertTrue(config.headerBlurEnabled)
        assertFalse(config.bottomBarBlurEnabled)
        assertTrue(config.liquidGlassEnabled)
        assertFalse(config.cardAnimationEnabled)
        assertTrue(config.cardTransitionEnabled)
        assertFalse(config.isDataSaverActive)
        assertTrue(config.preloadAheadCount == 5)
    }

    @Test
    fun dataSaverStillDisablesPreloadWhenTvProfileInactive() {
        val config = resolveTvHomePerformanceConfig(
            isTvDevice = false,
            isTvPerformanceProfileEnabled = true,
            headerBlurEnabled = true,
            bottomBarBlurEnabled = true,
            liquidGlassEnabled = true,
            cardAnimationEnabled = true,
            cardTransitionEnabled = true,
            isDataSaverActive = true,
            normalPreloadAheadCount = 5
        )

        assertTrue(config.isDataSaverActive)
        assertTrue(config.preloadAheadCount == 0)
    }
}
