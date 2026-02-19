package com.android.purebilibili.feature.home.components

import kotlin.math.roundToInt

data class MineSideDrawerLayoutPolicy(
    val drawerWidthFraction: Float,
    val drawerMinWidthDp: Int,
    val drawerMaxWidthDp: Int,
    val drawerEdgeRadiusDp: Int,
    val contentVerticalPaddingDp: Int,
    val sectionHorizontalPaddingDp: Int,
    val profileCardCornerRadiusDp: Int,
    val profileRowPaddingDp: Int,
    val profileAvatarSizeDp: Int,
    val profileChevronSizeDp: Int,
    val sectionCornerRadiusDp: Int,
    val dividerHorizontalPaddingDp: Int,
    val dividerVerticalPaddingDp: Int,
    val footerSpacerHeightDp: Int,
    val badgeFontSp: Int
)

fun resolveMineSideDrawerLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): MineSideDrawerLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return MineSideDrawerLayoutPolicy(
            drawerWidthFraction = 0.42f,
            drawerMinWidthDp = 320,
            drawerMaxWidthDp = 520,
            drawerEdgeRadiusDp = 18,
            contentVerticalPaddingDp = 14,
            sectionHorizontalPaddingDp = 16,
            profileCardCornerRadiusDp = 16,
            profileRowPaddingDp = 14,
            profileAvatarSizeDp = 52,
            profileChevronSizeDp = 20,
            sectionCornerRadiusDp = 16,
            dividerHorizontalPaddingDp = 20,
            dividerVerticalPaddingDp = 10,
            footerSpacerHeightDp = 24,
            badgeFontSp = 10
        )
    }

    if (widthDp >= 1200) {
        return MineSideDrawerLayoutPolicy(
            drawerWidthFraction = 0.48f,
            drawerMinWidthDp = 300,
            drawerMaxWidthDp = 460,
            drawerEdgeRadiusDp = 16,
            contentVerticalPaddingDp = 14,
            sectionHorizontalPaddingDp = 14,
            profileCardCornerRadiusDp = 14,
            profileRowPaddingDp = 13,
            profileAvatarSizeDp = 48,
            profileChevronSizeDp = 18,
            sectionCornerRadiusDp = 15,
            dividerHorizontalPaddingDp = 18,
            dividerVerticalPaddingDp = 9,
            footerSpacerHeightDp = 20,
            badgeFontSp = 10
        )
    }

    if (widthDp >= 600) {
        return MineSideDrawerLayoutPolicy(
            drawerWidthFraction = 0.56f,
            drawerMinWidthDp = 300,
            drawerMaxWidthDp = 420,
            drawerEdgeRadiusDp = 16,
            contentVerticalPaddingDp = 13,
            sectionHorizontalPaddingDp = 13,
            profileCardCornerRadiusDp = 13,
            profileRowPaddingDp = 12,
            profileAvatarSizeDp = 46,
            profileChevronSizeDp = 17,
            sectionCornerRadiusDp = 14,
            dividerHorizontalPaddingDp = 17,
            dividerVerticalPaddingDp = 8,
            footerSpacerHeightDp = 18,
            badgeFontSp = 9
        )
    }

    return MineSideDrawerLayoutPolicy(
        drawerWidthFraction = 0.72f,
        drawerMinWidthDp = 280,
        drawerMaxWidthDp = 360,
        drawerEdgeRadiusDp = 16,
        contentVerticalPaddingDp = 12,
        sectionHorizontalPaddingDp = 12,
        profileCardCornerRadiusDp = 12,
        profileRowPaddingDp = 12,
        profileAvatarSizeDp = 44,
        profileChevronSizeDp = 16,
        sectionCornerRadiusDp = 14,
        dividerHorizontalPaddingDp = 16,
        dividerVerticalPaddingDp = 8,
        footerSpacerHeightDp = 16,
        badgeFontSp = 9
    )
}

fun resolveMineSideDrawerWidthDp(
    screenWidthDp: Int,
    policy: MineSideDrawerLayoutPolicy
): Int {
    val target = (screenWidthDp * policy.drawerWidthFraction).roundToInt()
    return target.coerceIn(policy.drawerMinWidthDp, policy.drawerMaxWidthDp)
}
