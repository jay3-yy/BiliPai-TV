package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuTypeFilterPolicyTest {

    @Test
    fun scrollDanmaku_hiddenWhenScrollFilterDisabled() {
        val settings = DanmakuTypeFilterSettings(
            allowScroll = false,
            allowTop = true,
            allowBottom = true,
            allowColorful = true,
            allowSpecial = true
        )

        assertFalse(
            shouldDisplayStandardDanmaku(
                danmakuType = 1,
                color = 0xFFFFFF,
                settings = settings
            )
        )
    }

    @Test
    fun topDanmaku_hiddenWhenTopFilterDisabled() {
        val settings = DanmakuTypeFilterSettings(
            allowScroll = true,
            allowTop = false,
            allowBottom = true,
            allowColorful = true,
            allowSpecial = true
        )

        assertFalse(
            shouldDisplayStandardDanmaku(
                danmakuType = 5,
                color = 0xFFFFFF,
                settings = settings
            )
        )
    }

    @Test
    fun bottomDanmaku_hiddenWhenBottomFilterDisabled() {
        val settings = DanmakuTypeFilterSettings(
            allowScroll = true,
            allowTop = true,
            allowBottom = false,
            allowColorful = true,
            allowSpecial = true
        )

        assertFalse(
            shouldDisplayStandardDanmaku(
                danmakuType = 4,
                color = 0xFFFFFF,
                settings = settings
            )
        )
    }

    @Test
    fun whiteDanmaku_visibleWhenColorFilterDisabled() {
        val settings = DanmakuTypeFilterSettings(
            allowScroll = true,
            allowTop = true,
            allowBottom = true,
            allowColorful = false,
            allowSpecial = true
        )

        assertTrue(
            shouldDisplayStandardDanmaku(
                danmakuType = 1,
                color = 0xFFFFFF,
                settings = settings
            )
        )
    }

    @Test
    fun colorfulDanmaku_hiddenWhenColorFilterDisabled() {
        val settings = DanmakuTypeFilterSettings(
            allowScroll = true,
            allowTop = true,
            allowBottom = true,
            allowColorful = false,
            allowSpecial = true
        )

        assertFalse(
            shouldDisplayStandardDanmaku(
                danmakuType = 1,
                color = 0x00FF00,
                settings = settings
            )
        )
    }

    @Test
    fun advancedDanmaku_hiddenWhenSpecialFilterDisabled() {
        val settings = DanmakuTypeFilterSettings(
            allowScroll = true,
            allowTop = true,
            allowBottom = true,
            allowColorful = true,
            allowSpecial = false
        )

        assertFalse(
            shouldDisplayAdvancedDanmaku(
                color = 0xFFFFFF,
                settings = settings
            )
        )
    }
}
