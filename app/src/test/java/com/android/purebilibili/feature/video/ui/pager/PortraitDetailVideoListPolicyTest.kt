package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RelatedVideo
import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitDetailVideoListPolicyTest {

    private fun related(bvid: String, title: String): RelatedVideo {
        return RelatedVideo(
            bvid = bvid,
            title = title,
            owner = Owner(name = "up")
        )
    }

    @Test
    fun `buildPortraitDetailVideoList prefers watch later items`() {
        val result = buildPortraitDetailVideoList(
            currentBvid = "BV_CURRENT",
            watchLaterVideos = listOf(
                related("BV_A", "a"),
                related("BV_B", "b")
            ),
            recommendationVideos = listOf(
                related("BV_C", "c")
            )
        )

        assertEquals("稍后再看", result.title)
        assertEquals(listOf("BV_A", "BV_B"), result.videos.map { it.bvid })
    }

    @Test
    fun `buildPortraitDetailVideoList falls back to recommendations when watch later empty`() {
        val result = buildPortraitDetailVideoList(
            currentBvid = "BV_CURRENT",
            watchLaterVideos = emptyList(),
            recommendationVideos = listOf(
                related("BV_C", "c"),
                related("BV_D", "d")
            )
        )

        assertEquals("推荐视频", result.title)
        assertEquals(listOf("BV_C", "BV_D"), result.videos.map { it.bvid })
    }
}
