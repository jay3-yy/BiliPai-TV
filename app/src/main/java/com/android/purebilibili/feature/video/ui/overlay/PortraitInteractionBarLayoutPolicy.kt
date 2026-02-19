package com.android.purebilibili.feature.video.ui.overlay

data class PortraitInteractionBarLayoutPolicy(
    val endPaddingDp: Int,
    val bottomPaddingDp: Int,
    val itemSpacingDp: Int,
    val iconSizeDp: Int,
    val labelTopSpacingDp: Int,
    val labelFontSp: Int
)

fun resolvePortraitInteractionBarLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): PortraitInteractionBarLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return PortraitInteractionBarLayoutPolicy(
            endPaddingDp = 18,
            bottomPaddingDp = 220,
            itemSpacingDp = 28,
            iconSizeDp = 46,
            labelTopSpacingDp = 4,
            labelFontSp = 15
        )
    }

    if (widthDp >= 840) {
        return PortraitInteractionBarLayoutPolicy(
            endPaddingDp = 12,
            bottomPaddingDp = 196,
            itemSpacingDp = 24,
            iconSizeDp = 40,
            labelTopSpacingDp = 3,
            labelFontSp = 13
        )
    }

    if (widthDp >= 600) {
        return PortraitInteractionBarLayoutPolicy(
            endPaddingDp = 10,
            bottomPaddingDp = 188,
            itemSpacingDp = 22,
            iconSizeDp = 37,
            labelTopSpacingDp = 2,
            labelFontSp = 12
        )
    }

    return PortraitInteractionBarLayoutPolicy(
        endPaddingDp = 8,
        bottomPaddingDp = 180,
        itemSpacingDp = 20,
        iconSizeDp = 34,
        labelTopSpacingDp = 2,
        labelFontSp = 12
    )
}
