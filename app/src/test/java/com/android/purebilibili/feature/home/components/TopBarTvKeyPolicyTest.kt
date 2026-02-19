package com.android.purebilibili.feature.home.components

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopBarTvKeyPolicyTest {

    @Test
    fun tvSelectKey_triggersTopBarActionOnActionUp() {
        assertTrue(
            shouldTriggerTopBarActionOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP
            )
        )
        assertTrue(
            shouldTriggerTopBarActionOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_ENTER,
                action = KeyEvent.ACTION_UP
            )
        )
    }

    @Test
    fun tvSelectKey_ignoresActionDown_and_nonTv() {
        assertFalse(
            shouldTriggerTopBarActionOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_DOWN
            )
        )
        assertFalse(
            shouldTriggerTopBarActionOnTvKey(
                isTv = false,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP
            )
        )
    }
}
