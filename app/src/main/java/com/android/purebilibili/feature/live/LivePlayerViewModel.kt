// 文件路径: feature/live/LivePlayerViewModel.kt
package com.android.purebilibili.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.LiveQuality
import com.android.purebilibili.data.repository.LiveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.android.purebilibili.core.network.socket.DanmakuProtocol
import com.android.purebilibili.data.repository.DanmakuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

/**
 * 直播弹幕 UI 模型
 */
data class LiveDanmakuItem(
    val text: String,
    val color: Int = 16777215, // Default White
    val mode: Int = 1,         // 1=Scroll, 4=Bottom, 5=Top
    val uid: Long = 0,
    val uname: String = "",
    val isSelf: Boolean = false, // 是否自己发送
    val emoticonUrl: String? = null, // [NEW] B站自定义表情 URL
    // [新增] 视觉优化字段
    val medalName: String = "",
    val medalLevel: Int = 0,
    val medalColor: Int = 0,
    val userLevel: Int = 0,
    val isAdmin: Boolean = false,
    val guardLevel: Int = 0 // 0=none, 1=总督, 2=提督, 3=舰长
)

/**
 * 主播信息
 */
data class AnchorInfo(
    val uid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val followers: Long = 0,
    val officialTitle: String = ""
)

/**
 * 直播间信息
 */
data class RoomInfo(
    val roomId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val areaName: String = "",
    val parentAreaName: String = "",
    val online: Int = 0,
    val liveStatus: Int = 0,
    val liveStartTime: Long = 0,
    val description: String = "",
    val tags: String = ""
)

/**
 * 直播播放器 UI 状态
 */
sealed class LivePlayerState {
    object Loading : LivePlayerState()
    
    data class Success(
        val playUrl: String,
        val allPlayUrls: List<String> = emptyList(),  //  [新增] 所有可用的 CDN URL（用于故障转移）
        val currentUrlIndex: Int = 0,  //  [新增] 当前使用的 URL 索引
        val currentQuality: Int,
        val qualityList: List<LiveQuality>,
        val roomInfo: RoomInfo = RoomInfo(),
        val anchorInfo: AnchorInfo = AnchorInfo(),
        val isFollowing: Boolean = false,
        val isDanmakuEnabled: Boolean = true // [新增] 弹幕开关状态
    ) : LivePlayerState()
    
    data class Error(
        val message: String
    ) : LivePlayerState()
}

/**
 * 直播播放器 ViewModel - 增强版
 */
class LivePlayerViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow<LivePlayerState>(LivePlayerState.Loading)
    val uiState = _uiState.asStateFlow()
    
    // 直播弹幕流 (UI 观察此流进行渲染)
    private val _danmakuFlow = MutableSharedFlow<LiveDanmakuItem>(extraBufferCapacity = 100)
    val danmakuFlow = _danmakuFlow.asSharedFlow()
    
    private var danmakuClient: com.android.purebilibili.core.network.socket.LiveDanmakuClient? = null
    
    private var currentRoomId: Long = 0
    private var currentUid: Long = 0
    
    /**
     * 加载直播流和直播间详情
     */
    /**
     * 加载直播流和直播间详情
     */
    fun loadLiveStream(roomId: Long, qn: Int = 10000) {
        currentRoomId = roomId
        
        viewModelScope.launch {
            _uiState.value = LivePlayerState.Loading
            
            // 并行加载直播流和直播间详情
            val playUrlDeferred = async { LiveRepository.getLivePlayUrlWithQuality(roomId, qn) }
            val roomDetailDeferred = async { 
                try { 
                    NetworkModule.api.getLiveRoomDetail(roomId) 
                } catch (e: Exception) { 
                    e.printStackTrace()
                    null 
                } 
            }
            
            val playUrlResult = playUrlDeferred.await()
            val roomDetailResponse = roomDetailDeferred.await()
            
            var roomInfo = RoomInfo()
            var anchorInfo = AnchorInfo()
            var isFollowing = false
            
            // 尝试解析 LiveRoomDetail
            var roomData = roomDetailResponse?.data?.roomInfo
            var anchorData = roomDetailResponse?.data?.anchorInfo
            var watchedShow = roomDetailResponse?.data?.watchedShow
            
            // 如果主要 API 失败或缺少主播信息，尝试 Fallback 方案
            if (roomDetailResponse?.code != 0 || anchorData == null) {
                com.android.purebilibili.core.util.Logger.w("LivePlayerVM", "🔴 LiveRoomDetail failed or empty. Starting Fallback...")
                try {
                    // 1. 获取基础房间信息 (为了拿到 UID 和 在线人数)
                    val roomInfoResp = NetworkModule.api.getRoomInfo(roomId)
                    if (roomInfoResp.code == 0 && roomInfoResp.data != null) {
                        val basicInfo = roomInfoResp.data
                        currentUid = basicInfo.uid
                        
                        // 临时构建 RoomInfo
                        roomInfo = RoomInfo(
                            roomId = basicInfo.room_id,
                            title = basicInfo.title,
                            online = basicInfo.online,
                            liveStatus = basicInfo.liveStatus,
                            areaName = basicInfo.areaName
                        )
                        
                        // 2. 根据 UID 获取用户卡片 (为了拿到头像和名字)
                        if (currentUid > 0) {
                            val cardResp = NetworkModule.api.getUserCard(currentUid)
                            if (cardResp.code == 0 && cardResp.data?.card != null) {
                                val card = cardResp.data.card
                                anchorInfo = AnchorInfo(
                                    uid = currentUid,
                                    uname = card.name,
                                    face = card.face,
                                    followers = cardResp.data.follower.toLong(),
                                    officialTitle = card.Official?.title ?: ""
                                )
                                com.android.purebilibili.core.util.Logger.d("LivePlayerVM", "🔴 Fallback success: fetched anchor ${card.name}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // 如果主要 API 成功，或者 Fallback 失败但至少有部分数据
            if (anchorData != null || anchorInfo.uid > 0 || roomData != null) {
                 // 优先使用 LiveRoomDetail 的数据（如果不为空）
                 if (roomDetailResponse?.code == 0 && roomDetailResponse.data != null) {
                     val data = roomDetailResponse.data
                     currentUid = data.roomInfo?.uid ?: 0
                     
                     roomInfo = RoomInfo(
                        roomId = data.roomInfo?.roomId ?: roomInfo.roomId,
                        title = data.roomInfo?.title ?: roomInfo.title,
                        cover = data.roomInfo?.cover ?: roomInfo.cover,
                        areaName = data.roomInfo?.areaName ?: roomInfo.areaName,
                        parentAreaName = data.roomInfo?.parentAreaName ?: "",
                        online = data.watchedShow?.num ?: data.roomInfo?.online ?: roomInfo.online,
                        liveStatus = data.roomInfo?.liveStatus ?: roomInfo.liveStatus,
                        liveStartTime = data.roomInfo?.liveStartTime ?: 0,
                        description = data.roomInfo?.description ?: "",
                        tags = data.roomInfo?.tags ?: ""
                     )
                     
                     anchorInfo = AnchorInfo(
                        uid = data.roomInfo?.uid ?: 0,
                        uname = data.anchorInfo?.baseInfo?.uname ?: "主播",
                        face = data.anchorInfo?.baseInfo?.face ?: "",
                        followers = data.anchorInfo?.relationInfo?.attention ?: 0,
                        officialTitle = data.anchorInfo?.baseInfo?.officialInfo?.title ?: ""
                     )
                 }

                // 检查关注状态 (通用逻辑)
                if (currentUid > 0) {
                    try {
                        val relationResp = NetworkModule.api.getRelation(currentUid)
                        if (relationResp.code == 0 && relationResp.data != null) {
                            isFollowing = relationResp.data.isFollowing
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                
                com.android.purebilibili.core.util.Logger.d("LivePlayerVM", "🔴 Final State -> Room: ${roomInfo.title}, Anchor: ${anchorInfo.uname}")
            } else {
                com.android.purebilibili.core.util.Logger.e("LivePlayerVM", "🔴 All attempts to load room info failed.")
            }
            
            playUrlResult.onSuccess { data ->
                // ... (Keep existing Play URL logic) ...
                
                //  [修复] 收集所有可用的 CDN URL
                val allUrls = data.durl?.mapNotNull { it.url } ?: emptyList()
                
                //  [关键修复] 优先使用第二个 CDN（索引1）
                val preferredIndex = if (allUrls.size > 1) 1 else 0
                val url = allUrls.getOrNull(preferredIndex) ?: extractPlayUrl(data)
                
                if (url != null) {
                    val qualityList = data.quality_description?.takeIf { it.isNotEmpty() }
                        ?: data.playurl_info?.playurl?.gQnDesc
                        ?: emptyList()
                    
                    _uiState.value = LivePlayerState.Success(
                        playUrl = url,
                        allPlayUrls = allUrls,
                        currentUrlIndex = preferredIndex,
                        currentQuality = qn,
                        qualityList = qualityList,
                        roomInfo = roomInfo,     // 填入解析好的数据
                        anchorInfo = anchorInfo, // 填入解析好的数据
                        isFollowing = isFollowing
                    )
                } else {
                    _uiState.value = LivePlayerState.Error("无法获取直播流地址")
                }
            }.onFailure { e ->
                _uiState.value = LivePlayerState.Error(e.message ?: "加载失败")
            }

            // 启动弹幕连接
            startLiveDanmaku(roomId)
            
            // [新增] 加载弹幕表情
            launch(Dispatchers.IO) {
                val emojiResult = LiveRepository.getEmoticons(roomId)
                emojiResult.onSuccess { map ->
                    com.android.purebilibili.feature.live.components.DanmakuEmoticonMapper.update(map)
                }
            }
        }
    }


    
    /**
     * 检查关注状态
     */
    private suspend fun checkFollowStatus(uid: Long) {
        try {
            val api = NetworkModule.api
            val response = api.getRelation(uid)
            
            if (response.code == 0 && response.data != null) {
                val currentState = _uiState.value as? LivePlayerState.Success ?: return
                _uiState.value = currentState.copy(
                    isFollowing = response.data.isFollowing
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 关注/取关主播
     */
    fun toggleFollow() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        if (currentUid <= 0) return
        
        viewModelScope.launch {
            try {
                val api = NetworkModule.api
                val csrf = TokenManager.csrfCache ?: return@launch
                
                val act = if (currentState.isFollowing) 2 else 1  // 2=取关, 1=关注
                val response = api.modifyRelation(currentUid, act, csrf)
                
                if (response.code == 0) {
                    _uiState.value = currentState.copy(
                        isFollowing = !currentState.isFollowing,
                        anchorInfo = currentState.anchorInfo.copy(
                            followers = if (currentState.isFollowing) {
                                currentState.anchorInfo.followers - 1
                            } else {
                                currentState.anchorInfo.followers + 1
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 切换画质
     */
    fun changeQuality(qn: Int) {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        android.util.Log.d("LivePlayer", "🔴 changeQuality called: qn=$qn")
        
        viewModelScope.launch {
            val result = LiveRepository.getLivePlayUrlWithQuality(currentRoomId, qn)
            
            result.onSuccess { data ->
                android.util.Log.d("LivePlayer", "🔴 changeQuality success, durl count: ${data.durl?.size}")
                
                //  [修复] 收集所有 URL 并优先使用备用 CDN
                val allUrls = data.durl?.mapNotNull { it.url } ?: emptyList()
                val preferredIndex = if (allUrls.size > 1) 1 else 0
                val url = allUrls.getOrNull(preferredIndex) ?: extractPlayUrl(data)
                
                android.util.Log.d("LivePlayer", "🔴 changeQuality selected URL: ${url?.take(80)}")
                
                if (url != null) {
                    val newQualityList = data.quality_description?.takeIf { it.isNotEmpty() }
                        ?: data.playurl_info?.playurl?.gQnDesc
                        ?: currentState.qualityList
                    
                    _uiState.value = currentState.copy(
                        playUrl = url,
                        allPlayUrls = allUrls,
                        currentUrlIndex = preferredIndex,
                        currentQuality = qn,  //  [修复] 使用用户请求的 qn 值
                        qualityList = newQualityList
                    )
                } else {
                    android.util.Log.e("LivePlayer", " changeQuality: No URL found")
                }
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", " changeQuality failed: ${e.message}")
            }
        }
    
    }
    
    /**
     * [新增] 切换弹幕开关
     */
    fun toggleDanmaku() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        _uiState.value = currentState.copy(
            isDanmakuEnabled = !currentState.isDanmakuEnabled
        )
    }
    
    /**
     *  [新增] 尝试下一个 CDN URL（播放失败时调用）
     */
    fun tryNextUrl() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        
        val nextIndex = currentState.currentUrlIndex + 1
        if (nextIndex < currentState.allPlayUrls.size) {
            val nextUrl = currentState.allPlayUrls[nextIndex]
            android.util.Log.d("LivePlayer", " Trying next CDN URL (index=$nextIndex): ${nextUrl.take(80)}...")
            
            _uiState.value = currentState.copy(
                playUrl = nextUrl,
                currentUrlIndex = nextIndex
            )
        } else {
            android.util.Log.e("LivePlayer", " No more CDN URLs to try (tried all ${currentState.allPlayUrls.size})")
            // 所有 URL 都失败了，显示错误
            _uiState.value = LivePlayerState.Error("所有 CDN 均无法连接，请稍后重试")
        }
    }
    
    /**
     * 从响应数据中提取播放 URL
     */
    private fun extractPlayUrl(data: com.android.purebilibili.data.model.response.LivePlayUrlData): String? {
        android.util.Log.d("LivePlayer", "🔴 === extractPlayUrl ===")
        
        // 尝试新 xlive API
        data.playurl_info?.playurl?.stream?.let { streams ->
            android.util.Log.d("LivePlayer", "🔴 Found ${streams.size} streams")
            streams.forEachIndexed { index, s ->
                android.util.Log.d("LivePlayer", "🔴 Stream[$index]: protocol=${s.protocolName}")
            }
            
            val stream = streams.find { it.protocolName == "http_hls" }
                ?: streams.find { it.protocolName == "http_stream" }
                ?: streams.firstOrNull()
            
            android.util.Log.d("LivePlayer", "🔴 Selected stream: ${stream?.protocolName}")
            
            val format = stream?.format?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 Format: ${format?.formatName}")
            
            val codec = format?.codec?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 Codec: ${codec?.codecName}, baseUrl=${codec?.baseUrl?.take(50)}")
            
            val urlInfo = codec?.url_info?.firstOrNull()
            android.util.Log.d("LivePlayer", "🔴 UrlInfo: host=${urlInfo?.host}, extra=${urlInfo?.extra?.take(30)}")
            
            if (codec != null && urlInfo != null) {
                val url = urlInfo.host + codec.baseUrl + urlInfo.extra
                android.util.Log.d("LivePlayer", " Built URL from xlive API: ${url.take(100)}...")
                return url
            }
        }
        
        // 回退到旧 API
        android.util.Log.d("LivePlayer", "🔴 Trying durl fallback...")
        val durlUrl = data.durl?.firstOrNull()?.url
        if (durlUrl != null) {
            android.util.Log.d("LivePlayer", " Using durl URL: ${durlUrl.take(100)}...")
            return durlUrl
        }
        
        android.util.Log.e("LivePlayer", " No URL found in any structure!")
        return null
    }
    
    /**
     * 重试
     */
    fun retry() {
        loadLiveStream(currentRoomId)
    }
    
    /**
     * 启动直播弹幕
     */
    private fun startLiveDanmaku(roomId: Long) {
        // 先断开旧连接
        danmakuClient?.disconnect()
        danmakuClient = null
        
        viewModelScope.launch {
            val result = DanmakuRepository.startLiveDanmaku(this, roomId)
            result.onSuccess { client ->
                danmakuClient = client
                
                // 监听弹幕消息
                launch(Dispatchers.Default) {
                    client.messageFlow.collect { packet ->
                        handleDanmakuPacket(packet)
                    }
                }
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", "🔥 Danmaku connection failed: ${e.message}")
            }
        }
    }
    
    // [新增] 记录最近发送的弹幕（用于去重WebSocket回传）
    private var recentSentDanmaku: String? = null
    private var recentSentTime: Long = 0L
    
    /**
     * 发送弹幕
     */
    fun sendDanmaku(text: String) {
        if (text.isBlank() || currentRoomId == 0L) return
        
        viewModelScope.launch {
            val result = LiveRepository.sendDanmaku(currentRoomId, text)
            result.onSuccess {
                // 记录发送的弹幕（用于去重）
                recentSentDanmaku = text
                recentSentTime = System.currentTimeMillis()
                
                // 发送成功，模拟一条本地弹幕立即上屏
                val mid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
                val item = LiveDanmakuItem(
                    text = text,
                    color = 16777215, // White
                    mode = 1, // Scroll
                    uid = mid,
                    uname = "我",
                    isSelf = true
                )
                _danmakuFlow.tryEmit(item)
            }.onFailure { e ->
                android.util.Log.e("LivePlayer", "Send danmaku failed: ${e.message}")
            }
        }
    }
    
    /**
     * 点赞直播间（点亮）
     */
    fun clickLike() {
        val currentState = _uiState.value as? LivePlayerState.Success ?: return
        if (currentRoomId == 0L) return
        
        viewModelScope.launch {
            // 参数: roomId, uid, anchorId
            LiveRepository.clickLike(currentRoomId, currentUid, currentState.anchorInfo.uid)
        }
    }

    /**
     * 处理弹幕包
     * 
     * 修复记录:
     * - 使用 optXXX 替代 getXXX 避免数组越界
     * - 添加完善的异常处理
     */
    private fun handleDanmakuPacket(packet: DanmakuProtocol.Packet) {
        if (packet.operation != DanmakuProtocol.OP_MESSAGE) return
        
        try {
            // Body 是 JSON (Brotli/Zlib 解压后)
            val jsonStr = String(packet.body, Charsets.UTF_8)
            val json = JSONObject(jsonStr)
            val cmd = json.optString("cmd", "")
            
            if (!cmd.startsWith("DANMU_MSG")) return // 可能有 "DANMU_MSG:4:0:2:2:2:0" 这种格式
            
            val info = json.optJSONArray("info") ?: return
            if (info.length() < 3) return // 至少需要 meta, text, user
            
            // 解析基本信息 (使用 optXXX 安全访问)
            val meta = info.optJSONArray(0) ?: return
            val text = info.optString(1, "") 
            val user = info.optJSONArray(2) ?: return
            
            // 过滤空弹幕
            if (text.isEmpty()) return
            
            val mode = meta.optInt(1, 1)
            val color = meta.optInt(3, 16777215)
            val uid = user.optLong(0, 0L)
            val uname = user.optString(1, "")
            
            // 解析表情包 (位于 info[0][13])
            val emoticonUrl = if (meta.length() > 13) {
                meta.optJSONObject(13)?.optString("url")
            } else null
            
            // [去重] 检查是否是自己刚发送的弹幕的回传
            val myMid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
            val isRecentlyMySent = uid == myMid 
                && text == recentSentDanmaku 
                && (System.currentTimeMillis() - recentSentTime) < 10_000L
            
            if (isRecentlyMySent) {
                // 清除记录，避免后续相同文本的弹幕被误过滤
                recentSentDanmaku = null
                android.util.Log.d("LivePlayer", "🔄 Skipped duplicate self-sent danmaku: $text")
                return
            }
            
            // 安全解析粉丝牌信息 info[3] - [level, name, anchor_name, room_id, color, ...]
            var medalLevel = 0
            var medalName = ""
            var medalColor = 0
            if (info.length() > 3 && !info.isNull(3)) {
                val medalArray = info.optJSONArray(3)
                if (medalArray != null && medalArray.length() > 0) {
                    medalLevel = medalArray.optInt(0, 0)
                    if (medalArray.length() > 1) medalName = medalArray.optString(1, "")
                    if (medalArray.length() > 4) medalColor = medalArray.optInt(4, 0)
                }
            }
            
            // 安全解析用户等级 info[4][0]
            var userLevel = 0
            if (info.length() > 4 && !info.isNull(4)) {
                val levelArray = info.optJSONArray(4)
                if (levelArray != null && levelArray.length() > 0) {
                    userLevel = levelArray.optInt(0, 0)
                }
            }
            
            // 安全解析身份标识
            val isAdmin = if (user.length() > 2) user.optInt(2, 0) == 1 else false
            val guardLevel = if (info.length() > 7) info.optInt(7, 0) else 0 // 1=总督 2=提督 3=舰长
            
            val item = LiveDanmakuItem(
                text = text,
                color = color,
                mode = mode,
                uid = uid,
                uname = uname,
                isSelf = uid == myMid,
                emoticonUrl = emoticonUrl,
                medalLevel = medalLevel,
                medalName = medalName,
                medalColor = medalColor,
                userLevel = userLevel,
                isAdmin = isAdmin,
                guardLevel = guardLevel
            )
            _danmakuFlow.tryEmit(item)
            
            // TODO: 处理 SendGift, SystemMsg 等其他消息
            
        } catch (e: Exception) {
            // JSON 解析失败，记录日志但不崩溃
            android.util.Log.e("LivePlayer", "❌ Danmaku parse error: ${e.message}")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        danmakuClient?.disconnect()
    }
}
