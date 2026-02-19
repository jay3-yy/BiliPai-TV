package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiType
import org.junit.Assert.assertEquals
import org.junit.Test

class MyFollowPolicyTest {

    @Test
    fun `anime and guochuang should map to bangumi follow type`() {
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, defaultMyFollowTypeForSeasonType(BangumiType.ANIME.value))
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, defaultMyFollowTypeForSeasonType(BangumiType.GUOCHUANG.value))
    }

    @Test
    fun `movie tv documentary and variety should map to cinema follow type`() {
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.MOVIE.value))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.TV_SHOW.value))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.DOCUMENTARY.value))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, defaultMyFollowTypeForSeasonType(BangumiType.VARIETY.value))
    }

    @Test
    fun `explicit request type should override current type`() {
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, resolveMyFollowRequestType(1, currentType = 2))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, resolveMyFollowRequestType(2, currentType = 1))
    }

    @Test
    fun `null request type should keep current type`() {
        assertEquals(MY_FOLLOW_TYPE_BANGUMI, resolveMyFollowRequestType(null, currentType = 1))
        assertEquals(MY_FOLLOW_TYPE_CINEMA, resolveMyFollowRequestType(null, currentType = 2))
    }
}
