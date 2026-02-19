// 私信收件箱 ViewModel
package com.android.purebilibili.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.MessageUnreadData
import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 用户简要信息 (用于缓存)
 */
data class UserBasicInfo(
    val mid: Long,
    val name: String,
    val face: String
)

data class InboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val sessions: List<SessionItem> = emptyList(),
    val unreadData: MessageUnreadData? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val endTs: Long = 0, //  游标 (此会话列表中最后一条的 session_ts，微秒级)
    val userInfoMap: Map<Long, UserBasicInfo> = emptyMap()  //  用户信息缓存
)

class InboxViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    
    // 用户信息缓存 (跨刷新保持)
    private val userCache = mutableMapOf<Long, UserBasicInfo>()
    
    // WBI keys  缓存
    private var cachedImgKey: String = ""
    private var cachedSubKey: String = ""
    
    init {
        loadSessions()
    }
    
    /**
     * 加载会话列表
     */
    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 并行加载未读数和会话列表
            val unreadResult = MessageRepository.getUnreadCount()
            // 初始加载，endTs = 0
            val sessionsResult = MessageRepository.getSessions(
                sessionType = 4, // 4=所有消息 (包含企业号自动回复)
                endTs = 0
            )
            
            unreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(unreadData = data)
            }
            
            sessionsResult.fold(
                onSuccess = { data ->
                    val sessions = data.session_list ?: emptyList()
                    
                    //  计算下一次加载的游标
                    val lastSession = sessions.lastOrNull()
                    val nextEndTs = if (lastSession != null) {
                        // B站 API 需要微秒级时间戳作为游标
                        // session_ts 是秒级，或者已经是毫秒级？
                        // 通常 API 返回的是秒 (10位) 或者 毫秒 (13位)
                        // fetch_session_msgs 使用的是 end_seqno
                        // get_sessions 使用的是 session_ts
                        // 根据经验，B站 API 的 end_ts 通常是 session_time * 1000000 (微秒)
                        // 或者直接取列表最后一条的 session_ts (如果已经是长整型)
                        lastSession.session_ts * 1000000L
                    } else {
                        0L
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sessions = sessions,
                        hasMore = data.has_more == 1,
                        userInfoMap = userCache.toMap(),
                        endTs = nextEndTs
                    )
                    
                    // 异步加载用户信息
                    loadUserInfos(sessions.map { it.talker_id })
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
     * 获取 WBI keys (从 nav 接口)
     */
    private suspend fun ensureWbiKeys(): Boolean = withContext(Dispatchers.IO) {
        if (cachedImgKey.isNotEmpty() && cachedSubKey.isNotEmpty()) {
            return@withContext true
        }
        
        try {
            val navResp = NetworkModule.api.getNavInfo()
            val wbiImg = navResp.data?.wbi_img ?: return@withContext false
            cachedImgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
            cachedSubKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
            true
        } catch (e: Exception) {
            android.util.Log.e("InboxVM", "ensureWbiKeys error: ${e.message}")
            false
        }
    }
    
    /**
     * 异步批量加载用户信息
     */
    private fun loadUserInfos(mids: List<Long>) {
        viewModelScope.launch {
            // 确保有 WBI keys
            if (!ensureWbiKeys()) {
                android.util.Log.e("InboxVM", "Failed to get WBI keys, cannot fetch user info")
                return@launch
            }
            
            // 仅拉取缺失或缓存不完整的用户，避免空值缓存导致后续页面显示缺失
            val toFetch = mids
                .distinct()
                .filter { InboxUserInfoResolver.shouldFetchUserInfo(it, userCache) }
            
            toFetch.forEach { mid ->
                launch {
                    val merged = InboxUserInfoResolver.mergeFetchedUserInfo(
                        existing = userCache[mid],
                        fetched = fetchUserInfo(mid)
                    ) ?: return@launch

                    userCache[mid] = merged
                    // 更新UI状态
                    _uiState.value = _uiState.value.copy(
                        userInfoMap = userCache.toMap()
                    )
                }
            }
        }
    }
    
    /**
     * 获取单个用户信息
     */
    private suspend fun fetchUserInfo(mid: Long): UserBasicInfo? = withContext(Dispatchers.IO) {
        try {
            // WBI签名
            val params = WbiUtils.sign(
                mapOf("mid" to mid.toString()),
                cachedImgKey,
                cachedSubKey
            )
            val response = NetworkModule.spaceApi.getSpaceInfo(params)
            
            if (response.code == 0 && response.data != null) {
                InboxUserInfoResolver.mergeFetchedUserInfo(
                    existing = null,
                    fetched = UserBasicInfo(
                        mid = response.data.mid,
                        name = response.data.name,
                        face = response.data.face
                    )
                )
            } else {
                android.util.Log.w("InboxVM", "fetchUserInfo failed for $mid: ${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("InboxVM", "fetchUserInfo exception for $mid", e)
            null
        }
    }
    
    /**
     * 加载更多会话
     */
    fun loadMoreSessions() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        val nextPage = _uiState.value.page + 1

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            MessageRepository.getSessions(
                sessionType = 4, // 4=所有消息
                page = nextPage, // 使用下一页
                endTs = 0L // [关键] 禁用时间游标，仅使用页码
            ).fold(
                onSuccess = { data ->
                    val newSessions = data.session_list ?: emptyList()
                    
                    // 去重合并: 过滤掉已存在的会话 (基于 talker_id 和 session_type)
                    val existingKeys = _uiState.value.sessions.map { "${it.talker_id}_${it.session_type}" }.toSet()
                    val filteredNewSessions = newSessions.filter { 
                        "${it.talker_id}_${it.session_type}" !in existingKeys
                    }
                    
                    val allSessions = _uiState.value.sessions + filteredNewSessions
                    
                    // 计算更新后的 cursor (虽然不再用于请求，但可以保留在状态中作为参考，或者直接置0)
                    val nextEndTs = 0L
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        sessions = allSessions,
                        hasMore = data.has_more == 1 && filteredNewSessions.isNotEmpty(),
                        page = nextPage, // Update page
                        userInfoMap = userCache.toMap(),
                        endTs = nextEndTs
                    )

                    // 异步加载用户信息
                    loadUserInfos(filteredNewSessions.map { it.talker_id })
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    /**
     * 下拉刷新
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            
            val unreadResult = MessageRepository.getUnreadCount()
            // 刷新时重置游标
            val sessionsResult = MessageRepository.getSessions(
                sessionType = 4, // 4=所有消息
                endTs = 0
            )
            
            unreadResult.onSuccess { data ->
                _uiState.value = _uiState.value.copy(unreadData = data)
            }
            
            sessionsResult.fold(
                onSuccess = { data ->
                    val sessions = data.session_list ?: emptyList()
                    
                    // 计算 cursor
                    val lastSession = sessions.lastOrNull()
                    val nextEndTs = if (lastSession != null) {
                        lastSession.session_ts * 1000000L
                    } else {
                        0L
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        sessions = sessions,
                        hasMore = data.has_more == 1,
                        userInfoMap = userCache.toMap(),
                        endTs = nextEndTs
                    )
                    
                    // 异步加载用户信息
                    loadUserInfos(sessions.map { it.talker_id })
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        error = e.message ?: "刷新失败"
                    )
                }
            )
        }
    }
    
    /**
     * 移除会话
     */
    fun removeSession(session: SessionItem) {
        viewModelScope.launch {
            MessageRepository.removeSession(session.talker_id, session.session_type)
                .onSuccess {
                    // 从列表中移除
                    val newList = _uiState.value.sessions.filter { 
                        it.talker_id != session.talker_id 
                    }
                    _uiState.value = _uiState.value.copy(sessions = newList)
                }
        }
    }
    
    /**
     * 置顶/取消置顶会话
     */
    fun toggleTop(session: SessionItem) {
        viewModelScope.launch {
            val isCurrentlyTop = session.top_ts > 0
            MessageRepository.setSessionTop(session.talker_id, session.session_type, !isCurrentlyTop)
                .onSuccess {
                    // 刷新列表
                    refresh()
                }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
