package com.android.purebilibili.feature.video.ui.overlay

data class TopControlBarLayoutPolicy(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val timeFontSp: Int,
    val timeBottomSpacingDp: Int,
    val buttonSizeDp: Int,
    val iconSizeDp: Int,
    val backToTitleSpacingDp: Int,
    val sectionGapDp: Int,
    val actionSpacingDp: Int,
    val titleFontSp: Int,
    val onlineCountStartPaddingDp: Int,
    val onlineCountFontSp: Int,
    val onlineCountTopPaddingDp: Int
)

fun resolveTopControlBarLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): TopControlBarLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return TopControlBarLayoutPolicy(
            horizontalPaddingDp = 32,
            verticalPaddingDp = 12,
            timeFontSp = 15,
            timeBottomSpacingDp = 8,
            buttonSizeDp = 44,
            iconSizeDp = 30,
            backToTitleSpacingDp = 20,
            sectionGapDp = 28,
            actionSpacingDp = 30,
            titleFontSp = 17,
            onlineCountStartPaddingDp = 60,
            onlineCountFontSp = 13,
            onlineCountTopPaddingDp = 3
        )
    }

    if (widthDp >= 1200) {
        return TopControlBarLayoutPolicy(
            horizontalPaddingDp = 28,
            verticalPaddingDp = 11,
            timeFontSp = 14,
            timeBottomSpacingDp = 7,
            buttonSizeDp = 40,
            iconSizeDp = 28,
            backToTitleSpacingDp = 18,
            sectionGapDp = 26,
            actionSpacingDp = 28,
            titleFontSp = 17,
            onlineCountStartPaddingDp = 56,
            onlineCountFontSp = 12,
            onlineCountTopPaddingDp = 2
        )
    }

    if (widthDp >= 600) {
        return TopControlBarLayoutPolicy(
            horizontalPaddingDp = 26,
            verticalPaddingDp = 10,
            timeFontSp = 13,
            timeBottomSpacingDp = 6,
            buttonSizeDp = 36,
            iconSizeDp = 26,
            backToTitleSpacingDp = 16,
            sectionGapDp = 24,
            actionSpacingDp = 26,
            titleFontSp = 16,
            onlineCountStartPaddingDp = 52,
            onlineCountFontSp = 12,
            onlineCountTopPaddingDp = 2
        )
    }

    return TopControlBarLayoutPolicy(
        horizontalPaddingDp = 24,
        verticalPaddingDp = 10,
        timeFontSp = 12,
        timeBottomSpacingDp = 5,
        buttonSizeDp = 32,
        iconSizeDp = 24,
        backToTitleSpacingDp = 14,
        sectionGapDp = 20,
        actionSpacingDp = 24,
        titleFontSp = 16,
        onlineCountStartPaddingDp = 48,
        onlineCountFontSp = 11,
        onlineCountTopPaddingDp = 2
    )
}
