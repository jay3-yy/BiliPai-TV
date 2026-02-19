package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.MyFollowBangumiData
import com.android.purebilibili.data.model.response.UserStatus
import com.android.purebilibili.data.repository.BangumiRepository

internal const val FOLLOW_PRELOAD_PAGE_SIZE = 30
internal const val FOLLOW_PRELOAD_MAX_PAGES = 30

internal data class FollowPreloadResult(
    val total: Int,
    val requestSucceeded: Boolean
)

internal fun isBangumiFollowed(userStatus: UserStatus?): Boolean {
    if (userStatus == null) return false
    return userStatus.follow == 1 || userStatus.followStatus > 0
}

internal fun resolveFollowPreloadPageCount(
    total: Int,
    pageSize: Int,
    maxPages: Int
): Int {
    if (total <= 0 || pageSize <= 0 || maxPages <= 0) return 0
    val pages = (total + pageSize - 1) / pageSize
    return pages.coerceAtMost(maxPages)
}

internal suspend fun preloadFollowedSeasonsForType(
    type: Int,
    followedSeasonIds: MutableSet<Long>,
    pageSize: Int = FOLLOW_PRELOAD_PAGE_SIZE,
    maxPages: Int = FOLLOW_PRELOAD_MAX_PAGES,
    fetchPage: suspend (type: Int, page: Int, pageSize: Int) -> Result<MyFollowBangumiData> =
        { requestType, page, ps ->
            BangumiRepository.getMyFollowBangumi(type = requestType, page = page, pageSize = ps)
        }
): FollowPreloadResult {
    if (pageSize <= 0 || maxPages <= 0) {
        return FollowPreloadResult(total = 0, requestSucceeded = false)
    }

    var total = 0
    var requestSucceeded = false

    var effectivePageSize = pageSize

    for (page in 1..maxPages) {
        val data = fetchPage(type, page, pageSize).getOrElse {
            return if (requestSucceeded) {
                FollowPreloadResult(total = total, requestSucceeded = true)
            } else {
                FollowPreloadResult(total = 0, requestSucceeded = false)
            }
        }

        requestSucceeded = true
        if (page == 1) {
            total = data.total.coerceAtLeast(0)
            if (data.ps > 0) {
                effectivePageSize = data.ps
            }
        }

        val list = data.list.orEmpty()
        if (list.isNotEmpty()) {
            followedSeasonIds.addAll(list.map { it.seasonId })
        }

        val responsePageSize = if (data.ps > 0) data.ps else effectivePageSize
        val reachedEndByCount = total > 0 && page * responsePageSize >= total
        val reachedEndByPageSize = list.size < responsePageSize
        if (list.isEmpty() || reachedEndByCount || reachedEndByPageSize) {
            break
        }
    }

    return FollowPreloadResult(
        total = total,
        requestSucceeded = requestSucceeded
    )
}
