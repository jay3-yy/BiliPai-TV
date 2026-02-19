package com.android.purebilibili.feature.home.components.cards

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCardTvKeyPolicyTest {

    @Test
    fun tvSelectKey_triggersClickOnActionUp() {
        assertTrue(
            shouldTriggerHomeCardClickOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP
            )
        )
        assertTrue(
            shouldTriggerHomeCardClickOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_ENTER,
                action = KeyEvent.ACTION_UP
            )
        )
    }

    @Test
    fun tvSelectKey_ignoresActionDown_and_nonTv() {
        assertFalse(
            shouldTriggerHomeCardClickOnTvKey(
                isTv = true,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_DOWN
            )
        )
        assertFalse(
            shouldTriggerHomeCardClickOnTvKey(
                isTv = false,
                keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
                action = KeyEvent.ACTION_UP
            )
        )
    }
}
