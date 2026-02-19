package com.android.purebilibili.feature.home.components

enum class TopTabMaterialMode {
    PLAIN,
    BLUR,
    LIQUID_GLASS
}

data class TopTabVisualTuning(
    val nonFloatingIndicatorHeightDp: Float = 31f,
    val nonFloatingIndicatorCornerDp: Float = 14f,
    val nonFloatingIndicatorWidthRatio: Float = 0.72f,
    val nonFloatingIndicatorMinWidthDp: Float = 44f,
    val nonFloatingIndicatorHorizontalInsetDp: Float = 18f,
    val floatingIndicatorWidthMultiplier: Float = 1.34f,
    val floatingIndicatorMinWidthDp: Float = 96f,
    val floatingIndicatorMaxWidthDp: Float = 126f,
    val floatingIndicatorMaxWidthToItemRatio: Float = 1.34f,
    val floatingIndicatorHeightDp: Float = 50f,
    val tabTextSizeSp: Float = 11.6f,
    val tabTextLineHeightSp: Float = 12f,
    val tabContentMinHeightDp: Float = 34f
)

data class TopTabVisualState(
    val floating: Boolean,
    val materialMode: TopTabMaterialMode
)

fun resolveTopTabVisualTuning(): TopTabVisualTuning = TopTabVisualTuning()

fun resolveTopTabLabelTextSizeSp(labelMode: Int): Float {
    val tuning = resolveTopTabVisualTuning()
    return when (normalizeTopTabLabelMode(labelMode)) {
        2 -> tuning.tabTextSizeSp + 0.2f
        else -> tuning.tabTextSizeSp
    }
}

fun resolveTopTabLabelLineHeightSp(labelMode: Int): Float {
    val tuning = resolveTopTabVisualTuning()
    val textSize = resolveTopTabLabelTextSizeSp(labelMode)
    return maxOf(tuning.tabTextLineHeightSp, textSize)
}

fun resolveTopTabContentMinHeightDp(): Float {
    return resolveTopTabVisualTuning().tabContentMinHeightDp
}

fun resolveTopTabStyle(
    isBottomBarFloating: Boolean,
    isBottomBarBlurEnabled: Boolean,
    isLiquidGlassEnabled: Boolean
): TopTabVisualState {
    val materialMode = when {
        isBottomBarFloating && isLiquidGlassEnabled -> TopTabMaterialMode.LIQUID_GLASS
        isBottomBarBlurEnabled -> TopTabMaterialMode.BLUR
        else -> TopTabMaterialMode.PLAIN
    }

    return TopTabVisualState(
        floating = isBottomBarFloating,
        materialMode = materialMode
    )
}
