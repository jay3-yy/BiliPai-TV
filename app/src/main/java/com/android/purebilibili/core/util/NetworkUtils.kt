// 文件路径: core/util/NetworkUtils.kt
package com.android.purebilibili.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * 网络工具类
 * 
 * 用于检测网络类型，实现网络感知的清晰度默认值
 */
object NetworkUtils {
    
    /**
     * 检查当前是否使用 WiFi 网络
     */
    fun isWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * 检查当前是否使用移动数据
     */
    fun isMobileData(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
    
    /**
     * 获取网络感知的默认清晰度 ID
     * 
     * Bilibili 清晰度 ID:
     * - 116: 1080P60
     * - 80: 1080P
     * - 64: 720P
     * - 32: 480P
     * - 16: 360P
     * 
     * @return 根据用户设置和网络类型返回对应清晰度
     */
    fun getDefaultQualityId(context: Context): Int {
        val prefs = context.getSharedPreferences("quality_settings", Context.MODE_PRIVATE)
        
        // 🚀 [新增] 自动最高画质：忽略网络类型，始终返回最高画质
        val autoHighest = prefs.getBoolean("auto_highest_quality", false)
        if (autoHighest) {
            Logger.d("NetworkUtils", "🚀 自动最高画质已开启，使用最高画质 127 (8K)")
            return 127  // 8K (实际会降级到视频可用的最高画质)
        }
        
        val isOnWifi = isWifi(context)
        val quality = if (isOnWifi) {
            prefs.getInt("wifi_quality", 80)  // 默认 WiFi=1080P
        } else {
            prefs.getInt("mobile_quality", 64)  // 默认流量=720P
        }
        Logger.d("NetworkUtils", " 获取默认画质: isWifi=$isOnWifi, quality=$quality")
        return quality
    }
    
    /**
     * 获取网络类型描述
     */
    fun getNetworkTypeLabel(context: Context): String {
        return when {
            isWifi(context) -> "WiFi"
            isMobileData(context) -> "移动数据"
            else -> "未连接"
        }
    }
}
