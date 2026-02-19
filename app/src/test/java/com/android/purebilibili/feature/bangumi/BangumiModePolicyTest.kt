package com.android.purebilibili.feature.bangumi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BangumiModePolicyTest {

    @Test
    fun `top modes should keep list timeline order`() {
        assertEquals(
            listOf(BangumiDisplayMode.LIST, BangumiDisplayMode.TIMELINE),
            resolveBangumiTopModes()
        )
    }

    @Test
    fun `top modes should not include my follow`() {
        assertFalse(resolveBangumiTopModes().contains(BangumiDisplayMode.MY_FOLLOW))
    }
}
