package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.WatchLaterItem

internal data class PortraitDetailVideoList(
    val title: String,
    val videos: List<RelatedVideo>
)

internal fun buildPortraitDetailVideoList(
    currentBvid: String,
    watchLaterVideos: List<RelatedVideo>,
    recommendationVideos: List<RelatedVideo>
): PortraitDetailVideoList {
    val watchLaterFiltered = watchLaterVideos.filter { it.bvid != currentBvid }
    if (watchLaterFiltered.isNotEmpty()) {
        return PortraitDetailVideoList(
            title = "稍后再看",
            videos = watchLaterFiltered
        )
    }

    val recommendationFiltered = recommendationVideos.filter { it.bvid != currentBvid }
    return PortraitDetailVideoList(
        title = "推荐视频",
        videos = recommendationFiltered
    )
}

internal fun toRelatedVideoFromWatchLater(item: WatchLaterItem): RelatedVideo? {
    val bvid = item.bvid?.trim().orEmpty()
    if (bvid.isEmpty()) return null
    return RelatedVideo(
        aid = item.aid,
        bvid = bvid,
        title = item.title.orEmpty(),
        pic = item.pic.orEmpty(),
        owner = Owner(
            mid = item.owner?.mid ?: 0L,
            name = item.owner?.name.orEmpty(),
            face = item.owner?.face.orEmpty()
        ),
        stat = Stat(
            view = item.stat?.view ?: 0,
            danmaku = item.stat?.danmaku ?: 0,
            reply = item.stat?.reply ?: 0,
            like = item.stat?.like ?: 0,
            coin = item.stat?.coin ?: 0,
            favorite = item.stat?.favorite ?: 0,
            share = item.stat?.share ?: 0
        ),
        duration = item.duration ?: 0
    )
}
