package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceDynamicArchive
import com.android.purebilibili.data.model.response.SpaceDynamicContent
import com.android.purebilibili.data.model.response.SpaceDynamicItem
import com.android.purebilibili.data.model.response.SpaceDynamicMajor
import com.android.purebilibili.data.model.response.SpaceDynamicModules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpaceDynamicNavigationPolicyTest {

    @Test
    fun resolveSpaceDynamicClickAction_prefersVideoWhenArchiveBvidExists() {
        val dynamic = SpaceDynamicItem(
            id_str = "12345",
            modules = SpaceDynamicModules(
                module_dynamic = SpaceDynamicContent(
                    major = SpaceDynamicMajor(
                        archive = SpaceDynamicArchive(bvid = "BV1xx411c7mD")
                    )
                )
            )
        )

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.OpenVideo)
        assertEquals("BV1xx411c7mD", (action as SpaceDynamicClickAction.OpenVideo).bvid)
    }

    @Test
    fun resolveSpaceDynamicClickAction_usesMobileDynamicDetailWhenNoVideo() {
        val dynamic = SpaceDynamicItem(id_str = "987654321")

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.OpenWeb)
        assertEquals("https://m.bilibili.com/dynamic/987654321", (action as SpaceDynamicClickAction.OpenWeb).url)
    }

    @Test
    fun resolveSpaceDynamicClickAction_returnsNoneWhenNoBvidAndNoId() {
        val dynamic = SpaceDynamicItem(id_str = "   ")

        val action = resolveSpaceDynamicClickAction(dynamic)

        assertTrue(action is SpaceDynamicClickAction.None)
    }
}
