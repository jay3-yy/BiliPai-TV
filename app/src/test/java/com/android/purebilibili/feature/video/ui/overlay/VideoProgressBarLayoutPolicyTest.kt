package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoProgressBarLayoutPolicyTest {

    @Test
    fun compactPhone_usesDenseProgressBarLayout() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(20, policy.baseHeightWithoutChapterDp)
        assertEquals(32, policy.baseHeightWithChapterDp)
        assertEquals(100, policy.draggingContainerHeightDp)
        assertEquals(10, policy.chapterFontSp)
        assertEquals(12, policy.thumbIdleSizeDp)
        assertEquals(16, policy.thumbDraggingSizeDp)
    }

    @Test
    fun mediumTablet_improvesSeekPrecisionAndHitArea() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(23, policy.baseHeightWithoutChapterDp)
        assertEquals(36, policy.baseHeightWithChapterDp)
        assertEquals(110, policy.draggingContainerHeightDp)
        assertEquals(11, policy.chapterFontSp)
        assertEquals(13, policy.thumbIdleSizeDp)
        assertEquals(17, policy.thumbDraggingSizeDp)
    }

    @Test
    fun tablet_expandsPreviewAndChapterReadability() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 1024,
            isTv = false
        )

        assertEquals(26, policy.baseHeightWithoutChapterDp)
        assertEquals(40, policy.baseHeightWithChapterDp)
        assertEquals(120, policy.draggingContainerHeightDp)
        assertEquals(12, policy.chapterFontSp)
        assertEquals(14, policy.thumbIdleSizeDp)
        assertEquals(18, policy.thumbDraggingSizeDp)
    }

    @Test
    fun tv_usesLargestSeekTrackAndThumbs() {
        val policy = resolveVideoProgressBarLayoutPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(32, policy.baseHeightWithoutChapterDp)
        assertEquals(48, policy.baseHeightWithChapterDp)
        assertEquals(140, policy.draggingContainerHeightDp)
        assertEquals(14, policy.chapterFontSp)
        assertEquals(16, policy.thumbIdleSizeDp)
        assertEquals(22, policy.thumbDraggingSizeDp)
    }
}
