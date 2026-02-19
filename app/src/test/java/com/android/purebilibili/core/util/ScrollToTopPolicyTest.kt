package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScrollToTopPolicyTest {

    @Test
    fun resolveScrollToTopPlan_noPreJumpWhenNearTop() {
        assertNull(resolveScrollToTopPlan(0).preJumpIndex)
        assertNull(resolveScrollToTopPlan(10).preJumpIndex)
        assertNull(resolveScrollToTopPlan(14).preJumpIndex)
    }

    @Test
    fun resolveScrollToTopPlan_usesTieredPreJumpForFarDistance() {
        assertEquals(6, resolveScrollToTopPlan(20).preJumpIndex)
        assertEquals(12, resolveScrollToTopPlan(40).preJumpIndex)
        assertEquals(20, resolveScrollToTopPlan(120).preJumpIndex)
        assertEquals(28, resolveScrollToTopPlan(220).preJumpIndex)
    }
}
