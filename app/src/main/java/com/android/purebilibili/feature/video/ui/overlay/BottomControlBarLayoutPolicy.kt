package com.android.purebilibili.feature.video.ui.overlay

data class BottomControlBarLayoutPolicy(
    val bottomPaddingDp: Int,
    val progressSpacingDp: Int,
    val horizontalPaddingDp: Int,
    val playButtonSizeDp: Int,
    val playIconSizeDp: Int,
    val afterPlaySpacingDp: Int,
    val timeFontSp: Int,
    val afterTimeSpacingDp: Int,
    val danmakuIconSizeDp: Int,
    val danmakuSwitchToInputSpacingDp: Int,
    val danmakuInputHeightDp: Int,
    val danmakuInputStartPaddingDp: Int,
    val danmakuInputFontSp: Int,
    val danmakuSettingButtonSizeDp: Int,
    val danmakuSettingEndPaddingDp: Int,
    val danmakuSettingIconSizeDp: Int,
    val afterInputSpacingDp: Int,
    val rightActionSpacingDp: Int,
    val actionTextFontSp: Int,
    val fullscreenIconSizeDp: Int
)

fun resolveBottomControlBarLayoutPolicy(
    widthDp: Int,
    isTv: Boolean
): BottomControlBarLayoutPolicy {
    if (isTv || widthDp >= 1600) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 18,
            progressSpacingDp = 12,
            horizontalPaddingDp = 32,
            playButtonSizeDp = 48,
            playIconSizeDp = 36,
            afterPlaySpacingDp = 12,
            timeFontSp = 15,
            afterTimeSpacingDp = 24,
            danmakuIconSizeDp = 32,
            danmakuSwitchToInputSpacingDp = 16,
            danmakuInputHeightDp = 52,
            danmakuInputStartPaddingDp = 20,
            danmakuInputFontSp = 16,
            danmakuSettingButtonSizeDp = 44,
            danmakuSettingEndPaddingDp = 6,
            danmakuSettingIconSizeDp = 24,
            afterInputSpacingDp = 24,
            rightActionSpacingDp = 24,
            actionTextFontSp = 17,
            fullscreenIconSizeDp = 28
        )
    }

    if (widthDp >= 840) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 14,
            progressSpacingDp = 10,
            horizontalPaddingDp = 24,
            playButtonSizeDp = 40,
            playIconSizeDp = 32,
            afterPlaySpacingDp = 10,
            timeFontSp = 13,
            afterTimeSpacingDp = 20,
            danmakuIconSizeDp = 28,
            danmakuSwitchToInputSpacingDp = 14,
            danmakuInputHeightDp = 44,
            danmakuInputStartPaddingDp = 18,
            danmakuInputFontSp = 14,
            danmakuSettingButtonSizeDp = 36,
            danmakuSettingEndPaddingDp = 5,
            danmakuSettingIconSizeDp = 20,
            afterInputSpacingDp = 20,
            rightActionSpacingDp = 20,
            actionTextFontSp = 15,
            fullscreenIconSizeDp = 24
        )
    }

    if (widthDp >= 600) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 13,
            progressSpacingDp = 9,
            horizontalPaddingDp = 20,
            playButtonSizeDp = 36,
            playIconSizeDp = 30,
            afterPlaySpacingDp = 9,
            timeFontSp = 13,
            afterTimeSpacingDp = 18,
            danmakuIconSizeDp = 26,
            danmakuSwitchToInputSpacingDp = 13,
            danmakuInputHeightDp = 40,
            danmakuInputStartPaddingDp = 17,
            danmakuInputFontSp = 14,
            danmakuSettingButtonSizeDp = 34,
            danmakuSettingEndPaddingDp = 4,
            danmakuSettingIconSizeDp = 19,
            afterInputSpacingDp = 18,
            rightActionSpacingDp = 18,
            actionTextFontSp = 14,
            fullscreenIconSizeDp = 22
        )
    }

    return BottomControlBarLayoutPolicy(
        bottomPaddingDp = 12,
        progressSpacingDp = 8,
        horizontalPaddingDp = 16,
        playButtonSizeDp = 32,
        playIconSizeDp = 28,
        afterPlaySpacingDp = 8,
        timeFontSp = 12,
        afterTimeSpacingDp = 16,
        danmakuIconSizeDp = 24,
        danmakuSwitchToInputSpacingDp = 12,
        danmakuInputHeightDp = 36,
        danmakuInputStartPaddingDp = 16,
        danmakuInputFontSp = 13,
        danmakuSettingButtonSizeDp = 32,
        danmakuSettingEndPaddingDp = 4,
        danmakuSettingIconSizeDp = 18,
        afterInputSpacingDp = 16,
        rightActionSpacingDp = 16,
        actionTextFontSp = 13,
        fullscreenIconSizeDp = 20
    )
}
