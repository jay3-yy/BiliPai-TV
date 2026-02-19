package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.store.PlaybackCompletionBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackOrderDisplayPolicyTest {

    @Test
    fun `compact label should be short for portrait`() {
        assertEquals(
            "顺播",
            resolvePlaybackOrderDisplayLabel(
                behavior = PlaybackCompletionBehavior.PLAY_IN_ORDER,
                compact = true
            )
        )
    }

    @Test
    fun `full label should use behavior text for landscape`() {
        assertEquals(
            "自动连播",
            resolvePlaybackOrderDisplayLabel(
                behavior = PlaybackCompletionBehavior.CONTINUE_CURRENT_LOGIC,
                compact = false
            )
        )
    }
}
