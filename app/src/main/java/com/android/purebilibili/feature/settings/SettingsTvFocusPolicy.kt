package com.android.purebilibili.feature.settings

import android.view.KeyEvent

internal enum class SettingsTvFocusZone {
    CATEGORY_LIST,
    DETAIL_PANEL
}

internal data class SettingsTvFocusTransition(
    val nextZone: SettingsTvFocusZone,
    val consumeEvent: Boolean
)

private fun shouldHandleSettingsTvFocusKey(
    keyCode: Int,
    action: Int
): Boolean {
    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT -> action == KeyEvent.ACTION_DOWN

        KeyEvent.KEYCODE_BACK -> action == KeyEvent.ACTION_UP
        else -> false
    }
}

internal fun resolveInitialSettingsTvFocusZone(isTv: Boolean): SettingsTvFocusZone? {
    return if (isTv) SettingsTvFocusZone.CATEGORY_LIST else null
}

internal fun resolveSettingsTvFocusTransition(
    currentZone: SettingsTvFocusZone,
    keyCode: Int,
    action: Int
): SettingsTvFocusTransition {
    if (!shouldHandleSettingsTvFocusKey(keyCode = keyCode, action = action)) {
        return SettingsTvFocusTransition(nextZone = currentZone, consumeEvent = false)
    }

    return when (currentZone) {
        SettingsTvFocusZone.CATEGORY_LIST -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                SettingsTvFocusTransition(
                    nextZone = SettingsTvFocusZone.DETAIL_PANEL,
                    consumeEvent = true
                )
            } else {
                SettingsTvFocusTransition(nextZone = currentZone, consumeEvent = false)
            }
        }

        SettingsTvFocusZone.DETAIL_PANEL -> {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_BACK) {
                SettingsTvFocusTransition(
                    nextZone = SettingsTvFocusZone.CATEGORY_LIST,
                    consumeEvent = true
                )
            } else {
                SettingsTvFocusTransition(nextZone = currentZone, consumeEvent = false)
            }
        }
    }
}
