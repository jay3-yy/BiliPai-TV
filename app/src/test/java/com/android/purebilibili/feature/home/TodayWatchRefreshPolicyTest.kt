package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodayWatchRefreshPolicyTest {

    @Test
    fun `auto rebuild enabled only for recommend tab when expanded`() {
        assertTrue(
            shouldAutoRebuildTodayWatchPlan(
                currentCategory = HomeCategory.RECOMMEND,
                isTodayWatchEnabled = true,
                isTodayWatchCollapsed = false
            )
        )
    }

    @Test
    fun `auto rebuild disabled when today watch card collapsed`() {
        assertFalse(
            shouldAutoRebuildTodayWatchPlan(
                currentCategory = HomeCategory.RECOMMEND,
                isTodayWatchEnabled = true,
                isTodayWatchCollapsed = true
            )
        )
    }

    @Test
    fun `auto rebuild disabled outside recommend tab`() {
        assertFalse(
            shouldAutoRebuildTodayWatchPlan(
                currentCategory = HomeCategory.POPULAR,
                isTodayWatchEnabled = true,
                isTodayWatchCollapsed = false
            )
        )
    }

    @Test
    fun `manual refresh consumes current preview queue to rotate recommendations`() {
        val plan = TodayWatchPlan(
            videoQueue = listOf(
                VideoItem(bvid = "a", title = "A"),
                VideoItem(bvid = "b", title = "B"),
                VideoItem(bvid = "c", title = "C")
            )
        )

        val consumed = collectTodayWatchConsumedForManualRefresh(
            plan = plan,
            previewLimit = 2
        )

        assertEquals(setOf("a", "b"), consumed)
    }

    @Test
    fun `manual refresh consumption ignores blank bvids and empty plan`() {
        val plan = TodayWatchPlan(
            videoQueue = listOf(
                VideoItem(bvid = "", title = "NoId"),
                VideoItem(bvid = "b", title = "B")
            )
        )

        val consumed = collectTodayWatchConsumedForManualRefresh(
            plan = plan,
            previewLimit = 5
        )
        val emptyConsumed = collectTodayWatchConsumedForManualRefresh(
            plan = null,
            previewLimit = 3
        )

        assertEquals(setOf("b"), consumed)
        assertTrue(emptyConsumed.isEmpty())
    }
}
