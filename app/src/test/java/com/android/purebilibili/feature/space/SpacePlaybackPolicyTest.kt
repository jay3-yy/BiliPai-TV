package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceVideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpacePlaybackPolicyTest {

    private fun item(
        bvid: String,
        title: String,
        length: String,
        author: String = "up"
    ): SpaceVideoItem {
        return SpaceVideoItem(
            bvid = bvid,
            title = title,
            pic = "https://example.com/$bvid.jpg",
            length = length,
            author = author
        )
    }

    @Test
    fun buildExternalPlaylistFromSpaceVideos_startsFromClickedVideo() {
        val videos = listOf(
            item(bvid = "BV1", title = "first", length = "01:23"),
            item(bvid = "BV2", title = "second", length = "10:24"),
            item(bvid = "BV3", title = "third", length = "1:02:03")
        )

        val playlist = buildExternalPlaylistFromSpaceVideos(videos, clickedBvid = "BV2")

        assertEquals(1, playlist?.startIndex)
        assertEquals(listOf("BV1", "BV2", "BV3"), playlist?.playlistItems?.map { it.bvid })
        assertEquals(83L, playlist?.playlistItems?.get(0)?.duration)
        assertEquals(624L, playlist?.playlistItems?.get(1)?.duration)
        assertEquals(3723L, playlist?.playlistItems?.get(2)?.duration)
    }

    @Test
    fun buildExternalPlaylistFromSpaceVideos_fallbackToFirstWhenClickedMissing() {
        val videos = listOf(
            item(bvid = "BV1", title = "first", length = "00:30"),
            item(bvid = "BV2", title = "second", length = "01:00")
        )

        val playlist = buildExternalPlaylistFromSpaceVideos(videos, clickedBvid = "BV404")

        assertEquals(0, playlist?.startIndex)
    }

    @Test
    fun buildExternalPlaylistFromSpaceVideos_returnsNullForEmptyVideos() {
        assertNull(buildExternalPlaylistFromSpaceVideos(emptyList(), clickedBvid = "BV1"))
    }
}
