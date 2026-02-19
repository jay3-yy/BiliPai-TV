package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLabelModePolicyTest {

    @Test
    fun `normalize should keep known modes`() {
        assertTrue(shouldShowTopTabIcon(normalizeTopTabLabelMode(0)))
        assertTrue(shouldShowTopTabText(normalizeTopTabLabelMode(0)))

        assertTrue(shouldShowTopTabIcon(normalizeTopTabLabelMode(1)))
        assertFalse(shouldShowTopTabText(normalizeTopTabLabelMode(1)))

        assertFalse(shouldShowTopTabIcon(normalizeTopTabLabelMode(2)))
        assertTrue(shouldShowTopTabText(normalizeTopTabLabelMode(2)))
    }

    @Test
    fun `normalize should fallback to text only for unknown mode`() {
        val mode = normalizeTopTabLabelMode(99)
        assertFalse(shouldShowTopTabIcon(mode))
        assertTrue(shouldShowTopTabText(mode))
    }
}
