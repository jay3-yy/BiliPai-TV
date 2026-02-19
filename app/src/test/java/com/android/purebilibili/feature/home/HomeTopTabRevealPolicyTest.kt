package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeTopTabRevealPolicyTest {

    @Test
    fun returningFromVideo_withCardTransition_delaysTopTabsUntilCardSettles() {
        assertEquals(
            HOME_TOP_TABS_REVEAL_DELAY_MS,
            resolveHomeTopTabsRevealDelayMs(
                isReturningFromDetail = true,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun returningFromVideo_withoutCardTransition_showsTopTabsImmediately() {
        assertEquals(
            0L,
            resolveHomeTopTabsRevealDelayMs(
                isReturningFromDetail = true,
                cardTransitionEnabled = false
            )
        )
    }

    @Test
    fun normalHomeEntry_keepsTopTabsImmediate() {
        assertEquals(
            0L,
            resolveHomeTopTabsRevealDelayMs(
                isReturningFromDetail = false,
                cardTransitionEnabled = true
            )
        )
    }

    @Test
    fun forwardNavigationToDetail_hidesTopTabsImmediately() {
        assertFalse(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = true
            )
        )
    }

    @Test
    fun settlingAfterReturn_hidesTopTabs() {
        assertFalse(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = true,
                isForwardNavigatingToDetail = false
            )
        )
    }

    @Test
    fun idleHome_showsTopTabs() {
        assertTrue(
            resolveHomeTopTabsVisible(
                isDelayedForCardSettle = false,
                isForwardNavigatingToDetail = false
            )
        )
    }
}
