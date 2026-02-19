package com.android.purebilibili.feature.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingTvNavigationPolicyTest {

    @Test
    fun advance_beforeLastPage_movesToNextPage() {
        val decision = resolveOnboardingAdvanceDecision(
            currentPage = 1,
            lastPage = 3
        )

        assertEquals(2, decision.nextPage)
        assertFalse(decision.shouldFinish)
    }

    @Test
    fun advance_onLastPage_requestsFinish() {
        val decision = resolveOnboardingAdvanceDecision(
            currentPage = 3,
            lastPage = 3
        )

        assertEquals(3, decision.nextPage)
        assertTrue(decision.shouldFinish)
    }

    @Test
    fun horizontalMove_clampsInsideBounds() {
        assertEquals(
            0,
            resolveOnboardingHorizontalTargetPage(
                currentPage = 0,
                lastPage = 3,
                delta = -1
            )
        )
        assertEquals(
            3,
            resolveOnboardingHorizontalTargetPage(
                currentPage = 3,
                lastPage = 3,
                delta = 1
            )
        )
    }
}
