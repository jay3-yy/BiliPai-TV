package com.android.purebilibili.core.util

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.core.lifecycle.BackgroundManager
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 崩溃报告工具类
 *
 * 封装 Firebase Crashlytics，提供统一的错误上报接口：
 * - 全局崩溃前上下文补充
 * - 非致命错误上报与限流
 * - 用户/会话上下文 key 维护
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val RATE_LIMIT_WINDOW_MS = 60_000L
    private const val RATE_LIMIT_MAX_KEYS = 300

    @Volatile
    private var isEnabled: Boolean = true

    @Volatile
    private var globalHandlerInstalled = false

    private var previousUncaughtHandler: Thread.UncaughtExceptionHandler? = null
    private val nonFatalRateLimiter = ConcurrentHashMap<String, Long>()

    /**
     * 基础初始化：写入稳定环境信息
     */
    fun init(context: Context) {
        try {
            setCustomKey("app_version", BuildConfig.VERSION_NAME)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            setCustomKey("device_model", Build.MODEL ?: "unknown")
            setCustomKey("device_brand", Build.BRAND ?: "unknown")
            setCustomKey("android_version", Build.VERSION.SDK_INT)
            setCustomKey("locale", Locale.getDefault().toLanguageTag())
            setCustomKey(
                "process_name",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Application.getProcessName()
                } else {
                    context.packageName
                }
            )
            setCustomKey("app_in_foreground", !BackgroundManager.isInBackground)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init crash context", e)
        }
    }

    /**
     * 启用/禁用 Crashlytics 收集
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(enabled)
            Logger.d(TAG, " Crashlytics collection ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Crashlytics enabled state", e)
        }
    }

    /**
     * 安装全局未捕获异常处理器：
     * 在崩溃真正上报前，补充最后上下文信息。
     */
    fun installGlobalExceptionHandler() {
        if (globalHandlerInstalled) return
        globalHandlerInstalled = true

        previousUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                if (isEnabled) {
                    setCustomKey("fatal_thread_name", thread.name)
                    setCustomKey("fatal_thread_id", thread.threadId())
                    setCustomKey("app_in_foreground", !BackgroundManager.isInBackground)
                    log("FATAL: ${throwable.javaClass.simpleName}: ${throwable.message.orEmpty().take(200)}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enrich fatal crash context", e)
            } finally {
                previousUncaughtHandler?.uncaughtException(thread, throwable)
                    ?: run {
                        android.os.Process.killProcess(android.os.Process.myPid())
                        kotlin.system.exitProcess(10)
                    }
            }
        }
    }

    /**
     * 记录前后台状态，便于排查崩溃发生场景
     */
    fun setAppForegroundState(inForeground: Boolean) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey("app_in_foreground", inForeground)
            Firebase.crashlytics.log("App ${if (inForeground) "foreground" else "background"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set app foreground state", e)
        }
    }

    /**
     * 标记当前页面
     */
    fun setLastScreen(screenName: String) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey("last_screen", screenName.take(100))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set last screen", e)
        }
    }

    /**
     * 标记最近一次业务事件
     */
    fun setLastEvent(eventName: String) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey("last_event", eventName.take(100))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set last event", e)
        }
    }

    /**
     * 同步用户会话信息（匿名/已登录、VIP、无痕模式）
     */
    fun syncUserContext(mid: Long?, isVip: Boolean?, privacyModeEnabled: Boolean?) {
        if (!isEnabled) return
        try {
            if (mid != null && mid > 0) {
                Firebase.crashlytics.setUserId(mid.toString())
                Firebase.crashlytics.setCustomKey("is_logged_in", true)
            } else {
                Firebase.crashlytics.setUserId("")
                Firebase.crashlytics.setCustomKey("is_logged_in", false)
            }
            isVip?.let { Firebase.crashlytics.setCustomKey("is_vip", it) }
            privacyModeEnabled?.let { Firebase.crashlytics.setCustomKey("privacy_mode", it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user context", e)
        }
    }

    /**
     * 记录非致命异常
     * 用于捕获的异常，不会导致崩溃但需要追踪
     */
    fun logException(e: Throwable, message: String? = null) {
        if (!isEnabled) return
        val key = "exception:${e.javaClass.name}:${message ?: e.message.orEmpty().take(120)}"
        if (shouldDropByRateLimit(key)) return

        try {
            message?.let { Firebase.crashlytics.log(it) }
            Firebase.crashlytics.recordException(e)
            Logger.e(TAG, " Exception logged: ${e.message}", e)
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to log exception", ex)
        }
    }

    /**
     * 记录自定义日志
     * 这些日志会在崩溃报告中显示，帮助定位问题
     */
    fun log(message: String) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.log(message)
            Logger.d(TAG, " Log: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log message", e)
        }
    }

    /**
     * 设置用户标识符（用于追踪特定用户的问题）
     * 注意：请勿设置可识别个人身份的信息
     */
    fun setUserId(userId: String) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setUserId(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user ID", e)
        }
    }

    /**
     * 设置自定义键值对（崩溃时会附带这些信息）
     */
    fun setCustomKey(key: String, value: String) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }

    /**
     * 设置 Boolean 类型的自定义键
     */
    fun setCustomKey(key: String, value: Boolean) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }

    /**
     * 设置 Int 类型的自定义键
     */
    fun setCustomKey(key: String, value: Int) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }

    /**
     * 设置 Long 类型的自定义键
     */
    fun setCustomKey(key: String, value: Long) {
        if (!isEnabled) return
        try {
            Firebase.crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set custom key", e)
        }
    }

    // ========== 视频播放错误上报 ==========

    /**
     * 上报视频播放错误
     * @param bvid 视频 BV 号
     * @param errorType 错误类型 (如 "no_play_url", "network_error", "decode_error")
     * @param errorMessage 错误详情
     * @param exception 可选的异常对象
     */
    fun reportVideoError(
        bvid: String,
        errorType: String,
        errorMessage: String,
        exception: Throwable? = null
    ) {
        if (!isEnabled) return
        val key = "video:$errorType:${errorMessage.take(80)}"
        if (shouldDropByRateLimit(key)) return

        try {
            Firebase.crashlytics.setCustomKey("video_bvid", bvid)
            Firebase.crashlytics.setCustomKey("video_error_type", errorType)
            Firebase.crashlytics.log(" Video Error: [$errorType] $bvid - $errorMessage")
            Firebase.crashlytics.recordException(exception ?: VideoPlaybackException(errorType, errorMessage))
            Logger.e(TAG, " Video error reported: [$errorType] $bvid - $errorMessage", exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report video error", e)
        }
    }

    /**
     * 上报 API/网络错误
     * @param endpoint API 端点（不含 query）
     * @param httpCode HTTP 状态码（IO 异常可传 -1）
     * @param errorMessage 错误详情
     */
    fun reportApiError(
        endpoint: String,
        httpCode: Int,
        errorMessage: String,
        bvid: String? = null
    ) {
        if (!isEnabled) return
        val safeEndpoint = endpoint.substringBefore("?").take(160)
        val key = "api:$httpCode:$safeEndpoint:${errorMessage.take(80)}"
        if (shouldDropByRateLimit(key)) return

        try {
            Firebase.crashlytics.setCustomKey("api_endpoint", safeEndpoint)
            Firebase.crashlytics.setCustomKey("api_http_code", httpCode)
            Firebase.crashlytics.log("API Error: [$httpCode] $safeEndpoint - ${errorMessage.take(300)}")
            Firebase.crashlytics.recordException(ApiException(safeEndpoint, httpCode, errorMessage))
            Logger.e(TAG, "API error: [$httpCode] $safeEndpoint - $errorMessage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report API error", e)
        }
    }

    /**
     * 上报弹幕加载错误
     */
    fun reportDanmakuError(cid: Long, errorMessage: String, exception: Throwable? = null) {
        if (!isEnabled) return
        val key = "danmaku:$cid:${errorMessage.take(80)}"
        if (shouldDropByRateLimit(key)) return

        try {
            Firebase.crashlytics.setCustomKey("danmaku_cid", cid.toString())
            Firebase.crashlytics.log(" Danmaku Error: cid=$cid - $errorMessage")
            Firebase.crashlytics.recordException(exception ?: DanmakuException(cid, errorMessage))
            Logger.e(TAG, " Danmaku error reported: cid=$cid - $errorMessage", exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report danmaku error", e)
        }
    }

    /**
     * 上报直播播放错误
     */
    fun reportLiveError(
        roomId: Long,
        errorType: String,
        errorMessage: String,
        exception: Throwable? = null
    ) {
        if (!isEnabled) return
        val key = "live:$roomId:$errorType:${errorMessage.take(80)}"
        if (shouldDropByRateLimit(key)) return

        try {
            Firebase.crashlytics.setCustomKey("live_room_id", roomId.toString())
            Firebase.crashlytics.setCustomKey("live_error_type", errorType)
            Firebase.crashlytics.log("Live Error: [$errorType] roomId=$roomId - $errorMessage")
            Firebase.crashlytics.recordException(exception ?: LiveStreamException(roomId, errorType, errorMessage))
            Logger.e(TAG, " Live error reported: [$errorType] roomId=$roomId - $errorMessage", exception)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report live error", e)
        }
    }

    /**
     * 手动触发崩溃（仅用于测试）
     */
    fun testCrash() {
        throw RuntimeException("CrashReporter Test Crash")
    }

    private fun shouldDropByRateLimit(key: String): Boolean {
        val now = System.currentTimeMillis()
        val lastTs = nonFatalRateLimiter[key]
        if (lastTs != null && now - lastTs < RATE_LIMIT_WINDOW_MS) {
            return true
        }
        nonFatalRateLimiter[key] = now

        if (nonFatalRateLimiter.size > RATE_LIMIT_MAX_KEYS) {
            val expireBefore = now - RATE_LIMIT_WINDOW_MS * 2
            nonFatalRateLimiter.entries.removeIf { it.value < expireBefore }
        }
        return false
    }
}

// ========== 自定义异常类（用于 Crashlytics 分类） ==========

/**
 * 视频播放异常
 */
class VideoPlaybackException(
    val errorType: String,
    override val message: String
) : Exception("[$errorType] $message")

/**
 * API 请求异常
 */
class ApiException(
    val endpoint: String,
    val httpCode: Int,
    override val message: String
) : Exception("[$httpCode] $endpoint: $message")

/**
 * 弹幕加载异常
 */
class DanmakuException(
    val cid: Long,
    override val message: String
) : Exception("Danmaku cid=$cid: $message")

/**
 * 直播播放异常
 */
class LiveStreamException(
    val roomId: Long,
    val errorType: String,
    override val message: String
) : Exception("[$errorType] Live roomId=$roomId: $message")
