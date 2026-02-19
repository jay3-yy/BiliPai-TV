package com.android.purebilibili.feature.video.ui.overlay

data class VideoProgressBarLayoutPolicy(
    val baseHeightWithoutChapterDp: Int,
    val baseHeightWithChapterDp: Int,
    val draggingContainerHeightDp: Int,
    val previewBottomPaddingDp: Int,
    val chapterBottomPaddingDp: Int,
    val chapterStartPaddingDp: Int,
    val chapterIconSizeDp: Int,
    val chapterSpacingDp: Int,
    val chapterFontSp: Int,
    val touchContainerHeightDp: Int,
    val trackHeightDp: Float,
    val thumbIdleSizeDp: Int,
    val thumbDraggingSizeDp: Int,
    val thumbIdleOffsetDp: Int,
    val thumbDraggingOffsetDp: Int
)

fun resolveVideoProgressBarLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): VideoProgressBarLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return VideoProgressBarLayoutPolicy(
            baseHeightWithoutChapterDp = 32,
            baseHeightWithChapterDp = 48,
            draggingContainerHeightDp = 140,
            previewBottomPaddingDp = 30,
            chapterBottomPaddingDp = 6,
            chapterStartPaddingDp = 20,
            chapterIconSizeDp = 16,
            chapterSpacingDp = 8,
            chapterFontSp = 14,
            touchContainerHeightDp = 28,
            trackHeightDp = 5f,
            thumbIdleSizeDp = 16,
            thumbDraggingSizeDp = 22,
            thumbIdleOffsetDp = 8,
            thumbDraggingOffsetDp = 10
        )
    }

    if (widthDp >= 840) {
        return VideoProgressBarLayoutPolicy(
            baseHeightWithoutChapterDp = 26,
            baseHeightWithChapterDp = 40,
            draggingContainerHeightDp = 120,
            previewBottomPaddingDp = 26,
            chapterBottomPaddingDp = 5,
            chapterStartPaddingDp = 16,
            chapterIconSizeDp = 14,
            chapterSpacingDp = 6,
            chapterFontSp = 12,
            touchContainerHeightDp = 24,
            trackHeightDp = 4f,
            thumbIdleSizeDp = 14,
            thumbDraggingSizeDp = 18,
            thumbIdleOffsetDp = 7,
            thumbDraggingOffsetDp = 9
        )
    }

    if (widthDp >= 600) {
        return VideoProgressBarLayoutPolicy(
            baseHeightWithoutChapterDp = 23,
            baseHeightWithChapterDp = 36,
            draggingContainerHeightDp = 110,
            previewBottomPaddingDp = 25,
            chapterBottomPaddingDp = 4,
            chapterStartPaddingDp = 14,
            chapterIconSizeDp = 13,
            chapterSpacingDp = 5,
            chapterFontSp = 11,
            touchContainerHeightDp = 22,
            trackHeightDp = 3.5f,
            thumbIdleSizeDp = 13,
            thumbDraggingSizeDp = 17,
            thumbIdleOffsetDp = 6,
            thumbDraggingOffsetDp = 8
        )
    }

    return VideoProgressBarLayoutPolicy(
        baseHeightWithoutChapterDp = 20,
        baseHeightWithChapterDp = 32,
        draggingContainerHeightDp = 100,
        previewBottomPaddingDp = 24,
        chapterBottomPaddingDp = 4,
        chapterStartPaddingDp = 12,
        chapterIconSizeDp = 12,
        chapterSpacingDp = 4,
        chapterFontSp = 10,
        touchContainerHeightDp = 20,
        trackHeightDp = 3f,
        thumbIdleSizeDp = 12,
        thumbDraggingSizeDp = 16,
        thumbIdleOffsetDp = 6,
        thumbDraggingOffsetDp = 8
    )
}
