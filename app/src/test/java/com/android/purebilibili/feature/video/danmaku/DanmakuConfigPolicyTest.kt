package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuConfigPolicyTest {

    @Test
    fun `minimum visible lines should not degrade to single line`() {
        assertEquals(2, resolveDanmakuMinimumVisibleLines(0.25f))
        assertEquals(3, resolveDanmakuMinimumVisibleLines(0.5f))
        assertEquals(5, resolveDanmakuMinimumVisibleLines(0.75f))
        assertEquals(6, resolveDanmakuMinimumVisibleLines(1.0f))
    }

    @Test
    fun `fallback max lines should remain stable by area ratio`() {
        assertEquals(4, resolveDanmakuFallbackMaxLines(0.25f))
        assertEquals(8, resolveDanmakuFallbackMaxLines(0.5f))
        assertEquals(12, resolveDanmakuFallbackMaxLines(0.75f))
        assertEquals(16, resolveDanmakuFallbackMaxLines(1.0f))
    }
}

