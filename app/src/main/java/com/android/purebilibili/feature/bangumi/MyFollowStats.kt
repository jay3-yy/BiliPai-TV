package com.android.purebilibili.feature.bangumi

data class MyFollowStats(
    val bangumiTotal: Int = 0,
    val cinemaTotal: Int = 0
) {
    val total: Int get() = bangumiTotal + cinemaTotal

    fun totalForType(type: Int): Int {
        return if (type == MY_FOLLOW_TYPE_BANGUMI) bangumiTotal else cinemaTotal
    }
}

fun buildMyFollowStats(
    bangumiTotal: Int,
    cinemaTotal: Int
): MyFollowStats {
    return MyFollowStats(
        bangumiTotal = bangumiTotal.coerceAtLeast(0),
        cinemaTotal = cinemaTotal.coerceAtLeast(0)
    )
}
