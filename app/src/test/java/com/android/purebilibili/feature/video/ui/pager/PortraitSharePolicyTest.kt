package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitSharePolicyTest {

    @Test
    fun buildPortraitShareTextIncludesTitleAndBilibiliLink() {
        val text = buildPortraitShareText(
            title = "测试视频",
            bvid = "BV1xx411c7mD"
        )

        assertEquals(
            "【测试视频】\nhttps://www.bilibili.com/video/BV1xx411c7mD",
            text
        )
    }

    @Test
    fun buildPortraitShareTextFallsBackWhenTitleBlank() {
        val text = buildPortraitShareText(
            title = "  ",
            bvid = "BV17x411w7KC"
        )

        assertEquals(
            "https://www.bilibili.com/video/BV17x411w7KC",
            text
        )
    }
}
