package com.android.purebilibili.feature.video.ui.section

data class VideoPlayerUiLayoutPolicy(
    val gestureOverlaySizeDp: Int,
    val gestureIconSizeDp: Int,
    val seekFeedbackSizeDp: Int,
    val gestureBoundaryPaddingDp: Int,
    val restoreButtonBottomOffsetDp: Int,
    val restoreButtonHorizontalPaddingDp: Int,
    val restoreButtonVerticalPaddingDp: Int,
    val restoreButtonIconSizeDp: Int,
    val longPressBadgeHorizontalPaddingDp: Int,
    val longPressBadgeVerticalPaddingDp: Int
)

fun resolveVideoPlayerUiLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): VideoPlayerUiLayoutPolicy {
    if (isTv) {
        return VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 140,
            gestureIconSizeDp = 56,
            seekFeedbackSizeDp = 112,
            gestureBoundaryPaddingDp = 28,
            restoreButtonBottomOffsetDp = 112,
            restoreButtonHorizontalPaddingDp = 20,
            restoreButtonVerticalPaddingDp = 10,
            restoreButtonIconSizeDp = 18,
            longPressBadgeHorizontalPaddingDp = 24,
            longPressBadgeVerticalPaddingDp = 14
        )
    }

    return when {
        widthDp >= 1600 -> VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 140,
            gestureIconSizeDp = 56,
            seekFeedbackSizeDp = 112,
            gestureBoundaryPaddingDp = 28,
            restoreButtonBottomOffsetDp = 116,
            restoreButtonHorizontalPaddingDp = 20,
            restoreButtonVerticalPaddingDp = 10,
            restoreButtonIconSizeDp = 18,
            longPressBadgeHorizontalPaddingDp = 24,
            longPressBadgeVerticalPaddingDp = 14
        )
        widthDp >= 840 -> VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 132,
            gestureIconSizeDp = 52,
            seekFeedbackSizeDp = 108,
            gestureBoundaryPaddingDp = 26,
            restoreButtonBottomOffsetDp = 112,
            restoreButtonHorizontalPaddingDp = 18,
            restoreButtonVerticalPaddingDp = 9,
            restoreButtonIconSizeDp = 17,
            longPressBadgeHorizontalPaddingDp = 22,
            longPressBadgeVerticalPaddingDp = 13
        )
        else -> VideoPlayerUiLayoutPolicy(
            gestureOverlaySizeDp = 120,
            gestureIconSizeDp = 48,
            seekFeedbackSizeDp = 100,
            gestureBoundaryPaddingDp = 24,
            restoreButtonBottomOffsetDp = 100,
            restoreButtonHorizontalPaddingDp = 16,
            restoreButtonVerticalPaddingDp = 8,
            restoreButtonIconSizeDp = 16,
            longPressBadgeHorizontalPaddingDp = 20,
            longPressBadgeVerticalPaddingDp = 12
        )
    }
}
