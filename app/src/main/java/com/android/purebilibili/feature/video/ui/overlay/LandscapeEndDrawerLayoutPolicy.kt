package com.android.purebilibili.feature.video.ui.overlay

data class LandscapeEndDrawerLayoutPolicy(
    val drawerWidthDp: Int,
    val headerPaddingDp: Int,
    val avatarSizeDp: Int,
    val headerSpacingDp: Int,
    val titleFontSp: Int,
    val followButtonHeightDp: Int,
    val followButtonHorizontalPaddingDp: Int,
    val followButtonFontSp: Int,
    val sectionSpacingDp: Int,
    val listContentPaddingDp: Int,
    val listItemSpacingDp: Int,
    val videoItemHeightDp: Int,
    val episodeItemHeightDp: Int,
    val itemTitleFontSp: Int,
    val itemMetaFontSp: Int,
    val itemDurationFontSp: Int,
    val metaIconSizeDp: Int
)

fun resolveLandscapeEndDrawerLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): LandscapeEndDrawerLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return LandscapeEndDrawerLayoutPolicy(
            drawerWidthDp = 420,
            headerPaddingDp = 24,
            avatarSizeDp = 56,
            headerSpacingDp = 16,
            titleFontSp = 17,
            followButtonHeightDp = 40,
            followButtonHorizontalPaddingDp = 16,
            followButtonFontSp = 14,
            sectionSpacingDp = 20,
            listContentPaddingDp = 20,
            listItemSpacingDp = 18,
            videoItemHeightDp = 96,
            episodeItemHeightDp = 108,
            itemTitleFontSp = 16,
            itemMetaFontSp = 14,
            itemDurationFontSp = 12,
            metaIconSizeDp = 16
        )
    }

    if (widthDp >= 1200) {
        return LandscapeEndDrawerLayoutPolicy(
            drawerWidthDp = 380,
            headerPaddingDp = 20,
            avatarSizeDp = 48,
            headerSpacingDp = 14,
            titleFontSp = 16,
            followButtonHeightDp = 36,
            followButtonHorizontalPaddingDp = 14,
            followButtonFontSp = 13,
            sectionSpacingDp = 18,
            listContentPaddingDp = 18,
            listItemSpacingDp = 14,
            videoItemHeightDp = 84,
            episodeItemHeightDp = 94,
            itemTitleFontSp = 14,
            itemMetaFontSp = 12,
            itemDurationFontSp = 11,
            metaIconSizeDp = 14
        )
    }

    if (widthDp >= 600) {
        return LandscapeEndDrawerLayoutPolicy(
            drawerWidthDp = 348,
            headerPaddingDp = 18,
            avatarSizeDp = 44,
            headerSpacingDp = 13,
            titleFontSp = 15,
            followButtonHeightDp = 34,
            followButtonHorizontalPaddingDp = 13,
            followButtonFontSp = 13,
            sectionSpacingDp = 17,
            listContentPaddingDp = 17,
            listItemSpacingDp = 13,
            videoItemHeightDp = 76,
            episodeItemHeightDp = 86,
            itemTitleFontSp = 13,
            itemMetaFontSp = 12,
            itemDurationFontSp = 10,
            metaIconSizeDp = 13
        )
    }

    return LandscapeEndDrawerLayoutPolicy(
        drawerWidthDp = 320,
        headerPaddingDp = 16,
        avatarSizeDp = 40,
        headerSpacingDp = 12,
        titleFontSp = 15,
        followButtonHeightDp = 32,
        followButtonHorizontalPaddingDp = 12,
        followButtonFontSp = 12,
        sectionSpacingDp = 16,
        listContentPaddingDp = 16,
        listItemSpacingDp = 12,
        videoItemHeightDp = 70,
        episodeItemHeightDp = 80,
        itemTitleFontSp = 13,
        itemMetaFontSp = 11,
        itemDurationFontSp = 10,
        metaIconSizeDp = 12
    )
}
