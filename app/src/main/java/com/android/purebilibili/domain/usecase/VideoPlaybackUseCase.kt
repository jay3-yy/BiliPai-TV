// 文件路径: domain/usecase/VideoPlaybackUseCase.kt
package com.android.purebilibili.domain.usecase

import com.android.purebilibili.core.cache.PlayUrlCache
import com.android.purebilibili.data.model.response.DashAudio
import com.android.purebilibili.data.model.response.DashVideo
import com.android.purebilibili.data.model.response.PlayUrlData
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.model.response.getBestAudio
import com.android.purebilibili.data.model.response.getBestVideo
import com.android.purebilibili.data.repository.VideoRepository

/**
 * 视频播放数据封装类
 * 包含视频详情、播放地址和相关信息
 */
data class VideoPlaybackData(
    val info: ViewInfo,
    val videoUrl: String,
    val audioUrl: String?,
    val currentQuality: Int,
    val qualityIds: List<Int>,
    val qualityLabels: List<String>,
    val cachedDashVideos: List<DashVideo>,
    val cachedDashAudios: List<DashAudio>,
    val relatedVideos: List<RelatedVideo>,
    val emoteMap: Map<String, String>
)

/**
 * 画质切换结果封装
 */
data class QualitySwitchResult(
    val videoUrl: String,
    val audioUrl: String?,
    val realQuality: Int,
    val cachedDashVideos: List<DashVideo>,
    val cachedDashAudios: List<DashAudio>
)

/**
 * 视频播放 UseCase
 * 
 * 职责：
 * 1. 加载视频详情和播放地址
 * 2. 处理画质切换逻辑
 * 3. 管理播放地址缓存
 * 
 * 将业务逻辑从 PlayerViewModel 中抽离，提高可测试性和复用性
 */
class VideoPlaybackUseCase {
    
    /**
     * 加载视频完整信息（详情 + 播放地址 + 相关视频）
     * 
     * @param bvid 视频 BV 号
     * @return 视频播放数据，包含视频、音频 URL 和画质信息
     */
    suspend fun loadVideoDetails(bvid: String): Result<VideoPlaybackData> {
        return try {
            // 并行获取视频详情、相关视频和表情包
            val detailResult = VideoRepository.getVideoDetails(bvid)
            val relatedVideos = VideoRepository.getRelatedVideos(bvid)
            val emoteMap = com.android.purebilibili.data.repository.CommentRepository.getEmoteMap()
            
            detailResult.map { (info, playData) ->
                // 选择最佳视频和音频流
                val targetQn = playData.quality.takeIf { it > 0 } ?: 64
                val dashVideo = playData.dash?.getBestVideo(targetQn)
                val dashAudio = playData.dash?.getBestAudio()
                
                // 多层 fallback 确保能获取视频 URL
                val videoUrl = extractVideoUrl(dashVideo, playData)
                val audioUrl = extractAudioUrl(dashAudio, playData)
                val realQuality = dashVideo?.id 
                    ?: playData.dash?.video?.firstOrNull()?.id 
                    ?: playData.quality
                
                VideoPlaybackData(
                    info = info,
                    videoUrl = videoUrl,
                    audioUrl = audioUrl,
                    currentQuality = realQuality,
                    qualityIds = playData.accept_quality,
                    qualityLabels = playData.accept_description,
                    cachedDashVideos = playData.dash?.video ?: emptyList(),
                    cachedDashAudios = playData.dash?.audio ?: emptyList(),
                    relatedVideos = relatedVideos,
                    emoteMap = emoteMap
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 切换画质
     * 
     * 优先从缓存的 DASH 流中选择，避免重复 API 请求
     * 
     * @param bvid 视频 BV 号
     * @param cid 视频 CID
     * @param targetQuality 目标画质 ID
     * @param cachedVideos 缓存的 DASH 视频流列表
     * @param cachedAudios 缓存的 DASH 音频流列表
     * @return 切换结果，包含新的播放地址和实际画质
     */
    suspend fun changeQuality(
        bvid: String,
        cid: Long,
        targetQuality: Int,
        cachedVideos: List<DashVideo>,
        cachedAudios: List<DashAudio>
    ): Result<QualitySwitchResult> {
        return try {
            // 优先使用缓存的 DASH 流
            if (cachedVideos.isNotEmpty()) {
                val fromCache = changeQualityFromCache(targetQuality, cachedVideos, cachedAudios)
                if (fromCache != null) {
                    return Result.success(fromCache)
                }
            }
            
            // 缓存未命中，请求新的播放地址
            val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, targetQuality)
                ?: return Result.failure(Exception("无法获取播放地址"))
            
            val dashVideo = playUrlData.dash?.getBestVideo(targetQuality)
            val dashAudio = playUrlData.dash?.getBestAudio()
            val videoUrl = extractVideoUrl(dashVideo, playUrlData)
            val audioUrl = extractAudioUrl(dashAudio, playUrlData)
            val realQuality = dashVideo?.id ?: playUrlData.quality
            
            if (videoUrl.isEmpty()) {
                return Result.failure(Exception("该清晰度无法播放"))
            }
            
            Result.success(QualitySwitchResult(
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                realQuality = realQuality,
                cachedDashVideos = playUrlData.dash?.video ?: emptyList(),
                cachedDashAudios = playUrlData.dash?.audio ?: emptyList()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从缓存的 DASH 流中切换画质
     */
    private fun changeQualityFromCache(
        targetQuality: Int,
        cachedVideos: List<DashVideo>,
        cachedAudios: List<DashAudio>
    ): QualitySwitchResult? {
        // 查找目标画质或最接近的画质
        val dashVideo = cachedVideos.find { it.id == targetQuality }
            ?: cachedVideos.filter { it.id <= targetQuality }.maxByOrNull { it.id }
            ?: cachedVideos.minByOrNull { it.id }
            ?: return null
        
        val videoUrl = dashVideo.getValidUrl()
        if (videoUrl.isEmpty()) return null
        
        val dashAudio = cachedAudios.firstOrNull()
        val audioUrl = dashAudio?.getValidUrl()
        
        return QualitySwitchResult(
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            realQuality = dashVideo.id,
            cachedDashVideos = cachedVideos,
            cachedDashAudios = cachedAudios
        )
    }
    
    /**
     * 上报播放心跳
     */
    suspend fun reportHeartbeat(bvid: String, cid: Long, playedTimeSec: Long): Boolean {
        return VideoRepository.reportPlayHeartbeat(bvid, cid, playedTimeSec)
    }
    
    /**
     * 清除播放地址缓存
     */
    fun invalidateCache(bvid: String, cid: Long) {
        PlayUrlCache.invalidate(bvid, cid)
    }
    
    // ========== Private Helpers ==========
    
    private fun extractVideoUrl(dashVideo: DashVideo?, playData: PlayUrlData): String {
        return dashVideo?.getValidUrl()?.takeIf { it.isNotEmpty() }
            ?: playData.dash?.video?.firstOrNull()?.baseUrl?.takeIf { it.isNotEmpty() }
            ?: playData.dash?.video?.firstOrNull()?.backupUrl?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: playData.durl?.firstOrNull()?.url?.takeIf { it.isNotEmpty() }
            ?: playData.durl?.firstOrNull()?.backup_url?.firstOrNull()
            ?: ""
    }
    
    private fun extractAudioUrl(dashAudio: DashAudio?, playData: PlayUrlData): String? {
        return dashAudio?.getValidUrl()?.takeIf { it.isNotEmpty() }
            ?: playData.dash?.audio?.firstOrNull()?.baseUrl?.takeIf { it.isNotEmpty() }
    }
}
