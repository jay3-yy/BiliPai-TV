package com.android.purebilibili.feature.video.ui.pager

internal fun shouldReloadMainPlayerAfterPortraitExit(
    snapshotBvid: String?,
    currentBvid: String?
): Boolean {
    if (snapshotBvid.isNullOrBlank()) return false
    if (currentBvid.isNullOrBlank()) return true
    return snapshotBvid != currentBvid
}

internal fun shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

internal fun resolvePortraitInitialPlayingBvid(
    useSharedPlayer: Boolean,
    initialBvid: String
): String? {
    if (!useSharedPlayer) return null
    return initialBvid
}

internal fun shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

internal fun shouldExitPortraitForExternalNavigation(isPortraitFullscreen: Boolean): Boolean {
    return isPortraitFullscreen
}

internal fun shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen: Boolean): Boolean {
    return false
}
