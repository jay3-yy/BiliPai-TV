package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitMainPlayerSyncPolicyTest {

    @Test
    fun noReloadWhenSnapshotBvidBlank() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = " ",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun reloadWhenCurrentBvidMissingButSnapshotExists() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                currentBvid = null
            )
        )
    }

    @Test
    fun noReloadWhenSnapshotMatchesCurrent() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun reloadWhenSnapshotDiffersFromCurrent() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV17x411w7KC",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun sharedPlayerMode_shouldNotPauseMainPlayerOnPortraitEnter() {
        assertFalse(
            shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = true)
        )
        assertTrue(
            shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = false)
        )
    }

    @Test
    fun resolvePortraitInitialPlayingBvid_usesInitialBvidOnlyWhenShared() {
        assertEquals(
            "BV1xx411c7mD",
            resolvePortraitInitialPlayingBvid(
                useSharedPlayer = true,
                initialBvid = "BV1xx411c7mD"
            )
        )
        assertEquals(
            null,
            resolvePortraitInitialPlayingBvid(
                useSharedPlayer = false,
                initialBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun sharedPlayerMode_shouldNotMirrorPortraitProgressBackToMainPlayer() {
        assertFalse(shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = true))
        assertTrue(shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = false))
    }

    @Test
    fun externalNavigation_shouldExitPortraitBeforeJumping() {
        assertTrue(shouldExitPortraitForExternalNavigation(isPortraitFullscreen = true))
        assertFalse(shouldExitPortraitForExternalNavigation(isPortraitFullscreen = false))
    }

    @Test
    fun userSpaceNavigation_shouldKeepPortraitState() {
        assertFalse(shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen = true))
        assertFalse(shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen = false))
    }
}
