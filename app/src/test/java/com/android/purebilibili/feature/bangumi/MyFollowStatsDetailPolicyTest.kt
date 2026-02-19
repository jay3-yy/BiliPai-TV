package com.android.purebilibili.feature.bangumi

import org.junit.Assert.assertEquals
import org.junit.Test

class MyFollowStatsDetailPolicyTest {

    @Test
    fun `detail should clamp loaded count to current total`() {
        val detail = buildMyFollowStatsDetail(
            stats = MyFollowStats(bangumiTotal = 10, cinemaTotal = 2),
            currentType = MY_FOLLOW_TYPE_BANGUMI,
            loadedCount = 99
        )

        assertEquals(10, detail.currentTypeTotal)
        assertEquals(10, detail.loadedCount)
        assertEquals(1f, detail.loadedProgress, 0.0001f)
    }

    @Test
    fun `detail ratios should be zero when total is zero`() {
        val detail = buildMyFollowStatsDetail(
            stats = MyFollowStats(bangumiTotal = 0, cinemaTotal = 0),
            currentType = MY_FOLLOW_TYPE_CINEMA,
            loadedCount = 3
        )

        assertEquals(0f, detail.bangumiRatio, 0.0001f)
        assertEquals(0f, detail.cinemaRatio, 0.0001f)
        assertEquals(0f, detail.loadedProgress, 0.0001f)
    }
}
