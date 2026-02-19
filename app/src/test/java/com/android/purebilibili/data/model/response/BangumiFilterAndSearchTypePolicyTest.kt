package com.android.purebilibili.data.model.response

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiFilterAndSearchTypePolicyTest {

    @Test
    fun `order options should match documented api ids`() {
        assertTrue(BangumiFilter.ORDER_OPTIONS.contains(3 to "追番人数"))
        assertTrue(BangumiFilter.ORDER_OPTIONS.contains(4 to "最高评分"))
    }

    @Test
    fun `area options should not label canada as other`() {
        assertTrue(BangumiFilter.AREA_OPTIONS.contains(5 to "加拿大"))
    }

    @Test
    fun `search type should support media ft`() {
        val mediaFt = SearchType.fromValue("media_ft")
        assertEquals("media_ft", mediaFt.value)
    }

    @Test
    fun `year filter should be passed to year for anime and guochuang`() {
        val filter = BangumiFilter(year = "[2025,2026)")
        assertEquals("[2025,2026)", filter.toApiYear(BangumiType.ANIME.value))
        assertEquals("[2025,2026)", filter.toApiYear(BangumiType.GUOCHUANG.value))
        assertEquals("-1", filter.toApiYear(BangumiType.MOVIE.value))
    }

    @Test
    fun `year filter should convert to release date range for movie`() {
        val filter = BangumiFilter(year = "[2025,2026)")
        assertEquals(
            "[2025-01-01 00:00:00,2026-01-01 00:00:00)",
            filter.toApiReleaseDate(BangumiType.MOVIE.value)
        )
        assertEquals("-1", filter.toApiReleaseDate(BangumiType.ANIME.value))
    }
}
