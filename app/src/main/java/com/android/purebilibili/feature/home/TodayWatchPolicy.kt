package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.math.ln

private data class CreatorAgg(
    val mid: Long,
    val name: String,
    var watchCount: Int = 0,
    var score: Double = 0.0
)

private data class ScoredCandidate(
    val video: VideoItem,
    val score: Double,
    val explanation: String
)

internal data class TodayWatchCreatorSignal(
    val mid: Long,
    val name: String = "",
    val score: Double,
    val watchCount: Int = 1
)

internal data class TodayWatchPenaltySignals(
    val consumedBvids: Set<String> = emptySet(),
    val dislikedBvids: Set<String> = emptySet(),
    val dislikedCreatorMids: Set<Long> = emptySet(),
    val dislikedKeywords: Set<String> = emptySet()
)

/**
 * 基于本地历史记录和当前推荐候选，构建“今日推荐单”。
 *
 * 核心流程：
 * 1) 从历史观看与画像信号里汇总 UP 主亲和度；
 * 2) 用热度/亲和度/新鲜度/模式/夜间/负反馈等信号给候选视频打分；
 * 3) 在高分基础上做多样化队列，避免连续出现同一位 UP 主。
 */
internal fun buildTodayWatchPlan(
    historyVideos: List<VideoItem>,
    candidateVideos: List<VideoItem>,
    mode: TodayWatchMode,
    eyeCareNightActive: Boolean,
    nowEpochSec: Long = System.currentTimeMillis() / 1000L,
    upRankLimit: Int = 5,
    queueLimit: Int = 20,
    creatorSignals: List<TodayWatchCreatorSignal> = emptyList(),
    penaltySignals: TodayWatchPenaltySignals = TodayWatchPenaltySignals()
): TodayWatchPlan {
    // 仅保留有效历史记录，并按观看时间倒序。
    val cleanedHistory = historyVideos
        .filter { it.bvid.isNotBlank() && it.owner.mid > 0L }
        .sortedByDescending { it.view_at }

    // 根据真实观看行为聚合 UP 主偏好分。
    val creatorMap = linkedMapOf<Long, CreatorAgg>()
    cleanedHistory.forEach { item ->
        val mid = item.owner.mid
        val agg = creatorMap.getOrPut(mid) {
            CreatorAgg(mid = mid, name = item.owner.name.ifBlank { "UP主$mid" })
        }
        val completion = estimateCompletionRatio(item)
        val recencyBonus = recencyBonus(item.view_at, nowEpochSec)
        agg.watchCount += 1
        agg.score += 1.0 + completion * 1.2 + recencyBonus
    }

    // 合并持久化画像信号（跨会话偏好记忆）。
    creatorSignals
        .filter { it.mid > 0L }
        .forEach { signal ->
            val agg = creatorMap.getOrPut(signal.mid) {
                CreatorAgg(
                    mid = signal.mid,
                    name = signal.name.ifBlank { "UP主${signal.mid}" }
                )
            }
            agg.watchCount += signal.watchCount.coerceAtLeast(1)
            agg.score += signal.score
        }

    val creatorAffinity = creatorMap.mapValues { it.value.score }
    val seenBvids = cleanedHistory.map { it.bvid }.toSet()
    // 候选预处理：过滤无效项并去重。
    val dedupCandidates = candidateVideos
        .asSequence()
        .filter { it.bvid.isNotBlank() && it.title.isNotBlank() }
        .filter { it.bvid !in penaltySignals.consumedBvids }
        .distinctBy { it.bvid }
        .toList()

    // 给候选逐条打分，并保留用于 UI 展示的可读解释。
    val scoredCandidates = dedupCandidates
        .map { video ->
            val affinity = creatorAffinity[video.owner.mid] ?: 0.0
            val score = scoreCandidateVideo(
                video = video,
                creatorAffinity = affinity,
                mode = mode,
                eyeCareNightActive = eyeCareNightActive,
                alreadySeen = video.bvid in seenBvids,
                nowEpochSec = nowEpochSec,
                penaltySignals = penaltySignals
            )
            val explanation = buildRecommendationExplanation(
                video = video,
                mode = mode,
                eyeCareNightActive = eyeCareNightActive,
                creatorAffinity = affinity
            )
            ScoredCandidate(video = video, score = score, explanation = explanation)
        }
        .sortedByDescending { it.score }

    // 生成卡片头部“偏好 UP 榜”。
    val rankedUp = creatorMap.values
        .sortedByDescending { it.score }
        .take(upRankLimit.coerceIn(1, 20))
        .map {
            TodayUpRank(
                mid = it.mid,
                name = it.name,
                score = it.score,
                watchCount = it.watchCount
            )
        }

    // 在纯分数排序之上应用“多样化”策略。
    val queue = buildDiverseQueue(
        scoredCandidates = scoredCandidates,
        queueLimit = queueLimit.coerceIn(1, 60)
    )
    val explanationByBvid = queue.associate { video ->
        val explanation = scoredCandidates
            .firstOrNull { it.video.bvid == video.bvid }
            ?.explanation
            .orEmpty()
        video.bvid to explanation
    }

    return TodayWatchPlan(
        mode = mode,
        upRanks = rankedUp,
        videoQueue = queue,
        explanationByBvid = explanationByBvid,
        historySampleCount = cleanedHistory.size,
        nightSignalUsed = eyeCareNightActive,
        generatedAt = System.currentTimeMillis()
    )
}

private fun scoreCandidateVideo(
    video: VideoItem,
    creatorAffinity: Double,
    mode: TodayWatchMode,
    eyeCareNightActive: Boolean,
    alreadySeen: Boolean,
    nowEpochSec: Long,
    penaltySignals: TodayWatchPenaltySignals
): Double {
    val durationMin = (video.duration.coerceAtLeast(0) / 60.0).coerceAtMost(180.0)
    // “刺激度”近似指标：弹幕密度（弹幕数 / 播放量）。
    val intensity = video.stat.danmaku.toDouble() / (video.stat.view.toDouble().coerceAtLeast(1.0))
    val title = video.title.lowercase()

    // 两种模式共享的基础信号。
    val baseScore = ln(video.stat.view.toDouble() + 1.0) * 0.45
    val creatorScore = ln(creatorAffinity + 1.0) * 2.1
    val freshnessScore = freshnessScore(video.pubdate, nowEpochSec)
    val seenPenalty = if (alreadySeen) -2.6 else 0.0
    val calmScore = when {
        intensity < 0.004 -> 1.0
        intensity < 0.01 -> 0.3
        else -> -1.0
    }

    // 模式偏好信号（轻松看 / 学习看）。
    val modeScore = when (mode) {
        TodayWatchMode.RELAX -> {
            durationRelaxScore(durationMin) +
                keywordBonus(
                    title = title,
                    positiveKeywords = RELAX_KEYWORDS,
                    negativeKeywords = LEARN_KEYWORDS
                ) +
                calmScore
        }
        TodayWatchMode.LEARN -> {
            durationLearnScore(durationMin) +
                keywordBonus(
                    title = title,
                    positiveKeywords = LEARN_KEYWORDS,
                    negativeKeywords = RELAX_KEYWORDS
                ) +
                if (durationMin >= 10.0) 0.6 else -0.2
        }
    }

    // 夜间模式下，降低超长与高刺激内容权重。
    val nightScore = if (eyeCareNightActive) {
        val durationPenalty = when {
            durationMin <= 15.0 -> 1.2
            durationMin <= 25.0 -> 0.2
            else -> -((durationMin - 25.0) / 10.0).coerceAtMost(3.0)
        }
        val intensityPenalty = when {
            intensity < 0.006 -> 0.6
            intensity < 0.012 -> 0.0
            else -> -1.1
        }
        durationPenalty + intensityPenalty
    } else {
        0.0
    }

    // 用户负反馈始终生效，并给予明确惩罚分。
    val feedbackPenalty = feedbackPenalty(video, title, penaltySignals)

    return baseScore + creatorScore + freshnessScore + seenPenalty + modeScore + nightScore + feedbackPenalty
}

private fun buildDiverseQueue(
    scoredCandidates: List<ScoredCandidate>,
    queueLimit: Int
): List<VideoItem> {
    if (scoredCandidates.isEmpty()) return emptyList()
    val remaining = scoredCandidates.toMutableList()
    val queue = mutableListOf<VideoItem>()
    val creatorUsedCount = mutableMapOf<Long, Int>()
    var lastCreatorMid: Long? = null

    // 贪心选取：
    // 每轮选“调整后分数”最高的视频；
    // 调整项会抑制同一 UP 连续出现，并轻微奖励“新 UP”。
    while (queue.size < queueLimit && remaining.isNotEmpty()) {
        var bestIndex = 0
        var bestAdjustedScore = Double.NEGATIVE_INFINITY

        remaining.forEachIndexed { index, candidate ->
            val mid = candidate.video.owner.mid
            val usedCount = creatorUsedCount[mid] ?: 0
            val sameCreatorConsecutivePenalty = if (mid > 0L && lastCreatorMid == mid) 1.15 else 0.0
            val creatorRepeatPenalty = usedCount * 0.75
            val creatorNoveltyBonus = if (mid > 0L && usedCount == 0) 0.35 else 0.0
            val adjusted = candidate.score - sameCreatorConsecutivePenalty - creatorRepeatPenalty + creatorNoveltyBonus

            if (adjusted > bestAdjustedScore) {
                bestAdjustedScore = adjusted
                bestIndex = index
            }
        }

        val picked = remaining.removeAt(bestIndex).video
        queue += picked
        val mid = picked.owner.mid
        creatorUsedCount[mid] = (creatorUsedCount[mid] ?: 0) + 1
        lastCreatorMid = mid
    }

    return queue
}

private fun freshnessScore(pubdate: Long, nowEpochSec: Long): Double {
    if (pubdate <= 0L) return 0.0
    val days = ((nowEpochSec - pubdate).coerceAtLeast(0L) / 86_400.0)
    return when {
        days <= 1.0 -> 0.8
        days <= 3.0 -> 0.55
        days <= 7.0 -> 0.3
        days <= 30.0 -> 0.1
        else -> -0.05
    }
}

private fun feedbackPenalty(
    video: VideoItem,
    title: String,
    signals: TodayWatchPenaltySignals
): Double {
    // 惩罚强度顺序：
    // 明确不感兴趣视频 > 不感兴趣 UP > 不感兴趣关键词。
    val dislikedBvidPenalty = if (video.bvid in signals.dislikedBvids) -3.2 else 0.0
    val dislikedCreatorPenalty = if (video.owner.mid in signals.dislikedCreatorMids) -2.4 else 0.0
    val keywordHit = signals.dislikedKeywords.count { keyword ->
        keyword.isNotBlank() && title.contains(keyword.lowercase())
    }
    val dislikedKeywordPenalty = (keywordHit * -0.7).coerceAtLeast(-2.8)
    return dislikedBvidPenalty + dislikedCreatorPenalty + dislikedKeywordPenalty
}

private fun buildRecommendationExplanation(
    video: VideoItem,
    mode: TodayWatchMode,
    eyeCareNightActive: Boolean,
    creatorAffinity: Double
): String {
    val parts = mutableListOf<String>()
    parts += when (mode) {
        TodayWatchMode.RELAX -> "轻松向"
        TodayWatchMode.LEARN -> "学习向"
    }

    val durationMin = video.duration.coerceAtLeast(0) / 60.0
    when {
        durationMin in 3.0..15.0 -> parts += "短时长"
        durationMin in 15.0..35.0 -> parts += "中时长"
        durationMin > 35.0 -> parts += "长时长"
    }

    val intensity = video.stat.danmaku.toDouble() / video.stat.view.coerceAtLeast(1).toDouble()
    if (eyeCareNightActive) {
        if (durationMin <= 25.0 && intensity < 0.012) {
            parts += "夜间友好"
        } else {
            parts += "夜间已调权"
        }
    }

    if (creatorAffinity > 0.8) {
        parts += "偏好UP"
    }

    return parts.distinct().joinToString(" · ")
}

private fun durationRelaxScore(durationMin: Double): Double {
    return when {
        durationMin < 2.0 -> -0.2
        durationMin <= 12.0 -> 1.4
        durationMin <= 20.0 -> 0.6
        durationMin <= 35.0 -> -0.1
        else -> -0.9
    }
}

private fun durationLearnScore(durationMin: Double): Double {
    return when {
        durationMin < 5.0 -> -0.6
        durationMin <= 12.0 -> 0.5
        durationMin <= 35.0 -> 1.5
        durationMin <= 55.0 -> 0.8
        else -> -0.2
    }
}

private fun estimateCompletionRatio(item: VideoItem): Double {
    if (item.progress < 0) return 0.35
    if (item.duration <= 0) {
        return (item.progress / 600.0).coerceIn(0.0, 1.0)
    }
    return (item.progress.toDouble() / item.duration.toDouble()).coerceIn(0.0, 1.0)
}

private fun recencyBonus(viewAt: Long, nowEpochSec: Long): Double {
    if (viewAt <= 0L) return 0.25
    val days = ((nowEpochSec - viewAt).coerceAtLeast(0L) / 86_400.0)
    return when {
        days <= 1.0 -> 1.0
        days <= 3.0 -> 0.8
        days <= 7.0 -> 0.6
        days <= 30.0 -> 0.35
        else -> 0.15
    }
}

private fun keywordBonus(
    title: String,
    positiveKeywords: List<String>,
    negativeKeywords: List<String>
): Double {
    // 限幅，避免关键词分值压过结构化信号。
    val positive = positiveKeywords.count { title.contains(it) } * 0.55
    val negative = negativeKeywords.count { title.contains(it) } * 0.35
    return (positive - negative).coerceIn(-1.2, 1.8)
}

private val RELAX_KEYWORDS = listOf(
    "音乐", "vlog", "日常", "搞笑", "轻松", "治愈", "asmr", "旅行", "美食", "游戏"
)

private val LEARN_KEYWORDS = listOf(
    "教程", "科普", "知识", "学习", "原理", "实战", "复盘", "编程", "数学", "英语", "课程", "技术", "分析", "入门", "进阶"
)
