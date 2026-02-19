// 文件路径: feature/dynamic/DynamicViewModel.kt
package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.feature.dynamic.components.DynamicDisplayMode

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.FollowingUser
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.data.repository.DynamicRepository
import com.android.purebilibili.data.repository.LiveRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max

/**
 *  动态页面 ViewModel
 * 支持：动态列表、侧边栏关注用户、在线状态
 */
class DynamicViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()
    private val cachePrefs = appContext.getSharedPreferences(PREFS_DYNAMIC_CACHE, Context.MODE_PRIVATE)
    private val userPrefs = appContext.getSharedPreferences(PREFS_DYNAMIC_USERS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private var cachedLiveRooms: List<LiveRoom> = emptyList()
    
    //  [新增] 缓存关注列表
    private var cachedFollowings: List<FollowingUser> = emptyList()
    private var incrementalTimelineRefreshEnabled: Boolean = false

    private val _uiState = MutableStateFlow(DynamicUiState())
    val uiState: StateFlow<DynamicUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    //  [修复] 加载锁，防止并发加载请求
    private var isLoadingLocked = false

    //  侧边栏相关状态
    private val _followedUsers = MutableStateFlow<List<SidebarUser>>(emptyList())
    val followedUsers: StateFlow<List<SidebarUser>> = _followedUsers.asStateFlow()

    private val _selectedUserId = MutableStateFlow<Long?>(null)
    val selectedUserId: StateFlow<Long?> = _selectedUserId.asStateFlow()

    private val _isSidebarExpanded = MutableStateFlow(true)
    val isSidebarExpanded: StateFlow<Boolean> = _isSidebarExpanded.asStateFlow()

    private val _pinnedUserIds = MutableStateFlow<Set<Long>>(emptySet())
    val pinnedUserIds: StateFlow<Set<Long>> = _pinnedUserIds.asStateFlow()

    private val _hiddenUserIds = MutableStateFlow<Set<Long>>(emptySet())
    val hiddenUserIds: StateFlow<Set<Long>> = _hiddenUserIds.asStateFlow()

    private val _showHiddenUsers = MutableStateFlow(false)
    val showHiddenUsers: StateFlow<Boolean> = _showHiddenUsers.asStateFlow()

    //  [新增] 显示模式状态
    private val _displayMode = MutableStateFlow(DynamicDisplayMode.SIDEBAR)
    val displayMode: StateFlow<DynamicDisplayMode> = _displayMode.asStateFlow()

    init {
        viewModelScope.launch {
            SettingsManager.getIncrementalTimelineRefresh(appContext).collect { enabled ->
                incrementalTimelineRefreshEnabled = enabled
            }
        }
        loadUserPreferences()
        loadCachedDynamics()
        rebuildFollowedUsers()
        refreshInBackground()
        //  [新增] 加载关注列表
        viewModelScope.launch { loadAllFollowings() }
    }
    
    private fun loadUserPreferences() {
        val pinned = userPrefs.getStringSet(KEY_PINNED_USERS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        val hidden = userPrefs.getStringSet(KEY_HIDDEN_USERS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        _pinnedUserIds.value = pinned
        _hiddenUserIds.value = hidden

        // 加载显示模式
        val modeName = userPrefs.getString(KEY_DISPLAY_MODE, DynamicDisplayMode.SIDEBAR.name)
        _displayMode.value = try {
            DynamicDisplayMode.valueOf(modeName ?: DynamicDisplayMode.SIDEBAR.name)
        } catch (e: Exception) {
            DynamicDisplayMode.SIDEBAR
        }
    }

    private fun saveUserPreferences(pinned: Set<Long>, hidden: Set<Long>) {
        userPrefs.edit()
            .putStringSet(KEY_PINNED_USERS, pinned.map { it.toString() }.toSet())
            .putStringSet(KEY_HIDDEN_USERS, hidden.map { it.toString() }.toSet())
            .apply()
    }

    private fun loadCachedDynamics() {
        val cachedJson = cachePrefs.getString(KEY_DYNAMIC_CACHE, null) ?: return
        runCatching { json.decodeFromString<List<DynamicItem>>(cachedJson) }
            .onSuccess { items ->
                if (items.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false,
                        error = null
                    )
                }
            }
    }

    private fun saveDynamicCache(items: List<DynamicItem>) {
        if (items.isEmpty()) return
        val payload = json.encodeToString(items.take(MAX_CACHE_ITEMS))
        cachePrefs.edit()
            .putString(KEY_DYNAMIC_CACHE, payload)
            .putLong(KEY_DYNAMIC_CACHE_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun refreshInBackground() {
        viewModelScope.launch { refreshData(showRefreshIndicator = false) }
    }

    private suspend fun refreshData(showRefreshIndicator: Boolean) {
        if (showRefreshIndicator) {
            _isRefreshing.value = true
        }
        coroutineScope {
            val dynamicJob = async {
                loadDynamicFeedInternal(refresh = true, showLoading = _uiState.value.items.isEmpty())
            }
            val liveJob = async { loadFollowedUsersInternal() }
            val followingsJob = async { loadAllFollowings() }
            dynamicJob.await()
            liveJob.await()
            followingsJob.await()
        }
        if (showRefreshIndicator) {
            _isRefreshing.value = false
        }
    }

    /**
     *  加载关注用户列表及其直播状态
     */
    fun loadFollowedUsers() {
        viewModelScope.launch { loadFollowedUsersInternal() }
    }

    private suspend fun loadFollowedUsersInternal() {
        LiveRepository.getFollowedLive(page = 1).onSuccess { liveRooms ->
            cachedLiveRooms = liveRooms
            rebuildFollowedUsers()
        }
    }
    
    /**
     *  [新增] 加载完整的关注列表
     */
    private suspend fun loadAllFollowings() {
        try {
            // 先获取当前用户 mid
            val navResponse = NetworkModule.api.getNavInfo()
            val myMid = navResponse.data?.mid ?: return
            
            // 加载关注列表（最多加载前 5 页，共 250 人）
            val allFollowings = mutableListOf<FollowingUser>()
            for (page in 1..5) {
                val response = NetworkModule.api.getFollowings(vmid = myMid, pn = page, ps = 50)
                val users = response.data?.list ?: break
                allFollowings.addAll(users)
                if (users.size < 50) break // 没有更多了
            }
            
            cachedFollowings = allFollowings
            rebuildFollowedUsers()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 从动态列表提取用户
     */
    private fun extractUsersFromDynamics(items: List<DynamicItem>): List<SidebarUser> {
        val latestByUser = mutableMapOf<Long, SidebarUser>()
        items.mapNotNull { it.modules.module_author }.forEach { author ->
            if (author.mid <= 0 || author.name.isBlank()) return@forEach
            val lastActive = author.pub_ts.takeIf { it > 0 } ?: 0L
            val existing = latestByUser[author.mid]
            if (existing == null || lastActive > existing.lastActiveTs) {
                latestByUser[author.mid] = SidebarUser(
                    uid = author.mid,
                    name = author.name,
                    face = author.face,
                    isLive = false,
                    lastActiveTs = lastActive
                )
            }
        }
        return latestByUser.values.toList()
    }

    /**
     * 从直播列表提取用户（包含在线状态）
     */
    private fun extractUsersFromLive(rooms: List<LiveRoom>): List<SidebarUser> {
        val nowSeconds = System.currentTimeMillis() / 1000
        return rooms.map { room ->
            SidebarUser(
                uid = room.uid,
                name = room.uname,
                face = room.face,
                isLive = true,
                lastActiveTs = nowSeconds  // 直播中视作最近活跃
            )
        }
    }

    private fun rebuildFollowedUsers() {
        val mergedUsers = mergeUsers(
            extractUsersFromDynamics(_uiState.value.items),
            extractUsersFromLive(cachedLiveRooms),
            extractUsersFromFollowings(cachedFollowings)  //  [新增]
        )
        _followedUsers.value = applyUserPreferences(mergedUsers)
    }
    
    /**
     *  [新增] 从关注列表转换为侧边栏用户
     */
    private fun extractUsersFromFollowings(followings: List<FollowingUser>): List<SidebarUser> {
        return followings.map { user ->
            SidebarUser(
                uid = user.mid,
                name = user.uname,
                face = user.face,
                isLive = false,
                lastActiveTs = 0  // 关注列表没有活跃时间，排序优先级最低
            )
        }
    }

    private fun mergeUsers(
        dynamicUsers: List<SidebarUser>,
        liveUsers: List<SidebarUser>,
        followingUsers: List<SidebarUser> = emptyList()  //  [新增]
    ): List<SidebarUser> {
        val merged = mutableMapOf<Long, SidebarUser>()
        //  先添加关注列表（基础优先级），再添加动态和直播用户覆盖
        (followingUsers + dynamicUsers + liveUsers).forEach { user ->
            val existing = merged[user.uid]
            if (existing == null) {
                merged[user.uid] = user
            } else {
                merged[user.uid] = existing.copy(
                    name = if (user.name.isNotBlank()) user.name else existing.name,
                    face = if (user.face.isNotBlank()) user.face else existing.face,
                    isLive = existing.isLive || user.isLive,
                    lastActiveTs = max(existing.lastActiveTs, user.lastActiveTs)
                )
            }
        }
        return merged.values.toList()
    }

    private fun applyUserPreferences(users: List<SidebarUser>): List<SidebarUser> {
        val pinned = _pinnedUserIds.value
        val hidden = _hiddenUserIds.value
        val showHidden = _showHiddenUsers.value
        return users
            .map { user ->
                user.copy(
                    isPinned = pinned.contains(user.uid),
                    isHidden = hidden.contains(user.uid)
                )
            }
            .filter { showHidden || !it.isHidden }
            .sortedWith(
                compareByDescending<SidebarUser> { it.isPinned }
                    .thenByDescending { it.isLive }
                    .thenByDescending { it.lastActiveTs }
                    .thenBy { it.name }
            )
    }
    
    /**
     *  [修改] 选择用户过滤动态 - 改为加载该用户的专属动态
     */
    fun selectUser(uid: Long?) {
        val previousUid = _selectedUserId.value
        _selectedUserId.value = uid
        
        if (uid != null && uid != previousUid) {
            // 加载该用户的动态
            viewModelScope.launch {
                DynamicRepository.resetUserPagination()
                loadUserDynamics(uid, refresh = true)
            }
        } else if (uid == null) {
            // 清空用户动态
            _uiState.value = _uiState.value.copy(userItems = emptyList(), hasUserMore = true)
        }
    }
    
    /**
     *  [新增] 加载指定用户的动态
     */
    private suspend fun loadUserDynamics(uid: Long, refresh: Boolean = false) {
        if (isLoadingLocked && !refresh) return
        isLoadingLocked = true
        
        try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = DynamicRepository.getUserDynamicFeed(uid, refresh)
            
            result.fold(
                onSuccess = { items ->
                    val currentItems = if (refresh) emptyList() else _uiState.value.userItems
                    val mergedItems = currentItems + items
                    _uiState.value = _uiState.value.copy(
                        userItems = mergedItems,
                        isLoading = false,
                        error = null,
                        hasUserMore = DynamicRepository.userHasMoreData()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载失败"
                    )
                }
            )
        } finally {
            kotlinx.coroutines.delay(300)
            isLoadingLocked = false
        }
    }
    
    /**
     *  [新增] 加载更多用户动态
     */
    fun loadMoreUserDynamics() {
        val uid = _selectedUserId.value ?: return
        if (!_uiState.value.hasUserMore || _uiState.value.isLoading || isLoadingLocked) return
        viewModelScope.launch {
            loadUserDynamics(uid, refresh = false)
        }
    }
    
    /**
     * 切换侧边栏展开/收起
     */
    fun toggleSidebar() {
        _isSidebarExpanded.value = !_isSidebarExpanded.value
    }

    fun togglePinUser(uid: Long) {
        val pinned = _pinnedUserIds.value.toMutableSet()
        if (pinned.contains(uid)) {
            pinned.remove(uid)
        } else {
            pinned.add(uid)
        }
        _pinnedUserIds.value = pinned
        saveUserPreferences(pinned, _hiddenUserIds.value)
        rebuildFollowedUsers()
    }

    fun toggleHiddenUser(uid: Long) {
        val hidden = _hiddenUserIds.value.toMutableSet()
        val pinned = _pinnedUserIds.value.toMutableSet()
        val isNowHidden = if (hidden.contains(uid)) {
            hidden.remove(uid)
            false
        } else {
            hidden.add(uid)
            true
        }
        if (isNowHidden) {
            pinned.remove(uid)
            if (_selectedUserId.value == uid) {
                _selectedUserId.value = null
            }
        }
        _hiddenUserIds.value = hidden
        _pinnedUserIds.value = pinned
        saveUserPreferences(pinned, hidden)
        rebuildFollowedUsers()
    }

    fun toggleShowHiddenUsers() {
        val showHidden = !_showHiddenUsers.value
        _showHiddenUsers.value = showHidden
        if (!showHidden) {
            val selected = _selectedUserId.value
            if (selected != null && _hiddenUserIds.value.contains(selected)) {
                _selectedUserId.value = null
            }
        }
        rebuildFollowedUsers()
    }

    /**
     *  [新增] 切换显示模式并保存
     */
    fun setDisplayMode(mode: DynamicDisplayMode) {
        _displayMode.value = mode
        userPrefs.edit()
            .putString(KEY_DISPLAY_MODE, mode.name)
            .apply()
    }
    
    /**
     * 加载动态列表
     */
    fun loadDynamicFeed(refresh: Boolean = false) {
        if (!refresh && (_uiState.value.isLoading || _isRefreshing.value || isLoadingLocked)) return
        viewModelScope.launch {
            loadDynamicFeedInternal(
                refresh = refresh,
                showLoading = refresh && _uiState.value.items.isEmpty()
            )
        }
    }

    private suspend fun loadDynamicFeedInternal(
        refresh: Boolean,
        showLoading: Boolean = false
    ) {
        //  [修复] 使用加载锁防止并发请求
        if (isLoadingLocked && !refresh) return
        isLoadingLocked = true
        
        try {
            if (refresh) {
                if (showLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                } else {
                    _uiState.value = _uiState.value.copy(error = null)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            // [新增] 检查登录状态
            if (com.android.purebilibili.core.store.TokenManager.sessDataCache.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "未登录，请先登录",
                    items = emptyList()
                )
                return
            }

            val result = DynamicRepository.getDynamicFeed(refresh)

            result.fold(
                onSuccess = { items ->
                    val currentItems = _uiState.value.items
                    val mergedItems = when {
                        refresh && incrementalTimelineRefreshEnabled -> prependDistinctByKey(
                            existing = currentItems,
                            incoming = items,
                            keySelector = ::dynamicItemKey
                        )
                        refresh -> items
                        else -> appendDistinctByKey(
                            existing = currentItems,
                            incoming = items,
                            keySelector = ::dynamicItemKey
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        items = mergedItems,
                        isLoading = false,
                        error = null,
                        hasMore = DynamicRepository.hasMoreData()
                    )
                    saveDynamicCache(mergedItems)
                    rebuildFollowedUsers()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载失败"
                    )
                }
            )
        } finally {
            //  延迟解锁，防止快速连续请求
            kotlinx.coroutines.delay(300)
            isLoadingLocked = false
        }
    }
    
    fun refresh() {
        viewModelScope.launch { refreshData(showRefreshIndicator = true) }
    }
    
    fun loadMore() {
        if (!_uiState.value.hasMore || _uiState.value.isLoading || _isRefreshing.value || isLoadingLocked) return
        loadDynamicFeed(refresh = false)
    }

    private fun dynamicItemKey(item: DynamicItem): String {
        if (item.id_str.isNotBlank()) return item.id_str
        val authorMid = item.modules.module_author?.mid ?: 0L
        val pubTs = item.modules.module_author?.pub_ts ?: 0L
        return "${item.type}-$authorMid-$pubTs"
    }
    
    // ====================  动态评论/点赞/转发功能 ====================
    
    // 当前选中的动态（用于评论弹窗）
    private val _selectedDynamic = MutableStateFlow<DynamicItem?>(null)
    val selectedDynamicId: StateFlow<String?> = _selectedDynamic.asStateFlow().let { flow ->
        MutableStateFlow<String?>(null).also { derived ->
            viewModelScope.launch {
                flow.collect { derived.value = it?.id_str }
            }
        }
    }
    
    // 评论列表
    private val _comments = MutableStateFlow<List<com.android.purebilibili.data.model.response.ReplyItem>>(emptyList())
    val comments: StateFlow<List<com.android.purebilibili.data.model.response.ReplyItem>> = _comments.asStateFlow()
    
    // [新增] 动态评论总数 (从评论接口获取实时数据)
    private val _commentTotalCount = MutableStateFlow(0)
    val commentTotalCount: StateFlow<Int> = _commentTotalCount.asStateFlow()
    
    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()
    
    // 点赞状态缓存 (dynamicId -> isLiked)
    private val _likedDynamics = MutableStateFlow<Set<String>>(emptySet())
    val likedDynamics: StateFlow<Set<String>> = _likedDynamics.asStateFlow()
    
    /**
     *  [修复] 根据动态类型获取评论 oid 和 type
     * 
     * 方案1 (推荐): 使用 API 返回的 basic.comment_id_str 和 basic.comment_type
     * 方案2 (备用): 根据动态类型手动推断
     * 
     * 评论区类型参考: https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/comment/readme.md
     * - type=1, oid=aid → 视频
     * - type=11, oid=相簿id → 图片动态 (DRAW)
     * - type=17, oid=动态id → 纯文字/图文动态 (WORD/OPUS)
     */
    private fun getCommentParams(item: DynamicItem): Pair<Long, Int>? {
        // [方案1] 优先使用 API 返回的 basic 参数 - 这是最可靠的方式
        val basic = item.basic
        if (basic != null && basic.comment_id_str.isNotEmpty() && basic.comment_type > 0) {
            val oid = basic.comment_id_str.toLongOrNull()
            if (oid != null) {
                com.android.purebilibili.core.util.Logger.d("DynamicVM", 
                    "使用 basic 参数: oid=$oid, type=${basic.comment_type}")
                return Pair(oid, basic.comment_type)
            }
        }
        
        // [方案2] 备用: 根据 major.type 和 item.type 推断
        val major = item.modules.module_dynamic?.major
        val majorType = major?.type ?: ""
        
        com.android.purebilibili.core.util.Logger.d("DynamicVM", 
            "getCommentParams fallback: id_str=${item.id_str}, item.type=${item.type}, major.type=$majorType")
        
        // MAJOR_TYPE_OPUS 是 B站新版图文动态格式，统一使用 type=17
        if (majorType == "MAJOR_TYPE_OPUS") {
            val oid = item.id_str.toLongOrNull()
            com.android.purebilibili.core.util.Logger.d("DynamicVM", "OPUS 动态: oid=$oid, type=17")
            return if (oid != null) Pair(oid, 17) else null
        }
        
        // MAJOR_TYPE_ARCHIVE 是视频
        if (majorType == "MAJOR_TYPE_ARCHIVE") {
            val aid = major?.archive?.aid?.toLongOrNull()
            com.android.purebilibili.core.util.Logger.d("DynamicVM", "视频动态: aid=$aid, type=1")
            return if (aid != null) Pair(aid, 1) else null
        }
        
        // MAJOR_TYPE_DRAW 是旧版图片动态（相簿）
        if (majorType == "MAJOR_TYPE_DRAW") {
            val drawId = major?.draw?.id
            if (drawId != null && drawId > 0) {
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "图片动态: drawId=$drawId, type=11")
                return Pair(drawId, 11)
            }
        }
        
        // 根据 item.type 判断（兼容旧逻辑）
        val dynamicType = com.android.purebilibili.data.model.response.DynamicType.fromApiValue(item.type)
        return when (dynamicType) {
            com.android.purebilibili.data.model.response.DynamicType.VIDEO -> {
                val aid = major?.archive?.aid?.toLongOrNull()
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "VIDEO 类型: aid=$aid")
                if (aid != null) Pair(aid, 1) else null
            }
            com.android.purebilibili.data.model.response.DynamicType.DRAW -> {
                // 图片动态: 优先用 draw.id，没有则用 id_str + type=17
                val drawId = major?.draw?.id
                if (drawId != null && drawId > 0) {
                    com.android.purebilibili.core.util.Logger.d("DynamicVM", "DRAW 类型: drawId=$drawId, type=11")
                    Pair(drawId, 11)
                } else {
                    // OPUS 或其他没有 draw.id 的情况
                    val oid = item.id_str.toLongOrNull()
                    com.android.purebilibili.core.util.Logger.d("DynamicVM", "DRAW fallback: oid=$oid, type=17")
                    oid?.let { Pair(it, 17) }
                }
            }
            com.android.purebilibili.data.model.response.DynamicType.WORD -> {
                val oid = item.id_str.toLongOrNull()
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "WORD 类型: oid=$oid, type=17")
                if (oid != null) Pair(oid, 17) else null
            }
            com.android.purebilibili.data.model.response.DynamicType.FORWARD -> {
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "FORWARD 类型: 递归获取原动态参数")
                item.orig?.let { getCommentParams(it) }
            }
            else -> {
                // 未知类型: 统一使用 type=17 (动态评论)
                val oid = item.id_str.toLongOrNull()
                com.android.purebilibili.core.util.Logger.w("DynamicVM", "未知动态类型: ${item.type}, oid=$oid, type=17")
                oid?.let { Pair(it, 17) }
            }
        }
    }
    
    /**
     *  [修复] 根据动态ID获取动态对象 - 同时搜索 items 和 userItems
     */
    private fun findDynamicById(dynamicId: String): DynamicItem? {
        // 先在全部动态中搜索
        _uiState.value.items.find { it.id_str == dynamicId }?.let { return it }
        // 再在用户专属动态中搜索
        return _uiState.value.userItems.find { it.id_str == dynamicId }
    }
    
    /**
     *  打开评论弹窗
     */
    fun openCommentSheet(dynamicId: String) {
        val item = findDynamicById(dynamicId)
        _selectedDynamic.value = item
        if (item != null) {
            loadCommentsForDynamic(item)
        }
    }
    
    /**
     *  关闭评论弹窗
     */
    fun closeCommentSheet() {
        _selectedDynamic.value = null
        _comments.value = emptyList()
        // [新增] 清空计数
        _commentTotalCount.value = 0
    }
    
    /**
     *  加载动态评论 (使用正确的 oid 和 type)
     */
    private fun loadCommentsForDynamic(item: DynamicItem) {
        viewModelScope.launch {
            _commentsLoading.value = true
            // [新增] 默认先使用动态列表里的计数作为初始值
            _commentTotalCount.value = item.modules.module_stat?.comment?.count ?: 0
            
            try {
                val params = getCommentParams(item)
                if (params == null) {
                    com.android.purebilibili.core.util.Logger.e("DynamicVM", "无法获取评论参数: type=${item.type}")
                    return@launch
                }
                val (oid, type) = params
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "加载评论: oid=$oid, type=$type, dynamicType=${item.type}")
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .getDynamicReplies(oid = oid, type = type)
                com.android.purebilibili.core.util.Logger.d("DynamicVM", "评论响应: code=${response.code}, message=${response.message}, replies=${response.data?.replies?.size}")
                if (response.code == 0 && response.data != null) {
                    _comments.value = response.data.replies ?: emptyList()
                    // [新增] 更新评论总数 (优先使用 page.count 或 acount)
                    val realCount = response.data.page.count
                    if (realCount > 0) {
                        _commentTotalCount.value = realCount
                    }
                } else {
                    com.android.purebilibili.core.util.Logger.e("DynamicVM", "评论加载失败: code=${response.code}, msg=${response.message}")
                }
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.e("DynamicVM", "加载评论异常: ${e.message}")
                e.printStackTrace()
            } finally {
                _commentsLoading.value = false
            }
        }
    }
    
    /**
     *  加载评论 (兼容旧调用方式)
     */
    fun loadComments(dynamicId: String) {
        val item = findDynamicById(dynamicId)
        if (item != null) {
            loadCommentsForDynamic(item)
        }
    }
    
    /**
     *  发表评论
     */
    fun postComment(dynamicId: String, message: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val oid = dynamicId.toLongOrNull() ?: return@launch
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .addDynamicReply(oid = oid, type = 17, message = message, csrf = csrf)
                if (response.code == 0) {
                    onResult(true, "评论成功")
                    // 刷新评论列表
                    loadComments(dynamicId)
                } else {
                    onResult(false, response.message ?: "评论失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
    
    /**
     *  点赞动态
     */
    fun likeDynamic(dynamicId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val isLiked = _likedDynamics.value.contains(dynamicId)
                val up = if (isLiked) 2 else 1  // 1=点赞, 2=取消
                
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .likeDynamic(dynamicId = dynamicId, up = up, csrf = csrf)
                if (response.code == 0) {
                    // 更新本地状态
                    _likedDynamics.value = if (isLiked) {
                        _likedDynamics.value - dynamicId
                    } else {
                        _likedDynamics.value + dynamicId
                    }
                    onResult(true, if (isLiked) "已取消" else "已点赞")
                } else {
                    onResult(false, response.message ?: "操作失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }
    
    /**
     *  转发动态
     */
    fun repostDynamic(dynamicId: String, content: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
                if (csrf.isNullOrEmpty()) {
                    onResult(false, "请先登录")
                    return@launch
                }
                val response = com.android.purebilibili.core.network.NetworkModule.dynamicApi
                    .repostDynamic(dynIdStr = dynamicId, content = content, csrf = csrf)
                if (response.code == 0) {
                    onResult(true, "转发成功")
                } else {
                    onResult(false, response.message ?: "转发失败")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "网络错误")
            }
        }
    }

    companion object {
        private const val PREFS_DYNAMIC_CACHE = "dynamic_cache"
        private const val PREFS_DYNAMIC_USERS = "dynamic_user_prefs"
        private const val KEY_DYNAMIC_CACHE = "dynamic_items_cache"
        private const val KEY_DYNAMIC_CACHE_TIME = "dynamic_cache_time"
        private const val KEY_PINNED_USERS = "dynamic_pinned_users"
        private const val KEY_HIDDEN_USERS = "dynamic_hidden_users"
        private const val KEY_DISPLAY_MODE = "dynamic_display_mode"
        private const val MAX_CACHE_ITEMS = 100
    }
}

/**
 *  侧边栏用户数据
 */
data class SidebarUser(
    val uid: Long,
    val name: String,
    val face: String,
    val isLive: Boolean = false,
    val lastActiveTs: Long = 0L,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false
)

/**
 * 动态页面 UI 状态
 */
data class DynamicUiState(
    val items: List<DynamicItem> = emptyList(),
    val userItems: List<DynamicItem> = emptyList(), //  [新增] 选中 UP主的动态
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val hasUserMore: Boolean = true //  [新增] UP主动态是否有更多
)
