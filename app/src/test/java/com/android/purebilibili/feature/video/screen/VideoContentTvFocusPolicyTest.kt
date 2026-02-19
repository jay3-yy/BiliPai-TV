package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoContentTvFocusPolicyTest {

    @Test
    fun firstRelatedIndex_withoutPages_isSecondItemBlock() {
        val index = resolveFirstRelatedItemIndex(hasPages = false)
        assertEquals(2, index)
    }

    @Test
    fun firstRelatedIndex_withPages_isThirdItemBlock() {
        val index = resolveFirstRelatedItemIndex(hasPages = true)
        assertEquals(3, index)
    }
}
