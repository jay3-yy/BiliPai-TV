package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.RelatedVideo
import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitVideoPagerPolicyTest {

    @Test
    fun resolvePortraitInitialPageIndex_returnsFirstPageWhenInitialMatchesInfo() {
        val index = resolvePortraitInitialPageIndex(
            initialBvid = "BV1",
            initialInfoBvid = "BV1",
            recommendations = listOf(RelatedVideo(bvid = "BV2"))
        )

        assertEquals(0, index)
    }

    @Test
    fun resolvePortraitInitialPageIndex_pointsToRecommendationWhenMatched() {
        val index = resolvePortraitInitialPageIndex(
            initialBvid = "BV3",
            initialInfoBvid = "BV1",
            recommendations = listOf(
                RelatedVideo(bvid = "BV2"),
                RelatedVideo(bvid = "BV3"),
                RelatedVideo(bvid = "BV4")
            )
        )

        assertEquals(2, index)
    }

    @Test
    fun resolvePortraitInitialPageIndex_fallsBackToFirstPageWhenNotFound() {
        val index = resolvePortraitInitialPageIndex(
            initialBvid = "BV9",
            initialInfoBvid = "BV1",
            recommendations = listOf(RelatedVideo(bvid = "BV2"))
        )

        assertEquals(0, index)
    }
}
