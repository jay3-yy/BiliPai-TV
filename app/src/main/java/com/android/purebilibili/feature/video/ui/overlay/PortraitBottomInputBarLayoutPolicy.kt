package com.android.purebilibili.feature.video.ui.overlay

data class PortraitBottomInputBarLayoutPolicy(
    val horizontalPaddingDp: Int,
    val verticalPaddingDp: Int,
    val inputHeightDp: Int,
    val inputHorizontalPaddingDp: Int,
    val inputFontSp: Int,
    val afterInputSpacingDp: Int,
    val actionSpacingDp: Int,
    val actionButtonSizeDp: Int,
    val actionIconSizeDp: Int
)

fun resolvePortraitBottomInputBarLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): PortraitBottomInputBarLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return PortraitBottomInputBarLayoutPolicy(
            horizontalPaddingDp = 20,
            verticalPaddingDp = 12,
            inputHeightDp = 52,
            inputHorizontalPaddingDp = 20,
            inputFontSp = 18,
            afterInputSpacingDp = 16,
            actionSpacingDp = 10,
            actionButtonSizeDp = 54,
            actionIconSizeDp = 32
        )
    }

    if (widthDp >= 840) {
        return PortraitBottomInputBarLayoutPolicy(
            horizontalPaddingDp = 16,
            verticalPaddingDp = 10,
            inputHeightDp = 44,
            inputHorizontalPaddingDp = 16,
            inputFontSp = 16,
            afterInputSpacingDp = 14,
            actionSpacingDp = 8,
            actionButtonSizeDp = 46,
            actionIconSizeDp = 28
        )
    }

    if (widthDp >= 600) {
        return PortraitBottomInputBarLayoutPolicy(
            horizontalPaddingDp = 14,
            verticalPaddingDp = 9,
            inputHeightDp = 40,
            inputHorizontalPaddingDp = 14,
            inputFontSp = 15,
            afterInputSpacingDp = 13,
            actionSpacingDp = 7,
            actionButtonSizeDp = 43,
            actionIconSizeDp = 26
        )
    }

    return PortraitBottomInputBarLayoutPolicy(
        horizontalPaddingDp = 12,
        verticalPaddingDp = 8,
        inputHeightDp = 36,
        inputHorizontalPaddingDp = 12,
        inputFontSp = 14,
        afterInputSpacingDp = 12,
        actionSpacingDp = 6,
        actionButtonSizeDp = 40,
        actionIconSizeDp = 24
    )
}
