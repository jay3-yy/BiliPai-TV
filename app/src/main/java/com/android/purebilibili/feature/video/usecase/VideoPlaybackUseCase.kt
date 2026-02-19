// File: feature/video/usecase/VideoPlaybackUseCase.kt
package com.android.purebilibili.feature.video.usecase

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.core.cooldown.CooldownStatus
import com.android.purebilibili.core.cooldown.PlaybackCooldownManager
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.VideoLoadError
import com.android.purebilibili.data.model.response.*
import com.android.purebilibili.data.repository.ActionRepository
import com.android.purebilibili.data.repository.VideoRepository
import com.android.purebilibili.feature.video.controller.PlaybackProgressManager
import com.android.purebilibili.feature.video.controller.QualityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Video Playback UseCase
 * 
 * Handles video loading, playback, quality switching, and page switching.
 */

/**
 * Video playback result
 */
sealed class VideoLoadResult {
    data class Success(
        val info: ViewInfo,
        val playUrl: String,
        val audioUrl: String?,
        val related: List<RelatedVideo>,
        val quality: Int,
        val qualityIds: List<Int>,
        val qualityLabels: List<String>,
        val cachedDashVideos: List<DashVideo>,
        val cachedDashAudios: List<DashAudio>,
        val emoteMap: Map<String, String>,
        val isLoggedIn: Boolean,
        val isVip: Boolean,
        val isFollowing: Boolean,
        val isFavorited: Boolean,
        val isLiked: Boolean,
        val coinCount: Int,
        // [New] Duration (ms) from PlayUrlData
        val duration: Long = 0,
        // [New] Codec Info for UI display
        val videoCodecId: Int = 0,
        val audioCodecId: Int = 0,
        // [New] AI Translation Info
        val aiAudio: AiAudioInfo? = null,
        val curAudioLang: String? = null
    ) : VideoLoadResult()
    
    data class Error(
        val error: VideoLoadError,
        val canRetry: Boolean = true
    ) : VideoLoadResult()
}

/**
 * Quality switch result
 */
data class QualitySwitchResult(
    val videoUrl: String,
    val audioUrl: String?,
    val actualQuality: Int,
    val wasFallback: Boolean,
    val cachedDashVideos: List<DashVideo>,
    val cachedDashAudios: List<DashAudio>
)

class VideoPlaybackUseCase(
    private var progressManager: PlaybackProgressManager = PlaybackProgressManager(),
    private val qualityManager: QualityManager = QualityManager()
) {

    companion object {
        private val STANDARD_LOW_QUALITIES = listOf(32, 16)
        private const val API_ONLY_HIGH_QUALITY_FLOOR = 112
    }

    internal data class QualityMergeResult(
        val switchableQualities: List<Int>,
        val apiOnlyHighQualities: List<Int>,
        val mergedQualityIds: List<Int>
    )
    
    private var exoPlayer: ExoPlayer? = null
    
    /**
     * Initialize with context for persistent progress storage
     */
    fun initWithContext(context: android.content.Context) {
        progressManager = PlaybackProgressManager.getInstance(context)
    }
    
    /**
     * Attach ExoPlayer instance
     */
    fun attachPlayer(player: ExoPlayer) {
        exoPlayer = player
        player.volume = 1.0f
    }
    
    /**
     * Load video data
     * 
     * @param defaultQuality 网络感知的默认清晰度 (WiFi=80/1080P, Mobile=64/720P)
     * @param aid [修复] 视频 aid，用于移动端推荐流（可能只返回 aid）
     */
    suspend fun loadVideo(
        bvid: String,
        aid: Long = 0,  // [修复] 新增 aid 参数
        defaultQuality: Int = 64,
        audioQualityPreference: Int = -1,

        videoCodecPreference: String = "hev1",
        audioLang: String? = null, // [New] AI Translation Language

        playWhenReady: Boolean = true,  // [Added] Control auto-play
        onProgress: (String) -> Unit = {}
    ): VideoLoadResult {
        try {
            //  [风控冷却] 检查是否处于冷却期
            val videoIdentifier = bvid.ifEmpty { "aid:$aid" }
            when (val cooldownStatus = PlaybackCooldownManager.getCooldownStatus(videoIdentifier)) {
                is CooldownStatus.GlobalCooldown -> {
                    Logger.w("VideoPlaybackUseCase", "⏳ 全局冷却中，跳过请求: ${cooldownStatus.remainingMinutes}分${cooldownStatus.remainingSeconds}秒")
                    return VideoLoadResult.Error(
                        error = VideoLoadError.GlobalCooldown(
                            cooldownStatus.remainingMs, 
                            PlaybackCooldownManager.getConsecutiveFailures()
                        ),
                        canRetry = false
                    )
                }
                is CooldownStatus.VideoCooldown -> {
                    Logger.w("VideoPlaybackUseCase", "⏳ 视频冷却中: $videoIdentifier，剩余 ${cooldownStatus.remainingMinutes}分${cooldownStatus.remainingSeconds}秒")
                    return VideoLoadResult.Error(
                        error = VideoLoadError.RateLimited(cooldownStatus.remainingMs, videoIdentifier),
                        canRetry = false
                    )
                }
                is CooldownStatus.Ready -> {
                    // 可以继续请求
                }
            }
            
            onProgress("Loading video info...")
            
            //  [性能优化] 并行请求视频详情、相关推荐和其它数据
            val (detailResult, relatedVideos, emoteMap) = kotlinx.coroutines.coroutineScope {
                val detailDeferred = async { VideoRepository.getVideoDetails(bvid, aid, defaultQuality, audioLang) }
                val relatedDeferred = async { 
                    if (bvid.isNotEmpty()) VideoRepository.getRelatedVideos(bvid) else emptyList() 
                }
                val emoteDeferred = async { 
                    com.android.purebilibili.data.repository.CommentRepository.getEmoteMap() 
                }
                
                Triple(detailDeferred.await(), relatedDeferred.await(), emoteDeferred.await())
            }
            
            return detailResult.fold(
                onSuccess = { (info, playData) ->
                    //  [网络感知] 使用 API 返回的画质或传入的默认画质
                    // 🚀 [修复] 当 defaultQuality >= 127 时（自动最高画质），选择 accept_quality 中的最高画质
                    val targetQn = if (defaultQuality >= 127) {
                        // 自动最高画质：使用 API 返回的 accept_quality 列表
                        val acceptQualities = playData.accept_quality ?: emptyList()
                        
                        // 检测设备 HDR 支持能力
                        val isHdrSupported = com.android.purebilibili.core.util.MediaUtils.isHdrSupported()
                        val isDolbyVisionSupported = com.android.purebilibili.core.util.MediaUtils.isDolbyVisionSupported()
                        
                        // 根据设备能力过滤画质（不再硬编码 <= 120）
                        val deviceSafeQualities = acceptQualities.filter { qn ->
                            when (qn) {
                                127 -> true  // 8K - 大多数设备可以软解或降级
                                126 -> isDolbyVisionSupported  // 杜比视界需要硬件支持
                                125 -> isHdrSupported  // HDR 需要硬件支持
                                else -> true  // 其他画质都支持
                            }
                        }
                        
                        // 使用自定义优先级排序：考虑 HDR/60帧等特性
                        // 优先级（从高到低）：8K > 杜比 > HDR > 4K > 1080P60 > 1080P+ > 1080P > 720P60 > 720P > 480P > 360P
                        val qualityPriority = mapOf(
                            127 to 100,  // 8K
                            126 to 95,   // 杜比视界
                            125 to 90,   // HDR
                            120 to 85,   // 4K
                            116 to 80,   // 1080P60
                            112 to 75,   // 1080P+
                            70 to 70,    // 1080P
                            80 to 70,    // 1080P (fix duplicate key if any)
                            74 to 65,    // 720P60
                            64 to 60,    // 720P
                            32 to 50,    // 480P
                            16 to 40     // 360P
                        )
                        
                        // Fix map creation if duplicate keys exist (e.g. strict mapOf). 
                        // Actually let's keep it simple.
                        
                        val maxAccept = deviceSafeQualities.maxByOrNull { 
                             // Simplified priority check
                             when(it) {
                                 127 -> 100
                                 126 -> 95
                                 125 -> 90
                                 120 -> 85
                                 116 -> 80
                                 112 -> 75
                                 80 -> 70
                                 74 -> 65
                                 64 -> 60
                                 32 -> 50
                                 16 -> 40
                                 else -> 0
                             }
                        } ?: 80
                        Logger.d("VideoPlaybackUseCase", "🚀 自动最高画质: accept_quality=$acceptQualities, 设备支持HDR=$isHdrSupported, 杜比=$isDolbyVisionSupported, 选择 $maxAccept")
                        maxAccept
                    } else {
                        // 🚀 [修复] 优先使用用户设置的 defaultQuality，而不是 API 返回的 playData.quality
                        if (defaultQuality > 0) defaultQuality else playData.quality
                    }
                    
                    val isHevcSupported = com.android.purebilibili.core.util.MediaUtils.isHevcSupported()
                    val isAv1Supported = com.android.purebilibili.core.util.MediaUtils.isAv1Supported()
                    
                    val dashVideo = playData.dash?.getBestVideo(
                        targetQn, 
                        preferCodec = videoCodecPreference,
                        isHevcSupported = isHevcSupported,
                        isAv1Supported = isAv1Supported
                    )
                    val dashAudio = playData.dash?.getBestAudio(audioQualityPreference)
                    
                    val videoUrl = getValidVideoUrl(dashVideo, playData)
                    val audioUrl = dashAudio?.getValidUrl()?.takeIf { it.isNotEmpty() }
                    
                    if (videoUrl.isEmpty()) {
                        PlaybackCooldownManager.recordFailure(bvid, "播放地址为空")
                        return@fold VideoLoadResult.Error(
                            error = VideoLoadError.PlayUrlEmpty,
                            canRetry = true
                        )
                    }
                    
                    PlaybackCooldownManager.recordSuccess(bvid)
                    
                    val isLogin = !com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()
                    
                    var isVip = com.android.purebilibili.core.store.TokenManager.isVipCache
                    if (isLogin && !isVip) {
                        try {
                            val navResult = VideoRepository.getNavInfo()
                            navResult.onSuccess { navData ->
                                isVip = navData.vip.status == 1
                                com.android.purebilibili.core.store.TokenManager.isVipCache = isVip
                                Logger.d("VideoPlaybackUseCase", " Refreshed VIP status: $isVip")
                            }
                        } catch (e: Exception) {
                            Logger.d("VideoPlaybackUseCase", " Failed to refresh VIP status: ${e.message}")
                        }
                    }

                    // [New] 本地强制解锁 VIP 状态 - REVERTED
                    // val isUnlockHighQuality = ...
                    
                    val isEffectiveVip = isVip // || isUnlockHighQuality
                    // if (isUnlockHighQuality) ...
                    
                    //  [修复] 画质列表优先使用 DASH 实际轨道，避免展示“可选但不可切”的画质。
                    val apiQualities = playData.accept_quality ?: emptyList()
                    val dashVideoIds = playData.dash?.video?.map { it.id }?.distinct() ?: emptyList()

                    val qualityMergeResult = mergeQualityOptions(apiQualities, dashVideoIds)
                    val mergedQualityIds = qualityMergeResult.mergedQualityIds
                    
                    //  [修复] 生成对应的画质标签 - 使用更短的名称确保竖屏显示完整
                    val qualityLabelMap = mapOf(
                        127 to "8K",
                        126 to "杜比",
                        125 to "HDR",
                        120 to "4K",
                        116 to "60帧",   //  "1080P60" 改为 "60帧"
                        112 to "高码",   //  "1080P+" 改为 "高码"
                        80 to "1080P",
                        74 to "720P60",
                        64 to "720P",
                        32 to "480P",
                        16 to "360P"
                    )
                    val mergedQualityLabels = mergedQualityIds.map { qn ->
                        qualityLabelMap[qn] ?: "${qn}P"
                    }
                    
                    Logger.d(
                        "VideoPlaybackUseCase",
                        " Quality merge: api=$apiQualities, dash=$dashVideoIds, switchable=${qualityMergeResult.switchableQualities}, apiOnlyHigh=${qualityMergeResult.apiOnlyHighQualities}, merged=$mergedQualityIds"
                    )
                    
                    // Check user interaction status
                    val isFollowing = if (isLogin) ActionRepository.checkFollowStatus(info.owner.mid) else false
                    val isFavorited = if (isLogin) ActionRepository.checkFavoriteStatus(info.aid) else false
                    val isLiked = if (isLogin) ActionRepository.checkLikeStatus(info.aid) else false
                    val coinCount = if (isLogin) ActionRepository.checkCoinStatus(info.aid) else 0
                    
                    VideoLoadResult.Success(
                        info = info,
                        playUrl = videoUrl,
                        audioUrl = audioUrl,
                        related = relatedVideos,
                        quality = dashVideo?.id ?: playData.quality, // Prefer DASH quality ID
                        qualityIds = mergedQualityIds,
                        qualityLabels = mergedQualityLabels,
                        cachedDashVideos = playData.dash?.video ?: emptyList(),
                        cachedDashAudios = playData.dash?.audio ?: emptyList(),
                        emoteMap = emoteMap,
                        isLoggedIn = isLogin,
                        isVip = isEffectiveVip, // Pass effective VIP status (true if actual VIP or Unlocked)
                        isFollowing = isFollowing,
                        isFavorited = isFavorited,
                        isLiked = isLiked,

                        coinCount = coinCount,
                        duration = playData.timelength,
                        aiAudio = playData.aiAudio,
                        curAudioLang = playData.curLanguage
                    )
                },
                onFailure = { e ->
                    //  [风控冷却] 加载失败，记录失败
                    PlaybackCooldownManager.recordFailure(bvid, e.message ?: "unknown")
                    // Check if rate limited
                    val error = VideoLoadError.fromException(e)

                    VideoLoadResult.Error(
                        error = VideoLoadError.fromException(e),
                        canRetry = VideoLoadError.fromException(e).isRetryable()
                    )
                }
            )

        } catch (e: kotlinx.coroutines.CancellationException) {
            Logger.d("VideoPlaybackUseCase", "🚫 加载已取消: $bvid")
            throw e
        } catch (e: Exception) {
            //  [风控冷却] 异常失败，记录
            PlaybackCooldownManager.recordFailure(bvid, e.message ?: "exception")
            return VideoLoadResult.Error(
                error = VideoLoadError.fromException(e),
                canRetry = true
            )
        }
    }
    
    /**
     * Get cached position for video
     */
    fun getCachedPosition(bvid: String): Long {
        return progressManager.getCachedPosition(bvid)
    }
    
    /**
     * Save current playback position
     */
    fun savePosition(bvid: String) {
        val player = exoPlayer ?: return
        if (bvid.isNotEmpty() && player.currentPosition > 0) {
            progressManager.savePosition(bvid, player.currentPosition)
        }
    }
    
    /**
     * Play video with DASH format
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playDashVideo(videoUrl: String, audioUrl: String?, seekTo: Long = 0L, playWhenReady: Boolean = true) {
        val player = exoPlayer ?: return
        player.volume = 1.0f
        
        val headers = mapOf(
            "Referer" to "https://www.bilibili.com",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        val dataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
            NetworkModule.okHttpClient
        ).setDefaultRequestProperties(headers)
        
        val mediaSourceFactory = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
        val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
        
        val finalSource = if (audioUrl != null) {
            val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
            androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
        } else {
            videoSource
        }
        
        player.setMediaSource(finalSource)
        player.prepare()
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.playWhenReady = playWhenReady
    }
    
    /**
     * Play simple video URL
     */
    fun playVideo(url: String, seekTo: Long = 0L, playWhenReady: Boolean = true) {
        val player = exoPlayer ?: return
        player.volume = 1.0f
        
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        if (seekTo > 0) {
            player.seekTo(seekTo)
        }
        player.playWhenReady = playWhenReady
    }
    
    /**
     * Change quality using cached DASH streams
     */
    fun changeQualityFromCache(
        qualityId: Int,
        cachedVideos: List<DashVideo>,
        cachedAudios: List<DashAudio>,
        currentPos: Long,
        audioQualityPreference: Int = -1 // [新增] 传入音频偏好
    ): QualitySwitchResult? {
        if (cachedVideos.isEmpty()) {
            Logger.d("VideoPlaybackUseCase", " changeQualityFromCache: cache is EMPTY, returning null")
            return null
        }
        
        //  [调试] 输出缓存中的所有画质
        val availableIds = cachedVideos.map { it.id }.distinct().sortedDescending()
        Logger.d("VideoPlaybackUseCase", " changeQualityFromCache: target=$qualityId, available=$availableIds")
        
        // 只接受精确匹配；没有目标轨道时返回 null，让上层走 API 请求。
        val match = cachedVideos.find { it.id == qualityId }
        if (match == null) {
            Logger.d("VideoPlaybackUseCase", " Cache exact match missing for $qualityId, fallback to API")
            return null
        }

        Logger.d("VideoPlaybackUseCase", " Match found in cache: ${match.id}")
        val videoUrl = match.getValidUrl()
        
        // [修复] 音频也应该重新选择最佳匹配，而不是盲目取第一个
        val dashAudio = if (audioQualityPreference != -1) {
            // 使用 Dash.getBestAudio 逻辑的简化版 (因为这里只有 List<DashAudio>)
            cachedAudios.find { it.id == audioQualityPreference }
                ?: cachedAudios.minByOrNull { kotlin.math.abs(it.id - audioQualityPreference) }
        } else {
            cachedAudios.maxByOrNull { it.bandwidth }
        }
         
        val audioUrl = dashAudio?.getValidUrl()
        if (videoUrl.isNotEmpty()) {
            playDashVideo(videoUrl, audioUrl, currentPos, playWhenReady = true) // Switching quality should always auto-play
            return QualitySwitchResult(
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                actualQuality = match.id,
                wasFallback = false,
                cachedDashVideos = cachedVideos,
                cachedDashAudios = cachedAudios
            )
        }
        
        //  [降级逻辑] 缓存中没有目标画质，需要返回 null 让调用者请求 API
        Logger.d("VideoPlaybackUseCase", " Target quality $qualityId not in cache, returning null to trigger API request")
        return null
    }
    
    /**
     * Change quality via API request
     */
    suspend fun changeQualityFromApi(
        bvid: String,
        cid: Long,
        qualityId: Int,
        currentPos: Long,
        audioQualityPreference: Int = -1 // [新增] 传入音频偏好
    ): QualitySwitchResult? {
        Logger.d("VideoPlaybackUseCase", " changeQualityFromApi: bvid=$bvid, cid=$cid, target=$qualityId")
        
        val playUrlData = VideoRepository.getPlayUrlData(bvid, cid, qualityId) ?: run {
            Logger.d("VideoPlaybackUseCase", " getPlayUrlData returned null")
            return null
        }
        
        //  [调试] 输出 API 返回的画质信息
        val returnedQuality = playUrlData.quality
        val acceptQualities = playUrlData.accept_quality
        val dashVideoIds = playUrlData.dash?.video?.map { it.id }?.distinct()?.sortedDescending()
        Logger.d("VideoPlaybackUseCase", " API returned: quality=$returnedQuality, accept_quality=$acceptQualities")
        Logger.d("VideoPlaybackUseCase", " DASH videos available: $dashVideoIds")
        
        val dashVideo = playUrlData.dash?.getBestVideo(qualityId)

        val dashAudio = playUrlData.dash?.getBestAudio(audioQualityPreference) // [修复] 使用偏好
        
        Logger.d("VideoPlaybackUseCase", " getBestVideo selected: ${dashVideo?.id}")
        
        val videoUrl = getValidVideoUrl(dashVideo, playUrlData)
        val audioUrl = dashAudio?.getValidUrl()
        
        if (videoUrl.isEmpty()) {
            Logger.d("VideoPlaybackUseCase", " Video URL is empty")
            return null
        }
        
        if (dashVideo != null) {
            playDashVideo(videoUrl, audioUrl, currentPos, playWhenReady = true) // Switching quality should always auto-play
        } else {
            playVideo(videoUrl, currentPos, playWhenReady = true)
        }
        
        val actualQuality = dashVideo?.id ?: playUrlData.quality ?: qualityId
        Logger.d("VideoPlaybackUseCase", " Quality switch result: target=$qualityId, actual=$actualQuality")
        
        return QualitySwitchResult(
            videoUrl = videoUrl,
            audioUrl = audioUrl,
            actualQuality = actualQuality,
            wasFallback = actualQuality != qualityId,
            cachedDashVideos = playUrlData.dash?.video ?: emptyList(),
            cachedDashAudios = playUrlData.dash?.audio ?: emptyList()
        )
    }
    
    /**
     * Get player current position
     */
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L
    
    /**
     * Get player duration
     */
    fun getDuration(): Long {
        val duration = exoPlayer?.duration ?: 0L
        return if (duration < 0) 0L else duration
    }
    
    /**
     * Seek to position
     */
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }
    
    private fun getValidVideoUrl(dashVideo: DashVideo?, playData: PlayUrlData): String {
        return dashVideo?.getValidUrl()?.takeIf { it.isNotEmpty() }
            ?: playData.dash?.video?.firstOrNull()?.baseUrl?.takeIf { it.isNotEmpty() }
            ?: playData.dash?.video?.firstOrNull()?.backupUrl?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: playData.durl?.firstOrNull()?.url?.takeIf { it.isNotEmpty() }
            ?: playData.durl?.firstOrNull()?.backupUrl?.firstOrNull()
            ?: ""
    }

    internal fun mergeQualityOptions(
        apiQualities: List<Int>,
        dashVideoIds: List<Int>
    ): QualityMergeResult {
        val normalizedApi = apiQualities.distinct().sortedDescending()
        val normalizedDash = dashVideoIds.distinct().sortedDescending()
        val switchableQualities = if (normalizedDash.isNotEmpty()) normalizedDash else normalizedApi

        // Keep API-only high tiers visible (4K/1080P60/1080P+) so users can trigger re-fetch switching.
        val apiOnlyHighQualities = normalizedApi.filter { qualityId ->
            qualityId >= API_ONLY_HIGH_QUALITY_FLOOR && qualityId !in normalizedDash
        }

        val mergedQualityIds = (switchableQualities + apiOnlyHighQualities + STANDARD_LOW_QUALITIES)
            .distinct()
            .sortedDescending()

        return QualityMergeResult(
            switchableQualities = switchableQualities,
            apiOnlyHighQualities = apiOnlyHighQualities,
            mergedQualityIds = mergedQualityIds
        )
    }
}
