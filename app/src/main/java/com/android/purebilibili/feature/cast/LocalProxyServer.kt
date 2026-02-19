package com.android.purebilibili.feature.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.android.purebilibili.core.util.Logger
import fi.iki.elonen.NanoHTTPD
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URLEncoder

/**
 * 运行在手机上的轻量级 HTTP 代理服务器。
 * 作用：拦截 DLNA 设备的播放请求，转发给 Bilibili 服务器并修改请求头，从而绕过防盗链 (403 Forbidden)。
 *
 * 原理：
 * 1. 电视/DLNA 设备请求: http://<手机IP>:<端口>/proxy?url=<编码后的B站视频URL>
 * 2. 代理服务器解析 `url` 参数。
 * 3. 代理服务器伪装成合法客户端（添加 User-Agent, Referer）向 B站请求数据。
 * 4. 代理服务器将 B 站返回的数据流（InputStream）直接流式传输给电视。
 */
class LocalProxyServer(port: Int = 8901) : NanoHTTPD(port) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true) // 虽然电视可能只发起 HTTP 请求，但我们需要从 B 站获取 HTTPS 数据
        .build()

    override fun serve(session: IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        // 仅处理 /proxy 路径的请求
        if (uri != "/proxy") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }

        val params = session.parms
        val targetUrl = params["url"]
        
        // 基础校验：必须包含目标 URL
        if (targetUrl.isNullOrEmpty()) {
             return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing 'url' parameter")
        }
        if (!isSupportedTargetUrl(targetUrl)) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Unsupported target URL")
        }
        val parsedTargetUrl = targetUrl.toHttpUrlOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid target URL")
        
        Logger.d("LocalProxyServer", "📺 [Proxy] 正在代理请求: $targetUrl")

        try {
            // 构建发往 Bilibili 的请求
            // 关键点：设置 Referer 和 User-Agent 以绕过 B 站的防盗链检查
            val referer = params["referer"] ?: "https://www.bilibili.com"
            val userAgent = params["ua"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

            val request = Request.Builder()
                .url(parsedTargetUrl)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
            session.headers["range"]?.takeIf { it.isNotBlank() }?.let { rangeHeader ->
                request.header("Range", rangeHeader)
            }
            val upstreamRequest = request.build()

            val upstreamResponse = client.newCall(upstreamRequest).execute()
            
            if (!upstreamResponse.isSuccessful) {
                val body = upstreamResponse.body?.string().orEmpty()
                upstreamResponse.close()
                return newFixedLengthResponse(
                    mapToNanoStatus(upstreamResponse.code),
                    MIME_PLAINTEXT,
                    "Upstream Error: ${upstreamResponse.code} ${body.take(120)}"
                )
            }

            // 获取 B 站返回的视频流和元数据
            val body = upstreamResponse.body
            if (body == null) {
                upstreamResponse.close()
                return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "")
            }
            val inputStream = UpstreamRelayInputStream(upstreamResponse, body.byteStream())
            val contentType = upstreamResponse.header("Content-Type") ?: "video/mp4"
            val contentLength = body.contentLength()

            // 构造返回给电视的响应
            // 使用 ChunkedResponse 以支持流式传输，避免将整个视频加载到内存中
            val nanoResponse = newChunkedResponse(mapToNanoStatus(upstreamResponse.code), contentType, inputStream)
            
            // 转发关键响应头 (如 Content-Length)，这对播放器的进度条显示和拖动至关重要
            if (contentLength != -1L) {
                 nanoResponse.addHeader("Content-Length", contentLength.toString())
            }
            upstreamResponse.header("Content-Range")?.let { nanoResponse.addHeader("Content-Range", it) }
            upstreamResponse.header("Accept-Ranges")?.let { nanoResponse.addHeader("Accept-Ranges", it) }
            
            return nanoResponse

        } catch (e: Exception) {
            Logger.e("LocalProxyServer", "📺 [Proxy] 代理请求处理失败", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    companion object {
        const val PORT = 8901
        
        /**
         * 生成代理 URL供 DLNA 设备使用
         * @param context 用于获取 Wi-Fi IP 地址
         * @param targetUrl 实际的 B 站视频 URL
         * @return 代理服务器的完整 URL
         */
        fun getProxyUrl(context: Context, targetUrl: String): String {
            val ipAddress = resolveLocalIpv4Address(context)
            
            // 对目标 URL 进行编码，作为参数传递
            val encodedUrl = URLEncoder.encode(targetUrl, "UTF-8")
            
            return "http://$ipAddress:$PORT/proxy?url=$encodedUrl"
        }

        internal fun pickBestIpv4Address(addresses: List<InetAddress>): String? {
            return addresses.asSequence()
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        }

        internal fun isSupportedTargetUrl(url: String): Boolean {
            val scheme = url.toHttpUrlOrNull()?.scheme ?: return false
            return scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)
        }

        private fun resolveLocalIpv4Address(context: Context): String {
            val connectivityManager =
                context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val linkAddresses = connectivityManager.getLinkProperties(activeNetwork)
                ?.linkAddresses
                ?.map { it.address }
                .orEmpty()
            pickBestIpv4Address(linkAddresses)?.let { return it }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val fallbackIp = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            if (!fallbackIp.isNullOrBlank() && fallbackIp != "0.0.0.0") {
                return fallbackIp
            }
            return "127.0.0.1"
        }

        private fun mapToNanoStatus(code: Int): Response.Status {
            return when (code) {
                200 -> Response.Status.OK
                206 -> Response.Status.PARTIAL_CONTENT
                400 -> Response.Status.BAD_REQUEST
                401 -> Response.Status.UNAUTHORIZED
                403 -> Response.Status.FORBIDDEN
                404 -> Response.Status.NOT_FOUND
                else -> Response.Status.INTERNAL_ERROR
            }
        }
    }

    private class UpstreamRelayInputStream(
        private val upstreamResponse: okhttp3.Response,
        private val delegate: InputStream
    ) : InputStream() {
        override fun read(): Int = delegate.read()

        override fun read(b: ByteArray): Int = delegate.read(b)

        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)

        override fun close() {
            try {
                delegate.close()
            } finally {
                upstreamResponse.close()
            }
        }
    }
}
