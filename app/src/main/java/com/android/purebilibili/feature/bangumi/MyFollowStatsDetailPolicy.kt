package com.android.purebilibili.feature.bangumi

data class MyFollowStatsDetail(
    val currentTypeTotal: Int,
    val loadedCount: Int,
    val loadedProgress: Float,
    val bangumiRatio: Float,
    val cinemaRatio: Float
)

fun buildMyFollowStatsDetail(
    stats: MyFollowStats,
    currentType: Int,
    loadedCount: Int
): MyFollowStatsDetail {
    val currentTotal = stats.totalForType(currentType).coerceAtLeast(0)
    val normalizedLoaded = loadedCount.coerceAtLeast(0).coerceAtMost(currentTotal)
    val loadedProgress = if (currentTotal == 0) 0f else normalizedLoaded.toFloat() / currentTotal.toFloat()

    val total = stats.total.coerceAtLeast(0)
    val bangumiRatio = if (total == 0) 0f else stats.bangumiTotal.toFloat() / total.toFloat()
    val cinemaRatio = if (total == 0) 0f else stats.cinemaTotal.toFloat() / total.toFloat()

    return MyFollowStatsDetail(
        currentTypeTotal = currentTotal,
        loadedCount = normalizedLoaded,
        loadedProgress = loadedProgress.coerceIn(0f, 1f),
        bangumiRatio = bangumiRatio.coerceIn(0f, 1f),
        cinemaRatio = cinemaRatio.coerceIn(0f, 1f)
    )
}
