package com.android.purebilibili.feature.watchlater

import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.feature.video.player.PlaylistItem

data class WatchLaterExternalPlaylist(
    val playlistItems: List<PlaylistItem>,
    val startIndex: Int
)

fun buildExternalPlaylistFromWatchLater(
    items: List<VideoItem>,
    clickedBvid: String? = null
): WatchLaterExternalPlaylist? {
    if (items.isEmpty()) return null

    val playlistItems = items.map { video ->
        PlaylistItem(
            bvid = video.bvid,
            title = video.title,
            cover = video.pic,
            owner = video.owner.name,
            duration = video.duration.toLong()
        )
    }

    val startIndex = clickedBvid
        ?.takeIf { it.isNotBlank() }
        ?.let { bvid -> items.indexOfFirst { it.bvid == bvid }.takeIf { it >= 0 } }
        ?: 0

    return WatchLaterExternalPlaylist(
        playlistItems = playlistItems,
        startIndex = startIndex
    )
}
