package com.android.purebilibili.feature.bangumi

/**
 * 在通过 ep_id 进入详情时，路由中的 seasonId 可能是 0 或无效值。
 * 交互动作（追番、跳转播放）应优先使用详情接口返回的真实 seasonId。
 */
fun resolveBangumiActionSeasonId(routeSeasonId: Long, detailSeasonId: Long): Long {
    return if (detailSeasonId > 0) detailSeasonId else routeSeasonId
}
