package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiType

const val MY_FOLLOW_TYPE_BANGUMI = 1
const val MY_FOLLOW_TYPE_CINEMA = 2

fun defaultMyFollowTypeForSeasonType(seasonType: Int): Int {
    return when (seasonType) {
        BangumiType.ANIME.value, BangumiType.GUOCHUANG.value -> MY_FOLLOW_TYPE_BANGUMI
        else -> MY_FOLLOW_TYPE_CINEMA
    }
}

fun resolveMyFollowRequestType(requestedType: Int?, currentType: Int): Int {
    return requestedType ?: currentType
}
