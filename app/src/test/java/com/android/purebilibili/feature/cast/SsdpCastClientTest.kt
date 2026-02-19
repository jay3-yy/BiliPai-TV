package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpCastClientTest {

    @Test
    fun `parseAvTransportEndpoint resolves relative control URL`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <device>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                    <controlURL>/MediaRenderer/AVTransport/Control</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()

        val endpoint = SsdpCastClient.parseAvTransportEndpoint(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )

        assertNotNull(endpoint)
        assertEquals(
            "http://192.168.31.8:8899/MediaRenderer/AVTransport/Control",
            endpoint?.controlUrl
        )
        assertEquals("urn:schemas-upnp-org:service:AVTransport:1", endpoint?.serviceType)
    }

    @Test
    fun `parseAvTransportEndpoint returns null when AVTransport not found`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <root>
              <device>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>
                    <controlURL>/MediaRenderer/RenderingControl/Control</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()

        val endpoint = SsdpCastClient.parseAvTransportEndpoint(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )

        assertNull(endpoint)
    }

    @Test
    fun `buildSetUriActionBody escapes xml-sensitive characters`() {
        val body = SsdpCastClient.buildSetUriActionBody(
            serviceType = "urn:schemas-upnp-org:service:AVTransport:1",
            mediaUrl = "http://127.0.0.1:8901/proxy?url=a&b=c",
            metadata = "<tag attr=\"1\">value</tag>"
        )

        assertTrue(body.contains("&amp;"))
        assertTrue(body.contains("&lt;tag attr=&quot;1&quot;&gt;value&lt;/tag&gt;"))
    }
}
