package com.android.purebilibili.feature.bangumi

fun resolveBangumiTopModes(): List<BangumiDisplayMode> {
    return listOf(
        BangumiDisplayMode.LIST,
        BangumiDisplayMode.TIMELINE
    )
}
