package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.core.util.shouldHandleTvSelectKey

internal fun shouldTriggerHomeCardClickOnTvKey(
    isTv: Boolean,
    keyCode: Int,
    action: Int
): Boolean {
    if (!isTv) return false
    return shouldHandleTvSelectKey(keyCode = keyCode, action = action)
}
