package com.android.purebilibili.feature.home

import kotlin.math.pow

internal fun nonLinearWaterfallDelayMillis(
    index: Int,
    baseDelayMs: Int = 52,
    exponent: Float = 1.38f,
    maxDelayMs: Int = 620
): Int {
    if (index <= 0) return 0
    val normalizedBase = baseDelayMs.coerceAtLeast(1)
    val normalizedExponent = exponent.coerceIn(1.0f, 2.4f)
    val delay = (normalizedBase * index.toDouble().pow(normalizedExponent.toDouble())).toInt()
    return delay.coerceAtMost(maxDelayMs.coerceAtLeast(normalizedBase))
}
