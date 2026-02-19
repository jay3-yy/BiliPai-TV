package com.android.purebilibili.feature.home

internal fun shouldAutoRebuildTodayWatchPlan(
    currentCategory: HomeCategory,
    isTodayWatchEnabled: Boolean,
    isTodayWatchCollapsed: Boolean
): Boolean {
    if (!isTodayWatchEnabled) return false
    if (isTodayWatchCollapsed) return false
    return currentCategory == HomeCategory.RECOMMEND
}

internal fun collectTodayWatchConsumedForManualRefresh(
    plan: TodayWatchPlan?,
    previewLimit: Int
): Set<String> {
    val safePlan = plan ?: return emptySet()
    val safeLimit = previewLimit.coerceAtLeast(1)
    return safePlan.videoQueue
        .take(safeLimit)
        .mapNotNull { it.bvid.takeIf { bvid -> bvid.isNotBlank() } }
        .toSet()
}
