package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.MyFollowBangumiData
import com.android.purebilibili.data.model.response.UserStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BangumiFollowStatusPolicyTest {

    @Test
    fun `followed should be true when follow equals one`() {
        assertTrue(isBangumiFollowed(UserStatus(follow = 1, followStatus = 0)))
    }

    @Test
    fun `followed should be true when follow_status is positive`() {
        assertTrue(isBangumiFollowed(UserStatus(follow = 0, followStatus = 2)))
    }

    @Test
    fun `followed should be false when follow and follow_status are both zero`() {
        assertFalse(isBangumiFollowed(UserStatus(follow = 0, followStatus = 0)))
    }

    @Test
    fun `follow preload page count should cap by max pages`() {
        assertEquals(9, resolveFollowPreloadPageCount(total = 250, pageSize = 30, maxPages = 30))
        assertEquals(30, resolveFollowPreloadPageCount(total = 5000, pageSize = 30, maxPages = 30))
        assertEquals(0, resolveFollowPreloadPageCount(total = 0, pageSize = 30, maxPages = 30))
    }

    @Test
    fun `preload should merge ids across pages`() = runBlocking {
        val followedSeasonIds = mutableSetOf<Long>()
        val responses = mapOf(
            1 to Result.success(
                MyFollowBangumiData(
                    total = 3,
                    pn = 1,
                    ps = 2,
                    list = listOf(
                        FollowBangumiItem(seasonId = 11L),
                        FollowBangumiItem(seasonId = 22L)
                    )
                )
            ),
            2 to Result.success(
                MyFollowBangumiData(
                    total = 3,
                    pn = 2,
                    ps = 2,
                    list = listOf(FollowBangumiItem(seasonId = 33L))
                )
            )
        )

        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = followedSeasonIds,
            pageSize = 2,
            maxPages = 10,
            fetchPage = { _, page, _ ->
                responses[page] ?: Result.success(MyFollowBangumiData(total = 3, pn = page, ps = 2, list = emptyList()))
            }
        )

        assertTrue(result.requestSucceeded)
        assertEquals(3, result.total)
        assertEquals(setOf(11L, 22L, 33L), followedSeasonIds)
    }

    @Test
    fun `preload should report failed when first page request fails`() = runBlocking {
        val followedSeasonIds = mutableSetOf<Long>()
        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = followedSeasonIds,
            fetchPage = { _, _, _ -> Result.failure(Exception("network error")) }
        )

        assertFalse(result.requestSucceeded)
        assertEquals(0, result.total)
        assertTrue(followedSeasonIds.isEmpty())
    }

    @Test
    fun `preload should continue paging when server clamps page size`() = runBlocking {
        val followedSeasonIds = mutableSetOf<Long>()
        val firstPageIds = (1L..30L).map { FollowBangumiItem(seasonId = it) }
        val secondPageIds = (31L..60L).map { FollowBangumiItem(seasonId = it) }
        val thirdPageIds = (61L..65L).map { FollowBangumiItem(seasonId = it) }

        val result = preloadFollowedSeasonsForType(
            type = MY_FOLLOW_TYPE_BANGUMI,
            followedSeasonIds = followedSeasonIds,
            pageSize = 100,
            maxPages = 10,
            fetchPage = { _, page, _ ->
                when (page) {
                    1 -> Result.success(MyFollowBangumiData(total = 65, pn = 1, ps = 30, list = firstPageIds))
                    2 -> Result.success(MyFollowBangumiData(total = 65, pn = 2, ps = 30, list = secondPageIds))
                    3 -> Result.success(MyFollowBangumiData(total = 65, pn = 3, ps = 30, list = thirdPageIds))
                    else -> Result.success(MyFollowBangumiData(total = 65, pn = page, ps = 30, list = emptyList()))
                }
            }
        )

        assertTrue(result.requestSucceeded)
        assertEquals(65, result.total)
        assertEquals(65, followedSeasonIds.size)
        assertTrue(followedSeasonIds.contains(65L))
    }
}
