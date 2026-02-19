package com.android.purebilibili.core.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.store.TokenManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import java.util.Locale

/**
 *  Firebase Analytics 工具类
 * 封装 Firebase Analytics，提供统一的用户行为追踪接口
 * 
 * 追踪的事件类型：
 * - 屏幕浏览 (screen_view)
 * - 视频播放 (video_play, video_complete)
 * - 搜索行为 (search)
 * - 用户操作 (like, share, favorite, follow)
 * - 应用事件 (app_open, login)
 */
object AnalyticsHelper {
    
    private const val TAG = "AnalyticsHelper"
    
    private var analytics: FirebaseAnalytics? = null
    private var isEnabled: Boolean = true
    private var isInForeground: Boolean = false
    private var sessionStartMs: Long = 0L
    
    /**
     * 初始化 Analytics (在 Application 中调用)
     */
    fun init(context: Context) {
        try {
            analytics = Firebase.analytics
            analytics?.setUserProperty("app_version", BuildConfig.VERSION_NAME)
            analytics?.setUserProperty("build_type", BuildConfig.BUILD_TYPE)
            analytics?.setUserProperty("locale", Locale.getDefault().toLanguageTag())
            syncUserContext(
                mid = TokenManager.midCache,
                isVip = TokenManager.isVipCache,
                privacyModeEnabled = SettingsManager.isPrivacyModeEnabledSync(context)
            )
            Logger.d(TAG, " Firebase Analytics initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Firebase Analytics", e)
        }
    }
    
    /**
     * 启用/禁用 Analytics 收集
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            analytics?.setAnalyticsCollectionEnabled(enabled)
            CrashReporter.setLastEvent("analytics_collection_${if (enabled) "enabled" else "disabled"}")
            Logger.d(TAG, " Analytics collection ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Analytics enabled state", e)
        }
    }
    
    /**
     * 设置用户 ID (用于关联用户行为)
     * 注意：请勿设置可识别个人身份的信息
     */
    fun setUserId(userId: String?) {
        if (!isEnabled) return
        try {
            analytics?.setUserId(userId)
            val mid = userId?.toLongOrNull()
            CrashReporter.syncUserContext(mid = mid, isVip = null, privacyModeEnabled = null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
        }
    }
    
    /**
     * 设置用户属性 (用于用户分群分析)
     */
    fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        try {
            analytics?.setUserProperty(name, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property", e)
        }
    }

    /**
     * 同步用户上下文到 Analytics + Crashlytics
     */
    fun syncUserContext(mid: Long?, isVip: Boolean?, privacyModeEnabled: Boolean?) {
        if (!isEnabled) return
        try {
            analytics?.setUserId(mid?.takeIf { it > 0 }?.toString())
            analytics?.setUserProperty(
                "login_state",
                if (mid != null && mid > 0) "logged_in" else "guest"
            )
            isVip?.let { analytics?.setUserProperty("vip_state", if (it) "vip" else "normal") }
            privacyModeEnabled?.let {
                analytics?.setUserProperty("privacy_mode", if (it) "on" else "off")
            }
            CrashReporter.syncUserContext(mid, isVip, privacyModeEnabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user context", e)
        }
    }

    /**
     * App 进入前台
     */
    fun onAppForeground() {
        if (!isEnabled) return
        if (isInForeground) return
        sessionStartMs = System.currentTimeMillis()
        isInForeground = true
        try {
            analytics?.logEvent("app_foreground") {
                param("source", "process_lifecycle")
            }
            CrashReporter.setAppForegroundState(true)
            CrashReporter.setLastEvent("app_foreground")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log app foreground", e)
        }
    }

    /**
     * App 进入后台
     */
    fun onAppBackground() {
        if (!isEnabled) return
        if (!isInForeground) return
        val now = System.currentTimeMillis()
        val sessionDurationSec = if (sessionStartMs > 0L) ((now - sessionStartMs) / 1000L).coerceAtLeast(0L) else 0L
        isInForeground = false
        try {
            analytics?.logEvent("app_background") {
                param("source", "process_lifecycle")
                param("session_duration_sec", sessionDurationSec)
            }
            CrashReporter.setAppForegroundState(false)
            CrashReporter.setLastEvent("app_background")
            CrashReporter.setCustomKey("last_session_duration_sec", sessionDurationSec)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log app background", e)
        }
    }
    
    // ==========  屏幕浏览追踪 ==========
    
    /**
     * 记录屏幕浏览
     * @param screenName 屏幕名称 (如 "HomeScreen", "VideoDetailScreen")
     * @param screenClass 屏幕类名 (可选)
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                screenClass?.let { param(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
            }
            CrashReporter.setLastScreen(screenName)
            CrashReporter.setLastEvent("screen_view")
            Logger.d(TAG, " Screen view: $screenName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log screen view", e)
        }
    }
    
    /**
     * 记录视频播放开始
     * 🔒 隐私保护：不记录视频ID、标题、作者等可识别用户观看内容的信息
     * 仅记录事件发生次数用于统计
     */
    fun logVideoPlay(
        videoId: String,
        title: String,
        author: String? = null,
        duration: Long? = null
    ) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_play") {
                // 🔒 不记录 video_id 和 title，仅记录时长范围用于分析
                duration?.let { 
                    val durationRange = when {
                        it < 60 -> "under_1min"
                        it < 300 -> "1_5min"
                        it < 600 -> "5_10min"
                        it < 1800 -> "10_30min"
                        else -> "over_30min"
                    }
                    param("duration_range", durationRange)
                }
            }
            CrashReporter.setLastEvent("video_play")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video play", e)
        }
    }
    
    /**
     * 记录视频播放进度 (用于计算完播率)
     * @param videoId 视频 ID
     * @param progress 播放进度百分比 (0-100)
     * @param watchTime 实际观看时长 (秒)
     */
    fun logVideoProgress(
        videoId: String,
        progress: Int,
        watchTime: Long
    ) {
        if (!isEnabled) return
        // 只在关键节点记录: 25%, 50%, 75%, 100%
        if (progress !in listOf(25, 50, 75, 100)) return
        try {
            analytics?.logEvent("video_progress") {
                param("video_id", videoId)
                param("progress_percent", progress.toLong())
                param("watch_time_sec", watchTime)
            }
            Logger.d(TAG, " Video progress: $videoId at $progress%")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video progress", e)
        }
    }
    
    /**
     * 记录视频播放完成
     */
    fun logVideoComplete(videoId: String, totalWatchTime: Long) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_complete") {
                param("video_id", videoId)
                param("total_watch_time_sec", totalWatchTime)
            }
            Logger.d(TAG, " Video complete: $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video complete", e)
        }
    }
    
    /**
     * 记录搜索事件
     * 🔒 隐私保护：不记录搜索关键词，仅记录搜索行为
     */
    fun logSearch(query: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SEARCH) {
                // 🔒 不记录具体搜索词，仅记录搜索词长度范围
                val lengthRange = when {
                    query.length <= 2 -> "short"
                    query.length <= 10 -> "medium"
                    else -> "long"
                }
                param("query_length", lengthRange)
            }
            CrashReporter.setLastEvent("search")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log search", e)
        }
    }
    
    /**
     * 记录搜索结果点击
     * 🔒 隐私保护：不记录搜索词和视频ID
     */
    fun logSearchResultClick(query: String, videoId: String, position: Int) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("search_result_click") {
                // 🔒 仅记录点击位置用于分析搜索结果质量
                param("position", position.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log search result click", e)
        }
    }
    
    // ========== ❤️ 用户互动追踪 ==========
    
    /**
     * 记录点赞事件
     */
    fun logLike(videoId: String, isLiked: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(if (isLiked) "video_like" else "video_unlike") {
                param("video_id", videoId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log like", e)
        }
    }
    
    /**
     * 记录收藏事件
     */
    fun logFavorite(videoId: String, isFavorited: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(if (isFavorited) "video_favorite" else "video_unfavorite") {
                param("video_id", videoId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log favorite", e)
        }
    }
    
    /**
     * 记录分享事件
     */
    fun logShare(videoId: String, method: String? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.SHARE) {
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "video")
                param(FirebaseAnalytics.Param.ITEM_ID, videoId)
                method?.let { param(FirebaseAnalytics.Param.METHOD, it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log share", e)
        }
    }
    
    /**
     * 记录关注用户事件
     */
    fun logFollow(userId: String, isFollowed: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent(if (isFollowed) "user_follow" else "user_unfollow") {
                param("target_user_id", userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log follow", e)
        }
    }
    
    /**
     * 记录投币事件
     */
    fun logCoin(videoId: String, coinCount: Int) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_coin") {
                param("video_id", videoId)
                param("coin_count", coinCount.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log coin", e)
        }
    }
    
    // ==========  应用事件追踪 ==========
    
    /**
     * 记录应用打开
     */
    fun logAppOpen() {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
            CrashReporter.setLastEvent("app_open")
            Logger.d(TAG, " App open")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log app open", e)
        }
    }
    
    /**
     * 记录登录事件
     */
    fun logLogin(method: String = "qrcode") {
        if (!isEnabled) return
        try {
            analytics?.logEvent(FirebaseAnalytics.Event.LOGIN) {
                param(FirebaseAnalytics.Param.METHOD, method)
            }
            CrashReporter.setLastEvent("login")
            Logger.d(TAG, " Login: $method")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log login", e)
        }
    }
    
    /**
     * 记录登出事件
     */
    fun logLogout() {
        if (!isEnabled) return
        try {
            analytics?.logEvent("logout", null)
            syncUserContext(mid = null, isVip = false, privacyModeEnabled = null)
            CrashReporter.setLastEvent("logout")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log logout", e)
        }
    }
    
    // ========== 📂 分类/频道追踪 ==========
    
    /**
     * 记录分类切换
     */
    fun logCategoryView(categoryName: String, categoryId: Int? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("category_view") {
                param("category_name", categoryName)
                categoryId?.let { param("category_id", it.toLong()) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log category view", e)
        }
    }
    
    /**
     * 记录番剧播放
     */
    fun logBangumiPlay(seasonId: String, episodeId: String, title: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("bangumi_play") {
                param("season_id", seasonId)
                param("episode_id", episodeId)
                param("title", title.take(100))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log bangumi play", e)
        }
    }
    
    // ========== ⚙️ 设置变更追踪 ==========
    
    /**
     * 记录设置变更 (用于了解用户偏好)
     */
    fun logSettingChange(settingName: String, value: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("setting_change") {
                param("setting_name", settingName)
                param("setting_value", value)
            }
            CrashReporter.setLastEvent("setting_change:$settingName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log setting change", e)
        }
    }
    
    // ==========  直播追踪 ==========
    
    /**
     * 记录直播观看
     */
    fun logLivePlay(roomId: Long, title: String, upName: String? = null) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("live_play") {
                param("room_id", roomId.toString())
                param("title", title.take(100))
                upName?.let { param("up_name", it.take(50)) }
            }
            Logger.d(TAG, " Live play: roomId=$roomId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log live play", e)
        }
    }
    
    /**
     * 记录直播观看时长
     */
    fun logLiveWatchTime(roomId: Long, watchTimeSeconds: Long) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("live_watch_time") {
                param("room_id", roomId.toString())
                param("watch_time_sec", watchTimeSeconds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log live watch time", e)
        }
    }
    
    // ==========  错误事件追踪 (用于分析问题) ==========
    
    /**
     * 记录视频播放错误 (Analytics 层面，用于统计)
     */
    fun logVideoError(videoId: String, errorType: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_error") {
                param("video_id", videoId)
                param("error_type", errorType)
            }
            CrashReporter.setLastEvent("video_error")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video error", e)
        }
    }
    
    /**
     * 记录直播播放错误
     */
    fun logLiveError(roomId: Long, errorType: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("live_error") {
                param("room_id", roomId.toString())
                param("error_type", errorType)
            }
            CrashReporter.setLastEvent("live_error")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log live error", e)
        }
    }
    
    // ========== 🎯 功能使用追踪 ==========
    
    /**
     * 记录空降助手使用 (SponsorBlock)
     */
    fun logSponsorBlockSkip(videoId: String, segmentType: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("sponsorblock_skip") {
                param("video_id", videoId)
                param("segment_type", segmentType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log sponsorblock skip", e)
        }
    }
    
    /**
     * 记录画质切换
     */
    fun logQualityChange(videoId: String, fromQuality: Int, toQuality: Int) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("quality_change") {
                param("video_id", videoId)
                param("from_quality", fromQuality.toLong())
                param("to_quality", toQuality.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log quality change", e)
        }
    }
    
    /**
     * 记录弹幕开关
     */
    fun logDanmakuToggle(enabled: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("danmaku_toggle") {
                param("enabled", if (enabled) "true" else "false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log danmaku toggle", e)
        }
    }
    
    // ========== 📱 特色功能追踪 ==========
    
    /**
     * 记录画中画模式使用
     * @param videoId 视频 ID
     * @param action 动作: "enter" / "exit"
     */
    fun logPictureInPicture(videoId: String, action: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("picture_in_picture") {
                param("video_id", videoId)
                param("action", action)
            }
            Logger.d(TAG, "📱 PiP: $action for $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log PiP", e)
        }
    }
    
    /**
     * 记录后台播放使用
     * @param videoId 视频 ID
     * @param action 动作: "enter" / "exit"
     */
    fun logBackgroundPlay(videoId: String, action: String) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("background_play") {
                param("video_id", videoId)
                param("action", action)
            }
            Logger.d(TAG, "🔊 Background play: $action for $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log background play", e)
        }
    }
    
    /**
     * 记录音频模式使用
     * @param videoId 视频 ID
     * @param enabled 是否开启
     */
    fun logAudioMode(videoId: String, enabled: Boolean) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("audio_mode") {
                param("video_id", videoId)
                param("enabled", if (enabled) "true" else "false")
            }
            Logger.d(TAG, "🎵 Audio mode: ${if (enabled) "enabled" else "disabled"} for $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log audio mode", e)
        }
    }
    
    /**
     * 记录直播画质切换
     * @param roomId 直播间 ID
     * @param fromQuality 原画质
     * @param toQuality 新画质
     */
    fun logLiveQualityChange(roomId: Long, fromQuality: Int, toQuality: Int) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("live_quality_change") {
                param("room_id", roomId.toString())
                param("from_quality", fromQuality.toLong())
                param("to_quality", toQuality.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log live quality change", e)
        }
    }
    
    /**
     * 记录首页视频点击 (仅记录分区统计，不记录视频ID等隐私信息)
     * @param tid 分区 ID
     * @param tname 分区名称
     * @param position 在列表中的位置
     */
    fun logVideoClick(
        videoId: String,
        title: String,
        tid: Int? = null,
        tname: String? = null,
        position: Int? = null
    ) {
        if (!isEnabled) return
        try {
            analytics?.logEvent("video_click") {
                // 🔒 隐私保护：不记录 video_id 和 title
                tname?.let { param("category_name", it) }
                position?.let { param("list_position", it.toLong()) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log video click", e)
        }
    }
}
