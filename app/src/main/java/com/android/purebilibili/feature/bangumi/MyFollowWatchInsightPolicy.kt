package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.FollowBangumiItem
import kotlin.math.roundToInt

data class MyFollowWatchInsight(
    val loadedCount: Int,
    val inProgressCount: Int,
    val completedCount: Int,
    val notStartedCount: Int,
    val updatedCount: Int,
    val membershipOnlyCount: Int,
    val averageProgressPercent: Int,
    val estimatedWatchedEpisodes: Int,
    val estimatedTotalEpisodes: Int
)

private val WATCHING_EPISODE_REGEX = Regex("""看到第\s*(\d+)\s*[话集]?""")
private val WATCHING_EPISODE_FALLBACK_REGEX = Regex("""第\s*(\d+)\s*[话集]""")

private fun isCompletedProgressText(progress: String): Boolean {
    return progress.contains("已看完") || progress.contains("看完")
}

internal fun parseWatchedEpisodeFromProgress(progress: String): Int? {
    val normalized = progress.trim()
    if (normalized.isEmpty()) return null
    val direct = WATCHING_EPISODE_REGEX.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
    if (direct != null) return direct
    return WATCHING_EPISODE_FALLBACK_REGEX.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()
}

fun buildMyFollowWatchInsight(items: List<FollowBangumiItem>): MyFollowWatchInsight {
    if (items.isEmpty()) {
        return MyFollowWatchInsight(
            loadedCount = 0,
            inProgressCount = 0,
            completedCount = 0,
            notStartedCount = 0,
            updatedCount = 0,
            membershipOnlyCount = 0,
            averageProgressPercent = 0,
            estimatedWatchedEpisodes = 0,
            estimatedTotalEpisodes = 0
        )
    }

    var inProgress = 0
    var completed = 0
    var notStarted = 0
    var updated = 0
    var membershipOnly = 0
    var watchedEpisodes = 0
    var totalEpisodes = 0
    var ratioSum = 0f
    var ratioCount = 0

    items.forEach { item ->
        val progressText = item.progress.trim()
        val total = item.total.coerceAtLeast(0)
        val parsedEpisode = parseWatchedEpisodeFromProgress(progressText)
        val completedByText = isCompletedProgressText(progressText)
        val completedByEpisode = total > 0 && parsedEpisode != null && parsedEpisode >= total
        val isCompleted = completedByText || completedByEpisode
        val isInProgress = !isCompleted && (parsedEpisode ?: 0) > 0

        when {
            isCompleted -> completed++
            isInProgress -> inProgress++
            else -> notStarted++
        }

        if (item.newEp?.indexShow?.contains("更新") == true) {
            updated++
        }
        if (item.badge.contains("会员")) {
            membershipOnly++
        }

        if (total > 0) {
            val watched = when {
                isCompleted -> total
                parsedEpisode != null -> parsedEpisode.coerceIn(0, total)
                else -> 0
            }
            watchedEpisodes += watched
            totalEpisodes += total
            ratioSum += watched.toFloat() / total.toFloat()
            ratioCount++
        }
    }

    val averagePercent = if (ratioCount == 0) 0 else (ratioSum / ratioCount * 100f).roundToInt().coerceIn(0, 100)
    return MyFollowWatchInsight(
        loadedCount = items.size,
        inProgressCount = inProgress,
        completedCount = completed,
        notStartedCount = notStarted,
        updatedCount = updated,
        membershipOnlyCount = membershipOnly,
        averageProgressPercent = averagePercent,
        estimatedWatchedEpisodes = watchedEpisodes,
        estimatedTotalEpisodes = totalEpisodes
    )
}
