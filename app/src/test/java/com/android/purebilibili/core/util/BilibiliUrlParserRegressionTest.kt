package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BilibiliUrlParserRegressionTest {

    @Test
    fun parse_extractsBvidFromVideoUrl() {
        val result = BilibiliUrlParser.parse("https://www.bilibili.com/video/BV1xx411c7mD")
        assertTrue(result.isValid)
        assertEquals("BV1xx411c7mD", result.getVideoId())
    }

    @Test
    fun extractUrls_findsMultipleLinksInSharedText() {
        val text = "看看这个 https://www.bilibili.com/video/BV1xx411c7mD 还有这个 https://b23.tv/abc123"
        val urls = BilibiliUrlParser.extractUrls(text)
        assertEquals(2, urls.size)
        assertEquals("https://www.bilibili.com/video/BV1xx411c7mD", urls[0])
        assertEquals("https://b23.tv/abc123", urls[1])
    }
}
