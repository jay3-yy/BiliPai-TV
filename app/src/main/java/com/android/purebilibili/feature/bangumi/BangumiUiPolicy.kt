package com.android.purebilibili.feature.bangumi

internal fun resolveBangumiNavigationTitleFontSizeSp(screenWidthDp: Int): Float {
    return if (screenWidthDp >= 380) 22f else 20f
}

internal fun resolveBangumiTypeTabFontSizeSp(screenWidthDp: Int): Float {
    return if (screenWidthDp >= 380) 16f else 14f
}

internal fun resolveBangumiPlayerTopControlsPaddingTopDp(
    isFullscreen: Boolean,
    statusBarsInsetDp: Float
): Float {
    val safeInset = statusBarsInsetDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    return if (isFullscreen) 8f else safeInset + 8f
}

internal fun resolveBangumiDanmakuTopInsetDp(
    isFullscreen: Boolean,
    statusBarsInsetDp: Float
): Float {
    val safeInset = statusBarsInsetDp.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    return if (isFullscreen) 0f else safeInset + 52f
}

