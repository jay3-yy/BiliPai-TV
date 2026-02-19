package com.android.purebilibili.feature.video.ui.pager

internal fun shouldApplyPortraitProgressSync(
    snapshotBvid: String?,
    currentBvid: String?
): Boolean {
    if (snapshotBvid.isNullOrBlank()) return false
    if (currentBvid.isNullOrBlank()) return false
    return snapshotBvid == currentBvid
}
