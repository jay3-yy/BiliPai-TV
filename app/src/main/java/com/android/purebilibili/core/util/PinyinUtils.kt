package com.android.purebilibili.core.util

import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Locale

object PinyinUtils {

    /**
     * 智能匹配：检查文本是否包含查询字符串 (支持中文原声、全拼、拼音首字母)
     */
    fun matches(text: String, query: String): Boolean {
        if (query.isBlank()) return true
        
        // 1. 原始文本匹配 (最快)
        if (text.contains(query, ignoreCase = true)) return true
        
        // 2. 转换拼音 (全拼 + 首字母)
        val (fullPinyin, firstLetters) = toPinyin(text)
        
        // 3. 检查匹配
        return fullPinyin.contains(query, ignoreCase = true) || 
               firstLetters.contains(query, ignoreCase = true)
    }

    /**
     * 转换字符串为拼音元组 (全拼, 首字母)
     */
    private fun toPinyin(text: String): Pair<String, String> {
        val fullSb = StringBuilder()
        val firstSb = StringBuilder()
        
        for (c in text) {
            // 转换中文字符
            val pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c)
            if (pinyinArray != null && pinyinArray.isNotEmpty()) {
                // 取第一个读音 (如 "zhang1")
                val pinyin = pinyinArray[0].replace(Regex("\\d"), "") // 去掉声调 -> "zhang"
                
                fullSb.append(pinyin)
                if (pinyin.isNotEmpty()) {
                    firstSb.append(pinyin[0]) // 取首字母 -> "z"
                }
            } else {
                // 非中文直接保留 (如英文、数字)
                check(c.toString().length == 1)
                fullSb.append(c)
                firstSb.append(c)
            }
        }
        return fullSb.toString() to firstSb.toString()
    }
}
