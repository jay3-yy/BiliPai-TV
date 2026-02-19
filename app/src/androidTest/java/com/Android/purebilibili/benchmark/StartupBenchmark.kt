package com.android.purebilibili.benchmark

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.purebilibili.feature.settings.RELEASE_DISCLAIMER_ACK_KEY
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    private val packageName = "com.android.purebilibili"

    private fun runShell(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return FileInputStream(pfd.fileDescriptor).bufferedReader().use { it.readText() }
    }

    private fun prepareHomeEntry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("app_welcome", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("first_launch_shown", true)
            .putBoolean(RELEASE_DISCLAIMER_ACK_KEY, true)
            .commit()
    }

    @Test
    fun startupRoute_recordsTotalAndWaitTime() {
        prepareHomeEntry()
        runShell("input keyevent 3")

        val output = runShell("am start -W -n $packageName/.MainActivity")
        val totalTime = Regex("TotalTime:\\s*(\\d+)").find(output)?.groupValues?.get(1)?.toLongOrNull() ?: -1L
        val waitTime = Regex("WaitTime:\\s*(\\d+)").find(output)?.groupValues?.get(1)?.toLongOrNull() ?: -1L

        assertTrue("TotalTime must be > 0. output=$output", totalTime > 0)
        assertTrue("WaitTime must be > 0. output=$output", waitTime > 0)
    }
}
