package com.android.purebilibili.feature.onboarding

data class OnboardingAdvanceDecision(
    val nextPage: Int,
    val shouldFinish: Boolean
)

fun resolveOnboardingAdvanceDecision(
    currentPage: Int,
    lastPage: Int
): OnboardingAdvanceDecision {
    val safeLastPage = lastPage.coerceAtLeast(0)
    val safeCurrentPage = currentPage.coerceIn(0, safeLastPage)
    return if (safeCurrentPage >= safeLastPage) {
        OnboardingAdvanceDecision(nextPage = safeLastPage, shouldFinish = true)
    } else {
        OnboardingAdvanceDecision(nextPage = safeCurrentPage + 1, shouldFinish = false)
    }
}

fun resolveOnboardingHorizontalTargetPage(
    currentPage: Int,
    lastPage: Int,
    delta: Int
): Int {
    val safeLastPage = lastPage.coerceAtLeast(0)
    val safeCurrentPage = currentPage.coerceIn(0, safeLastPage)
    return (safeCurrentPage + delta).coerceIn(0, safeLastPage)
}
