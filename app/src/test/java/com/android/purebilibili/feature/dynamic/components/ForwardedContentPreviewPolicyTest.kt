package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.DrawMajor
import com.android.purebilibili.data.model.response.DrawItem
import com.android.purebilibili.data.model.response.OpusMajor
import com.android.purebilibili.data.model.response.OpusPic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ForwardedContentPreviewPolicyTest {

    @Test
    fun resolveForwardedDrawPreviewState_returnsAllImagesWithClickedIndex() {
        val draw = DrawMajor(
            items = listOf(
                DrawItem(src = "a.jpg"),
                DrawItem(src = "b.jpg"),
                DrawItem(src = "c.jpg")
            )
        )

        val state = resolveForwardedDrawPreviewState(draw, clickedIndex = 1)

        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg"), state?.images)
        assertEquals(1, state?.initialIndex)
    }

    @Test
    fun resolveForwardedOpusPreviewState_returnsNullWhenIndexInvalid() {
        val opus = OpusMajor(
            pics = listOf(
                OpusPic(url = "1.jpg"),
                OpusPic(url = "2.jpg")
            )
        )

        val state = resolveForwardedOpusPreviewState(opus, clickedIndex = 2)

        assertNull(state)
    }
}
