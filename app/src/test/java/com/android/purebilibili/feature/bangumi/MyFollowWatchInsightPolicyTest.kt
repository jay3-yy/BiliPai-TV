package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.FollowBangumiItem
import com.android.purebilibili.data.model.response.NewEpInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class MyFollowWatchInsightPolicyTest {

    @Test
    fun `build watch insight should derive status buckets and progress`() {
        val insight = buildMyFollowWatchInsight(
            listOf(
                FollowBangumiItem(
                    seasonId = 1,
                    total = 24,
                    progress = "看到第12话",
                    newEp = NewEpInfo(indexShow = "更新至第24话")
                ),
                FollowBangumiItem(
                    seasonId = 2,
                    total = 13,
                    progress = "全13话"
                ),
                FollowBangumiItem(
                    seasonId = 3,
                    total = 39,
                    progress = "已看完"
                ),
                FollowBangumiItem(
                    seasonId = 4,
                    total = 0,
                    progress = "",
                    badge = "大会员"
                )
            )
        )

        assertEquals(4, insight.loadedCount)
        assertEquals(1, insight.inProgressCount)
        assertEquals(1, insight.completedCount)
        assertEquals(2, insight.notStartedCount)
        assertEquals(1, insight.updatedCount)
        assertEquals(1, insight.membershipOnlyCount)
        assertEquals(50, insight.averageProgressPercent)
    }

    @Test
    fun `progress parser should support clock format text`() {
        assertEquals(1, parseWatchedEpisodeFromProgress("看到第1话 23:08"))
        assertEquals(12, parseWatchedEpisodeFromProgress("看到第12集"))
    }
}
