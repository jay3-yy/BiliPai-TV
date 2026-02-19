package com.android.purebilibili.feature.video.ui.overlay

data class PortraitTopBarLayoutPolicy(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val leftSectionSpacingDp: Int,
    val rightSectionSpacingDp: Int,
    val buttonSizeDp: Int,
    val iconSizeDp: Int,
    val chipFontSp: Int,
    val chipHorizontalPaddingDp: Int,
    val chipVerticalPaddingDp: Int,
    val onlineCountFontSp: Int
)

fun resolvePortraitTopBarLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): PortraitTopBarLayoutPolicy {
    if (isTv || widthDp >= 1400) {
        return PortraitTopBarLayoutPolicy(
            horizontalPaddingDp = 24,
            verticalPaddingDp = 12,
            leftSectionSpacingDp = 14,
            rightSectionSpacingDp = 10,
            buttonSizeDp = 52,
            iconSizeDp = 28,
            chipFontSp = 15,
            chipHorizontalPaddingDp = 14,
            chipVerticalPaddingDp = 8,
            onlineCountFontSp = 14
        )
    }

    if (widthDp >= 840) {
        return PortraitTopBarLayoutPolicy(
            horizontalPaddingDp = 16,
            verticalPaddingDp = 10,
            leftSectionSpacingDp = 10,
            rightSectionSpacingDp = 8,
            buttonSizeDp = 40,
            iconSizeDp = 22,
            chipFontSp = 13,
            chipHorizontalPaddingDp = 10,
            chipVerticalPaddingDp = 6,
            onlineCountFontSp = 12
        )
    }

    if (widthDp >= 600) {
        return PortraitTopBarLayoutPolicy(
            horizontalPaddingDp = 12,
            verticalPaddingDp = 9,
            leftSectionSpacingDp = 9,
            rightSectionSpacingDp = 6,
            buttonSizeDp = 36,
            iconSizeDp = 20,
            chipFontSp = 12,
            chipHorizontalPaddingDp = 9,
            chipVerticalPaddingDp = 5,
            onlineCountFontSp = 11
        )
    }

    return PortraitTopBarLayoutPolicy(
        horizontalPaddingDp = 8,
        verticalPaddingDp = 8,
        leftSectionSpacingDp = 8,
        rightSectionSpacingDp = 4,
        buttonSizeDp = 32,
        iconSizeDp = 18,
        chipFontSp = 11,
        chipHorizontalPaddingDp = 8,
        chipVerticalPaddingDp = 4,
        onlineCountFontSp = 11
    )
}
