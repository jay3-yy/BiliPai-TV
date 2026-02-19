package com.android.purebilibili.feature.bangumi

import org.junit.Assert.assertEquals
import org.junit.Test

class BangumiSeasonActionPolicyTest {

    @Test
    fun `detail season id should take precedence for action`() {
        assertEquals(123L, resolveBangumiActionSeasonId(routeSeasonId = 456L, detailSeasonId = 123L))
    }

    @Test
    fun `route season id should be fallback when detail season id is missing`() {
        assertEquals(456L, resolveBangumiActionSeasonId(routeSeasonId = 456L, detailSeasonId = 0L))
    }
}
