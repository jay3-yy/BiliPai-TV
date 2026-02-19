// 聊天详情 ViewModel
package com.android.purebilibili.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.EmoteInfo
import com.android.purebilibili.data.model.response.PrivateMessageItem
import com.android.purebilibili.data.model.response.ViewInfo
import com.android.purebilibili.data.repository.MessageRepository
import com.android.purebilibili.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 视频预览信息 (用于链接预览)
 */
data class VideoPreviewInfo(
    val bvid: String,
    val title: String,
    val cover: String,
    val ownerName: String,
    val viewCount: Long,
    val duration: Long = 0
)

data class ChatUiState(
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val isLoadingMore: Boolean = false,
    val messages: List<PrivateMessageItem> = emptyList(),
    val emoteInfos: List<EmoteInfo> = emptyList(),
    val hasMore: Boolean = false,
    val minSeqno: Long = 0,
    val error: String? = null,
    val sendError: String? = null,
    val videoPreviews: Map<String, VideoPreviewInfo> = emptyMap()  // bvid -> VideoPreviewInfo
)

class ChatViewModel(
    private val talkerId: Long,
    private val sessionType: Int
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // 视频预览缓存
    private val videoPreviewCache = mutableMapOf<String, VideoPreviewInfo>()
    
    // 正在加载的 bvid 集合 (防止重复请求)
    private val loadingBvids = mutableSetOf<String>()
    
    // 当前用户 mid
    val currentUserMid: Long
        get() = TokenManager.midCache ?: 0
    
    // BV号匹配正则
    private val bvPattern = Regex("BV[a-zA-Z0-9]{10}")
    private val avPattern = Regex("av(\\d+)", RegexOption.IGNORE_CASE)
    
    init {
        loadMessages()
    }
    
    /**
     * 加载消息
     */
    fun loadMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            MessageRepository.getMessages(
                talkerId = talkerId,
                sessionType = sessionType,
                size = 30
            ).fold(
                onSuccess = { data ->
                    val messages = data.messages?.reversed() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages,
                        emoteInfos = data.e_infos ?: emptyList(),
                        hasMore = data.has_more == 1,
                        minSeqno = data.min_seqno,
                        videoPreviews = videoPreviewCache.toMap()
                    )
                    
                    // 标记为已读
                    data.max_seqno.takeIf { it > 0 }?.let { seqno ->
                        markAsRead(seqno)
                    }
                    
                    // 扫描并预加载视频信息
                    scanAndLoadVideoPreviews(messages)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            )
        }
    }
    
    /**
     * 扫描消息中的视频链接并预加载
     */
    private fun scanAndLoadVideoPreviews(messages: List<PrivateMessageItem>) {
        val bvids = mutableSetOf<String>()
        
        messages.forEach { msg ->
            if (msg.msg_type == 1) {
                val content = parseTextContent(msg.content)
                // 查找 BV 号
                bvPattern.findAll(content).forEach { match ->
                    bvids.add(match.value)
                }
            }
        }
        
        // 加载未缓存的视频信息
        bvids.forEach { bvid ->
            loadVideoPreview(bvid)
        }
    }
    
    /**
     * 加载单个视频预览信息
     */
    fun loadVideoPreview(bvid: String) {
        // 已缓存或正在加载则跳过
        if (videoPreviewCache.containsKey(bvid) || loadingBvids.contains(bvid)) {
            return
        }
        
        loadingBvids.add(bvid)
        
        viewModelScope.launch {
            try {
                val result = VideoRepository.getVideoDetails(bvid)
                result.onSuccess { (viewInfo, _) ->
                    val preview = VideoPreviewInfo(
                        bvid = viewInfo.bvid,
                        title = viewInfo.title,
                        cover = viewInfo.pic,
                        ownerName = viewInfo.owner.name,
                        viewCount = viewInfo.stat.view.toLong(),
                        duration = viewInfo.pages.firstOrNull()?.duration ?: 0
                    )
                    videoPreviewCache[bvid] = preview
                    
                    // 更新 UI
                    _uiState.value = _uiState.value.copy(
                        videoPreviews = videoPreviewCache.toMap()
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "Failed to load video preview for $bvid", e)
            } finally {
                loadingBvids.remove(bvid)
            }
        }
    }
    
    /**
     * 解析文本消息内容
     */
    /**
     * 解析文本消息内容
     * 增强版: 处理非JSON内容，以及提取 "content" 字段
     */
    private fun parseTextContent(content: String): String {
        if (content.isBlank()) return ""
        
        // 如果不是 JSON 格式 (不以 { 开头)，直接返回
        if (!content.trim().startsWith("{")) {
            return content
        }

        return try {
            val element = kotlinx.serialization.json.Json.parseToJsonElement(content)
            
            // 尝试提取 "content" 字段
            if (element is kotlinx.serialization.json.JsonObject) {
                 element["content"]?.let { 
                    if (it is kotlinx.serialization.json.JsonPrimitive) it.content else it.toString()
                 } ?: content // 如果没有 content 字段，返回原字符串
            } else {
                content
            }
        } catch (e: Exception) {
            // 解析失败，说明不是合法JSON，直接返回原文
            content
        }
    }
    
    private val kotlinx.serialization.json.JsonElement.jsonObject
        get() = this as? kotlinx.serialization.json.JsonObject
    
    private val kotlinx.serialization.json.JsonElement.jsonPrimitive
        get() = this as? kotlinx.serialization.json.JsonPrimitive
    
    private val kotlinx.serialization.json.JsonPrimitive.content: String
        get() = this.toString().trim('"')
    
    /**
     * 加载更多历史消息
     */
    fun loadMoreMessages() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            MessageRepository.getMessages(
                talkerId = talkerId,
                sessionType = sessionType,
                size = 30,
                endSeqno = _uiState.value.minSeqno
            ).fold(
                onSuccess = { data ->
                    val newMessages = data.messages?.reversed() ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        messages = newMessages + _uiState.value.messages,
                        hasMore = data.has_more == 1,
                        minSeqno = data.min_seqno
                    )
                    
                    // 扫描新消息中的视频链接
                    scanAndLoadVideoPreviews(newMessages)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }
    
    /**
     * 发送文字消息
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, sendError = null)
            
            MessageRepository.sendTextMessage(
                receiverId = talkerId,
                content = content.trim(),
                receiverType = sessionType
            ).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(isSending = false)
                    // 刷新消息列表
                    loadMessages()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        sendError = e.message ?: "发送失败"
                    )
                }
            )
        }
    }
    
    /**
     * 标记为已读
     */
    private fun markAsRead(seqno: Long) {
        viewModelScope.launch {
            MessageRepository.markAsRead(talkerId, sessionType, seqno)
        }
    }
    
    /**
     * 清除发送错误
     */
    fun clearSendError() {
        _uiState.value = _uiState.value.copy(sendError = null)
    }
    
    /**
     * 工厂类
     */
    class Factory(
        private val talkerId: Long,
        private val sessionType: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(talkerId, sessionType) as T
        }
    }
}
