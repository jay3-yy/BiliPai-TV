package com.android.purebilibili.feature.home.components.cards

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCardLongPressPolicyTest {

    @Test
    fun shouldOpenLongPressMenu_opensWhenNoPreviewAndMenuAvailable() {
        assertTrue(shouldOpenLongPressMenu(hasPreviewAction = false, hasMenuAction = true))
    }

    @Test
    fun shouldOpenLongPressMenu_notOpenWhenPreviewActionExists() {
        assertFalse(shouldOpenLongPressMenu(hasPreviewAction = true, hasMenuAction = true))
    }

    @Test
    fun shouldOpenLongPressMenu_notOpenWhenMenuUnavailable() {
        assertFalse(shouldOpenLongPressMenu(hasPreviewAction = false, hasMenuAction = false))
    }
}
