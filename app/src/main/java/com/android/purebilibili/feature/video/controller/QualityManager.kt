// File: feature/video/controller/QualityManager.kt
package com.android.purebilibili.feature.video.controller

import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo

/**
 * Quality Manager
 * 
 * Handles quality selection and switching logic:
 * - Select best matching quality
 * - Handle quality fallback
 * - Provide quality labels
 * 
 * Requirement Reference: AC1.3 - Quality managed by QualityManager
 */
class QualityManager {
    
    companion object {
        private const val TAG = "QualityManager"
        
        // Quality ID to label mapping
        private val QUALITY_LABELS = mapOf(
            127 to "8K",
            126 to "Dolby Vision",
            125 to "HDR",
            120 to "4K",
            116 to "1080P60",
            112 to "1080P+",
            80 to "1080P",
            74 to "720P60",
            64 to "720P",
            32 to "480P",
            16 to "360P"
        )
        
        // Quality fallback chain: high to low
        private val QUALITY_CHAIN = listOf(127, 126, 125, 120, 116, 112, 80, 74, 64, 32, 16)
    }
    
    /**
     * Find best matching quality from cached DASH video list
     */
    fun findBestQuality(
        targetQn: Int,
        availableVideos: List<DashVideo>
    ): DashVideo? {
        if (availableVideos.isEmpty()) return null
        
        // 1. Exact match
        val exactMatch = availableVideos.find { it.id == targetQn }
        if (exactMatch != null) {
            Logger.d(TAG, "Exact match found: qn=$targetQn")
            return exactMatch
        }
        
        // 2. Downgrade: find highest quality below target
        val lowerQualities = availableVideos.filter { it.id <= targetQn }
        if (lowerQualities.isNotEmpty()) {
            val best = lowerQualities.maxByOrNull { it.id }
            Logger.d(TAG, "Downgrade to: qn=${best?.id} (target was $targetQn)")
            return best
        }
        
        // 3. Upgrade: find lowest quality above target
        val higherQualities = availableVideos.filter { it.id > targetQn }
        if (higherQualities.isNotEmpty()) {
            val best = higherQualities.minByOrNull { it.id }
            Logger.d(TAG, "Upgrade to: qn=${best?.id} (target was $targetQn)")
            return best
        }
        
        // 4. Fallback: return any available
        return availableVideos.firstOrNull()
    }
    
    /**
     * Find best audio stream from cached DASH audio list
     */
    fun findBestAudio(availableAudios: List<DashAudio>): DashAudio? {
        if (availableAudios.isEmpty()) return null
        
        // Prefer higher bandwidth audio
        return availableAudios.maxByOrNull { it.bandwidth ?: 0 }
    }
    
    /**
     * Execute quality change
     */
    fun changeQuality(
        targetQualityId: Int,
        cachedVideos: List<DashVideo>,
        cachedAudios: List<DashAudio>
    ): QualityChangeResult {
        Logger.d(TAG, "changeQuality: target=$targetQualityId, cachedVideos=${cachedVideos.map { it.id }}")
        
        if (cachedVideos.isEmpty()) {
            return QualityChangeResult.NoCachedData
        }
        
        val selectedVideo = findBestQuality(targetQualityId, cachedVideos)
        if (selectedVideo == null) {
            return QualityChangeResult.NoMatchingQuality
        }
        
        val selectedAudio = findBestAudio(cachedAudios)
        val videoUrl = selectedVideo.getValidUrl()
        
        if (videoUrl.isEmpty()) {
            return QualityChangeResult.InvalidUrl
        }
        
        val audioUrl = selectedAudio?.getValidUrl()
        val actualQuality = selectedVideo.id
        val wasDowngraded = actualQuality < targetQualityId
        
        Logger.d(TAG, "Quality change result: actual=$actualQuality, wasDowngraded=$wasDowngraded")
        
        return QualityChangeResult.Success(
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            actualQualityId = actualQuality,
            wasDowngraded = wasDowngraded
        )
    }
    
    /**
     * Get quality label
     */
    fun getQualityLabel(qualityId: Int): String {
        return QUALITY_LABELS[qualityId] ?: "${qualityId}P"
    }
    
    /**
     * Check if quality requires VIP
     */
    fun requiresVip(qualityId: Int): Boolean {
        return qualityId >= 112
    }
    
    /**
     * Check if quality requires login
     */
    fun requiresLogin(qualityId: Int): Boolean {
        return qualityId >= 80
    }
    
    /**
     * 检查用户是否有权限使用指定画质
     * 
     * @param qualityId 目标画质 ID
     * @param isLoggedIn 是否已登录
     * @param isVip 是否为大会员
     * @param context [New] 上下文，用于检查解锁设置
     * @return 权限检查结果
     */
    fun checkQualityPermission(
        qualityId: Int,
        isLoggedIn: Boolean,
        isVip: Boolean
    ): QualityPermissionResult {
        // [New] 检查是否开启“解锁高画质” - REVERTED
        // if (context != null) ...
        
        val label = getQualityLabel(qualityId)
        
        return when {
            // VIP 画质 (≥112): 需要大会员
            requiresVip(qualityId) && !isVip -> {
                Logger.d(TAG, "Quality $qualityId requires VIP, user isVip=$isVip")
                QualityPermissionResult.RequiresVip(label)
            }
            // 需要登录的画质 (≥80): 需要登录
            requiresLogin(qualityId) && !isLoggedIn -> {
                Logger.d(TAG, "Quality $qualityId requires login, user isLoggedIn=$isLoggedIn")
                QualityPermissionResult.RequiresLogin(label)
            }
            // 有权限
            else -> {
                Logger.d(TAG, "Quality $qualityId permitted for user (isLoggedIn=$isLoggedIn, isVip=$isVip)")
                QualityPermissionResult.Permitted
            }
        }
    }
    
    /**
     * 获取用户可用的最高画质
     * 
     * @param availableQualities 视频支持的画质列表
     * @param isLoggedIn 是否已登录
     * @param isVip 是否为大会员
     * @param context [New] 上下文
     * @return 最高可用画质 ID，如果无可用画质返回默认值 64 (720P)
     */
    fun getMaxAvailableQuality(
        availableQualities: List<Int>,
        isLoggedIn: Boolean,
        isVip: Boolean
    ): Int {
        if (availableQualities.isEmpty()) return 64
        
        // 按画质从高到低排序，找到第一个有权限的画质
        val sortedQualities = availableQualities.sortedDescending()
        
        for (quality in sortedQualities) {
            val permission = checkQualityPermission(quality, isLoggedIn, isVip)
            if (permission is QualityPermissionResult.Permitted) {
                Logger.d(TAG, "Max available quality for user: $quality")
                return quality
            }
        }
        
        // 如果没有符合权限的画质，返回最低画质
        val fallback = sortedQualities.lastOrNull() ?: 64
        Logger.d(TAG, "No permitted quality found, fallback to: $fallback")
        return fallback
    }
}

/**
 * 画质权限检查结果
 */
sealed class QualityPermissionResult {
    /** 有权限使用该画质 */
    object Permitted : QualityPermissionResult()
    
    /** 需要大会员 */
    data class RequiresVip(val qualityLabel: String) : QualityPermissionResult()
    
    /** 需要登录 */
    data class RequiresLogin(val qualityLabel: String) : QualityPermissionResult()
}

/**
 * Quality change result
 */
sealed class QualityChangeResult {
    /**
     * Success
     */
    data class Success(
        val videoUrl: String,
        val audioUrl: String?,
        val actualQualityId: Int,
        val wasDowngraded: Boolean
    ) : QualityChangeResult()
    
    /**
     * No cached data, need to re-request API
     */
    object NoCachedData : QualityChangeResult()
    
    /**
     * No matching quality
     */
    object NoMatchingQuality : QualityChangeResult()
    
    /**
     * Invalid URL
     */
    object InvalidUrl : QualityChangeResult()
}
