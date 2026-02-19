// 文件路径: core/util/ShareUtils.kt
package com.android.purebilibili.core.util

import android.content.Context
import android.content.Intent

/**
 * 🔗 分享工具类
 * 
 * 提供视频分享功能，自动去除B站跟踪参数
 */
object ShareUtils {
    
    //  B站常见的跟踪参数列表
    private val TRACKING_PARAMS = setOf(
        "spm_id_from",      // 来源追踪
        "from_spmid",       // 来源追踪
        "from_source",      // 来源追踪
        "share_source",     // 分享来源
        "share_medium",     // 分享媒介
        "share_plat",       // 分享平台
        "share_session_id", // 分享会话ID
        "share_tag",        // 分享标签
        "share_from",       // 分享来源
        "share_times",      // 分享次数
        "timestamp",        // 时间戳
        "unique_k",         // 唯一标识
        "vd_source",        // 视频来源
        "from",             // 来源
        "seid",             // 会话ID
        "bbid",             // 设备ID
        "ts",               // 时间戳
        "is_story_h5",      // 故事模式标记
        "mid",              // 用户ID
        "p",                // 分P
        "plat_id",          // 平台ID
        "buvid",            // 设备标识
        "up_id",            // UP主ID
        "session_id"        // 会话ID
    )
    
    /**
     * 生成干净的B站视频链接（不带任何跟踪参数）
     * 
     * @param bvid 视频BV号
     * @return 干净的视频链接
     */
    fun cleanBilibiliUrl(bvid: String): String {
        return "https://www.bilibili.com/video/$bvid"
    }
    
    /**
     * 清理URL中的跟踪参数
     * 
     * @param url 原始URL
     * @return 清理后的URL
     */
    fun cleanUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            val builder = uri.buildUpon().clearQuery()
            
            // 只保留非跟踪参数
            uri.queryParameterNames.forEach { param ->
                if (param.lowercase() !in TRACKING_PARAMS) {
                    uri.getQueryParameter(param)?.let { value ->
                        builder.appendQueryParameter(param, value)
                    }
                }
            }
            
            builder.build().toString()
        } catch (e: Exception) {
            url // 解析失败时返回原URL
        }
    }
    
    /**
     * 分享视频到其他应用
     * 
     * @param context 上下文
     * @param title 视频标题
     * @param bvid 视频BV号
     */
    fun shareVideo(context: Context, title: String, bvid: String) {
        val cleanUrl = cleanBilibiliUrl(bvid)
        val shareText = "$title\n$cleanUrl"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "分享视频"))
            //  记录分享事件
            AnalyticsHelper.logShare(bvid, "system_share")
        } catch (e: Exception) {
            Logger.e("ShareUtils", "分享失败: ${e.message}")
        }
    }
    
    /**
     * 分享番剧/影视到其他应用
     * 
     * @param context 上下文
     * @param title 标题
     * @param seasonId 季度ID
     * @param epId 剧集ID（可选）
     */
    fun shareBangumi(context: Context, title: String, seasonId: Long, epId: Long? = null) {
        val url = if (epId != null) {
            "https://www.bilibili.com/bangumi/play/ep$epId"
        } else {
            "https://www.bilibili.com/bangumi/play/ss$seasonId"
        }
        val shareText = "$title\n$url"
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "分享"))
        } catch (e: Exception) {
            Logger.e("ShareUtils", "分享失败: ${e.message}")
        }
    }
}
