package com.android.purebilibili.feature.cast

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main-process DLNA manager backed by cross-process cast bridge service.
 */
object DlnaManager {
    private const val TAG = "DlnaManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var bridgeService: ICastBridgeService? = null
    private var isServiceBound = false
    private var pollingJob: Job? = null

    private val _devices = MutableStateFlow<List<CastDeviceInfo>>(emptyList())
    val devices: StateFlow<List<CastDeviceInfo>> = _devices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            bridgeService = ICastBridgeService.Stub.asInterface(service)
            isServiceBound = true

            runCatching { bridgeService?.connect() }
                .onFailure { Logger.e(TAG, "Failed to connect cast bridge", it) }

            _isConnected.value = true
            startPolling()
            refresh()
            Logger.i(TAG, "Cast bridge bound")
        }

        override fun onServiceDisconnected(className: ComponentName) {
            bridgeService = null
            isServiceBound = false
            stopPolling()
            _isConnected.value = false
            _devices.value = emptyList()
            Logger.w(TAG, "Cast bridge disconnected")
        }
    }

    fun bindService(context: Context) {
        if (isServiceBound) {
            Logger.d(TAG, "bindService ignored: already bound")
            return
        }
        val intent = Intent(context, CastBridgeService::class.java)
        isServiceBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!isServiceBound) {
            _isConnected.value = false
            Logger.e(TAG, "Failed to bind CastBridgeService")
        }
    }

    fun unbindService(context: Context) {
        stopPolling()
        runCatching { bridgeService?.disconnect() }
            .onFailure { Logger.w(TAG, "Bridge disconnect ignored: ${it.message}") }

        if (isServiceBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Logger.w(TAG, "unbind ignored: ${e.message}")
            }
        }

        bridgeService = null
        isServiceBound = false
        _isConnected.value = false
        _devices.value = emptyList()
    }

    fun refresh() {
        runCatching { bridgeService?.refresh() }
            .onFailure { Logger.e(TAG, "refresh failed", it) }
        scope.launch {
            syncStateFromBridge()
        }
    }

    fun cast(device: CastDeviceInfo, url: String, title: String, creator: String) {
        val dispatched = runCatching {
            bridgeService?.cast(device.udn, url, title, creator) == true
        }.getOrDefault(false)

        if (!dispatched) {
            Logger.e(TAG, "Cast dispatch failed for ${device.name}")
        }
    }

    fun stop(device: CastDeviceInfo) {
        val dispatched = runCatching {
            bridgeService?.stop(device.udn) == true
        }.getOrDefault(false)

        if (!dispatched) {
            Logger.e(TAG, "Stop dispatch failed for ${device.name}")
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isServiceBound) {
                syncStateFromBridge()
                delay(1200)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun syncStateFromBridge() {
        val service = bridgeService ?: return
        val snapshot = withContext(Dispatchers.IO) {
            val connected = runCatching { service.isConnected() }.getOrDefault(false)
            val devices = runCatching { service.devices }.getOrElse { emptyList() }
            connected to devices
        }
        _isConnected.value = snapshot.first
        _devices.value = snapshot.second
    }
}
