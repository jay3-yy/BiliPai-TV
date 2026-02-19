package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MineSideDrawerLayoutPolicyTest {

    @Test
    fun compactPhone_keepsNarrowDrawerRange() {
        val policy = resolveMineSideDrawerLayoutPolicy(widthDp = 393, isTv = false)
        val width = resolveMineSideDrawerWidthDp(screenWidthDp = 393, policy = policy)

        assertEquals(0.72f, policy.drawerWidthFraction)
        assertEquals(283, width)
        assertEquals(44, policy.profileAvatarSizeDp)
    }

    @Test
    fun mediumTablet_expandsDrawerBeyondPhoneCap() {
        val policy = resolveMineSideDrawerLayoutPolicy(widthDp = 720, isTv = false)
        val width = resolveMineSideDrawerWidthDp(screenWidthDp = 720, policy = policy)

        assertEquals(0.56f, policy.drawerWidthFraction)
        assertEquals(403, width)
        assertTrue(width > 360)
        assertEquals(46, policy.profileAvatarSizeDp)
    }

    @Test
    fun largeTablet_capsDrawerAtReadableMaxWidth() {
        val policy = resolveMineSideDrawerLayoutPolicy(widthDp = 1280, isTv = false)
        val width = resolveMineSideDrawerWidthDp(screenWidthDp = 1280, policy = policy)

        assertEquals(0.48f, policy.drawerWidthFraction)
        assertEquals(460, width)
        assertEquals(48, policy.profileAvatarSizeDp)
        assertEquals(18, policy.profileChevronSizeDp)
    }

    @Test
    fun tv_usesTenFootScale() {
        val policy = resolveMineSideDrawerLayoutPolicy(widthDp = 1080, isTv = true)
        val width = resolveMineSideDrawerWidthDp(screenWidthDp = 1080, policy = policy)

        assertEquals(0.42f, policy.drawerWidthFraction)
        assertEquals(454, width)
        assertEquals(52, policy.profileAvatarSizeDp)
        assertEquals(20, policy.profileChevronSizeDp)
    }
}
