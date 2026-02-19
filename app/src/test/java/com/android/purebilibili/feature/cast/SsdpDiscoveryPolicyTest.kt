package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpDiscoveryPolicyTest {

    @Test
    fun `search payloads include renderer and avtransport targets`() {
        val payloads = SsdpDiscovery.resolveSsdpSearchPayloads()

        assertEquals(3, payloads.size)
        assertTrue(payloads.any { it.contains("ST: ssdp:all") })
        assertTrue(payloads.any { it.contains("ST: urn:schemas-upnp-org:device:MediaRenderer:1") })
        assertTrue(payloads.any { it.contains("ST: urn:schemas-upnp-org:service:AVTransport:1") })
    }
}
