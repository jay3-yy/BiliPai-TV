// 文件路径: domain/usecase/SponsorBlockUseCase.kt
package com.android.purebilibili.domain.usecase

import android.content.Context
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.data.repository.SponsorBlockRepository
import kotlinx.coroutines.flow.first

/**
 * 空降跳过检查结果
 */
sealed class SponsorCheckResult {
    object NoSegment : SponsorCheckResult()
    data class ShowButton(val segment: SponsorSegment) : SponsorCheckResult()
    data class AutoSkipped(val segment: SponsorSegment) : SponsorCheckResult()
}

/**
 * 空降助手 (SponsorBlock) UseCase
 * 
 * 职责：
 * 1. 加载空降片段数据
 * 2. 检查当前播放位置是否在空降片段内
 * 3. 根据设置决定自动跳过或显示按钮
 */
class SponsorBlockUseCase {
    
    private val skippedSegmentIds = mutableSetOf<String>()
    
    /**
     * 加载视频的空降片段
     * 
     * @param bvid 视频 BV 号
     * @return 空降片段列表
     */
    suspend fun loadSegments(bvid: String): List<SponsorSegment> {
        return try {
            val segments = SponsorBlockRepository.getSegments(bvid)
            skippedSegmentIds.clear()
            segments
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.w("SponsorBlockUseCase", "Failed to load segments: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 检查当前位置是否需要跳过
     * 
     * @param context Android Context (用于读取设置)
     * @param segments 空降片段列表
     * @param currentPositionMs 当前播放位置（毫秒）
     * @return 检查结果
     */
    suspend fun checkSponsorAtPosition(
        context: Context,
        segments: List<SponsorSegment>,
        currentPositionMs: Long
    ): SponsorCheckResult {
        if (segments.isEmpty()) return SponsorCheckResult.NoSegment
        
        val segment = SponsorBlockRepository.findSegmentAtPosition(segments, currentPositionMs)
            ?: return SponsorCheckResult.NoSegment
        
        if (segment.UUID in skippedSegmentIds) {
            return SponsorCheckResult.NoSegment
        }
        
        val autoSkip = SettingsManager.getSponsorBlockAutoSkip(context).first()
        
        return if (autoSkip) {
            skippedSegmentIds.add(segment.UUID)
            SponsorCheckResult.AutoSkipped(segment)
        } else {
            SponsorCheckResult.ShowButton(segment)
        }
    }
    
    /**
     * 标记片段已跳过（手动跳过时调用）
     */
    fun markSegmentSkipped(segmentId: String) {
        skippedSegmentIds.add(segmentId)
    }
    
    /**
     * 忽略当前片段（用户选择不跳过）
     */
    fun dismissSegment(segmentId: String) {
        skippedSegmentIds.add(segmentId)
    }
    
    /**
     * 重置已跳过记录（切换视频时调用）
     */
    fun reset() {
        skippedSegmentIds.clear()
    }
}
