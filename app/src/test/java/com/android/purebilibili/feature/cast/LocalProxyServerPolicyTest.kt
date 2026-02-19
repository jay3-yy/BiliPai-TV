package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class LocalProxyServerPolicyTest {

    @Test
    fun `pickBestIpv4Address chooses first non-loopback ipv4`() {
        val addresses = listOf(
            InetAddress.getByName("::1"),
            InetAddress.getByName("127.0.0.1"),
            InetAddress.getByName("192.168.50.23"),
            InetAddress.getByName("10.0.0.8")
        )

        val chosen = LocalProxyServer.pickBestIpv4Address(addresses)

        assertEquals("192.168.50.23", chosen)
    }

    @Test
    fun `isSupportedTargetUrl only allows http and https`() {
        assertTrue(LocalProxyServer.isSupportedTargetUrl("https://example.com/video.m4s"))
        assertTrue(LocalProxyServer.isSupportedTargetUrl("http://example.com/video.mp4"))
        assertFalse(LocalProxyServer.isSupportedTargetUrl("file:///sdcard/video.mp4"))
        assertFalse(LocalProxyServer.isSupportedTargetUrl("javascript:alert(1)"))
    }
}
