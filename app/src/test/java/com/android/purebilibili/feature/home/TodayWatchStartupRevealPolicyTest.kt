package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals

class TodayWatchStartupRevealPolicyTest {

    @Test
    fun `reveal when startup has plan and list is not at top`() {
        val decision = decideTodayWatchStartupReveal(
            startupElapsedMs = 3_000L,
            isPluginEnabled = true,
            currentCategory = HomeCategory.RECOMMEND,
            hasTodayPlan = true,
            firstVisibleItemIndex = 4,
            firstVisibleItemOffset = 12
        )

        assertEquals(TodayWatchStartupRevealDecision.REVEAL, decision)
    }

    @Test
    fun `skip when list already near top`() {
        val decision = decideTodayWatchStartupReveal(
            startupElapsedMs = 2_000L,
            isPluginEnabled = true,
            currentCategory = HomeCategory.RECOMMEND,
            hasTodayPlan = true,
            firstVisibleItemIndex = 0,
            firstVisibleItemOffset = 18
        )

        assertEquals(TodayWatchStartupRevealDecision.SKIP, decision)
    }

    @Test
    fun `wait when plan not ready yet`() {
        val decision = decideTodayWatchStartupReveal(
            startupElapsedMs = 2_000L,
            isPluginEnabled = true,
            currentCategory = HomeCategory.RECOMMEND,
            hasTodayPlan = false,
            firstVisibleItemIndex = 5,
            firstVisibleItemOffset = 0
        )

        assertEquals(TodayWatchStartupRevealDecision.WAIT, decision)
    }

    @Test
    fun `skip when startup window expired`() {
        val decision = decideTodayWatchStartupReveal(
            startupElapsedMs = 35_000L,
            isPluginEnabled = true,
            currentCategory = HomeCategory.RECOMMEND,
            hasTodayPlan = true,
            firstVisibleItemIndex = 3,
            firstVisibleItemOffset = 0
        )

        assertEquals(TodayWatchStartupRevealDecision.SKIP, decision)
    }
}
