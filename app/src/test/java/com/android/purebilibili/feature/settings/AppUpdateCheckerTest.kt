package com.android.purebilibili.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun `normalizeVersion should trim v prefix and prerelease suffix`() {
        assertEquals("5.3.1", AppUpdateChecker.normalizeVersion("v5.3.1-beta.1"))
        assertEquals("5.3.1", AppUpdateChecker.normalizeVersion(" V5.3.1 "))
    }

    @Test
    fun `isRemoteNewer should compare semantic version parts`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3.2"))
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3.1", "5.4.0"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3.1"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.2", "5.3.1"))
    }

    @Test
    fun `isRemoteNewer should handle different part lengths`() {
        assertTrue(AppUpdateChecker.isRemoteNewer("5.3", "5.3.1"))
        assertFalse(AppUpdateChecker.isRemoteNewer("5.3.1", "5.3"))
    }
}

