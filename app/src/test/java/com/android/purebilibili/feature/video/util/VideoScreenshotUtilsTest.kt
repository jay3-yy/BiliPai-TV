package com.android.purebilibili.feature.video.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoScreenshotUtilsTest {

    @Test
    fun validVideoSize_prefersVideoResolution() {
        val result = resolveScreenshotDimensions(
            videoWidth = 1920,
            videoHeight = 1080,
            surfaceWidth = 1280,
            surfaceHeight = 720
        )

        assertEquals(Pair(1920, 1080), result)
    }

    @Test
    fun invalidVideoSize_fallsBackToSurfaceResolution() {
        val result = resolveScreenshotDimensions(
            videoWidth = 0,
            videoHeight = 0,
            surfaceWidth = 1280,
            surfaceHeight = 720
        )

        assertEquals(Pair(1280, 720), result)
    }

    @Test
    fun emptySize_fallsBackToOnePixel() {
        val result = resolveScreenshotDimensions(
            videoWidth = 0,
            videoHeight = 0,
            surfaceWidth = 0,
            surfaceHeight = 0
        )

        assertEquals(Pair(1, 1), result)
    }

    @Test
    fun screenshotName_usesPngAndSanitizesIllegalChars() {
        val fileName = buildScreenshotFileName(
            videoTitle = "A/B:C*?\"<test>|",
            timestampMs = 1700000000000L
        )

        assertTrue(fileName.endsWith(".png"))
        assertFalse(fileName.contains('/'))
        assertFalse(fileName.contains(':'))
        assertFalse(fileName.contains('*'))
        assertFalse(fileName.contains('?'))
        assertFalse(fileName.contains('"'))
        assertFalse(fileName.contains('<'))
        assertFalse(fileName.contains('>'))
        assertFalse(fileName.contains('|'))
    }
}
