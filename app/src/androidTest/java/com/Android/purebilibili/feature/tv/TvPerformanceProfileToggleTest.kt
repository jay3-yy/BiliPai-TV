package com.android.purebilibili.feature.tv

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.feature.settings.RELEASE_DISCLAIMER_ACK_KEY
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvPerformanceProfileToggleTest {

    private fun prepareHomeEntry(context: Context) {
        val prefs = context.getSharedPreferences("app_welcome", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("first_launch_shown", true)
            .putBoolean(RELEASE_DISCLAIMER_ACK_KEY, true)
            .commit()
    }

    @Test
    fun prepareTvPerfScenario() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prepareHomeEntry(context)
        val prefs = context.getSharedPreferences("app_welcome", Context.MODE_PRIVATE)
        assertTrue(prefs.getBoolean("first_launch_shown", false))
        assertTrue(prefs.getBoolean(RELEASE_DISCLAIMER_ACK_KEY, false))
    }

    @Test
    fun enableTvPerformanceProfile() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prepareHomeEntry(context)
        SettingsManager.setTvPerformanceProfileEnabled(context, true)
        assertEquals(true, SettingsManager.getTvPerformanceProfileEnabledSync(context))
    }

    @Test
    fun disableTvPerformanceProfile() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prepareHomeEntry(context)
        SettingsManager.setTvPerformanceProfileEnabled(context, false)
        assertEquals(false, SettingsManager.getTvPerformanceProfileEnabledSync(context))
    }
}
