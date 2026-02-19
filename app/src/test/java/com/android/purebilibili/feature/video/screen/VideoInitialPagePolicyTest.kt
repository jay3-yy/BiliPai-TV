package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.data.model.response.Page
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoInitialPagePolicyTest {

    @Test
    fun `resolveInitialPageIndex returns matching page index`() {
        val pages = listOf(
            Page(cid = 11L, page = 1),
            Page(cid = 22L, page = 2)
        )

        val index = resolveInitialPageIndex(
            requestedCid = 22L,
            currentCid = 11L,
            pages = pages
        )

        assertEquals(1, index)
    }

    @Test
    fun `resolveInitialPageIndex returns null when requested cid already active`() {
        val pages = listOf(Page(cid = 11L, page = 1))

        val index = resolveInitialPageIndex(
            requestedCid = 11L,
            currentCid = 11L,
            pages = pages
        )

        assertNull(index)
    }
}
