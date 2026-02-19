package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.core.store.PlaybackCompletionBehavior

internal enum class PlaybackEndAction {
    STOP,
    PLAY_NEXT_IN_PLAYLIST,
    PLAY_NEXT_IN_PLAYLIST_LOOP,
    REPEAT_CURRENT,
    AUTO_CONTINUE
}

internal fun resolvePlaybackEndAction(
    behavior: PlaybackCompletionBehavior,
    autoPlayEnabled: Boolean,
    isExternalPlaylist: Boolean
): PlaybackEndAction {
    return when (behavior) {
        PlaybackCompletionBehavior.STOP_AFTER_CURRENT -> PlaybackEndAction.STOP
        PlaybackCompletionBehavior.PLAY_IN_ORDER -> PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST
        PlaybackCompletionBehavior.REPEAT_ONE -> PlaybackEndAction.REPEAT_CURRENT
        PlaybackCompletionBehavior.LOOP_PLAYLIST -> PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP
        PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC -> {
            if (autoPlayEnabled || isExternalPlaylist) {
                PlaybackEndAction.AUTO_CONTINUE
            } else {
                PlaybackEndAction.STOP
            }
        }
    }
}
