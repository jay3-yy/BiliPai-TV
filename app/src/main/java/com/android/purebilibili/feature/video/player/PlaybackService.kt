package com.android.purebilibili.feature.video.player

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.android.purebilibili.core.util.Logger

/**
 * 这是一个 Foreground Service，用于将 MiniPlayerManager 构建的通知提升为前台通知。
 * 这样可以确保在后台播放时，系统媒体控制中心（下拉通知栏）能够正常显示。
 */
class PlaybackService : Service() {

    companion object {
        private const val TAG = "PlaybackService"
        const val ACTION_START_FOREGROUND = "com.android.purebilibili.action.START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.android.purebilibili.action.STOP_FOREGROUND"
        const val NOTIFICATION_ID = 1002 // 必须与 MiniPlayerManager 中的 ID 一致
        @Volatile private var isForegroundStarted = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Logger.d(TAG, "onStartCommand: action=$action")

        when (action) {
            ACTION_START_FOREGROUND -> {
                if (isForegroundStarted) {
                    return START_STICKY
                }
                try {
                    // 获取 MiniPlayerManager 中构建好的通知
                    val notification = MiniPlayerManager.getInstance(applicationContext).currentNotification
                    if (notification != null) {
                        Logger.d(TAG, "Starting foreground service with notification")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ServiceCompat.startForeground(
                                this,
                                NOTIFICATION_ID,
                                notification,
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            )
                        } else {
                            startForeground(NOTIFICATION_ID, notification)
                        }
                        isForegroundStarted = true
                    } else {
                        Logger.w(TAG, "Notification is null, cannot start foreground")
                        isForegroundStarted = false
                        stopSelf()
                    }
                } catch (e: Exception) {
                    isForegroundStarted = false
                    Logger.e(TAG, "Failed to start foreground service", e)
                    stopSelf()
                }
            }
            ACTION_STOP_FOREGROUND -> {
                Logger.d(TAG, "Stopping foreground service")
                isForegroundStarted = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isForegroundStarted = false
        Logger.d(TAG, "onDestroy")
    }
}
