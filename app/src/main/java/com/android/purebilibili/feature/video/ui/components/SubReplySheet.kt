// 文件路径: feature/video/SubReplySheet.kt
package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.data.model.response.ReplyItem
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubReplySheet(
    state: SubReplyUiState,
    emoteMap: Map<String, String>,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    onTimestampClick: ((Long) -> Unit)? = null,
    onImagePreview: ((List<String>, Int, androidx.compose.ui.geometry.Rect?) -> Unit)? = null,
    onReplyClick: ((ReplyItem) -> Unit)? = null,
    // [新增] 删除评论相关
    currentMid: Long = 0,
    onDissolveStart: ((Long) -> Unit)? = null,
    onDeleteComment: ((Long) -> Unit)? = null,
    // [新增] 点赞
    onCommentLike: ((Long) -> Unit)? = null,
    likedComments: Set<Long> = emptySet(),
    onUrlClick: ((String) -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null
) {
    if (state.visible && state.rootReply != null) {
        com.android.purebilibili.core.ui.IOSModalBottomSheet(
            onDismissRequest = onDismiss
        ) {
            SubReplyList(
                rootReply = state.rootReply!!,
                subReplies = state.items,
                isLoading = state.isLoading,
                isEnd = state.isEnd,
                emoteMap = emoteMap,
                onLoadMore = onLoadMore,
                onTimestampClick = onTimestampClick,
                upMid = state.upMid,
                onImagePreview = onImagePreview,
                onReplyClick = onReplyClick,
                // [新增] 消散动画相关
                dissolvingIds = state.dissolvingIds,
                currentMid = currentMid,
                onDissolveStart = onDissolveStart,
                onDeleteComment = onDeleteComment,
                onCommentLike = onCommentLike,
                likedComments = likedComments,
                onUrlClick = onUrlClick,
                onAvatarClick = onAvatarClick
            )
        }
    }
}

@Composable
fun SubReplyList(
    rootReply: ReplyItem,
    subReplies: List<ReplyItem>,
    isLoading: Boolean,
    isEnd: Boolean,
    emoteMap: Map<String, String>,
    onLoadMore: () -> Unit,
    onTimestampClick: ((Long) -> Unit)? = null,
    upMid: Long = 0,
    onImagePreview: ((List<String>, Int, androidx.compose.ui.geometry.Rect?) -> Unit)? = null,
    onReplyClick: ((ReplyItem) -> Unit)? = null,
    // [新增] 消散动画相关
    dissolvingIds: Set<Long> = emptySet(),
    currentMid: Long = 0,
    onDissolveStart: ((Long) -> Unit)? = null,
    onDeleteComment: ((Long) -> Unit)? = null,
    onCommentLike: ((Long) -> Unit)? = null,
    likedComments: Set<Long> = emptySet(),
    onUrlClick: ((String) -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= layoutInfo.totalItemsCount - 2 && !isLoading && !isEnd
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMore() }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                "评论详情",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                ReplyItemView(
                    item = rootReply,
                    upMid = upMid,
                    emoteMap = emoteMap, 
                    onClick = { onReplyClick?.invoke(rootReply) },
                    onSubClick = {},
                    onTimestampClick = onTimestampClick,
                    onImagePreview = onImagePreview,
                    hideSubPreview = true,
                    onReplyClick = { onReplyClick?.invoke(rootReply) },
                    // [新增] 删除按钮
                    onDeleteClick = if (currentMid > 0 && rootReply.mid == currentMid) {
                        { onDeleteComment?.invoke(rootReply.rpid) }
                    } else null,
                    // [新增] 点赞
                    onLikeClick = { onCommentLike?.invoke(rootReply.rpid) },
                    isLiked = rootReply.action == 1 || rootReply.rpid in likedComments,
                    onUrlClick = onUrlClick,
                    onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit }
                )
                HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
            items(subReplies, key = { it.rpid }) { item ->
                // [新增] 使用 DissolvableVideoCard 添加消散动画
                com.android.purebilibili.core.ui.animation.DissolvableVideoCard(
                    isDissolving = item.rpid in dissolvingIds,
                    onDissolveComplete = { onDeleteComment?.invoke(item.rpid) },
                    cardId = "subreply_${item.rpid}",
                    modifier = Modifier.padding(bottom = 1.dp)
                ) {
                    ReplyItemView(
                        item = item,
                        upMid = upMid,
                        emoteMap = emoteMap, 
                        onClick = { onReplyClick?.invoke(item) },
                        onSubClick = {},
                        onTimestampClick = onTimestampClick,
                        onImagePreview = onImagePreview,
                        onReplyClick = { onReplyClick?.invoke(item) },
                        // [修改] 点击删除触发消散动画
                        onDeleteClick = if (currentMid > 0 && item.mid == currentMid) {
                            { onDissolveStart?.invoke(item.rpid) }
                        } else null,
                        // [新增] 点赞
                        onLikeClick = { onCommentLike?.invoke(item.rpid) },
                        isLiked = item.action == 1 || item.rpid in likedComments,
                        onUrlClick = onUrlClick,
                        onAvatarClick = { onAvatarClick?.invoke(it) ?: Unit }
                    )
                }
            }
            item {
                // [修复] 滚动到底部时自动加载
                LaunchedEffect(Unit) {
                    if (!isLoading && !isEnd) {
                        onLoadMore()
                    }
                }
                
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    when {
                        isLoading -> CupertinoActivityIndicator()
                        else -> Text("没有更多回复了", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}