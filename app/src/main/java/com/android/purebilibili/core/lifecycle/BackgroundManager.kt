// 文件路径: core/lifecycle/BackgroundManager.kt
package com.android.purebilibili.core.lifecycle

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.purebilibili.core.util.Logger

private const val TAG = "BackgroundManager"

/**
 * 📱 应用级后台状态管理器
 * 
 * 使用 ProcessLifecycleOwner 统一检测应用前后台状态，
 * 当应用进入后台时触发资源优化，返回前台时恢复。
 */
object BackgroundManager : DefaultLifecycleObserver {
    
    // 当前是否在后台
    @Volatile
    var isInBackground: Boolean = false
        private set
    
    // 后台状态变化监听器
    private val listeners = mutableListOf<BackgroundStateListener>()
    
    /**
     * 初始化 - 在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Logger.d(TAG, "✅ BackgroundManager initialized")
    }
    
    /**
     * 注册后台状态监听器
     */
    fun addListener(listener: BackgroundStateListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }
    
    /**
     * 移除后台状态监听器
     */
    fun removeListener(listener: BackgroundStateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    // ========== Lifecycle Callbacks ==========
    
    override fun onStart(owner: LifecycleOwner) {
        // 应用返回前台
        isInBackground = false
        Logger.d(TAG, "🌅 App entered FOREGROUND")
        notifyListeners(false)
    }
    
    override fun onStop(owner: LifecycleOwner) {
        // 应用进入后台
        isInBackground = true
        Logger.d(TAG, "🌙 App entered BACKGROUND")
        notifyListeners(true)
    }
    
    private fun notifyListeners(inBackground: Boolean) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                try {
                    if (inBackground) {
                        listener.onEnterBackground()
                    } else {
                        listener.onEnterForeground()
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Listener callback error", e)
                }
            }
        }
    }
    
    /**
     * 后台状态变化监听器接口
     */
    interface BackgroundStateListener {
        fun onEnterBackground()
        fun onEnterForeground()
    }
}
