package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuSettingsPolicyTest {

    @Test
    fun `opacity should fallback to default when value is null`() {
        assertEquals(DANMAKU_DEFAULT_OPACITY, normalizeDanmakuOpacity(null), 0.0001f)
    }

    @Test
    fun `opacity should clamp to minimum`() {
        assertEquals(DANMAKU_MIN_OPACITY, normalizeDanmakuOpacity(0f), 0.0001f)
        assertEquals(DANMAKU_MIN_OPACITY, normalizeDanmakuOpacity(-0.5f), 0.0001f)
    }

    @Test
    fun `opacity should clamp to maximum`() {
        assertEquals(DANMAKU_MAX_OPACITY, normalizeDanmakuOpacity(2f), 0.0001f)
    }
}
