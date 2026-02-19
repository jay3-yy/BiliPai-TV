package com.android.purebilibili.feature.cast

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.purebilibili.core.util.Logger
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.avtransport.callback.Play
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI
import org.fourthline.cling.support.avtransport.callback.Stop
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.ProtocolInfo
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.item.VideoItem
import java.util.concurrent.ConcurrentHashMap

/**
 * Cross-process bridge around Cling so Main process doesn't depend on local Binder casts.
 */
class CastBridgeService : Service() {

    companion object {
        private const val TAG = "CastBridgeService"
    }

    private var upnpService: AndroidUpnpService? = null
    private var clingBound = false
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null

    private val deviceMap = ConcurrentHashMap<String, Device<*, *, *>>()
    @Volatile
    private var deviceCache: List<CastDeviceInfo> = emptyList()

    private val registryListener = object : DefaultRegistryListener() {
        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            refreshDevices()
        }

        override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
            refreshDevices()
        }

        override fun localDeviceAdded(registry: Registry, device: LocalDevice) {
            refreshDevices()
        }

        override fun localDeviceRemoved(registry: Registry, device: LocalDevice) {
            refreshDevices()
        }
    }

    private val clingConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val localService = service as? AndroidUpnpService
            if (localService == null) {
                Logger.e(TAG, "Unexpected binder type from Cling service: ${service::class.java.name}")
                return
            }

            upnpService = localService
            upnpService?.registry?.removeAllRemoteDevices()
            upnpService?.registry?.addListener(registryListener)
            upnpService?.controlPoint?.search()
            refreshDevices()
            Logger.i(TAG, "Cling bridge connected")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            upnpService = null
            clearDevices()
            Logger.w(TAG, "Cling bridge disconnected")
        }
    }

    private val binder = object : ICastBridgeService.Stub() {
        override fun connect() {
            connectInternal()
        }

        override fun disconnect() {
            disconnectInternal()
        }

        override fun isConnected(): Boolean {
            return clingBound && upnpService != null
        }

        override fun refresh() {
            upnpService?.registry?.removeAllRemoteDevices()
            upnpService?.controlPoint?.search()
            refreshDevices()
        }

        override fun getDevices(): MutableList<CastDeviceInfo> {
            return deviceCache.toMutableList()
        }

        override fun cast(udn: String, url: String, title: String, creator: String): Boolean {
            val target = deviceMap[udn] ?: return false
            val service = target.findService(org.fourthline.cling.model.types.UDAServiceId("AVTransport"))
                ?: return false
            val controlPoint = upnpService?.controlPoint ?: return false
            val metadata = createMetadata(url, title, creator)

            controlPoint.execute(object : SetAVTransportURI(service, url, metadata) {
                override fun success(invocation: org.fourthline.cling.model.action.ActionInvocation<*>) {
                    controlPoint.execute(object : Play(service) {
                        override fun success(invocation: org.fourthline.cling.model.action.ActionInvocation<*>) {
                            Logger.d(TAG, "Cast play command success: $udn")
                        }

                        override fun failure(
                            invocation: org.fourthline.cling.model.action.ActionInvocation<*>,
                            operation: org.fourthline.cling.model.message.UpnpResponse,
                            defaultMsg: String
                        ) {
                            Logger.e(TAG, "Cast play command failed: $defaultMsg")
                        }
                    })
                }

                override fun failure(
                    invocation: org.fourthline.cling.model.action.ActionInvocation<*>,
                    operation: org.fourthline.cling.model.message.UpnpResponse,
                    defaultMsg: String
                ) {
                    Logger.e(TAG, "Cast set uri failed: $defaultMsg")
                }
            })
            return true
        }

        override fun stop(udn: String): Boolean {
            val target = deviceMap[udn] ?: return false
            val service = target.findService(org.fourthline.cling.model.types.UDAServiceId("AVTransport"))
                ?: return false
            val controlPoint = upnpService?.controlPoint ?: return false
            controlPoint.execute(object : Stop(service) {
                override fun success(invocation: org.fourthline.cling.model.action.ActionInvocation<*>) {
                    Logger.d(TAG, "Cast stop command success: $udn")
                }

                override fun failure(
                    invocation: org.fourthline.cling.model.action.ActionInvocation<*>,
                    operation: org.fourthline.cling.model.message.UpnpResponse,
                    defaultMsg: String
                ) {
                    Logger.e(TAG, "Cast stop command failed: $defaultMsg")
                }
            })
            return true
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        disconnectInternal()
        super.onDestroy()
    }

    private fun connectInternal() {
        if (clingBound) return

        acquireMulticastLock()
        val intent = Intent(this, AndroidUpnpServiceImpl::class.java)
        clingBound = bindService(intent, clingConnection, Context.BIND_AUTO_CREATE)
        if (!clingBound) {
            Logger.e(TAG, "Failed to bind AndroidUpnpServiceImpl from bridge")
            releaseMulticastLock()
        }
    }

    private fun disconnectInternal() {
        upnpService?.registry?.removeListener(registryListener)
        if (clingBound) {
            try {
                unbindService(clingConnection)
            } catch (e: IllegalArgumentException) {
                Logger.w(TAG, "unbind ignored: ${e.message}")
            }
        }
        clingBound = false
        upnpService = null
        clearDevices()
        releaseMulticastLock()
    }

    private fun refreshDevices() {
        val allDevices = upnpService?.registry?.devices?.toList().orEmpty()
        val renderers = allDevices.filter { device ->
            val isMediaRenderer = device.type?.type?.contains("MediaRenderer", ignoreCase = true) == true
            val hasAVTransport = device.findService(org.fourthline.cling.model.types.UDAServiceId("AVTransport")) != null
            isMediaRenderer || hasAVTransport
        }

        val nextMap = LinkedHashMap<String, Device<*, *, *>>()
        val nextCache = mutableListOf<CastDeviceInfo>()
        renderers.forEach { device ->
            val udn = device.identity?.udn?.identifierString.orEmpty()
            if (udn.isBlank()) return@forEach
            nextMap[udn] = device
            nextCache += CastDeviceInfo(
                udn = udn,
                name = device.details?.friendlyName ?: "Unknown Device",
                description = device.displayString.ifEmpty { device.type?.type ?: "Unknown" },
                location = device.details?.presentationURI?.toString()
            )
        }

        deviceMap.clear()
        deviceMap.putAll(nextMap)
        deviceCache = nextCache
        Logger.i(TAG, "Bridge refresh complete: ${nextCache.size} renderer(s)")
    }

    private fun clearDevices() {
        deviceMap.clear()
        deviceCache = emptyList()
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            multicastLock = wifiManager.createMulticastLock("CastBridgeService")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to acquire multicast lock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to release multicast lock", e)
        }
        multicastLock = null
    }

    private fun createMetadata(url: String, title: String, creator: String): String {
        return try {
            val didl = DIDLContent()
            val res = Res(ProtocolInfo("http-get:*:video/mp4:*"), null, url)
            val item = VideoItem("1", "0", title, creator, res)
            didl.addItem(item)
            org.fourthline.cling.support.contentdirectory.DIDLParser().generate(didl)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to build cast metadata", e)
            ""
        }
    }
}
