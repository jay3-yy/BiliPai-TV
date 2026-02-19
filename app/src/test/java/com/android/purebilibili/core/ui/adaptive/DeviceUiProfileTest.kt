package com.android.purebilibili.core.ui.adaptive

import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceUiProfileTest {

    @Test
    fun tv_withPerformanceProfile_mapsToReducedMotionAndTenFootUi() {
        val profile = resolveDeviceUiProfile(
            isTv = true,
            widthSizeClass = WindowWidthSizeClass.Expanded,
            tvPerformanceProfileEnabled = true
        )

        assertEquals(MotionTier.Reduced, profile.motionTier)
        assertTrue(profile.isTenFootUi)
        assertTrue(profile.isTv)
    }

    @Test
    fun tv_withoutPerformanceProfile_keepsNormalMotion() {
        val profile = resolveDeviceUiProfile(
            isTv = true,
            widthSizeClass = WindowWidthSizeClass.Expanded,
            tvPerformanceProfileEnabled = false
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
    }

    @Test
    fun expandedTablet_prefersEnhancedMotionTier() {
        val profile = resolveDeviceUiProfile(
            isTv = false,
            widthSizeClass = WindowWidthSizeClass.Expanded,
            tvPerformanceProfileEnabled = false
        )

        assertEquals(MotionTier.Enhanced, profile.motionTier)
        assertTrue(profile.isTablet)
        assertFalse(profile.isTv)
    }

    @Test
    fun compactPhone_usesNormalMotionAndNotTablet() {
        val profile = resolveDeviceUiProfile(
            isTv = false,
            widthSizeClass = WindowWidthSizeClass.Compact,
            tvPerformanceProfileEnabled = false
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
        assertFalse(profile.isTablet)
    }
}
