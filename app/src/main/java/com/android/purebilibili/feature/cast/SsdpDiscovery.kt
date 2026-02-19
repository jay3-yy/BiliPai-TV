package com.android.purebilibili.feature.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

/**
 * 手动 SSDP 发现实现
 * 用于在 Cling 库不工作时作为备用方案
 */
object SsdpDiscovery {
    private const val TAG = "SsdpDiscovery"
    
    // SSDP 多播地址和端口
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    
    // M-SEARCH 请求 - 搜索所有设备
    private val M_SEARCH_ALL = """
        M-SEARCH * HTTP/1.1
        HOST: 239.255.255.250:1900
        MAN: "ssdp:discover"
        MX: 3
        ST: ssdp:all
        
    """.trimIndent().replace("\n", "\r\n")
    
    // M-SEARCH 请求 - 仅搜索 MediaRenderer
    private val M_SEARCH_RENDERER = """
        M-SEARCH * HTTP/1.1
        HOST: 239.255.255.250:1900
        MAN: "ssdp:discover"
        MX: 3
        ST: urn:schemas-upnp-org:device:MediaRenderer:1
        
    """.trimIndent().replace("\n", "\r\n")

    private val M_SEARCH_AV_TRANSPORT = """
        M-SEARCH * HTTP/1.1
        HOST: 239.255.255.250:1900
        MAN: "ssdp:discover"
        MX: 3
        ST: urn:schemas-upnp-org:service:AVTransport:1
        
    """.trimIndent().replace("\n", "\r\n")
    
    data class SsdpDevice(
        val location: String,
        val server: String,
        val usn: String,
        val st: String
    )
    
    /**
     * 执行 SSDP 发现
     * @param timeoutMs 超时时间（毫秒）
     * @return 发现的设备列表
     */
    suspend fun discover(context: Context, timeoutMs: Int = 5000): List<SsdpDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SsdpDevice>()
        var socket: MulticastSocket? = null
        
        try {
            Logger.i(TAG, "📺 [DLNA] Starting SSDP discovery (timeout: ${timeoutMs}ms)")
            
            // 创建 UDP socket
            socket = MulticastSocket(null)
            socket.reuseAddress = true
            socket.broadcast = true
            socket.bind(InetSocketAddress(0))
            socket.timeToLive = 4
            bindSocketToLocalNetworkInterface(context, socket)

            Logger.d(TAG, "📺 [DLNA] Socket bound to local port ${socket.localPort}")
            
            // 发送 M-SEARCH 请求
            val multicastAddress = InetAddress.getByName(SSDP_ADDRESS)
            val payloads = resolveSsdpSearchPayloads()
            val retryCount = 2
            repeat(retryCount) { round ->
                payloads.forEach { payload ->
                    val data = payload.toByteArray()
                    val packet = DatagramPacket(data, data.size, multicastAddress, SSDP_PORT)
                    socket.send(packet)
                }
                if (round < retryCount - 1) {
                    delay(250)
                }
            }
            Logger.i(TAG, "📺 [DLNA] M-SEARCH sent (${payloads.size} targets x $retryCount rounds)")
            
            // 接收响应
            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()
            val seenUsns = mutableSetOf<String>()
            var responseCount = 0
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val elapsed = System.currentTimeMillis() - startTime
                    val remaining = (timeoutMs - elapsed).toInt().coerceAtLeast(1)
                    socket.soTimeout = remaining.coerceAtMost(1200)
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    responseCount++
                    
                    val response = String(responsePacket.data, 0, responsePacket.length)
                    
                    // 解析响应
                    val device = parseResponse(response)
                    if (device != null && device.usn !in seenUsns) {
                        seenUsns.add(device.usn)
                        devices.add(device)
                        // 隐私安全日志：只显示设备类型和服务器信息，不显示完整 URL 和 IP
                        Logger.i(TAG, "📺 [DLNA] Found device: server=${device.server.take(50)}, type=${device.st.substringAfterLast(":")}")
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // 分段超时，继续直到总超时
                }
            }
            
            val elapsed = System.currentTimeMillis() - startTime
            Logger.i(TAG, "📺 [DLNA] Discovery completed in ${elapsed}ms: received $responseCount responses, found ${devices.size} unique devices")
            
        } catch (e: Exception) {
            Logger.e(TAG, "📺 [DLNA] Discovery error: ${e.javaClass.simpleName} - ${e.message}")
        } finally {
            socket?.close()
        }
        
        devices
    }

    internal fun resolveSsdpSearchPayloads(): List<String> = listOf(
        M_SEARCH_ALL,
        M_SEARCH_RENDERER,
        M_SEARCH_AV_TRANSPORT
    )

    private fun bindSocketToLocalNetworkInterface(context: Context, socket: MulticastSocket) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                Logger.w(TAG, "📺 [DLNA] No active network while binding SSDP socket")
                return
            }

            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            val isLocalNetwork =
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
            if (!isLocalNetwork) {
                Logger.w(TAG, "📺 [DLNA] Active network is not WiFi/Ethernet; discovery may fail")
            }

            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            val ipv4Address = linkProperties
                ?.linkAddresses
                ?.map { it.address }
                ?.firstOrNull { address -> address is Inet4Address && !address.isLoopbackAddress }
                as? Inet4Address

            if (ipv4Address == null) {
                Logger.w(TAG, "📺 [DLNA] No IPv4 address found on active network")
                return
            }

            val networkInterface = NetworkInterface.getByInetAddress(ipv4Address)
            if (networkInterface == null) {
                Logger.w(TAG, "📺 [DLNA] No network interface for IPv4 address ${ipv4Address.hostAddress}")
                return
            }

            socket.networkInterface = networkInterface
            Logger.i(
                TAG,
                "📺 [DLNA] SSDP socket bound to interface=${networkInterface.displayName}, ip=${ipv4Address.hostAddress}"
            )
        } catch (e: Exception) {
            Logger.w(TAG, "📺 [DLNA] Failed to bind SSDP socket to local interface: ${e.message}")
        }
    }
    
    private fun parseResponse(response: String): SsdpDevice? {
        val lines = response.split("\r\n", "\n")
        var location = ""
        var server = ""
        var usn = ""
        var st = ""
        
        for (line in lines) {
            when {
                line.startsWith("LOCATION:", ignoreCase = true) -> {
                    location = line.substringAfter(":").trim()
                }
                line.startsWith("SERVER:", ignoreCase = true) -> {
                    server = line.substringAfter(":").trim()
                }
                line.startsWith("USN:", ignoreCase = true) -> {
                    usn = line.substringAfter(":").trim()
                }
                line.startsWith("ST:", ignoreCase = true) -> {
                    st = line.substringAfter(":").trim()
                }
            }
        }
        
        return if (location.isNotEmpty() && usn.isNotEmpty()) {
            SsdpDevice(location, server, usn, st)
        } else {
            null
        }
    }
}
