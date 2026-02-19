package com.android.purebilibili.feature.home.components

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SideBarTvKeyPolicyTest {

    @Test
    fun tvSelectKey_triggersSidebarItemClickOnActionUp() {
        assertTrue(
            shouldTriggerSideBarItemClickOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP
            )
        )
        assertTrue(
            shouldTriggerSideBarItemClickOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_ENTER,
                action = KeyEvent.ACTION_UP
            )
        )
    }

    @Test
    fun tvSelectKey_ignoresActionDown_and_nonTv() {
        assertFalse(
            shouldTriggerSideBarItemClickOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_DOWN
            )
        )
        assertFalse(
            shouldTriggerSideBarItemClickOnTvKey(
                isTv = false,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP
            )
        )
    }
}
