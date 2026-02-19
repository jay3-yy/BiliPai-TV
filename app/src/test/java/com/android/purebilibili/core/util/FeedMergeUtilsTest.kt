package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedMergeUtilsTest {

    @Test
    fun prependDistinctByKeyOnlyPrependsNewItems() {
        val existing = listOf("a", "b", "c")
        val incoming = listOf("d", "b", "a")

        val merged = prependDistinctByKey(existing, incoming) { it }

        assertEquals(listOf("d", "a", "b", "c"), merged)
    }

    @Test
    fun prependDistinctByKeyDeduplicatesIncoming() {
        val existing = listOf("x")
        val incoming = listOf("a", "a", "b", "b")

        val merged = prependDistinctByKey(existing, incoming) { it }

        assertEquals(listOf("a", "b", "x"), merged)
    }

    @Test
    fun appendDistinctByKeyOnlyAppendsNewItems() {
        val existing = listOf("a", "b")
        val incoming = listOf("b", "c", "a", "d")

        val merged = appendDistinctByKey(existing, incoming) { it }

        assertEquals(listOf("a", "b", "c", "d"), merged)
    }

    @Test
    fun appendDistinctByKeyDeduplicatesWhenExistingEmpty() {
        val merged = appendDistinctByKey(emptyList(), listOf("a", "a", "b")) { it }

        assertEquals(listOf("a", "b"), merged)
    }
}
