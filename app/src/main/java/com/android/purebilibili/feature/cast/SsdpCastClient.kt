package com.android.purebilibili.feature.cast

import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

/**
 * SSDP fallback caster.
 * 当设备仅通过 SSDP 被发现而没有进入 Cling registry 时，直接通过 SOAP 调 AVTransport。
 */
object SsdpCastClient {
    private const val TAG = "SsdpCastClient"
    private val soapContentType = "text/xml; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class AvTransportEndpoint(
        val controlUrl: String,
        val serviceType: String
    )

    suspend fun cast(
        device: SsdpDiscovery.SsdpDevice,
        mediaUrl: String,
        title: String,
        creator: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = fetchAvTransportEndpoint(device.location)
                ?: error("设备不支持 AVTransport 控制")
            val metadata = buildDidlMetadata(mediaUrl, title, creator)

            sendSoapAction(
                endpoint = endpoint,
                action = "SetAVTransportURI",
                actionBody = buildSetUriActionBody(endpoint.serviceType, mediaUrl, metadata)
            )
            sendSoapAction(
                endpoint = endpoint,
                action = "Play",
                actionBody = buildPlayActionBody(endpoint.serviceType)
            )
            Logger.i(TAG, "📺 [SSDP] Cast command sent to ${device.server.take(40)}")
        }
    }

    private fun fetchAvTransportEndpoint(descriptionLocation: String): AvTransportEndpoint? {
        val request = Request.Builder()
            .url(descriptionLocation)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Logger.w(TAG, "📺 [SSDP] Fetch device description failed: ${response.code}")
                return null
            }
            val descriptionXml = response.body?.string().orEmpty()
            return parseAvTransportEndpoint(descriptionXml, descriptionLocation)
        }
    }

    internal fun parseAvTransportEndpoint(
        descriptionXml: String,
        descriptionLocation: String
    ): AvTransportEndpoint? {
        if (descriptionXml.isBlank()) return null
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(descriptionXml.byteInputStream())
            val services = document.getElementsByTagNameNS("*", "service")

            for (i in 0 until services.length) {
                val service = services.item(i) as? Element ?: continue
                val serviceType = service.getFirstChildContent("serviceType")
                if (!serviceType.contains("AVTransport", ignoreCase = true)) continue

                val controlUrlRaw = service.getFirstChildContent("controlURL")
                if (controlUrlRaw.isBlank()) continue

                val resolvedControlUrl = URI(descriptionLocation).resolve(controlUrlRaw.trim()).toString()
                return AvTransportEndpoint(
                    controlUrl = resolvedControlUrl,
                    serviceType = serviceType
                )
            }
            null
        }.getOrElse { error ->
            Logger.e(TAG, "📺 [SSDP] Parse description failed: ${error.message}")
            null
        }
    }

    internal fun buildSetUriActionBody(
        serviceType: String,
        mediaUrl: String,
        metadata: String
    ): String {
        val escapedMediaUrl = escapeXml(mediaUrl)
        val escapedMetadata = escapeXml(metadata)
        return """
            <u:SetAVTransportURI xmlns:u="$serviceType">
                <InstanceID>0</InstanceID>
                <CurrentURI>$escapedMediaUrl</CurrentURI>
                <CurrentURIMetaData>$escapedMetadata</CurrentURIMetaData>
            </u:SetAVTransportURI>
        """.trimIndent()
    }

    private fun buildPlayActionBody(serviceType: String): String = """
        <u:Play xmlns:u="$serviceType">
            <InstanceID>0</InstanceID>
            <Speed>1</Speed>
        </u:Play>
    """.trimIndent()

    private fun buildDidlMetadata(url: String, title: String, creator: String): String {
        val escapedUrl = escapeXml(url)
        val escapedTitle = escapeXml(title.ifBlank { "BiliPai Video" })
        val escapedCreator = escapeXml(creator.ifBlank { "BiliPai" })
        return """
            <DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"
                xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/">
                <item id="1" parentID="0" restricted="1">
                    <dc:title>$escapedTitle</dc:title>
                    <upnp:class>object.item.videoItem</upnp:class>
                    <dc:creator>$escapedCreator</dc:creator>
                    <res protocolInfo="http-get:*:video/mp4:*">$escapedUrl</res>
                </item>
            </DIDL-Lite>
        """.trimIndent()
    }

    private fun sendSoapAction(
        endpoint: AvTransportEndpoint,
        action: String,
        actionBody: String
    ) {
        val envelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    $actionBody
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val request = Request.Builder()
            .url(endpoint.controlUrl)
            .header("SOAPACTION", "\"${endpoint.serviceType}#$action\"")
            .post(envelope.toRequestBody(soapContentType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val payload = response.body?.string().orEmpty().take(180)
                error("SOAP $action failed (${response.code}): $payload")
            }
        }
    }

    private fun escapeXml(value: String): String = buildString(value.length + 16) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    private fun Element.getFirstChildContent(tagName: String): String {
        val nodes = getElementsByTagNameNS("*", tagName)
        if (nodes.length == 0) return ""
        return nodes.item(0)?.textContent?.trim().orEmpty()
    }
}
