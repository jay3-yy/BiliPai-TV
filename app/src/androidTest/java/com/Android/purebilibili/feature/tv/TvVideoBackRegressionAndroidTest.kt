package com.android.purebilibili.feature.tv

import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.screen.shouldRotateToPortraitOnSplitBack
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvVideoBackRegressionAndroidTest {

    @Test
    fun tvSplitLayoutBack_neverForcesPortraitRotation() {
        val shouldRotate = shouldRotateToPortraitOnSplitBack(
            useTabletLayout = true,
            smallestScreenWidthDp = 540,
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            isTvDevice = true
        )

        assertFalse(shouldRotate)
    }

    @Test
    fun phoneLandscapeSplitBack_stillForcesPortraitRotation() {
        val shouldRotate = shouldRotateToPortraitOnSplitBack(
            useTabletLayout = true,
            smallestScreenWidthDp = 540,
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            isTvDevice = false
        )

        assertTrue(shouldRotate)
    }
}
