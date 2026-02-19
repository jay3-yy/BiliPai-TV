package com.android.purebilibili.feature.video.ui.overlay

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoOverlayTvNavigationPolicyTest {

    @Test
    fun tvVisibleOverlayStartsFromCenterFocusZone() {
        val initial = resolveInitialVideoOverlayTvFocusZone(
            isTv = true,
            overlayVisible = true
        )

        assertEquals(VideoOverlayTvFocusZone.CENTER, initial)
    }

    @Test
    fun hiddenOverlayDoesNotRequestInitialFocus() {
        val initial = resolveInitialVideoOverlayTvFocusZone(
            isTv = true,
            overlayVisible = false
        )

        assertEquals(null, initial)
    }

    @Test
    fun rightFromCenterMovesToDrawerEntry() {
        val next = resolveVideoOverlayTvFocusZone(
            current = VideoOverlayTvFocusZone.CENTER,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoOverlayTvFocusZone.DRAWER_ENTRY, next)
    }

    @Test
    fun leftFromDrawerEntryReturnsCenter() {
        val next = resolveVideoOverlayTvFocusZone(
            current = VideoOverlayTvFocusZone.DRAWER_ENTRY,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoOverlayTvFocusZone.CENTER, next)
    }

    @Test
    fun upFromCenterMovesToTop() {
        val next = resolveVideoOverlayTvFocusZone(
            current = VideoOverlayTvFocusZone.CENTER,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoOverlayTvFocusZone.TOP_BAR, next)
    }

    @Test
    fun downFromCenterMovesToBottom() {
        val next = resolveVideoOverlayTvFocusZone(
            current = VideoOverlayTvFocusZone.CENTER,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoOverlayTvFocusZone.BOTTOM_BAR, next)
    }

    @Test
    fun selectOnDrawerEntryTogglesDrawer() {
        val action = resolveVideoOverlayTvSelectAction(VideoOverlayTvFocusZone.DRAWER_ENTRY)
        assertEquals(VideoOverlayTvSelectAction.TOGGLE_DRAWER, action)
    }

    @Test
    fun selectOnTopTriggersBack() {
        val action = resolveVideoOverlayTvSelectAction(VideoOverlayTvFocusZone.TOP_BAR)
        assertEquals(VideoOverlayTvSelectAction.BACK, action)
    }

    @Test
    fun selectOnCenterTogglesPlayPause() {
        val action = resolveVideoOverlayTvSelectAction(VideoOverlayTvFocusZone.CENTER)
        assertEquals(VideoOverlayTvSelectAction.TOGGLE_PLAY_PAUSE, action)
    }

    @Test
    fun backKey_withDrawerVisible_dismissesDrawerFirst() {
        val action = resolveVideoOverlayTvBackAction(
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_UP,
            drawerVisible = true
        )

        assertEquals(VideoOverlayTvBackAction.DISMISS_DRAWER, action)
    }

    @Test
    fun backKey_withoutDrawerVisible_navigatesBack() {
        val action = resolveVideoOverlayTvBackAction(
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_UP,
            drawerVisible = false
        )

        assertEquals(VideoOverlayTvBackAction.NAVIGATE_BACK, action)
    }

    @Test
    fun nonBackKey_doesNotTriggerBackAction() {
        val action = resolveVideoOverlayTvBackAction(
            keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
            action = KeyEvent.ACTION_UP,
            drawerVisible = false
        )

        assertEquals(VideoOverlayTvBackAction.NOOP, action)
    }

    @Test
    fun directionalKeyUp_doesNotMoveFocusZone() {
        val next = resolveVideoOverlayTvFocusZone(
            current = VideoOverlayTvFocusZone.CENTER,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoOverlayTvFocusZone.CENTER, next)
    }
}
