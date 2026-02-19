package com.android.purebilibili.feature.tv

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.screen.VideoDetailTvFocusTarget
import com.android.purebilibili.feature.video.screen.resolveInitialVideoDetailTvFocusTarget
import com.android.purebilibili.feature.video.screen.resolveVideoDetailTvFocusTarget
import com.android.purebilibili.feature.video.ui.overlay.VideoOverlayTvFocusZone
import com.android.purebilibili.feature.video.ui.overlay.VideoOverlayTvBackAction
import com.android.purebilibili.feature.video.ui.overlay.VideoOverlayTvSelectAction
import com.android.purebilibili.feature.video.ui.overlay.resolveInitialVideoOverlayTvFocusZone
import com.android.purebilibili.feature.video.ui.overlay.resolveVideoOverlayTvBackAction
import com.android.purebilibili.feature.video.ui.overlay.resolveVideoOverlayTvFocusZone
import com.android.purebilibili.feature.video.ui.overlay.resolveVideoOverlayTvSelectAction
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvFocusNavigationTest {

    @Test
    fun videoDetail_firstFocus_defaultsToPlayerOnTv() {
        val initial = resolveInitialVideoDetailTvFocusTarget(isTv = true)
        assertEquals(VideoDetailTvFocusTarget.PLAYER, initial)
    }

    @Test
    fun videoDetail_dpadDownAndUp_movesBetweenPlayerAndContent() {
        val content = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.PLAYER,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP
        )
        val backToPlayer = resolveVideoDetailTvFocusTarget(
            current = content,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoDetailTvFocusTarget.CONTENT, content)
        assertEquals(VideoDetailTvFocusTarget.PLAYER, backToPlayer)
    }

    @Test
    fun videoOverlay_firstFocus_defaultsToCenterWhenVisible() {
        val initial = resolveInitialVideoOverlayTvFocusZone(
            isTv = true,
            overlayVisible = true
        )
        assertEquals(VideoOverlayTvFocusZone.CENTER, initial)
    }

    @Test
    fun videoOverlay_dpadGraph_centerRightLeftUpDown_isDeterministic() {
        val drawerEntry = resolveVideoOverlayTvFocusZone(
            current = VideoOverlayTvFocusZone.CENTER,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_UP
        )
        val centerAgain = resolveVideoOverlayTvFocusZone(
            current = drawerEntry,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_UP
        )
        val topBar = resolveVideoOverlayTvFocusZone(
            current = centerAgain,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_UP
        )
        val backToCenter = resolveVideoOverlayTvFocusZone(
            current = topBar,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP
        )
        val bottomBar = resolveVideoOverlayTvFocusZone(
            current = backToCenter,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoOverlayTvFocusZone.DRAWER_ENTRY, drawerEntry)
        assertEquals(VideoOverlayTvFocusZone.CENTER, centerAgain)
        assertEquals(VideoOverlayTvFocusZone.TOP_BAR, topBar)
        assertEquals(VideoOverlayTvFocusZone.CENTER, backToCenter)
        assertEquals(VideoOverlayTvFocusZone.BOTTOM_BAR, bottomBar)
    }

    @Test
    fun videoOverlay_selectAction_matchesFocusZone() {
        assertEquals(
            VideoOverlayTvSelectAction.BACK,
            resolveVideoOverlayTvSelectAction(VideoOverlayTvFocusZone.TOP_BAR)
        )
        assertEquals(
            VideoOverlayTvSelectAction.TOGGLE_PLAY_PAUSE,
            resolveVideoOverlayTvSelectAction(VideoOverlayTvFocusZone.CENTER)
        )
        assertEquals(
            VideoOverlayTvSelectAction.TOGGLE_DRAWER,
            resolveVideoOverlayTvSelectAction(VideoOverlayTvFocusZone.DRAWER_ENTRY)
        )
    }

    @Test
    fun videoOverlay_backKey_prioritizesDrawerDismiss_thenNavigateBack() {
        val dismissDrawer = resolveVideoOverlayTvBackAction(
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_UP,
            drawerVisible = true
        )
        val navigateBack = resolveVideoOverlayTvBackAction(
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_UP,
            drawerVisible = false
        )

        assertEquals(VideoOverlayTvBackAction.DISMISS_DRAWER, dismissDrawer)
        assertEquals(VideoOverlayTvBackAction.NAVIGATE_BACK, navigateBack)
    }
}
