package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabletCinemaLayoutPolicyTest {

    @Test
    fun largeTabletGetsWiderCurtainAndPlayerCap() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1800,
            isTv = false
        )

        assertTrue(policy.curtainOpenWidthDp >= 470)
        assertTrue(policy.playerMaxWidthDp >= 1270)
    }

    @Test
    fun cinemaPolicyScalesSmoothlyAcrossTabletWidths() {
        val compact = resolveTabletCinemaLayoutPolicy(
            widthDp = 960,
            isTv = false
        )
        val medium = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            isTv = false
        )
        val large = resolveTabletCinemaLayoutPolicy(
            widthDp = 1600,
            isTv = false
        )

        assertTrue(medium.curtainOpenWidthDp > compact.curtainOpenWidthDp)
        assertTrue(large.curtainOpenWidthDp > medium.curtainOpenWidthDp)
        assertTrue(medium.curtainPeekWidthDp > compact.curtainPeekWidthDp)
        assertTrue(large.curtainPeekWidthDp > medium.curtainPeekWidthDp)
        assertTrue(medium.horizontalPaddingDp > compact.horizontalPaddingDp)
        assertTrue(large.horizontalPaddingDp > medium.horizontalPaddingDp)
        assertTrue(medium.playerMaxWidthDp > compact.playerMaxWidthDp)
        assertTrue(large.playerMaxWidthDp > medium.playerMaxWidthDp)
    }

    @Test
    fun mediumTabletUsesBalancedCinemaPolicy() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            isTv = false
        )

        assertTrue(policy.curtainPeekWidthDp in 61..64)
        assertTrue(policy.curtainOpenWidthDp in 379..382)
        assertTrue(policy.horizontalPaddingDp in 16..17)
        assertTrue(policy.playerMaxWidthDp in 1090..1100)
    }

    @Test
    fun tvUsesDedicatedCinemaPolicy() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1920,
            isTv = true
        )

        assertEquals(72, policy.curtainPeekWidthDp)
        assertEquals(460, policy.curtainOpenWidthDp)
        assertEquals(24, policy.horizontalPaddingDp)
    }

    @Test
    fun curtainWidthFollowsStateMachine() {
        val policy = TabletCinemaLayoutPolicy(
            curtainPeekWidthDp = 60,
            curtainOpenWidthDp = 400,
            horizontalPaddingDp = 16,
            playerMaxWidthDp = 1080
        )

        assertEquals(0, resolveCurtainWidthDp(TabletSideCurtainState.HIDDEN, policy))
        assertEquals(60, resolveCurtainWidthDp(TabletSideCurtainState.PEEK, policy))
        assertEquals(400, resolveCurtainWidthDp(TabletSideCurtainState.OPEN, policy))
    }

    @Test
    fun initialCurtainStateUsesScreenWidthBuckets() {
        assertEquals(
            TabletSideCurtainState.PEEK,
            resolveInitialCurtainState(widthDp = 1280)
        )
        assertEquals(
            TabletSideCurtainState.OPEN,
            resolveInitialCurtainState(widthDp = 1366)
        )
    }

    @Test
    fun autoBehaviorCollapsesOpenCurtainWhenPlaying() {
        assertEquals(
            TabletSideCurtainState.PEEK,
            resolveCurtainStateAfterAutoBehavior(
                currentState = TabletSideCurtainState.OPEN,
                isActivelyPlaying = true
            )
        )
    }

    @Test
    fun autoBehaviorAvoidsFullyHiddenCurtainWhenPaused() {
        assertEquals(
            TabletSideCurtainState.PEEK,
            resolveCurtainStateAfterAutoBehavior(
                currentState = TabletSideCurtainState.HIDDEN,
                isActivelyPlaying = false
            )
        )
    }
}
