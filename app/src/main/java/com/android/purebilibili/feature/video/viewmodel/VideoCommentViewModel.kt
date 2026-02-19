package com.android.purebilibili.feature.video.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.repository.CommentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 评论排序模式：
// - mode=3: 最热（WBI）
// - mode=2: 最新（Legacy sort=0）
// - mode=4: 点赞最多（Legacy sort=1）
// - mode=1: 回复最多（Legacy sort=2）
enum class CommentSortMode(val apiMode: Int, val label: String) {
    HOT(3, "最热"),       // 按热度排序 (mode=3, 默认)
    NEWEST(2, "最新"),    // 按时间排序（最新优先）(mode=2)
    LIKE(4, "点赞"),      // 按点赞数排序 (使用旧版 API sort=1)
    REPLY(1, "回复");     // 按回复数排序 (使用旧版 API sort=2)

    companion object {
        fun fromApiMode(mode: Int): CommentSortMode = entries.find { it.apiMode == mode } ?: HOT
    }
}

// 评论状态
data class CommentUiState(
    val replies: List<ReplyItem> = emptyList(),
    val isRepliesLoading: Boolean = false,
    val replyCount: Int = 0,
    val repliesError: String? = null,
    val isRepliesEnd: Boolean = false,
    val nextPage: Int = 1,
    //  [新增] 排序和筛选状态
    val sortMode: CommentSortMode = CommentSortMode.HOT,
    val upOnlyFilter: Boolean = false,
    val upMid: Long = 0,  // UP主的 mid，用于筛选
    // [新增] 评论交互状态
    val isSending: Boolean = false,
    val sendError: String? = null,
    val replyTarget: ReplyItem? = null,  // 回复目标评论（为空则是一级评论）
    val likedComments: Set<Long> = emptySet(),  // 已点赞的评论 rpid 集合
    val hatedComments: Set<Long> = emptySet(),   // 已点踩的评论 rpid 集合
    // [新增] 删除与动画状态
    val dissolvingIds: Set<Long> = emptySet(), // 正在播放消散动画的评论 ID
    // [新增] 当前登录用户 Mid (直接暴露以便 UI 判断是否显示删除按钮)
    val currentMid: Long = 0,
    // [新增] 评论输入控制
    val rootInputHint: String = "进来唠会嗑呗~",
    val childInputHint: String = "回复一下吧~",
    val canUploadImage: Boolean = true,
    val canInputComment: Boolean = true
)

// 二级评论状态 (从 PlayerViewModel 移过来)
data class SubReplyUiState(
    val visible: Boolean = false,
    val rootReply: ReplyItem? = null,
    val items: List<ReplyItem> = emptyList(),
    val isLoading: Boolean = false,
    val page: Int = 1,
    val isEnd: Boolean = false,
    val error: String? = null,
    val upMid: Long = 0,
    // [新增] 消散动画状态
    val dissolvingIds: Set<Long> = emptySet()
)

class VideoCommentViewModel : ViewModel() {
    private val _commentState = MutableStateFlow(CommentUiState())
    val commentState = _commentState.asStateFlow()

    private val _subReplyState = MutableStateFlow(SubReplyUiState())
    val subReplyState = _subReplyState.asStateFlow()

    private var currentAid: Long = 0
    
    //  存储原始评论列表（未经筛选），用于筛选切换
    private var allReplies: List<ReplyItem> = emptyList()

    // 初始化/重置
    fun init(
        aid: Long,
        upMid: Long = 0,
        preferredSortMode: CommentSortMode = CommentSortMode.HOT
    ) {
        android.util.Log.d("CommentVM", " init called with aid=$aid, upMid=$upMid, currentAid=$currentAid")
        if (currentAid == aid && _commentState.value.upMid == upMid) {
            // [修复] 即使视频相同，也刷新 currentMid（防止登录状态变化后不更新）
            refreshCurrentMid()
            return
        }
        currentAid = aid
        allReplies = emptyList()
        // 获取当前登录用户 mid
        val myMid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
        android.util.Log.d("CommentVM", " init: myMid=$myMid")
        _commentState.value = CommentUiState(
            sortMode = preferredSortMode,
            upMid = upMid,
            currentMid = myMid
        )
        loadComments()
    }
    
    // [新增] 刷新当前登录用户 mid
    fun refreshCurrentMid() {
        val myMid = com.android.purebilibili.core.store.TokenManager.midCache ?: 0L
        val current = _commentState.value
        if (current.currentMid != myMid) {
            android.util.Log.d("CommentVM", " refreshCurrentMid: ${current.currentMid} -> $myMid")
            _commentState.value = current.copy(currentMid = myMid)
        }
    }
    
    //  [新增] 设置 UP 主 mid（用于只看UP主筛选）
    fun setUpMid(mid: Long) {
        if (_commentState.value.upMid != mid) {
            _commentState.value = _commentState.value.copy(upMid = mid)
        }
    }

    //  [修复] 切换排序模式 - 与"只看UP主"互斥
    fun setSortMode(mode: CommentSortMode) {
        val currentState = _commentState.value
        if (currentState.sortMode == mode && !currentState.upOnlyFilter) return
        
        android.util.Log.d("CommentVM", " setSortMode: ${currentState.sortMode} -> $mode, clearing upOnlyFilter")
        
        //  [修复] 切换排序时清除"只看UP主"筛选
        allReplies = emptyList()
        _commentState.value = CommentUiState(
            sortMode = mode,
            upOnlyFilter = false,  //  互斥：清除 UP 筛选
            upMid = currentState.upMid,
            currentMid = currentState.currentMid
        )
        loadComments()
    }
    
    //  [修复] 切换只看UP主筛选 - 与"最热/最新"互斥
    fun toggleUpOnly() {
        val currentState = _commentState.value
        val newUpOnly = !currentState.upOnlyFilter
        
        android.util.Log.d("CommentVM", " toggleUpOnly: $newUpOnly, upMid=${currentState.upMid}")
        
        if (newUpOnly) {
            //  [修复] 开启 UP 筛选时，从当前已加载的评论中筛选
            val filteredReplies = if (currentState.upMid > 0) {
                allReplies.filter { it.mid == currentState.upMid }
            } else {
                emptyList()
            }
            
            _commentState.value = currentState.copy(
                upOnlyFilter = true,
                replies = filteredReplies
            )
        } else {
            //  关闭 UP 筛选时，恢复显示所有评论
            _commentState.value = currentState.copy(
                upOnlyFilter = false,
                replies = allReplies
            )
        }
    }

    fun loadComments() {
        val currentState = _commentState.value
        if (currentState.isRepliesEnd || currentState.isRepliesLoading) return

        _commentState.value = currentState.copy(isRepliesLoading = true, repliesError = null)

        viewModelScope.launch {
            val pageToLoad = currentState.nextPage
            //  使用当前排序模式
            val result = CommentRepository.getComments(
                aid = currentAid, 
                page = pageToLoad, 
                ps = 20,
                mode = currentState.sortMode.apiMode
            )

            result.onSuccess { data ->
                val current = _commentState.value
                val newReplies = data.replies ?: emptyList()
                
                // 第一页时先合并置顶和热评
                val topReplies = if (pageToLoad == 1) data.collectTopReplies() else emptyList()
                val hotReplies = if (pageToLoad == 1) data.hots ?: emptyList() else emptyList()
                
                // 合并到原始列表（置顶 -> 热评 -> 普通评论）
                val combinedReplies = if (pageToLoad == 1) {
                    (topReplies + hotReplies + newReplies).distinctBy { it.rpid }
                } else {
                    (allReplies + newReplies).distinctBy { it.rpid }
                }
                allReplies = combinedReplies
                
                // 统一获取评论总数和结束标志 (兼容 WBI 和旧版 API)
                val totalCount = data.getAllCount()
                val isEnd = if (data.cursor.allCount > 0) {
                    data.cursor.isEnd
                } else {
                    data.getIsEnd(pageToLoad, combinedReplies.size) || newReplies.isEmpty()
                }
                
                android.util.Log.d(
                    "CommentVM",
                    " loadComments result: page=$pageToLoad, new=${newReplies.size}, hot=${hotReplies.size}, top=${topReplies.size}, total=${allReplies.size}, allCount=$totalCount, isEnd=$isEnd"
                )
                
                //  [修复] 加载后重新应用筛选（确保排序切换后筛选仍生效）
                val filteredReplies = if (current.upOnlyFilter && current.upMid > 0) {
                    combinedReplies.filter { it.mid == current.upMid }
                } else {
                    combinedReplies
                }
                
                _commentState.value = current.copy(
                    replies = filteredReplies,
                    replyCount = totalCount,
                    isRepliesLoading = false,
                    repliesError = null,
                    isRepliesEnd = isEnd,
                    nextPage = pageToLoad + 1,
                    rootInputHint = data.control?.rootInputText?.takeIf { it.isNotBlank() } ?: current.rootInputHint,
                    childInputHint = data.control?.childInputText?.takeIf { it.isNotBlank() } ?: current.childInputHint,
                    canUploadImage = data.control?.canUploadPicture ?: current.canUploadImage,
                    canInputComment = data.control?.inputDisable?.not() ?: current.canInputComment
                )
            }.onFailure { e ->
                android.util.Log.e("CommentVM", " loadComments error: ${e.message}")
                _commentState.value = _commentState.value.copy(
                    isRepliesLoading = false,
                    repliesError = e.message ?: "加载评论失败",
                    isRepliesEnd = true  //  [修复] 出错时也标记为结束，防止无限重试
                )
            }
        }
    }

    // --- 二级评论逻辑 ---

    fun openSubReply(rootReply: ReplyItem) {
        _subReplyState.value = SubReplyUiState(
            visible = true,
            rootReply = rootReply,
            isLoading = true,
            page = 1,
            upMid = _commentState.value.upMid  // [修复] 使用正确的 UP 主 mid
        )
        loadSubReplies(rootReply.oid, rootReply.rpid, 1)
    }

    fun closeSubReply() {
        _subReplyState.value = _subReplyState.value.copy(visible = false)
    }

    fun loadMoreSubReplies() {
        val state = _subReplyState.value
        if (state.isLoading || state.isEnd || state.rootReply == null) return
        val nextPage = state.page + 1
        _subReplyState.value = state.copy(isLoading = true)
        loadSubReplies(state.rootReply.oid, state.rootReply.rpid, nextPage)
    }

    private fun loadSubReplies(oid: Long, rootId: Long, page: Int) {
        viewModelScope.launch {
            val result = CommentRepository.getSubComments(oid, rootId, page)
            result.onSuccess { data ->
                val current = _subReplyState.value
                val newItems = data.replies ?: emptyList()
                val isEnd = data.cursor.isEnd || newItems.isEmpty()

                _subReplyState.value = current.copy(
                    items = if (page == 1) newItems else (current.items + newItems).distinctBy { it.rpid },
                    isLoading = false,
                    page = page,
                    isEnd = isEnd,
                    error = null
                )
            }.onFailure {
                _subReplyState.value = _subReplyState.value.copy(
                    isLoading = false,
                    error = it.message
                )
            }
        }
    }
    
    // --- [新增] 评论交互逻辑 ---
    
    fun sendComment(message: String) {
        if (message.isBlank()) return
        val currentState = _commentState.value
        if (currentState.isSending) return
        
        _commentState.value = currentState.copy(isSending = true, sendError = null)
        
        viewModelScope.launch {
            val replyTarget = currentState.replyTarget
            // [修复] 正确计算 root ID
            // 如果是在二级评论页回复，root 为当前二级评论页的根评论 ID
            // 如果是一级评论页回复某评论，root 为该评论 ID
            // 如果是直接发表评论，root 为 0
            val subReplyState = _subReplyState.value
            val isSubReplyContext = subReplyState.visible && subReplyState.rootReply != null
            
            val root = if (isSubReplyContext) {
                subReplyState.rootReply!!.rpid
            } else {
                replyTarget?.rpid ?: 0
            }
            // parent 总是回复目标的 ID (如果没有回复目标，则是 0)
            val parent = replyTarget?.rpid ?: 0
            
            val result = CommentRepository.addComment(currentAid, message, root, parent)
            
            result.onSuccess { newReply ->
                android.util.Log.d("CommentVM", " sendComment success: newReply=${newReply?.rpid}, root=$root, parent=$parent")
                val current = _commentState.value
                
                // 1. 如果是主层级评论 (root=0)
                if (root == 0L) {
                    if (newReply != null) {
                        // 有返回评论对象，直接添加到列表顶部
                        val updatedReplies = listOf(newReply) + allReplies
                        allReplies = updatedReplies
                        _commentState.value = current.copy(
                            replies = updatedReplies,
                            isSending = false,
                            sendError = null,
                            replyTarget = null,
                            replyCount = current.replyCount + 1
                        )
                    } else {
                        // [修复] newReply 为 null 时，重新加载评论列表以显示新评论
                        android.util.Log.d("CommentVM", " sendComment: newReply is null, reloading comments...")
                        _commentState.value = current.copy(
                            isSending = false,
                            sendError = null,
                            replyTarget = null
                        )
                        // 重置并重新加载
                        allReplies = emptyList()
                        _commentState.value = _commentState.value.copy(
                            replies = emptyList(),
                            nextPage = 1,
                            isRepliesEnd = false
                        )
                        loadComments()
                    }
                } 
                // 2. 如果是二级评论，刷新二级评论列表
                else if (isSubReplyContext) {
                    if (newReply != null) {
                        // 有返回评论对象，直接添加到列表顶部
                        val currentSub = _subReplyState.value
                        _subReplyState.value = currentSub.copy(
                            items = listOf(newReply) + currentSub.items,
                        )
                    } else {
                        // [修复] newReply 为 null 时，重新加载二级评论列表
                        android.util.Log.d("CommentVM", " sendComment: sub-reply newReply is null, reloading...")
                        val currentSub = _subReplyState.value
                        currentSub.rootReply?.let { root ->
                            _subReplyState.value = currentSub.copy(
                                items = emptyList(),
                                page = 1,
                                isEnd = false,
                                isLoading = true
                            )
                            loadSubReplies(root.oid, root.rpid, 1)
                        }
                    }
                    
                    _commentState.value = current.copy(
                        isSending = false,
                        sendError = null,
                        replyTarget = null
                    )
                }
                // 3. 一级列表回复 (展开二级或直接回复)
                else {
                    _commentState.value = current.copy(
                        isSending = false,
                        sendError = null,
                        replyTarget = null,
                        replyCount = current.replyCount + 1
                    )
                }
            }.onFailure { e ->
                android.util.Log.e("CommentVM", " sendComment failed: ${e.message}")
                _commentState.value = _commentState.value.copy(isSending = false, sendError = e.message)
            }
        }
    }
    
    fun replyTo(reply: ReplyItem) {
        _commentState.value = _commentState.value.copy(replyTarget = reply)
    }
    
    fun cancelReply() {
        _commentState.value = _commentState.value.copy(replyTarget = null)
    }
    
    fun likeComment(rpid: Long) {
        val currentState = _commentState.value
        val isCurrentlyLiked = rpid in currentState.likedComments
        val newLikedComments = if (isCurrentlyLiked) currentState.likedComments - rpid else currentState.likedComments + rpid
        val newHatedComments = currentState.hatedComments - rpid
        _commentState.value = currentState.copy(likedComments = newLikedComments, hatedComments = newHatedComments)
        
        viewModelScope.launch {
            CommentRepository.likeComment(currentAid, rpid, !isCurrentlyLiked).onFailure {
                _commentState.value = _commentState.value.copy(likedComments = currentState.likedComments, hatedComments = currentState.hatedComments)
            }
        }
    }
    
    fun hateComment(rpid: Long) {
        val currentState = _commentState.value
        val isCurrentlyHated = rpid in currentState.hatedComments
        val newHatedComments = if (isCurrentlyHated) currentState.hatedComments - rpid else currentState.hatedComments + rpid
        val newLikedComments = currentState.likedComments - rpid
        _commentState.value = currentState.copy(likedComments = newLikedComments, hatedComments = newHatedComments)
        
        viewModelScope.launch {
            CommentRepository.hateComment(currentAid, rpid, !isCurrentlyHated).onFailure {
                _commentState.value = _commentState.value.copy(likedComments = currentState.likedComments, hatedComments = currentState.hatedComments)
            }
        }
    }
    

    
    fun reportComment(rpid: Long, reason: Int, content: String = "") {
        viewModelScope.launch { CommentRepository.reportComment(currentAid, rpid, reason, content) }
    }

    // --- [新增] 删除动画逻辑 ---

    /**
     * 开始删除动画
     * UI 调用此方法触发消散动画，动画结束后 UI 回调 deleteComment
     */
    fun startDissolve(rpid: Long) {
        val current = _commentState.value
        _commentState.value = current.copy(dissolvingIds = current.dissolvingIds + rpid)
    }

    /**
     * 实际删除操作 (动画完成后调用)
     */
    fun deleteComment(rpid: Long) {
        // 先从 UI 移除 (乐观更新)
        val current = _commentState.value
        val updatedReplies = allReplies.filter { it.rpid != rpid }
        allReplies = updatedReplies
        
        // 移除 dissolvingId
        _commentState.value = current.copy(
            replies = current.replies.filter { it.rpid != rpid },
            replyCount = maxOf(0, current.replyCount - 1),
            dissolvingIds = current.dissolvingIds - rpid
        )

        // 发起网络请求
        viewModelScope.launch {
            CommentRepository.deleteComment(currentAid, rpid).onFailure { e ->
                // 如果删除失败，可能需要恢复? 暂时只需提示
                // 实际场景中很少失败，除非网络极差
                // 若要严格一致性，可以在这里重新加载评论列表
                android.util.Log.e("CommentVM", "Delete failed for $rpid: ${e.message}")
            }
        }
    }
    
    /**
     * [新增] 开始二级评论消散动画
     */
    fun startSubDissolve(rpid: Long) {
        val current = _subReplyState.value
        _subReplyState.value = current.copy(dissolvingIds = current.dissolvingIds + rpid)
    }
    
    /**
     * [新增] 删除二级评论（动画完成后调用）
     */
    fun deleteSubComment(rpid: Long) {
        val current = _subReplyState.value
        // 从二级评论列表中移除
        val updatedItems = current.items.filter { it.rpid != rpid }
        _subReplyState.value = current.copy(
            items = updatedItems,
            dissolvingIds = current.dissolvingIds - rpid
        )
        
        android.util.Log.d("CommentVM", " deleteSubComment: rpid=$rpid, remaining=${updatedItems.size}")
        
        // 发起网络删除请求（使用 rootReply 的 oid）
        val oid = current.rootReply?.oid ?: return
        viewModelScope.launch {
            CommentRepository.deleteComment(oid, rpid).onFailure { e ->
                android.util.Log.e("CommentVM", "Delete sub-comment failed for $rpid: ${e.message}")
            }
        }
    }
}
