// 文件路径: core/plugin/json/RuleEngine.kt
package com.android.purebilibili.core.plugin.json

import com.android.purebilibili.core.plugin.DanmakuItem
import com.android.purebilibili.core.plugin.DanmakuStyle
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.VideoItem
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.json.*

private const val TAG = "RuleEngine"

/**
 * 🔧 规则引擎
 * 
 * 评估 JSON 规则并执行相应动作。
 *  支持 AND/OR 复合条件的递归评估。
 */
object RuleEngine {
    
    /**
     * 评估视频是否应该显示
     */
    fun shouldShowVideo(video: VideoItem, rules: List<Rule>): Boolean {
        for (rule in rules) {
            if (rule.action != RuleAction.HIDE) continue
            
            val condition = rule.toCondition() ?: continue
            if (evaluateCondition(condition) { field -> getVideoFieldValue(video, field) }) {
                Logger.d(TAG, "🚫 隐藏视频: ${video.title} (规则匹配)")
                return false
            }
        }
        return true
    }
    
    /**
     * 评估弹幕是否应该显示
     */
    fun shouldShowDanmaku(danmaku: DanmakuItem, rules: List<Rule>): Boolean {
        for (rule in rules) {
            if (rule.action != RuleAction.HIDE) continue
            
            val condition = rule.toCondition() ?: continue
            if (evaluateCondition(condition) { field -> getDanmakuFieldValue(danmaku, field) }) {
                return false
            }
        }
        return true
    }
    
    /**
     * 获取弹幕高亮样式
     */
    fun getDanmakuHighlightStyle(danmaku: DanmakuItem, rules: List<Rule>): DanmakuStyle? {
        for (rule in rules) {
            if (rule.action != RuleAction.HIGHLIGHT) continue
            
            val condition = rule.toCondition() ?: continue
            if (evaluateCondition(condition) { field -> getDanmakuFieldValue(danmaku, field) }) {
                return rule.style?.toDanmakuStyle()
            }
        }
        return null
    }
    
    // ============  复合条件评估 ============
    
    /**
     * 递归评估条件表达式
     * 
     * @param condition 条件对象（Simple/And/Or）
     * @param fieldValueGetter 字段值获取函数
     * @return 条件是否满足
     */
    private fun evaluateCondition(
        condition: Condition,
        fieldValueGetter: (String) -> Any?
    ): Boolean {
        return when (condition) {
            is Condition.Simple -> {
                val fieldValue = fieldValueGetter(condition.field)
                evaluatePrimitive(fieldValue, condition.op, condition.value)
            }
            is Condition.And -> {
                // AND: 所有子条件都必须满足
                condition.conditions.all { child -> evaluateCondition(child, fieldValueGetter) }
            }
            is Condition.Or -> {
                // OR: 任一子条件满足即可
                condition.conditions.any { child -> evaluateCondition(child, fieldValueGetter) }
            }
        }
    }
    
    // ============ 字段值获取 ============
    
    /**
     * 获取视频字段值
     */
    private fun getVideoFieldValue(video: VideoItem, field: String): Any? {
        return when (field) {
            "title" -> video.title
            "duration" -> video.duration
            "bvid" -> video.bvid
            "owner.mid" -> video.owner?.mid
            "owner.name" -> video.owner?.name
            "stat.view" -> video.stat?.view
            "stat.like" -> video.stat?.like
            "stat.danmaku" -> video.stat?.danmaku
            "stat.coin" -> video.stat?.coin
            "stat.favorite" -> video.stat?.favorite
            else -> null
        }
    }
    
    /**
     * 获取弹幕字段值
     */
    private fun getDanmakuFieldValue(danmaku: DanmakuItem, field: String): Any? {
        return when (field) {
            "content" -> danmaku.content
            "userId" -> danmaku.userId
            "type" -> danmaku.type
            else -> null
        }
    }
    
    // ============ 基础条件评估 ============
    
    /**
     * 评估基础条件（单个字段比较）
     */
    private fun evaluatePrimitive(fieldValue: Any?, op: String, ruleValue: JsonElement): Boolean {
        if (fieldValue == null) return false
        
        return when (op) {
            RuleOperator.EQ -> compareEquals(fieldValue, ruleValue)
            RuleOperator.NE -> !compareEquals(fieldValue, ruleValue)
            RuleOperator.LT -> compareNumber(fieldValue, ruleValue) { a, b -> a < b }
            RuleOperator.LE -> compareNumber(fieldValue, ruleValue) { a, b -> a <= b }
            RuleOperator.GT -> compareNumber(fieldValue, ruleValue) { a, b -> a > b }
            RuleOperator.GE -> compareNumber(fieldValue, ruleValue) { a, b -> a >= b }
            RuleOperator.CONTAINS -> fieldValue.toString().contains(ruleValue.jsonPrimitive.content, ignoreCase = true)
            RuleOperator.STARTS_WITH -> fieldValue.toString().startsWith(ruleValue.jsonPrimitive.content, ignoreCase = true)
            RuleOperator.ENDS_WITH -> fieldValue.toString().endsWith(ruleValue.jsonPrimitive.content, ignoreCase = true)
            RuleOperator.REGEX -> {
                try {
                    Regex(ruleValue.jsonPrimitive.content).containsMatchIn(fieldValue.toString())
                } catch (e: Exception) {
                    false
                }
            }
            RuleOperator.IN -> {
                if (ruleValue is JsonArray) {
                    ruleValue.any { compareEquals(fieldValue, it) }
                } else false
            }
            else -> false
        }
    }
    
    private fun compareEquals(fieldValue: Any, ruleValue: JsonElement): Boolean {
        return when (fieldValue) {
            is String -> fieldValue == ruleValue.jsonPrimitive.contentOrNull
            is Int -> fieldValue == ruleValue.jsonPrimitive.intOrNull
            is Long -> fieldValue == ruleValue.jsonPrimitive.longOrNull
            is Double -> fieldValue == ruleValue.jsonPrimitive.doubleOrNull
            is Boolean -> fieldValue == ruleValue.jsonPrimitive.booleanOrNull
            else -> fieldValue.toString() == ruleValue.jsonPrimitive.contentOrNull
        }
    }
    
    private fun compareNumber(fieldValue: Any, ruleValue: JsonElement, comparator: (Double, Double) -> Boolean): Boolean {
        val a = when (fieldValue) {
            is Int -> fieldValue.toDouble()
            is Long -> fieldValue.toDouble()
            is Double -> fieldValue
            is Float -> fieldValue.toDouble()
            else -> return false
        }
        val b = ruleValue.jsonPrimitive.doubleOrNull ?: return false
        return comparator(a, b)
    }
    
    /**
     * 转换高亮样式
     */
    private fun HighlightStyle.toDanmakuStyle(): DanmakuStyle {
        val textColor = color?.let { 
            try {
                Color(android.graphics.Color.parseColor(it))
            } catch (e: Exception) { null }
        }
        return DanmakuStyle(
            textColor = textColor,
            borderColor = null,
            backgroundColor = null,
            bold = bold,
            scale = scale
        )
    }
}

