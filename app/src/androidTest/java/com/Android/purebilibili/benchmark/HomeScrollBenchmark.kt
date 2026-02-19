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
class HomeScrollBenchmark {

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
    fun homeDpadScenario_recordsFrameAndPss() {
        prepareHomeEntry()
        runShell("input keyevent 3")
        runShell("am start -W -n $packageName/.MainActivity")
        Thread.sleep(10_000)

        runShell("dumpsys gfxinfo $packageName reset")

        repeat(20) {
            runShell("input keyevent 20")
            Thread.sleep(100)
            runShell("input keyevent 22")
            Thread.sleep(100)
            runShell("input keyevent 22")
            Thread.sleep(100)
            runShell("input keyevent 23")
            Thread.sleep(100)
            runShell("input keyevent 21")
            Thread.sleep(100)
            runShell("input keyevent 21")
            Thread.sleep(100)
            runShell("input keyevent 19")
            Thread.sleep(100)
            runShell("input keyevent 23")
            Thread.sleep(100)
        }

        Thread.sleep(2_000)

        val gfx = runShell("dumpsys gfxinfo $packageName")
        val mem = runShell("dumpsys meminfo $packageName")

        val totalFrames = Regex("Total frames rendered:\\s*(\\d+)").find(gfx)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val jankPercent = Regex("Janky frames:\\s*\\d+\\s*\\(([0-9.]+)%\\)").find(gfx)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
        val pssKb = Regex("TOTAL PSS:\\s*([0-9,]+)").find(mem)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0

        assertTrue("Total frames should be >= 300 for valid scenario. frames=$totalFrames", totalFrames >= 300)
        assertTrue("Jank percent should be parsable. jank=$jankPercent", jankPercent >= 0f)
        assertTrue("TOTAL PSS should be parsable. pss=$pssKb", pssKb > 0)
    }
}
