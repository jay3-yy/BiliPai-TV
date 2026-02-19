package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodayWatchMotionPolicyTest {

    @Test
    fun `waterfall delay starts at zero and grows`() {
        assertEquals(0, nonLinearWaterfallDelayMillis(index = 0))
        assertTrue(nonLinearWaterfallDelayMillis(index = 1) > 0)
        assertTrue(nonLinearWaterfallDelayMillis(index = 4) > nonLinearWaterfallDelayMillis(index = 2))
    }

    @Test
    fun `waterfall delay growth is nonlinear`() {
        val d1 = nonLinearWaterfallDelayMillis(index = 1)
        val d2 = nonLinearWaterfallDelayMillis(index = 2)
        val d3 = nonLinearWaterfallDelayMillis(index = 3)

        val firstGap = d2 - d1
        val secondGap = d3 - d2
        assertTrue(secondGap > firstGap)
    }

    @Test
    fun `waterfall delay is capped`() {
        val delay = nonLinearWaterfallDelayMillis(
            index = 40,
            baseDelayMs = 60,
            exponent = 1.5f,
            maxDelayMs = 560
        )
        assertEquals(560, delay)
    }
}
