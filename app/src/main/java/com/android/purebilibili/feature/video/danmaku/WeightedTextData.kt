package com.android.purebilibili.feature.video.danmaku

import com.bytedance.danmaku.render.engine.render.draw.text.TextData

/**
 * 带有权重的文本弹幕数据
 * 
 * 扩展标准 TextData，携带 Bilibili API 返回的高级属性：
 * - weight: AI 智能评分 (0-10)，越高越重要
 * - pool: 弹幕池类型 (0:普通, 1:字幕, 2:特殊)
 */
class WeightedTextData : TextData() {
    var danmakuId: Long = 0L
    var userHash: String = ""
    var weight: Int = 0
    var pool: Int = 0
    
    // 调试用
    override fun toString(): String {
        return "WeightedTextData(id=$danmakuId, userHash='$userHash', text='$text', time=$showAtTime, weight=$weight)"
    }
}
