package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ViewInfo

internal fun resolveCommittedPage(
    isScrollInProgress: Boolean,
    currentPage: Int,
    lastCommittedPage: Int
): Int? {
    if (isScrollInProgress) return null
    if (currentPage == lastCommittedPage) return null
    return currentPage
}

internal fun shouldApplyLoadResult(
    requestGeneration: Int,
    activeGeneration: Int,
    expectedBvid: String,
    currentPlayingBvid: String?
): Boolean {
    if (requestGeneration != activeGeneration) return false
    if (expectedBvid != currentPlayingBvid) return false
    return true
}

internal fun shouldShowPortraitCover(
    isLoading: Boolean,
    isCurrentPage: Boolean,
    isPlayerReadyForThisVideo: Boolean,
    hasRenderedFirstFrame: Boolean
): Boolean {
    if (isLoading) return true
    if (!isCurrentPage) return true
    if (!isPlayerReadyForThisVideo) return true
    if (!hasRenderedFirstFrame) return true
    return false
}

internal fun shouldShowPortraitPauseIcon(
    isCurrentPage: Boolean,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    isLoading: Boolean,
    isSeekGesture: Boolean
): Boolean {
    if (!isCurrentPage) return false
    if (isLoading) return false
    if (isSeekGesture) return false
    if (isPlaying) return false
    if (playWhenReady) return false
    return true
}

internal fun resolvePortraitInitialProgressPosition(
    isFirstPage: Boolean,
    initialStartPositionMs: Long
): Long {
    if (!isFirstPage) return 0L
    return initialStartPositionMs.coerceAtLeast(0L)
}

internal fun toViewInfoForPortraitDetail(related: RelatedVideo): ViewInfo {
    return ViewInfo(
        bvid = related.bvid,
        aid = related.aid,
        title = related.title,
        desc = "",
        pic = related.pic,
        owner = related.owner,
        stat = related.stat,
        pages = listOf(
            Page(duration = related.duration.toLong())
        )
    )
}
