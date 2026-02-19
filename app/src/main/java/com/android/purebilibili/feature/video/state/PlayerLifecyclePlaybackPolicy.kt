package com.android.purebilibili.feature.video.state

import androidx.media3.common.Player

internal fun isPlaybackActiveForLifecycle(
    isPlaying: Boolean,
    playWhenReady: Boolean,
    playbackState: Int
): Boolean {
    return isPlaying || (playWhenReady && playbackState == Player.STATE_BUFFERING)
}

internal fun shouldResumeAfterLifecyclePause(
    wasPlaybackActive: Boolean,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    playbackState: Int
): Boolean {
    val currentlyActive = isPlaybackActiveForLifecycle(
        isPlaying = isPlaying,
        playWhenReady = playWhenReady,
        playbackState = playbackState
    )
    return wasPlaybackActive && !currentlyActive
}
