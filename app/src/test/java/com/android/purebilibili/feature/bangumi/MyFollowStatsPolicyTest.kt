package com.android.purebilibili.feature.bangumi

import org.junit.Assert.assertEquals
import org.junit.Test

class MyFollowStatsPolicyTest {

    @Test
    fun `build stats should normalize negative values`() {
        val stats = buildMyFollowStats(
            bangumiTotal = -2,
            cinemaTotal = -1
        )

        assertEquals(0, stats.bangumiTotal)
        assertEquals(0, stats.cinemaTotal)
        assertEquals(0, stats.total)
    }

    @Test
    fun `build stats should keep api totals`() {
        val stats = buildMyFollowStats(
            bangumiTotal = 120,
            cinemaTotal = 38
        )

        assertEquals(120, stats.bangumiTotal)
        assertEquals(38, stats.cinemaTotal)
        assertEquals(158, stats.total)
    }

    @Test
    fun `stats should return total for current follow type`() {
        val stats = buildMyFollowStats(
            bangumiTotal = 88,
            cinemaTotal = 12
        )

        assertEquals(88, stats.totalForType(MY_FOLLOW_TYPE_BANGUMI))
        assertEquals(12, stats.totalForType(MY_FOLLOW_TYPE_CINEMA))
    }
}
