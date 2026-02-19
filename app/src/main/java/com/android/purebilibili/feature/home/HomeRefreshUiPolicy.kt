package com.android.purebilibili.feature.home

internal fun shouldHandleRefreshNewItemsEvent(
    refreshKey: Long,
    handledKey: Long
): Boolean {
    if (refreshKey <= 0L) return false
    return refreshKey > handledKey
}
