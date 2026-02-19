package com.android.purebilibili.feature.video.screen

import android.view.KeyEvent

internal enum class VideoDetailTvFocusTarget {
    PLAYER,
    INFO,
    RELATED
}

internal fun resolveInitialVideoDetailTvFocusTarget(isTv: Boolean): VideoDetailTvFocusTarget? {
    return if (isTv) VideoDetailTvFocusTarget.PLAYER else null
}

internal fun resolveVideoDetailTvFocusLabel(target: VideoDetailTvFocusTarget): String {
    return when (target) {
        VideoDetailTvFocusTarget.PLAYER -> "播放器"
        VideoDetailTvFocusTarget.INFO -> "信息"
        VideoDetailTvFocusTarget.RELATED -> "推荐"
    }
}

internal fun normalizeVideoDetailTvFocusTarget(
    target: VideoDetailTvFocusTarget,
    hasRelatedContent: Boolean
): VideoDetailTvFocusTarget {
    return if (!hasRelatedContent && target == VideoDetailTvFocusTarget.RELATED) {
        VideoDetailTvFocusTarget.INFO
    } else {
        target
    }
}

internal fun resolveVideoDetailTvFocusTarget(
    current: VideoDetailTvFocusTarget,
    keyCode: Int,
    action: Int
): VideoDetailTvFocusTarget {
    if (action != KeyEvent.ACTION_DOWN) return current
    val moveToContent = keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
    val moveToPlayer = keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
    return when {
        current == VideoDetailTvFocusTarget.PLAYER && moveToContent -> {
            VideoDetailTvFocusTarget.INFO
        }

        current == VideoDetailTvFocusTarget.INFO && moveToContent -> {
            VideoDetailTvFocusTarget.RELATED
        }

        current == VideoDetailTvFocusTarget.RELATED && moveToPlayer -> {
            VideoDetailTvFocusTarget.INFO
        }

        current == VideoDetailTvFocusTarget.INFO && moveToPlayer -> {
            VideoDetailTvFocusTarget.PLAYER
        }

        else -> current
    }
}

internal fun resolveVideoDetailTvFocusTarget(
    current: VideoDetailTvFocusTarget,
    keyCode: Int,
    action: Int,
    hasRelatedContent: Boolean
): VideoDetailTvFocusTarget {
    val next = resolveVideoDetailTvFocusTarget(
        current = current,
        keyCode = keyCode,
        action = action
    )
    return normalizeVideoDetailTvFocusTarget(
        target = next,
        hasRelatedContent = hasRelatedContent
    )
}
