package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.store.PlaybackCompletionBehavior

internal fun resolvePlaybackOrderDisplayLabel(
    behavior: PlaybackCompletionBehavior,
    compact: Boolean
): String {
    if (!compact) return behavior.label
    return when (behavior) {
        PlaybackCompletionBehavior.STOP_AFTER_CURRENT -> "暂停"
        PlaybackCompletionBehavior.PLAY_IN_ORDER -> "顺播"
        PlaybackCompletionBehavior.REPEAT_ONE -> "单循"
        PlaybackCompletionBehavior.LOOP_PLAYLIST -> "列循"
        PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC -> "连播"
    }
}
