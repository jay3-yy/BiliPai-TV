package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryPlaybackPolicyTest {

    @Test
    fun `resolveHistoryPlaybackCid prefers cid from history item`() {
        val historyItem = HistoryItem(
            videoItem = VideoItem(bvid = "BV1"),
            business = HistoryBusiness.ARCHIVE,
            cid = 9527L
        )

        val resolved = resolveHistoryPlaybackCid(
            clickedCid = 0L,
            historyItem = historyItem
        )

        assertEquals(9527L, resolved)
    }

    @Test
    fun `resolveHistoryPlaybackCid falls back to clicked cid when history cid missing`() {
        val historyItem = HistoryItem(
            videoItem = VideoItem(bvid = "BV1"),
            business = HistoryBusiness.ARCHIVE,
            cid = 0L
        )

        val resolved = resolveHistoryPlaybackCid(
            clickedCid = 114L,
            historyItem = historyItem
        )

        assertEquals(114L, resolved)
    }
}
