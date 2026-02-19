// 文件路径: core/util/Logger.kt
package com.android.purebilibili.core.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.android.purebilibili.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 *  统一日志工具类
 * 
 * 在 Release 版本中自动禁用日志输出，减少性能开销
 * 同时收集日志到内存缓冲区，支持导出供用户反馈
 */
object Logger {
    
    private val isDebug = BuildConfig.DEBUG
    
    /**
     * Debug 日志 - 仅在 Debug 版本输出
     */
    fun d(tag: String, message: String) {
        if (isDebug) Log.d(tag, message)
        LogCollector.add("D", tag, message)
    }
    
    /**
     * Info 日志 - 仅在 Debug 版本输出
     */
    fun i(tag: String, message: String) {
        if (isDebug) Log.i(tag, message)
        LogCollector.add("I", tag, message)
    }
    
    /**
     * Warning 日志 - 始终输出
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else message
        
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
        LogCollector.add("W", tag, fullMessage)
    }
    
    /**
     * Error 日志 - 始终输出
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else message
        
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        LogCollector.add("E", tag, fullMessage)
    }
}

/**
 *  日志收集器
 * 
 * 使用环形缓冲区保留最近 1000 条日志，支持导出分享
 */
object LogCollector {
    
    private const val MAX_ENTRIES = 1000
    private const val DUPLICATE_SUPPRESS_WINDOW_MS = 250L
    private val lock = Any()
    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private var lastEntryFingerprint: String? = null
    private var lastEntryTimestamp: Long = 0L
    
    /**
     * 日志条目
     */
    data class LogEntry(
        val timestamp: Long,
        val level: String,
        val tag: String,
        val message: String
    ) {
        fun format(): String {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
            return "[$time] $level/$tag: $message"
        }
    }
    
    /**
     * 添加日志条目（轻量路径，脱敏延后到导出）
     */
    fun add(level: String, tag: String, message: String) {
        val now = System.currentTimeMillis()
        val fingerprint = "$level|$tag|$message"
        synchronized(lock) {
            // 高频重复日志直接抑制，避免日志风暴拖垮主线程
            if (fingerprint == lastEntryFingerprint &&
                now - lastEntryTimestamp <= DUPLICATE_SUPPRESS_WINDOW_MS) {
                lastEntryTimestamp = now
                return
            }

            lastEntryFingerprint = fingerprint
            lastEntryTimestamp = now

            buffer.addLast(
                LogEntry(
                    timestamp = now,
                    level = level,
                    tag = tag,
                    message = message
                )
            )

            while (buffer.size > MAX_ENTRIES) {
                if (buffer.isNotEmpty()) {
                    buffer.removeFirst()
                } else {
                    break
                }
            }
        }
    }
    
    /**
     *  隐私脱敏：移除敏感信息
     * 
     * 覆盖范围：
     * - Cookie 值 (SESSDATA, bili_jct, DedeUserID 等)
     * - Token / Key (access_token, refresh_token 等)
     * - 用户标识 (mid, uid, 手机号, 邮箱)
     * - 网络信息 (IP 地址, MAC 地址)
     * - 文件路径 (可能包含用户名)
     * - 其他敏感参数
     */
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        // ========== Cookie 脱敏 ==========
        sanitized = sanitized.replace(Regex("SESSDATA=[^;\\s]+"), "SESSDATA=***")
        sanitized = sanitized.replace(Regex("bili_jct=[^;\\s]+"), "bili_jct=***")
        sanitized = sanitized.replace(Regex("DedeUserID=[^;\\s]+"), "DedeUserID=***")
        sanitized = sanitized.replace(Regex("DedeUserID__ckMd5=[^;\\s]+"), "DedeUserID__ckMd5=***")
        sanitized = sanitized.replace(Regex("sid=[^;\\s]+"), "sid=***")
        sanitized = sanitized.replace(Regex("buvid3=[^;\\s]+"), "buvid3=***")
        sanitized = sanitized.replace(Regex("buvid4=[^;\\s]+"), "buvid4=***")
        sanitized = sanitized.replace(Regex("b_nut=[^;\\s]+"), "b_nut=***")
        sanitized = sanitized.replace(Regex("_uuid=[^;\\s]+"), "_uuid=***")
        
        // ========== Token / Key 脱敏 ==========
        sanitized = sanitized.replace(Regex("access_token=[^&\\s]+"), "access_token=***")
        sanitized = sanitized.replace(Regex("refresh_token=[^&\\s]+"), "refresh_token=***")
        sanitized = sanitized.replace(Regex("access_key=[^&\\s]+"), "access_key=***")
        sanitized = sanitized.replace(Regex("appkey=[^&\\s]+"), "appkey=***")
        sanitized = sanitized.replace(Regex("sign=[^&\\s]+"), "sign=***")
        sanitized = sanitized.replace(Regex("csrf=[^&\\s]+"), "csrf=***")
        sanitized = sanitized.replace(Regex("\"token\":\"[^\"]+\""), "\"token\":\"***\"")
        sanitized = sanitized.replace(Regex("\"csrf\":\"[^\"]+\""), "\"csrf\":\"***\"")
        sanitized = sanitized.replace(Regex("Authorization:\\s*[^\\s]+"), "Authorization: ***")
        sanitized = sanitized.replace(Regex("Bearer\\s+[^\\s]+"), "Bearer ***")
        
        // ========== 用户 ID 脱敏 ==========
        // Bilibili mid/uid (通常为 6-11 位数字，在特定上下文中)
        sanitized = sanitized.replace(Regex("mid[=:]\\s*\\d{4,}"), "mid=***")
        sanitized = sanitized.replace(Regex("\"mid\":\\s*\\d+"), "\"mid\":***")
        sanitized = sanitized.replace(Regex("uid[=:]\\s*\\d{4,}"), "uid=***")
        sanitized = sanitized.replace(Regex("\"uid\":\\s*\\d+"), "\"uid\":***")
        sanitized = sanitized.replace(Regex("vmid[=:]\\s*\\d+"), "vmid=***")
        
        // ========== 手机号脱敏 (11位中国手机号) ==========
        sanitized = sanitized.replace(Regex("\\b1[3-9]\\d{9}\\b"), "1**********")
        
        // ========== 邮箱脱敏 ==========
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) { 
            val email = it.value
            val atIndex = email.indexOf('@')
            if (atIndex > 2) {
                email.substring(0, 2) + "***" + email.substring(atIndex)
            } else {
                "***" + email.substring(atIndex)
            }
        }
        
        // ========== IP 地址脱敏 ==========
        // IPv4
        sanitized = sanitized.replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b")) {
            val parts = it.value.split(".")
            if (parts.size == 4 && parts.all { p -> p.toIntOrNull() in 0..255 }) {
                "${parts[0]}.***.***.*"
            } else {
                it.value
            }
        }
        // IPv6 (简化处理)
        sanitized = sanitized.replace(Regex("\\b[0-9a-fA-F:]{15,}\\b"), "***:***:***")
        
        // ========== MAC 地址脱敏 ==========
        sanitized = sanitized.replace(Regex("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}"), "**:**:**:**:**:**")
        
        // ========== 文件路径脱敏 (隐藏用户名) ==========
        // Android 路径
        sanitized = sanitized.replace(Regex("/data/user/\\d+/[^/]+/"), "/data/user/0/***/")
        sanitized = sanitized.replace(Regex("/storage/emulated/\\d+/"), "/storage/emulated/0/")
        // 通用 home 目录
        sanitized = sanitized.replace(Regex("/home/[^/]+/"), "/home/***/")
        sanitized = sanitized.replace(Regex("/Users/[^/]+/"), "/Users/***/")
        
        // ========== 设备标识脱敏 ==========
        sanitized = sanitized.replace(Regex("device_id=[^&\\s]+"), "device_id=***")
        sanitized = sanitized.replace(Regex("\"device_id\":\"[^\"]+\""), "\"device_id\":\"***\"")
        sanitized = sanitized.replace(Regex("android_id=[^&\\s]+"), "android_id=***")
        sanitized = sanitized.replace(Regex("imei=[^&\\s]+"), "imei=***")
        
        // ========== 敏感 JSON 字段脱敏 ==========
        sanitized = sanitized.replace(Regex("\"face\":\"[^\"]+\""), "\"face\":\"***\"")
        sanitized = sanitized.replace(Regex("\"tel\":\"[^\"]+\""), "\"tel\":\"***\"")
        sanitized = sanitized.replace(Regex("\"name\":\"[^\"]{2,}\"")) {
            // 保留名字首字符
            val name = it.value
            val start = name.indexOf(":\"") + 2
            val end = name.lastIndexOf("\"")
            if (end > start + 1) {
                "\"name\":\"${name[start]}***\""
            } else {
                it.value
            }
        }
        
        // ========== 🎬 视频内容脱敏（保护用户观看记录隐私） ==========
        // 视频 BVID
        sanitized = sanitized.replace(Regex("BV[0-9A-Za-z]{10}"), "BV***")
        // 视频 AID/AV 号
        sanitized = sanitized.replace(Regex("\\bav\\d{4,}\\b", RegexOption.IGNORE_CASE), "av***")
        sanitized = sanitized.replace(Regex("\"aid\":\\s*\\d+"), "\"aid\":***")
        // CID
        sanitized = sanitized.replace(Regex("\\bcid[=:]\\s*\\d+"), "cid=***")
        sanitized = sanitized.replace(Regex("\"cid\":\\s*\\d+"), "\"cid\":***")
        // 直播房间号
        sanitized = sanitized.replace(Regex("room_id[=:]\\s*\\d+"), "room_id=***")
        sanitized = sanitized.replace(Regex("roomId[=:]\\s*\\d+"), "roomId=***")
        // Season ID (番剧)
        sanitized = sanitized.replace(Regex("season_id[=:]\\s*\\d+"), "season_id=***")
        sanitized = sanitized.replace(Regex("ep_id[=:]\\s*\\d+"), "ep_id=***")
        
        // ========== 🔍 搜索关键词脱敏 ==========
        sanitized = sanitized.replace(Regex("keyword=[^&\\s]+"), "keyword=***")
        sanitized = sanitized.replace(Regex("\"keyword\":\"[^\"]+\""), "\"keyword\":\"***\"")
        sanitized = sanitized.replace(Regex("Search:\\s*[^\\n]+"), "Search: ***")
        
        // ========== 📝 视频标题脱敏（仅保留前两个字符） ==========
        sanitized = sanitized.replace(Regex("video_title=[^&\\s]{3,}")) { 
            val title = it.value.substringAfter("=")
            "video_title=${title.take(2)}***"
        }
        sanitized = sanitized.replace(Regex("\"title\":\"[^\"]{3,}\"")) {
            val content = it.value
            val titleStart = content.indexOf(":\"") + 2
            val title = content.substring(titleStart, content.length - 1)
            "\"title\":\"${title.take(2)}***\""
        }
        
        return sanitized
    }
    
    /**
     * 获取所有日志条目
     */
    fun getEntries(): List<LogEntry> = synchronized(lock) { buffer.toList() }
    
    /**
     * 获取日志条目数量
     */
    fun getCount(): Int = synchronized(lock) { buffer.size }
    
    /**
     * 清空日志
     */
    fun clear() {
        synchronized(lock) {
            buffer.clear()
            lastEntryFingerprint = null
            lastEntryTimestamp = 0L
        }
    }
    
    /**
     * 导出日志到文件并通过系统分享
     * 
     * 日志会保存到 Download/BiliPai/logs/ 目录，方便 MT 管理器等工具直接访问
     */
    fun exportAndShare(context: Context) {
        try {
            val entries = getEntries()
            if (entries.isEmpty()) {
                Toast.makeText(context, "暂无日志记录", Toast.LENGTH_SHORT).show()
                return
            }
            
            // 生成日志内容
            val header = buildString {
                appendLine("========================================")
                appendLine("BiliPai 应用日志导出")
                appendLine("========================================")
                appendLine("导出时间: ${dateFormat.format(Date())}")
                appendLine("应用版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("设备信息: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("Android版本: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
                appendLine("日志条数: ${entries.size}")
                appendLine("========================================")
                appendLine()
            }
            
            val content = header + entries.joinToString("\n") { entry ->
                entry.copy(message = sanitizeMessage(entry.message)).format()
            }
            val fileName = "bilipai_log_${fileDateFormat.format(Date())}.txt"
            
            //  [优化] 保存到外部 Download 目录，MT 管理器可直接访问
            val savedPath = saveToExternalDownload(context, fileName, content)
            
            if (savedPath != null) {
                // 保存成功，显示路径并提供分享选项
                val displayPath = savedPath.substringAfter("Download/")
                Toast.makeText(
                    context, 
                    "📁 已保存到: Download/$displayPath\n\n点击分享按钮可发送给开发者", 
                    Toast.LENGTH_LONG
                ).show()
                
                // 通过 FileProvider 分享（兼容所有 Android 版本）
                shareLogFile(context, savedPath, fileName)
            } else {
                // 外部存储不可用，回退到内部缓存
                val cacheDir = File(context.cacheDir, "logs")
                cacheDir.mkdirs()
                val logFile = File(cacheDir, fileName)
                logFile.writeText(content)
                
                Toast.makeText(context, "日志已保存，点击分享发送", Toast.LENGTH_SHORT).show()
                shareLogFileFromCache(context, logFile)
            }
            
        } catch (e: Exception) {
            Log.e("LogCollector", "导出日志失败", e)
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     *  保存日志到外部 Download 目录
     * 
     * 路径: /storage/emulated/0/Download/BiliPai/logs/xxx.txt
     * MT管理器路径: Download/BiliPai/logs/
     */
    private fun saveToExternalDownload(context: Context, fileName: String, content: String): String? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore API
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Download/BiliPai/logs")
                }
                
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    "Download/BiliPai/logs/$fileName"
                }
            } else {
                // Android 9 及以下直接写入
                @Suppress("DEPRECATION")
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val logDir = File(downloadDir, "BiliPai/logs")
                logDir.mkdirs()
                val logFile = File(logDir, fileName)
                logFile.writeText(content)
                logFile.absolutePath
            }
        } catch (e: Exception) {
            Log.w("LogCollector", "无法保存到外部存储", e)
            null
        }
    }
    
    /**
     * 分享日志文件（从外部存储）
     */
    private fun shareLogFile(context: Context, filePath: String, fileName: String) {
        try {
            // 构建文件 URI
            val file = if (filePath.startsWith("Download/")) {
                // MediaStore 路径，需要重新查询
                @Suppress("DEPRECATION")
                val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                File(downloadDir, filePath.substringAfter("Download/"))
            } else {
                File(filePath)
            }
            
            if (!file.exists()) {
                // 文件可能是通过 MediaStore 创建的，使用缓存备份分享
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BiliPai 日志反馈")
                putExtra(Intent.EXTRA_TEXT, "请查看附件中的日志文件\n\n文件位置: $filePath")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "分享日志"))
        } catch (e: Exception) {
            Log.e("LogCollector", "分享失败", e)
        }
    }
    
    /**
     * 分享日志文件（从缓存目录）
     */
    private fun shareLogFileFromCache(context: Context, logFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BiliPai 日志反馈")
                putExtra(Intent.EXTRA_TEXT, "请查看附件中的日志文件")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "分享日志"))
        } catch (e: Exception) {
            Log.e("LogCollector", "分享失败", e)
        }
    }
}
