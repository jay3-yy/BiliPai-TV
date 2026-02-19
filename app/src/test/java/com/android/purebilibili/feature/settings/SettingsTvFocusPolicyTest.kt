package com.android.purebilibili.feature.settings

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTvFocusPolicyTest {

    @Test
    fun initialFocus_defaultsToCategoryOnTv() {
        assertEquals(
            SettingsTvFocusZone.CATEGORY_LIST,
            resolveInitialSettingsTvFocusZone(isTv = true)
        )
    }

    @Test
    fun rightFromCategoryMovesToDetailPanel() {
        val transition = resolveSettingsTvFocusTransition(
            currentZone = SettingsTvFocusZone.CATEGORY_LIST,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(SettingsTvFocusZone.DETAIL_PANEL, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun leftFromDetailMovesBackToCategory() {
        val transition = resolveSettingsTvFocusTransition(
            currentZone = SettingsTvFocusZone.DETAIL_PANEL,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(SettingsTvFocusZone.CATEGORY_LIST, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun backFromDetailMovesBackToCategory() {
        val transition = resolveSettingsTvFocusTransition(
            currentZone = SettingsTvFocusZone.DETAIL_PANEL,
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(SettingsTvFocusZone.CATEGORY_LIST, transition.nextZone)
        assertEquals(true, transition.consumeEvent)
    }

    @Test
    fun backFromDetailOnKeyDown_doesNotTriggerTransition() {
        val transition = resolveSettingsTvFocusTransition(
            currentZone = SettingsTvFocusZone.DETAIL_PANEL,
            keyCode = KeyEvent.KEYCODE_BACK,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(SettingsTvFocusZone.DETAIL_PANEL, transition.nextZone)
        assertEquals(false, transition.consumeEvent)
    }
}
