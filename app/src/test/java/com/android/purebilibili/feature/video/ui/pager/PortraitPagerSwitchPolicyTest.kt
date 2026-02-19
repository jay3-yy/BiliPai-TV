package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.Stat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortraitPagerSwitchPolicyTest {

    @Test
    fun resolveCommittedPage_returnsNullWhileScrolling() {
        val target = resolveCommittedPage(
            isScrollInProgress = true,
            currentPage = 2,
            lastCommittedPage = 1
        )

        assertNull(target)
    }

    @Test
    fun resolveCommittedPage_returnsPageWhenSettledAndChanged() {
        val target = resolveCommittedPage(
            isScrollInProgress = false,
            currentPage = 2,
            lastCommittedPage = 1
        )

        assertEquals(2, target)
    }

    @Test
    fun resolveCommittedPage_returnsNullWhenSettledButUnchanged() {
        val target = resolveCommittedPage(
            isScrollInProgress = false,
            currentPage = 2,
            lastCommittedPage = 2
        )

        assertNull(target)
    }

    @Test
    fun shouldApplyLoadResult_acceptsOnlyLatestGenerationForSameVideo() {
        assertTrue(
            shouldApplyLoadResult(
                requestGeneration = 5,
                activeGeneration = 5,
                expectedBvid = "BV1xx411c7mD",
                currentPlayingBvid = "BV1xx411c7mD"
            )
        )

        assertFalse(
            shouldApplyLoadResult(
                requestGeneration = 4,
                activeGeneration = 5,
                expectedBvid = "BV1xx411c7mD",
                currentPlayingBvid = "BV1xx411c7mD"
            )
        )

        assertFalse(
            shouldApplyLoadResult(
                requestGeneration = 5,
                activeGeneration = 5,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun shouldShowPortraitCover_showOnlyWhenLoadingOrNotReady() {
        assertTrue(
            shouldShowPortraitCover(
                isLoading = true,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = false
            )
        )

        assertTrue(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = false,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = false
            )
        )

        assertTrue(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = false,
                hasRenderedFirstFrame = false
            )
        )

        assertTrue(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = false
            )
        )

        assertFalse(
            shouldShowPortraitCover(
                isLoading = false,
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasRenderedFirstFrame = true
            )
        )
    }

    @Test
    fun shouldShowPortraitPauseIcon_hidesWhileBufferingOrLoading() {
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = true,
                isLoading = false,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = false,
                isLoading = true,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = false,
                isLoading = false,
                isSeekGesture = true
            )
        )
    }

    @Test
    fun shouldShowPortraitPauseIcon_showsOnlyWhenActuallyPaused() {
        assertTrue(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = false,
                playWhenReady = false,
                isLoading = false,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = false,
                isPlaying = false,
                playWhenReady = false,
                isLoading = false,
                isSeekGesture = false
            )
        )
        assertFalse(
            shouldShowPortraitPauseIcon(
                isCurrentPage = true,
                isPlaying = true,
                playWhenReady = true,
                isLoading = false,
                isSeekGesture = false
            )
        )
    }

    @Test
    fun resolvePortraitInitialProgressPosition_usesEntryPositionOnlyForFirstPage() {
        assertEquals(
            12000L,
            resolvePortraitInitialProgressPosition(
                isFirstPage = true,
                initialStartPositionMs = 12000L
            )
        )
        assertEquals(
            0L,
            resolvePortraitInitialProgressPosition(
                isFirstPage = false,
                initialStartPositionMs = 12000L
            )
        )
    }

    @Test
    fun toViewInfoForPortraitDetail_mapsCoreFieldsFromRelatedVideo() {
        val related = RelatedVideo(
            aid = 1001L,
            bvid = "BV1xK4y1d7Q1",
            title = "sample",
            pic = "https://example.com/cover.jpg",
            owner = Owner(mid = 123L, name = "up"),
            stat = Stat(view = 999, like = 77),
            duration = 65
        )

        val info = toViewInfoForPortraitDetail(related)

        assertEquals(related.bvid, info.bvid)
        assertEquals(related.aid, info.aid)
        assertEquals(related.title, info.title)
        assertEquals(related.pic, info.pic)
        assertEquals(related.owner.mid, info.owner.mid)
        assertEquals(related.stat.view, info.stat.view)
        assertEquals(65L, info.pages.firstOrNull()?.duration)
    }
}
