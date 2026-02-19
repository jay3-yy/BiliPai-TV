package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeClickGesturePolicyTest {

    @Test
    fun homeUsesCombinedGestureOnlyWhenSelected() {
        assertTrue(
            shouldUseHomeCombinedClickable(
                item = BottomNavItem.HOME,
                isSelected = true
            )
        )
        assertFalse(
            shouldUseHomeCombinedClickable(
                item = BottomNavItem.HOME,
                isSelected = false
            )
        )
    }

    @Test
    fun nonHomeNeverUsesCombinedGesture() {
        assertFalse(
            shouldUseHomeCombinedClickable(
                item = BottomNavItem.PROFILE,
                isSelected = true
            )
        )
    }

    @Test
    fun dynamicUsesReselectCombinedGestureOnlyWhenSelected() {
        assertTrue(
            shouldUseBottomReselectCombinedClickable(
                item = BottomNavItem.DYNAMIC,
                isSelected = true
            )
        )
        assertFalse(
            shouldUseBottomReselectCombinedClickable(
                item = BottomNavItem.DYNAMIC,
                isSelected = false
            )
        )
    }
}
