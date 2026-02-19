package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitProgressSyncPolicyTest {

    @Test
    fun applySyncWhenSnapshotBvidMatchesCurrentBvid() {
        assertTrue(
            shouldApplyPortraitProgressSync(
                snapshotBvid = "BV1xx411c7mD",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun doNotApplySyncWhenSnapshotBvidMismatchesCurrentBvid() {
        assertFalse(
            shouldApplyPortraitProgressSync(
                snapshotBvid = "BV1xx411c7mD",
                currentBvid = "BV9xx411c7mD"
            )
        )
    }

    @Test
    fun doNotApplySyncWhenSnapshotBvidBlank() {
        assertFalse(
            shouldApplyPortraitProgressSync(
                snapshotBvid = " ",
                currentBvid = "BV1xx411c7mD"
            )
        )
    }
}
