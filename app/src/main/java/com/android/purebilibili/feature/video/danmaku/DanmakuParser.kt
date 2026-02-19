// 文件路径: feature/video/danmaku/DanmakuParser.kt
package com.android.purebilibili.feature.video.danmaku

import android.util.Log
import android.util.Xml
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_TOP_CENTER
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_BOTTOM_CENTER
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream

/**
 * 弹幕解析器
 * 
 * 支持两种格式：
 * 1. XML 格式 (旧版 API)
 * 2. Protobuf 格式 (新版 seg.so API)
 */
object DanmakuParser {
    
    private const val TAG = "DanmakuParser"
    
    /**
     *  [新增] 解析 Protobuf 弹幕数据 (推荐)
     * 
     * @param segments Protobuf 分段数据列表
     * @return ParsedDanmaku (标准弹幕 + 高级弹幕)
     */
    fun parseProtobuf(segments: List<ByteArray>): ParsedDanmaku {
        val standardList = mutableListOf<DanmakuData>()
        val advancedList = mutableListOf<AdvancedDanmakuData>()
        
        if (segments.isEmpty()) {
            Log.w(TAG, " No segments to parse")
            return ParsedDanmaku(standardList, advancedList)
        }
        
        Log.d(TAG, " Parsing ${segments.size} Protobuf segments...")
        
        var totalParsed = 0
        for ((index, segment) in segments.withIndex()) {
            try {
                val elems = DanmakuProto.parse(segment)
                Log.d(TAG, " Segment ${index + 1}: parsed ${elems.size} danmakus")
                
                for (elem in elems) {
                    // 尝试解析为高级弹幕 (Mode 7)
                    if (elem.mode == 7) {
                        try {
                            val advanced = parseAdvancedDanmaku(elem.content, elem.progress.toLong(), elem.color)
                            if (advanced != null) {
                                advancedList.add(advanced)
                                totalParsed++
                                continue // 成功解析为高级弹幕，跳过标准解析
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, " Failed to parse advanced danmaku: ${e.message}")
                        }
                    }
                    
                    // 标准弹幕解析
                    val textData = createTextDataFromProto(elem)
                    if (textData != null) {
                        standardList.add(textData)
                        totalParsed++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, " Failed to parse segment ${index + 1}: ${e.message}")
            }
        }
        
        //  [关键] 按时间排序 - DanmakuRenderEngine 需要有序数据
        standardList.sortBy { it.showAtTime }
        advancedList.sortBy { it.startTimeMs }
        
        // 统计信息
        if (totalParsed > 0) {
            val times = standardList.map { it.showAtTime }
            val minTime = times.minOrNull() ?: 0
            val maxTime = times.maxOrNull() ?: 0
            Log.w(TAG, " Parsed result: Standard=${standardList.size}, Advanced=${advancedList.size} | Time: ${minTime}ms ~ ${maxTime}ms")
        } else {
            Log.w(TAG, " No danmakus parsed from Protobuf!")
        }
        
        return ParsedDanmaku(standardList, advancedList)
    }
    
    /**
     * 从 Protobuf DanmakuElem 创建 TextData
     */
    private fun createTextDataFromProto(elem: DanmakuProto.DanmakuElem): TextData? {
        if (elem.content.isEmpty()) return null
        
        // Mode 8/9 代码弹幕目前暂不支持
        if (elem.mode >= 8) return null
        
        val layerType = mapLayerType(elem.mode)
        val colorWithAlpha = elem.color or 0xFF000000.toInt()
        
        // [API 完整利用] 使用 WeightedTextData 携带 weight 和 pool 信息
        return WeightedTextData().apply {
            this.danmakuId = elem.id
            this.userHash = elem.midHash
            this.text = elem.content
            this.showAtTime = elem.progress.toLong()
            this.layerType = layerType
            this.textColor = colorWithAlpha
            
            // 填充 Bilibili 特有属性
            this.weight = elem.weight
            this.pool = elem.pool
        }
    }
    
    private var debugLogCount = 0  // 用于限制调试日志数量
    
    /**
     * 解析 XML 弹幕数据 (旧版 API，作为后备方案)
     * 
     * @param rawData 原始 XML 数据
     * @return ParsedDanmaku (标准弹幕 + 高级弹幕)
     */
    fun parse(rawData: ByteArray): ParsedDanmaku {
        val standardList = mutableListOf<DanmakuData>()
        val advancedList = mutableListOf<AdvancedDanmakuData>()
        
        try {
            val parser = Xml.newPullParser()
            parser.setInput(ByteArrayInputStream(rawData), "UTF-8")
            
            var eventType = parser.eventType
            var count = 0
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "d") {
                    val pAttr = parser.getAttributeValue(null, "p")
                    parser.next()
                    val content = if (parser.eventType == XmlPullParser.TEXT) parser.text else ""
                    
                    if (pAttr != null && content.isNotEmpty()) {
                        val parts = pAttr.split(",")
                        if (parts.size >= 2) {
                            val mode = parts[1].toIntOrNull() ?: 1
                            val timeMs = ((parts[0].toFloatOrNull() ?: 0f) * 1000).toLong()
                            
                            val colorInt = (parts.getOrNull(3)?.toLongOrNull() ?: 0xFFFFFF).toInt()
                            
                            if (mode == 7) {
                                val advanced = parseAdvancedDanmaku(content, timeMs, colorInt)
                                if (advanced != null) {
                                    advancedList.add(advanced)
                                    count++
                                    continue
                                }
                            }
                            
                            val danmaku = createTextData(pAttr, content)
                            if (danmaku != null) {
                                standardList.add(danmaku)
                                count++
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            standardList.sortBy { it.showAtTime }
            advancedList.sortBy { it.startTimeMs }
            
            Log.w(TAG, " XML Parsed: Standard=${standardList.size}, Advanced=${advancedList.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, " XML parse error: ${e.message}", e)
        }
        
        return ParsedDanmaku(standardList, advancedList)
    }
    
    /**
     * 解析 DanmakuView 元数据 (x/v2/dm/web/view)
     */
    fun parseWebViewReply(data: ByteArray): DanmakuProto.DmWebViewReply {
        return DanmakuProto.parseWebViewReply(data)
    }

    /**
     * 从 JSON 格式内容解析高级弹幕 (Mode 7)
     * 格式: [startX, startY, mode, duration, content, rotateZ, rotateY]
     * 注意：部分高级弹幕的颜色可能在 JSON 中，也可以使用外层属性的颜色
     */
    private fun parseAdvancedDanmaku(jsonContent: String, startTimeMs: Long, color: Int): AdvancedDanmakuData? {
        try {
            // 简单的 JSON 数组检查
            if (!jsonContent.trim().startsWith("[")) return null
            
            val jsonArray = org.json.JSONArray(jsonContent)
            if (jsonArray.length() < 5) return null
            
            val startX = jsonArray.optDouble(0, 0.0).toFloat()
            val startY = jsonArray.optDouble(1, 0.0).toFloat()
            // index 2 represents mode string like "1-1", ignoring for now
            val duration = (jsonArray.optDouble(3, 1.0) * 1000).toLong() // usually seconds
            val content = jsonArray.optString(4, "")
            val rotateZ = jsonArray.optDouble(5, 0.0).toFloat()
            val rotateY = jsonArray.optDouble(6, 0.0).toFloat()
            
            return AdvancedDanmakuData(
                content = content,
                startTimeMs = startTimeMs,
                durationMs = duration,
                startX = startX,
                startY = startY,
                rotateZ = rotateZ,
                rotateY = rotateY,
                color = color // 使用传入的颜色
            )
        } catch (e: Exception) {
            // Log.d(TAG, "Not a valid Mode 7 JSON: $jsonContent")
            return null
        }
    }
    
    /**
     * 从属性字符串创建 TextData
     */
    private fun createTextData(pAttr: String, content: String): TextData? {
        try {
            val parts = pAttr.split(",")
            if (parts.size < 4) return null
            
            val biliType = parts[1].toIntOrNull() ?: 1
            
            // 过滤 Mode 7/8/9
            if (biliType >= 7) return null
            
            val timeSeconds = parts[0].toFloatOrNull() ?: 0f
            val timeMs = (timeSeconds * 1000).toLong()  // 转换为毫秒
            val fontSize = parts[2].toFloatOrNull() ?: 25f
            val colorInt = parts[3].toLongOrNull() ?: 0xFFFFFF
            val pool = parts.getOrNull(5)?.toIntOrNull() ?: 0
            val userHash = parts.getOrNull(6).orEmpty()
            val danmakuId = parts.getOrNull(7)?.toLongOrNull() ?: 0L
            
            val layerType = mapLayerType(biliType)
            
            return WeightedTextData().apply {
                this.danmakuId = danmakuId
                this.userHash = userHash
                this.pool = pool
                this.text = content
                this.showAtTime = timeMs
                this.layerType = layerType
                this.textColor = (colorInt.toInt() or 0xFF000000.toInt())
                this.textSize = fontSize
            }
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 映射 Bilibili 弹幕类型到 DanmakuRenderEngine LayerType
     */
    private fun mapLayerType(biliType: Int): Int = when (biliType) {
        1, 2, 3, 6 -> LAYER_TYPE_SCROLL    // 滚动弹幕
        4 -> LAYER_TYPE_BOTTOM_CENTER      // 底部固定
        5 -> LAYER_TYPE_TOP_CENTER         // 顶部固定
        else -> LAYER_TYPE_SCROLL
    }
}

