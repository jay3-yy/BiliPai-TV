package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.feature.video.player.PlaylistItem

data class SpaceExternalPlaylist(
    val playlistItems: List<PlaylistItem>,
    val startIndex: Int
)

fun buildExternalPlaylistFromSpaceVideos(
    videos: List<SpaceVideoItem>,
    clickedBvid: String? = null
): SpaceExternalPlaylist? {
    if (videos.isEmpty()) return null

    val playlistItems = videos.map { video ->
        PlaylistItem(
            bvid = video.bvid,
            title = video.title,
            cover = video.pic,
            owner = video.author,
            duration = parseSpaceVideoLengthToSeconds(video.length)
        )
    }

    val startIndex = clickedBvid
        ?.takeIf { it.isNotBlank() }
        ?.let { bvid -> videos.indexOfFirst { it.bvid == bvid }.takeIf { it >= 0 } }
        ?: 0

    return SpaceExternalPlaylist(
        playlistItems = playlistItems,
        startIndex = startIndex
    )
}

internal fun parseSpaceVideoLengthToSeconds(length: String): Long {
    val normalized = length.trim()
    if (normalized.isEmpty()) return 0L
    val parts = normalized.split(":").mapNotNull { it.toLongOrNull() }
    if (parts.isEmpty()) return 0L

    return when (parts.size) {
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> 0L
    }
}
