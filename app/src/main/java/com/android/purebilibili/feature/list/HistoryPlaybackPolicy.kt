package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryItem

internal fun resolveHistoryPlaybackCid(
    clickedCid: Long,
    historyItem: HistoryItem?
): Long {
    val historyCid = historyItem?.cid ?: 0L
    return if (historyCid > 0L) historyCid else clickedCid.coerceAtLeast(0L)
}
