package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

/**
 * 直播弹幕表情映射器
 * 负责管理表情数据，并提供文本解析功能
 */
object DanmakuEmoticonMapper {
    
    // 关键词 -> 图片URL
    private val _emoticonMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val emoticonMap = _emoticonMap.asStateFlow()
    
    // 匹配 [xxx] 格式的正则
    private val EMOTICON_PATTERN = Pattern.compile("\\[(.*?)\\]")
    
    /**
     * 更新表情数据
     */
    fun update(newMap: Map<String, String>) {
        // 合并现有数据，防止清空通用表情
        val current = _emoticonMap.value.toMutableMap()
        current.putAll(newMap)
        _emoticonMap.value = current
    }
    
    /**
     * 解析弹幕文本，将表情转换为 InlineContent
     */
    fun parse(text: String, map: Map<String, String> = _emoticonMap.value): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        
        return buildAnnotatedString {
            val matcher = EMOTICON_PATTERN.matcher(text)
            var lastIndex = 0
            
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val tag = matcher.group(0) // [dog]
                val key = matcher.group(1) // dog
                
                // 添加前一段普通文本
                if (start > lastIndex) {
                    append(text.substring(lastIndex, start))
                }
                
                // 检查是否有对应的表情 URL
                // 尝试完全匹配 key，或者有些表情可能带有额外后缀
                // 注意：B站表情通常就是 [name] 对应 emoji字段 name
                if (map.containsKey(tag) || map.containsKey(key)) {
                    val id = tag // 使用 tag 作为 ID
                    appendInlineContent(id, tag)
                } else {
                    // 没有对应表情，保持原样
                    append(tag)
                }
                
                lastIndex = end
            }
            
            // 添加剩余文本
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }
}
