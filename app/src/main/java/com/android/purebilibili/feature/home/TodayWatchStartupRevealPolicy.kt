package com.android.purebilibili.feature.home

internal const val TODAY_WATCH_STARTUP_REVEAL_WINDOW_MS = 20_000L
private const val TODAY_WATCH_NEAR_TOP_OFFSET_PX = 48

internal enum class TodayWatchStartupRevealDecision {
    WAIT,
    REVEAL,
    SKIP
}

/**
 * 冷启动时判定是否自动曝光“今日推荐单”。
 *
 * 目标：
 * 1) 仅在启动时间窗口内执行；
 * 2) 等待插件与推荐单都准备完成；
 * 3) 仅当推荐列表不在顶部时触发回顶曝光。
 */
internal fun decideTodayWatchStartupReveal(
    startupElapsedMs: Long,
    isPluginEnabled: Boolean,
    currentCategory: HomeCategory,
    hasTodayPlan: Boolean,
    firstVisibleItemIndex: Int,
    firstVisibleItemOffset: Int
): TodayWatchStartupRevealDecision {
    if (startupElapsedMs >= TODAY_WATCH_STARTUP_REVEAL_WINDOW_MS) {
        return TodayWatchStartupRevealDecision.SKIP
    }
    if (!isPluginEnabled || !hasTodayPlan || currentCategory != HomeCategory.RECOMMEND) {
        return TodayWatchStartupRevealDecision.WAIT
    }
    val alreadyNearTop = firstVisibleItemIndex == 0 && firstVisibleItemOffset <= TODAY_WATCH_NEAR_TOP_OFFSET_PX
    return if (alreadyNearTop) {
        TodayWatchStartupRevealDecision.SKIP
    } else {
        TodayWatchStartupRevealDecision.REVEAL
    }
}
