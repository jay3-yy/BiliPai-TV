package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackCompletionPolicyTest {

    @Test
    fun `stop mode always stops after ended`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.STOP_AFTER_CURRENT,
                autoPlayEnabled = true,
                isExternalPlaylist = true
            )
        )
    }

    @Test
    fun `play in order should request next in playlist`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER,
                autoPlayEnabled = false,
                isExternalPlaylist = false
            )
        )
    }

    @Test
    fun `repeat one should replay current`() {
        assertEquals(
            PlaybackEndAction.REPEAT_CURRENT,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.REPEAT_ONE,
                autoPlayEnabled = false,
                isExternalPlaylist = true
            )
        )
    }

    @Test
    fun `loop playlist should request next with loop`() {
        assertEquals(
            PlaybackEndAction.PLAY_NEXT_IN_PLAYLIST_LOOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.LOOP_PLAYLIST,
                autoPlayEnabled = false,
                isExternalPlaylist = false
            )
        )
    }

    @Test
    fun `auto continue uses legacy autoplay gate for normal videos`() {
        assertEquals(
            PlaybackEndAction.STOP,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = false
            )
        )
        assertEquals(
            PlaybackEndAction.AUTO_CONTINUE,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = true,
                isExternalPlaylist = false
            )
        )
    }

    @Test
    fun `auto continue still continues for external playlist when autoplay off`() {
        assertEquals(
            PlaybackEndAction.AUTO_CONTINUE,
            resolvePlaybackEndAction(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                autoPlayEnabled = false,
                isExternalPlaylist = true
            )
        )
    }
}
