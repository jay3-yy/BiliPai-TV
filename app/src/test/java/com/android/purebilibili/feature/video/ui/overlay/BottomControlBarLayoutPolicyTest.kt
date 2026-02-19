package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class BottomControlBarLayoutPolicyTest {

    @Test
    fun compactPhone_usesDenseBottomControls() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 393,
            isTv = false
        )

        assertEquals(32, policy.playButtonSizeDp)
        assertEquals(28, policy.playIconSizeDp)
        assertEquals(12, policy.timeFontSp)
        assertEquals(13, policy.actionTextFontSp)
        assertEquals(4, policy.danmakuSettingEndPaddingDp)
    }

    @Test
    fun mediumTablet_increasesTapTargetsWithoutOverexpansion() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 720,
            isTv = false
        )

        assertEquals(36, policy.playButtonSizeDp)
        assertEquals(30, policy.playIconSizeDp)
        assertEquals(13, policy.timeFontSp)
        assertEquals(14, policy.actionTextFontSp)
        assertEquals(4, policy.danmakuSettingEndPaddingDp)
    }

    @Test
    fun tabletWidth_expandsTouchTargetsAndSpacing() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 1024,
            isTv = false
        )

        assertEquals(40, policy.playButtonSizeDp)
        assertEquals(32, policy.playIconSizeDp)
        assertEquals(13, policy.timeFontSp)
        assertEquals(15, policy.actionTextFontSp)
        assertEquals(5, policy.danmakuSettingEndPaddingDp)
    }

    @Test
    fun tv_forcesLargestBottomControlScale() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 1080,
            isTv = true
        )

        assertEquals(48, policy.playButtonSizeDp)
        assertEquals(36, policy.playIconSizeDp)
        assertEquals(15, policy.timeFontSp)
        assertEquals(17, policy.actionTextFontSp)
        assertEquals(6, policy.danmakuSettingEndPaddingDp)
    }
}
