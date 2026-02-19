package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryModelsMappingTest {

    @Test
    fun `toVideoItem keeps cid for multi page resume`() {
        val data = HistoryData(
            title = "t",
            history = HistoryPage(
                oid = 1L,
                bvid = "BV1",
                cid = 7788L,
                page = 3,
                business = "archive"
            )
        )

        val item = data.toVideoItem()

        assertEquals(7788L, item.cid)
    }

    @Test
    fun `toHistoryItem keeps cid page and progress`() {
        val data = HistoryData(
            title = "t",
            progress = 321,
            history = HistoryPage(
                oid = 1L,
                bvid = "BV1",
                cid = 7788L,
                page = 3,
                business = "archive"
            )
        )

        val item = data.toHistoryItem()

        assertEquals(7788L, item.cid)
        assertEquals(3, item.page)
        assertEquals(321, item.progress)
    }
}
