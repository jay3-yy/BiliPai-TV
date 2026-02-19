// 文件路径: core/plugin/FeedPlugin.kt
package com.android.purebilibili.core.plugin

import com.android.purebilibili.data.model.response.VideoItem

/**
 * 📰 信息流处理插件接口
 * 
 * 用于实现首页推荐流的增强功能，如：
 * - 过滤广告
 * - 过滤推广内容
 * - 自定义过滤规则
 */
interface FeedPlugin : Plugin {
    
    /**
     * 判断是否显示该推荐项
     * 
     * @param item 推荐项数据
     * @return true 表示显示，false 表示隐藏
     */
    fun shouldShowItem(item: VideoItem): Boolean
}
