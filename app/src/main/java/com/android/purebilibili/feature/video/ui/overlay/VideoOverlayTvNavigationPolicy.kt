package com.android.purebilibili.feature.video.ui.overlay

import android.view.KeyEvent

internal enum class VideoOverlayTvFocusZone {
    TOP_BAR,
    CENTER,
    BOTTOM_BAR,
    DRAWER_ENTRY
}

internal enum class VideoOverlayTvSelectAction {
    BACK,
    TOGGLE_PLAY_PAUSE,
    TOGGLE_DRAWER,
    NOOP
}

internal enum class VideoOverlayTvBackAction {
    DISMISS_DRAWER,
    NAVIGATE_BACK,
    NOOP
}

internal fun resolveInitialVideoOverlayTvFocusZone(
    isTv: Boolean,
    overlayVisible: Boolean
): VideoOverlayTvFocusZone? {
    return if (isTv && overlayVisible) VideoOverlayTvFocusZone.CENTER else null
}

internal fun resolveVideoOverlayTvFocusZone(
    current: VideoOverlayTvFocusZone,
    keyCode: Int,
    action: Int
): VideoOverlayTvFocusZone {
    if (action != KeyEvent.ACTION_DOWN) return current
    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> when (current) {
            VideoOverlayTvFocusZone.CENTER -> VideoOverlayTvFocusZone.TOP_BAR
            VideoOverlayTvFocusZone.BOTTOM_BAR -> VideoOverlayTvFocusZone.CENTER
            else -> current
        }

        KeyEvent.KEYCODE_DPAD_DOWN -> when (current) {
            VideoOverlayTvFocusZone.TOP_BAR -> VideoOverlayTvFocusZone.CENTER
            VideoOverlayTvFocusZone.CENTER -> VideoOverlayTvFocusZone.BOTTOM_BAR
            else -> current
        }

        KeyEvent.KEYCODE_DPAD_RIGHT -> when (current) {
            VideoOverlayTvFocusZone.CENTER -> VideoOverlayTvFocusZone.DRAWER_ENTRY
            else -> current
        }

        KeyEvent.KEYCODE_DPAD_LEFT -> when (current) {
            VideoOverlayTvFocusZone.DRAWER_ENTRY -> VideoOverlayTvFocusZone.CENTER
            else -> current
        }

        else -> current
    }
}

internal fun resolveVideoOverlayTvSelectAction(
    focusZone: VideoOverlayTvFocusZone
): VideoOverlayTvSelectAction {
    return when (focusZone) {
        VideoOverlayTvFocusZone.TOP_BAR -> VideoOverlayTvSelectAction.BACK
        VideoOverlayTvFocusZone.CENTER -> VideoOverlayTvSelectAction.TOGGLE_PLAY_PAUSE
        VideoOverlayTvFocusZone.DRAWER_ENTRY -> VideoOverlayTvSelectAction.TOGGLE_DRAWER
        VideoOverlayTvFocusZone.BOTTOM_BAR -> VideoOverlayTvSelectAction.NOOP
    }
}

internal fun resolveVideoOverlayTvBackAction(
    keyCode: Int,
    action: Int,
    drawerVisible: Boolean
): VideoOverlayTvBackAction {
    if (keyCode != KeyEvent.KEYCODE_BACK || action != KeyEvent.ACTION_UP) {
        return VideoOverlayTvBackAction.NOOP
    }

    return if (drawerVisible) {
        VideoOverlayTvBackAction.DISMISS_DRAWER
    } else {
        VideoOverlayTvBackAction.NAVIGATE_BACK
    }
}
