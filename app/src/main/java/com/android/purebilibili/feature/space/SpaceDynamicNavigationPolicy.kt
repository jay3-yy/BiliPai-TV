package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceDynamicItem

internal sealed interface SpaceDynamicClickAction {
    data class OpenVideo(val bvid: String) : SpaceDynamicClickAction
    data class OpenWeb(val url: String) : SpaceDynamicClickAction
    data object None : SpaceDynamicClickAction
}

internal fun resolveSpaceDynamicClickAction(dynamic: SpaceDynamicItem): SpaceDynamicClickAction {
    val bvid = dynamic.modules.module_dynamic?.major?.archive?.bvid
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (bvid != null) {
        return SpaceDynamicClickAction.OpenVideo(bvid)
    }

    val dynamicId = dynamic.id_str.trim().takeIf { it.isNotEmpty() } ?: return SpaceDynamicClickAction.None
    return SpaceDynamicClickAction.OpenWeb("https://m.bilibili.com/dynamic/$dynamicId")
}
