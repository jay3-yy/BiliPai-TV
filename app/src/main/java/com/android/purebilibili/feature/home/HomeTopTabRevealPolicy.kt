package com.android.purebilibili.feature.home

const val HOME_TOP_TABS_REVEAL_DELAY_MS: Long = 380L

fun resolveHomeTopTabsRevealDelayMs(
    isReturningFromDetail: Boolean,
    cardTransitionEnabled: Boolean
): Long {
    return if (isReturningFromDetail && cardTransitionEnabled) {
        HOME_TOP_TABS_REVEAL_DELAY_MS
    } else {
        0L
    }
}

fun resolveHomeTopTabsVisible(
    isDelayedForCardSettle: Boolean,
    isForwardNavigatingToDetail: Boolean
): Boolean {
    return !isDelayedForCardSettle && !isForwardNavigatingToDetail
}
