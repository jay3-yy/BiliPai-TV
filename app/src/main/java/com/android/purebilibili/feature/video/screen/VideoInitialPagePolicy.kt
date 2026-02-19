package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.data.model.response.Page

internal fun resolveInitialPageIndex(
    requestedCid: Long,
    currentCid: Long,
    pages: List<Page>
): Int? {
    if (requestedCid <= 0L || requestedCid == currentCid) return null
    val index = pages.indexOfFirst { it.cid == requestedCid }
    return index.takeIf { it >= 0 }
}
