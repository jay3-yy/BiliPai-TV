package com.android.purebilibili.feature.video.screen

import android.view.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoDetailTvFocusPolicyTest {

    @Test
    fun tvInitialFocusDefaultsToPlayer() {
        val initial = resolveInitialVideoDetailTvFocusTarget(isTv = true)
        assertEquals(VideoDetailTvFocusTarget.PLAYER, initial)
    }

    @Test
    fun nonTvHasNoForcedInitialFocus() {
        val initial = resolveInitialVideoDetailTvFocusTarget(isTv = false)
        assertEquals(null, initial)
    }

    @Test
    fun downFromPlayerMovesToInfo() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.PLAYER,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoDetailTvFocusTarget.INFO, next)
    }

    @Test
    fun rightFromPlayerMovesToInfo() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.PLAYER,
            keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoDetailTvFocusTarget.INFO, next)
    }

    @Test
    fun downFromInfoMovesToRelated() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.INFO,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoDetailTvFocusTarget.RELATED, next)
    }

    @Test
    fun upFromRelatedMovesToInfo() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.RELATED,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoDetailTvFocusTarget.INFO, next)
    }

    @Test
    fun upFromInfoMovesToPlayer() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.INFO,
            keyCode = KeyEvent.KEYCODE_DPAD_UP,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoDetailTvFocusTarget.PLAYER, next)
    }

    @Test
    fun leftFromRelatedMovesToInfo() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.RELATED,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_DOWN
        )

        assertEquals(VideoDetailTvFocusTarget.INFO, next)
    }

    @Test
    fun nonNavigationKeyKeepsCurrentTarget() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.RELATED,
            keyCode = KeyEvent.KEYCODE_DPAD_LEFT,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoDetailTvFocusTarget.RELATED, next)
    }

    @Test
    fun directionalKeyUp_keepsCurrentTarget() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.PLAYER,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_UP
        )

        assertEquals(VideoDetailTvFocusTarget.PLAYER, next)
    }

    @Test
    fun focusLabelMatchesTarget() {
        assertEquals("播放器", resolveVideoDetailTvFocusLabel(VideoDetailTvFocusTarget.PLAYER))
        assertEquals("信息", resolveVideoDetailTvFocusLabel(VideoDetailTvFocusTarget.INFO))
        assertEquals("推荐", resolveVideoDetailTvFocusLabel(VideoDetailTvFocusTarget.RELATED))
    }

    @Test
    fun downFromInfoWithoutRelated_staysAtInfo() {
        val next = resolveVideoDetailTvFocusTarget(
            current = VideoDetailTvFocusTarget.INFO,
            keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
            action = KeyEvent.ACTION_DOWN,
            hasRelatedContent = false
        )

        assertEquals(VideoDetailTvFocusTarget.INFO, next)
    }

    @Test
    fun selectRelatedWithoutRelated_normalizesToInfo() {
        val normalized = normalizeVideoDetailTvFocusTarget(
            target = VideoDetailTvFocusTarget.RELATED,
            hasRelatedContent = false
        )

        assertEquals(VideoDetailTvFocusTarget.INFO, normalized)
    }
}
