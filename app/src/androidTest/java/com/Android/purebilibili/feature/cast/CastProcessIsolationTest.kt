package com.android.purebilibili.feature.cast

import android.content.ComponentName
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.purebilibili.feature.cast.CastBridgeService
import com.android.purebilibili.feature.video.player.PlaybackService
import kotlinx.coroutines.runBlocking
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CastProcessIsolationTest {

    @Test
    fun dlnaService_canBindUnbindAndReconnect() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        DlnaManager.unbindService(context)
        DlnaManager.bindService(context)

        assertTrue("DlnaManager should connect after bind", waitUntil(8_000) {
            DlnaManager.isConnected.value
        })

        DlnaManager.unbindService(context)
        assertTrue("DlnaManager should disconnect after unbind", waitUntil(3_000) {
            !DlnaManager.isConnected.value
        })

        DlnaManager.bindService(context)
        assertTrue("DlnaManager should reconnect after second bind", waitUntil(8_000) {
            DlnaManager.isConnected.value
        })

        DlnaManager.unbindService(context)
    }

    @Test
    fun playbackService_staysInMainProcess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = getServiceInfo(context, ComponentName(context, PlaybackService::class.java))
        assertEquals(
            "PlaybackService must stay in main process",
            context.packageName,
            info.processName
        )
    }

    @Test
    fun castServices_runInCastProcess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedCastProcess = "${context.packageName}:cast"
        val bridgeInfo = getServiceInfo(context, ComponentName(context, CastBridgeService::class.java))
        val clingInfo = getServiceInfo(context, ComponentName(context, AndroidUpnpServiceImpl::class.java))
        assertEquals("CastBridgeService must run in :cast process", expectedCastProcess, bridgeInfo.processName)
        assertEquals("AndroidUpnpServiceImpl must run in :cast process", expectedCastProcess, clingInfo.processName)
    }

    @Test
    fun bindCastService_only() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DlnaManager.unbindService(context)
        DlnaManager.bindService(context)
        assertTrue("DlnaManager should connect after bind", waitUntil(8_000) {
            DlnaManager.isConnected.value
        })
    }

    @Test
    fun bindCastService_holdForMemSnapshot() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DlnaManager.unbindService(context)
        DlnaManager.bindService(context)
        assertTrue("DlnaManager should connect after bind", waitUntil(8_000) {
            DlnaManager.isConnected.value
        })
        Thread.sleep(7_000)
        DlnaManager.unbindService(context)
    }

    @Test
    fun unbindCastService_only() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DlnaManager.unbindService(context)
        assertTrue("DlnaManager should disconnect after unbind", waitUntil(3_000) {
            !DlnaManager.isConnected.value
        })
    }

    private fun waitUntil(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (predicate()) return true
            Thread.sleep(100)
        }
        return predicate()
    }

    private fun getServiceInfo(context: Context, component: ComponentName): ServiceInfo {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getServiceInfo(component, android.content.pm.PackageManager.ComponentInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getServiceInfo(component, 0)
        }
    }
}
