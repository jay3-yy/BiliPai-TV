package com.android.purebilibili.core.ui.adaptive

import com.android.purebilibili.core.util.WindowWidthSizeClass

enum class MotionTier {
    Reduced,
    Normal,
    Enhanced
}

data class DeviceUiProfile(
    val widthSizeClass: WindowWidthSizeClass,
    val isTablet: Boolean,
    val isTv: Boolean,
    val isTenFootUi: Boolean,
    val motionTier: MotionTier
)

fun resolveDeviceUiProfile(
    isTv: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    tvPerformanceProfileEnabled: Boolean
): DeviceUiProfile {
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
    val motionTier = when {
        isTv && tvPerformanceProfileEnabled -> MotionTier.Reduced
        isTv -> MotionTier.Normal
        widthSizeClass == WindowWidthSizeClass.Expanded -> MotionTier.Enhanced
        else -> MotionTier.Normal
    }

    return DeviceUiProfile(
        widthSizeClass = widthSizeClass,
        isTablet = isTablet,
        isTv = isTv,
        isTenFootUi = isTv,
        motionTier = motionTier
    )
}
