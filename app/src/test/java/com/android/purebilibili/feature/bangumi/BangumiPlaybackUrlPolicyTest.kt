package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.Durl
import org.junit.Assert.assertEquals
import org.junit.Test

class BangumiPlaybackUrlPolicyTest {

    @Test
    fun `should keep all playable durl segments in order`() {
        val urls = collectPlayableDurlUrls(
            listOf(
                Durl(order = 1, url = "https://cdn-1/video-1.m4s"),
                Durl(order = 2, url = "https://cdn-1/video-2.m4s"),
                Durl(order = 3, url = "https://cdn-1/video-3.m4s")
            )
        )

        assertEquals(
            listOf(
                "https://cdn-1/video-1.m4s",
                "https://cdn-1/video-2.m4s",
                "https://cdn-1/video-3.m4s"
            ),
            urls
        )
    }

    @Test
    fun `should fallback to backup url when primary url missing`() {
        val urls = collectPlayableDurlUrls(
            listOf(
                Durl(order = 1, url = "", backupUrl = listOf("https://backup/video-1.m4s")),
                Durl(order = 2, url = "https://cdn-1/video-2.m4s")
            )
        )

        assertEquals(
            listOf("https://backup/video-1.m4s", "https://cdn-1/video-2.m4s"),
            urls
        )
    }

    @Test
    fun `should ignore empty segments`() {
        val urls = collectPlayableDurlUrls(
            listOf(
                Durl(order = 1, url = "", backupUrl = emptyList()),
                Durl(order = 2, url = "https://cdn-1/video-2.m4s")
            )
        )

        assertEquals(listOf("https://cdn-1/video-2.m4s"), urls)
    }
}
