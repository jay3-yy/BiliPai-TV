package com.android.purebilibili.feature.video.state

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerLifecyclePlaybackPolicyTest {

    @Test
    fun bufferingWithPlayWhenReadyIsTreatedAsActivePlayback() {
        assertTrue(
            isPlaybackActiveForLifecycle(
                isPlaying = false,
                playWhenReady = true,
                playbackState = Player.STATE_BUFFERING
            )
        )
    }

    @Test
    fun pausedReadyStateIsNotActivePlayback() {
        assertFalse(
            isPlaybackActiveForLifecycle(
                isPlaying = false,
                playWhenReady = false,
                playbackState = Player.STATE_READY
            )
        )
    }

    @Test
    fun resumeNeededWhenWasActiveButNowInactive() {
        assertTrue(
            shouldResumeAfterLifecyclePause(
                wasPlaybackActive = true,
                isPlaying = false,
                playWhenReady = false,
                playbackState = Player.STATE_READY
            )
        )
    }

    @Test
    fun resumeNotNeededWhenStillBuffering() {
        assertFalse(
            shouldResumeAfterLifecyclePause(
                wasPlaybackActive = true,
                isPlaying = false,
                playWhenReady = true,
                playbackState = Player.STATE_BUFFERING
            )
        )
    }
}
