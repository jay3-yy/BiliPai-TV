package com.android.purebilibili.feature.video.ui.pager

internal fun buildPortraitShareText(title: String, bvid: String): String {
    val url = "https://www.bilibili.com/video/$bvid"
    val normalizedTitle = title.trim()
    if (normalizedTitle.isEmpty()) return url
    return "【$normalizedTitle】\n$url"
}
