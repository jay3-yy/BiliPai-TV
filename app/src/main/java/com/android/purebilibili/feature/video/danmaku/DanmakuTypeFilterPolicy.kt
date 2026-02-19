package com.android.purebilibili.feature.video.danmaku

private const val DANMAKU_WHITE_RGB = 0x00FFFFFF

data class DanmakuTypeFilterSettings(
    val allowScroll: Boolean = true,
    val allowTop: Boolean = true,
    val allowBottom: Boolean = true,
    val allowColorful: Boolean = true,
    val allowSpecial: Boolean = true,
)

fun shouldDisplayStandardDanmaku(
    danmakuType: Int,
    color: Int,
    settings: DanmakuTypeFilterSettings,
): Boolean {
    val typeAllowed = when (danmakuType) {
        4 -> settings.allowBottom
        5 -> settings.allowTop
        else -> settings.allowScroll
    }
    if (!typeAllowed) {
        return false
    }

    if (!settings.allowColorful && isColorfulDanmaku(color)) {
        return false
    }

    return true
}

fun shouldDisplayAdvancedDanmaku(
    color: Int,
    settings: DanmakuTypeFilterSettings,
): Boolean {
    if (!settings.allowSpecial) {
        return false
    }
    if (!settings.allowColorful && isColorfulDanmaku(color)) {
        return false
    }
    return true
}

fun isColorfulDanmaku(color: Int): Boolean {
    val rgb = color and DANMAKU_WHITE_RGB
    return rgb != DANMAKU_WHITE_RGB
}
