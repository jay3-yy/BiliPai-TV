package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class LandscapeEndDrawerLayoutPolicyTest {

    @Test
    fun compactPhone_usesDefaultDrawerDensity() {
        val policy = resolveLandscapeEndDrawerLayoutPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(320, policy.drawerWidthDp)
        assertEquals(16, policy.headerPaddingDp)
        assertEquals(40, policy.avatarSizeDp)
        assertEquals(15, policy.titleFontSp)
        assertEquals(70, policy.videoItemHeightDp)
        assertEquals(80, policy.episodeItemHeightDp)
        assertEquals(13, policy.itemTitleFontSp)
        assertEquals(11, policy.itemMetaFontSp)
        assertEquals(12, policy.metaIconSizeDp)
    }

    @Test
    fun mediumTablet_balancesDrawerReadabilityAndDensity() {
        val policy = resolveLandscapeEndDrawerLayoutPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(348, policy.drawerWidthDp)
        assertEquals(18, policy.headerPaddingDp)
        assertEquals(44, policy.avatarSizeDp)
        assertEquals(13, policy.followButtonFontSp)
        assertEquals(76, policy.videoItemHeightDp)
        assertEquals(86, policy.episodeItemHeightDp)
        assertEquals(13, policy.itemTitleFontSp)
        assertEquals(12, policy.itemMetaFontSp)
        assertEquals(13, policy.metaIconSizeDp)
    }

    @Test
    fun largeTablet_expandsDrawerAndHitTargets() {
        val policy = resolveLandscapeEndDrawerLayoutPolicy(
            widthDp = 1280,
            isTv = false
        )

        assertEquals(380, policy.drawerWidthDp)
        assertEquals(20, policy.headerPaddingDp)
        assertEquals(48, policy.avatarSizeDp)
        assertEquals(13, policy.followButtonFontSp)
        assertEquals(84, policy.videoItemHeightDp)
        assertEquals(94, policy.episodeItemHeightDp)
        assertEquals(14, policy.itemTitleFontSp)
        assertEquals(12, policy.itemMetaFontSp)
        assertEquals(14, policy.metaIconSizeDp)
    }

    @Test
    fun tv_usesTenFootLayoutScale() {
        val policy = resolveLandscapeEndDrawerLayoutPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(420, policy.drawerWidthDp)
        assertEquals(24, policy.headerPaddingDp)
        assertEquals(56, policy.avatarSizeDp)
        assertEquals(14, policy.followButtonFontSp)
        assertEquals(96, policy.videoItemHeightDp)
        assertEquals(108, policy.episodeItemHeightDp)
        assertEquals(16, policy.itemTitleFontSp)
        assertEquals(14, policy.itemMetaFontSp)
        assertEquals(16, policy.metaIconSizeDp)
    }
}
