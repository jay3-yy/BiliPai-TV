package com.android.purebilibili.feature.video.ui.overlay

data class PortraitFullscreenOverlayLayoutPolicy(
    val compactMode: Boolean,
    val topHorizontalPaddingDp: Int,
    val topVerticalPaddingDp: Int,
    val topBackButtonSizeDp: Int,
    val topBackIconSizeDp: Int,
    val topActionIconSizeDp: Int,
    val topActionSpacingDp: Int,
    val topViewCountFontSp: Int,
    val topViewCountStartSpacingDp: Int,
    val infoWidthFraction: Float,
    val infoHorizontalPaddingDp: Int,
    val infoBottomPaddingDp: Int,
    val bottomInputSpacerHeightDp: Int,
    val authorRowBottomPaddingDp: Int,
    val avatarSizeDp: Int,
    val avatarNameSpacingDp: Int,
    val authorNameFontSp: Int,
    val followButtonHeightDp: Int,
    val followButtonCornerRadiusDp: Int,
    val followButtonHorizontalPaddingDp: Int,
    val followIconSizeDp: Int,
    val followIconSpacingDp: Int,
    val followTextFontSp: Int,
    val titleFontSp: Int,
    val titleLineHeightSp: Int
)

fun resolvePortraitFullscreenOverlayLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): PortraitFullscreenOverlayLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return PortraitFullscreenOverlayLayoutPolicy(
            compactMode = false,
            topHorizontalPaddingDp = 24,
            topVerticalPaddingDp = 12,
            topBackButtonSizeDp = 44,
            topBackIconSizeDp = 28,
            topActionIconSizeDp = 28,
            topActionSpacingDp = 20,
            topViewCountFontSp = 14,
            topViewCountStartSpacingDp = 8,
            infoWidthFraction = 0.78f,
            infoHorizontalPaddingDp = 24,
            infoBottomPaddingDp = 16,
            bottomInputSpacerHeightDp = 64,
            authorRowBottomPaddingDp = 12,
            avatarSizeDp = 44,
            avatarNameSpacingDp = 10,
            authorNameFontSp = 18,
            followButtonHeightDp = 32,
            followButtonCornerRadiusDp = 16,
            followButtonHorizontalPaddingDp = 12,
            followIconSizeDp = 14,
            followIconSpacingDp = 3,
            followTextFontSp = 13,
            titleFontSp = 18,
            titleLineHeightSp = 26
        )
    }

    if (widthDp >= 840) {
        return PortraitFullscreenOverlayLayoutPolicy(
            compactMode = false,
            topHorizontalPaddingDp = 18,
            topVerticalPaddingDp = 10,
            topBackButtonSizeDp = 40,
            topBackIconSizeDp = 24,
            topActionIconSizeDp = 24,
            topActionSpacingDp = 16,
            topViewCountFontSp = 13,
            topViewCountStartSpacingDp = 6,
            infoWidthFraction = 0.82f,
            infoHorizontalPaddingDp = 20,
            infoBottomPaddingDp = 14,
            bottomInputSpacerHeightDp = 56,
            authorRowBottomPaddingDp = 10,
            avatarSizeDp = 40,
            avatarNameSpacingDp = 9,
            authorNameFontSp = 17,
            followButtonHeightDp = 30,
            followButtonCornerRadiusDp = 15,
            followButtonHorizontalPaddingDp = 11,
            followIconSizeDp = 13,
            followIconSpacingDp = 2,
            followTextFontSp = 12,
            titleFontSp = 16,
            titleLineHeightSp = 24
        )
    }

    if (widthDp >= 600) {
        return PortraitFullscreenOverlayLayoutPolicy(
            compactMode = false,
            topHorizontalPaddingDp = 16,
            topVerticalPaddingDp = 10,
            topBackButtonSizeDp = 38,
            topBackIconSizeDp = 23,
            topActionIconSizeDp = 23,
            topActionSpacingDp = 15,
            topViewCountFontSp = 12,
            topViewCountStartSpacingDp = 5,
            infoWidthFraction = 0.84f,
            infoHorizontalPaddingDp = 18,
            infoBottomPaddingDp = 13,
            bottomInputSpacerHeightDp = 54,
            authorRowBottomPaddingDp = 9,
            avatarSizeDp = 38,
            avatarNameSpacingDp = 8,
            authorNameFontSp = 16,
            followButtonHeightDp = 28,
            followButtonCornerRadiusDp = 14,
            followButtonHorizontalPaddingDp = 10,
            followIconSizeDp = 12,
            followIconSpacingDp = 2,
            followTextFontSp = 11,
            titleFontSp = 15,
            titleLineHeightSp = 22
        )
    }

    val compact = widthDp <= 360
    return PortraitFullscreenOverlayLayoutPolicy(
        compactMode = compact,
        topHorizontalPaddingDp = if (compact) 10 else 16,
        topVerticalPaddingDp = if (compact) 8 else 10,
        topBackButtonSizeDp = if (compact) 34 else 36,
        topBackIconSizeDp = if (compact) 22 else 24,
        topActionIconSizeDp = if (compact) 20 else 22,
        topActionSpacingDp = if (compact) 8 else 14,
        topViewCountFontSp = if (compact) 11 else 12,
        topViewCountStartSpacingDp = 4,
        infoWidthFraction = 0.85f,
        infoHorizontalPaddingDp = 16,
        infoBottomPaddingDp = 12,
        bottomInputSpacerHeightDp = 52,
        authorRowBottomPaddingDp = 8,
        avatarSizeDp = 36,
        avatarNameSpacingDp = 8,
        authorNameFontSp = 16,
        followButtonHeightDp = 26,
        followButtonCornerRadiusDp = 14,
        followButtonHorizontalPaddingDp = 10,
        followIconSizeDp = 12,
        followIconSpacingDp = 2,
        followTextFontSp = 11,
        titleFontSp = 15,
        titleLineHeightSp = 22
    )
}
