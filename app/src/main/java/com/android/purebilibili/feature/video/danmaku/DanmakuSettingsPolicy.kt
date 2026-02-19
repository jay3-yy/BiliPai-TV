package com.android.purebilibili.feature.video.danmaku

const val DANMAKU_MIN_OPACITY = 0.1f
const val DANMAKU_MAX_OPACITY = 1.0f
const val DANMAKU_DEFAULT_OPACITY = 0.85f

fun normalizeDanmakuOpacity(value: Float?): Float {
    val fallback = DANMAKU_DEFAULT_OPACITY
    val raw = value ?: fallback
    if (!raw.isFinite()) return fallback
    return raw.coerceIn(DANMAKU_MIN_OPACITY, DANMAKU_MAX_OPACITY)
}
