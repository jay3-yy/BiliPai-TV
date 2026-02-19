package com.android.purebilibili.feature.video.ui.overlay

data class BottomRightControlsLayoutPolicy(
    val rowSpacingDp: Int,
    val menuOffsetYDp: Int,
    val chipCornerRadiusDp: Int,
    val chipFontSp: Int,
    val chipHorizontalPaddingDp: Int,
    val chipVerticalPaddingDp: Int
)

fun resolveBottomRightControlsLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): BottomRightControlsLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return BottomRightControlsLayoutPolicy(
            rowSpacingDp = 12,
            menuOffsetYDp = -12,
            chipCornerRadiusDp = 8,
            chipFontSp = 14,
            chipHorizontalPaddingDp = 12,
            chipVerticalPaddingDp = 7
        )
    }

    if (widthDp >= 840) {
        return BottomRightControlsLayoutPolicy(
            rowSpacingDp = 10,
            menuOffsetYDp = -11,
            chipCornerRadiusDp = 7,
            chipFontSp = 13,
            chipHorizontalPaddingDp = 11,
            chipVerticalPaddingDp = 6
        )
    }

    if (widthDp >= 600) {
        return BottomRightControlsLayoutPolicy(
            rowSpacingDp = 9,
            menuOffsetYDp = -10,
            chipCornerRadiusDp = 6,
            chipFontSp = 12,
            chipHorizontalPaddingDp = 10,
            chipVerticalPaddingDp = 6
        )
    }

    return BottomRightControlsLayoutPolicy(
        rowSpacingDp = 8,
        menuOffsetYDp = -10,
        chipCornerRadiusDp = 6,
        chipFontSp = 12,
        chipHorizontalPaddingDp = 10,
        chipVerticalPaddingDp = 6
    )
}
