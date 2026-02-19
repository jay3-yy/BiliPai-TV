package com.android.purebilibili.feature.video.screen

import android.app.Activity
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.data.model.response.ViewPoint
import com.android.purebilibili.feature.dynamic.components.ImagePreviewDialog
import com.android.purebilibili.feature.video.state.VideoPlayerState
import com.android.purebilibili.feature.video.ui.components.CommentSortFilterBar
import com.android.purebilibili.feature.video.ui.components.RelatedVideoItem
import com.android.purebilibili.feature.video.ui.components.ReplyItemView
import com.android.purebilibili.feature.video.ui.section.ActionButtonsRow
import com.android.purebilibili.feature.video.ui.section.VideoPlayerSection
import com.android.purebilibili.feature.video.viewmodel.CommentUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import com.android.purebilibili.feature.video.viewmodel.VideoCommentViewModel
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TabletCinemaLayout(
    playerState: VideoPlayerState,
    uiState: PlayerUiState,
    commentState: CommentUiState,
    viewModel: PlayerViewModel,
    commentViewModel: VideoCommentViewModel,
    configuration: Configuration,
    isVerticalVideo: Boolean,
    sleepTimerMinutes: Int?,
    viewPoints: List<ViewPoint>,
    bvid: String,
    coverUrl: String = "",
    onBack: () -> Unit,
    onUpClick: (Long) -> Unit,
    onNavigateToAudioMode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    isInPipMode: Boolean,
    onPipClick: () -> Unit,
    isPortraitFullscreen: Boolean = false,
    currentCodec: String = "hev1",
    onCodecChange: (String) -> Unit = {},
    currentAudioQuality: Int = -1,
    onAudioQualityChange: (Int) -> Unit = {},
    transitionEnabled: Boolean = false,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode =
        com.android.purebilibili.feature.video.player.PlayMode.SEQUENTIAL,
    onPlayModeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val isTvDevice = rememberIsTvDevice()
    val policy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveTabletCinemaLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }
    val success = uiState as? PlayerUiState.Success
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val initialCurtainState = remember(configuration.screenWidthDp) {
        resolveInitialCurtainState(configuration.screenWidthDp).name
    }
    var curtainStateName by rememberSaveable(bvid) { mutableStateOf(initialCurtainState) }
    val curtainState = remember(curtainStateName) {
        runCatching { TabletSideCurtainState.valueOf(curtainStateName) }
            .getOrDefault(resolveInitialCurtainState(configuration.screenWidthDp))
    }
    var selectedTab by rememberSaveable(bvid) { mutableIntStateOf(0) }
    val curtainWidth by animateDpAsState(
        targetValue = resolveCurtainWidthDp(curtainState, policy).dp,
        animationSpec = tween(durationMillis = 240),
        label = "cinemaCurtainWidth"
    )

    LaunchedEffect(success?.related?.size, commentState.replyCount) {
        if (selectedTab == 0 && commentState.replyCount == 0 && !success?.related.isNullOrEmpty()) {
            selectedTab = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLowest,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = policy.horizontalPaddingDp.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CinemaStagePlayer(
                    playerState = playerState,
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = onBack,
                    bvid = bvid,
                    coverUrl = coverUrl,
                    onNavigateToAudioMode = onNavigateToAudioMode,
                    onToggleFullscreen = onToggleFullscreen,
                    isInPipMode = isInPipMode,
                    onPipClick = onPipClick,
                    sleepTimerMinutes = sleepTimerMinutes,
                    viewPoints = viewPoints,
                    isVerticalVideo = isVerticalVideo,
                    isPortraitFullscreen = isPortraitFullscreen,
                    currentCodec = currentCodec,
                    onCodecChange = onCodecChange,
                    currentAudioQuality = currentAudioQuality,
                    onAudioQualityChange = onAudioQualityChange,
                    transitionEnabled = transitionEnabled,
                    currentPlayMode = currentPlayMode,
                    onPlayModeClick = onPlayModeClick,
                    onRelatedVideoClick = onRelatedVideoClick,
                    playerMaxWidth = policy.playerMaxWidthDp.dp
                )

                if (success != null) {
                    CinemaMetaPanel(
                        success = success,
                        downloadProgress = downloadProgress,
                        modifier = Modifier.weight(1f),
                        onFavoriteClick = { viewModel.toggleFavorite() },
                        onLikeClick = { viewModel.toggleLike() },
                        onCoinClick = { viewModel.openCoinDialog() },
                        onTripleClick = { viewModel.doTripleAction() },
                        onDownloadClick = { viewModel.openDownloadDialog() },
                        onWatchLaterClick = { viewModel.toggleWatchLater() },
                        onOpenComments = {
                            selectedTab = 0
                            curtainStateName = TabletSideCurtainState.OPEN.name
                        }
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CupertinoActivityIndicator()
                        }
                    }
                }
            }

            CinemaSideCurtain(
                state = curtainState,
                width = curtainWidth,
                selectedTab = selectedTab,
                onToggle = {
                    curtainStateName = when (curtainState) {
                        TabletSideCurtainState.OPEN -> TabletSideCurtainState.PEEK.name
                        TabletSideCurtainState.PEEK -> TabletSideCurtainState.OPEN.name
                        TabletSideCurtainState.HIDDEN -> TabletSideCurtainState.PEEK.name
                    }
                },
                onTabSelected = { tab ->
                    selectedTab = tab
                    curtainStateName = TabletSideCurtainState.OPEN.name
                },
                success = success,
                commentState = commentState,
                commentViewModel = commentViewModel,
                viewModel = viewModel,
                playerState = playerState,
                onUpClick = onUpClick,
                onRelatedVideoClick = onRelatedVideoClick,
                context = context
            )
        }
    }
}

@Composable
private fun CinemaStagePlayer(
    playerState: VideoPlayerState,
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    bvid: String,
    coverUrl: String,
    onNavigateToAudioMode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    isInPipMode: Boolean,
    onPipClick: () -> Unit,
    sleepTimerMinutes: Int?,
    viewPoints: List<ViewPoint>,
    isVerticalVideo: Boolean,
    isPortraitFullscreen: Boolean,
    currentCodec: String,
    onCodecChange: (String) -> Unit,
    currentAudioQuality: Int,
    onAudioQualityChange: (Int) -> Unit,
    transitionEnabled: Boolean,
    currentPlayMode: com.android.purebilibili.feature.video.player.PlayMode,
    onPlayModeClick: () -> Unit,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    playerMaxWidth: Dp
) {
    val context = LocalContext.current
    val success = uiState as? PlayerUiState.Success
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 4.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val playerWidth = minOf(maxWidth, playerMaxWidth)
            val videoHeight = playerWidth * 9f / 16f
            Box(
                modifier = Modifier
                    .width(playerWidth)
                    .height(videoHeight)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black)
            ) {
                VideoPlayerSection(
                    playerState = playerState,
                    uiState = uiState,
                    isFullscreen = false,
                    isInPipMode = isInPipMode,
                    onToggleFullscreen = onToggleFullscreen,
                    onQualityChange = { qid, pos -> viewModel.changeQuality(qid, pos) },
                    onBack = onBack,
                    bvid = bvid,
                    coverUrl = coverUrl,
                    onDoubleTapLike = { viewModel.toggleLike() },
                    onReloadVideo = { viewModel.reloadVideo() },
                    currentCdnIndex = success?.currentCdnIndex ?: 0,
                    cdnCount = success?.cdnCount ?: 1,
                    onSwitchCdn = { viewModel.switchCdn() },
                    onSwitchCdnTo = { viewModel.switchCdnTo(it) },
                    isAudioOnly = false,
                    onAudioOnlyToggle = {
                        viewModel.setAudioMode(true)
                        onNavigateToAudioMode()
                    },
                    sleepTimerMinutes = sleepTimerMinutes,
                    onSleepTimerChange = { viewModel.setSleepTimer(it) },
                    videoshotData = success?.videoshotData,
                    viewPoints = viewPoints,
                    isVerticalVideo = isVerticalVideo,
                    onPortraitFullscreen = { playerState.setPortraitFullscreen(true) },
                    isPortraitFullscreen = isPortraitFullscreen,
                    onPipClick = onPipClick,
                    currentCodec = currentCodec,
                    onCodecChange = onCodecChange,
                    currentAudioQuality = currentAudioQuality,
                    onAudioQualityChange = onAudioQualityChange,
                    onSaveCover = { viewModel.saveCover(context) },
                    onDownloadAudio = { viewModel.downloadAudio(context) },
                    currentPlayMode = currentPlayMode,
                    onPlayModeClick = onPlayModeClick,
                    onRelatedVideoClick = onRelatedVideoClick,
                    relatedVideos = success?.related ?: emptyList(),
                    ugcSeason = success?.info?.ugc_season,
                    isFollowed = success?.isFollowing ?: false,
                    isLiked = success?.isLiked ?: false,
                    isCoined = success?.coinCount?.let { it > 0 } ?: false,
                    isFavorited = success?.isFavorited ?: false,
                    onToggleFollow = { viewModel.toggleFollow() },
                    onToggleLike = { viewModel.toggleLike() },
                    onCoin = { viewModel.showCoinDialog() },
                    onToggleFavorite = { viewModel.showFavoriteFolderDialog() },
                    onTriple = { viewModel.doTripleAction() }
                )
            }
        }
    }
}

@Composable
private fun CinemaMetaPanel(
    success: PlayerUiState.Success,
    downloadProgress: Float,
    modifier: Modifier = Modifier,
    onFavoriteClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCoinClick: () -> Unit,
    onTripleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onOpenComments: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ActionButtonsRow(
                    info = success.info,
                    isFavorited = success.isFavorited,
                    isLiked = success.isLiked,
                    coinCount = success.coinCount,
                    downloadProgress = downloadProgress,
                    isInWatchLater = success.isInWatchLater,
                    onFavoriteClick = onFavoriteClick,
                    onLikeClick = onLikeClick,
                    onCoinClick = onCoinClick,
                    onTripleClick = onTripleClick,
                    onDownloadClick = onDownloadClick,
                    onWatchLaterClick = onWatchLaterClick,
                    onCommentClick = onOpenComments
                )
            }
            item {
                CinemaVideoIntroSection(success = success)
            }
        }
    }
}

@Composable
private fun CinemaVideoIntroSection(
    success: PlayerUiState.Success
) {
    var expanded by rememberSaveable(success.info.bvid) { mutableStateOf(false) }
    val descriptionText = success.info.desc.takeIf { it.isNotBlank() } ?: "暂无简介"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = success.info.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${FormatUtils.formatStat(success.info.stat.view.toLong())} 播放 · " +
                    "${FormatUtils.formatStat(success.info.stat.danmaku.toLong())} 弹幕",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (expanded) "收起简介" else "展开简介",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun CinemaSideCurtain(
    state: TabletSideCurtainState,
    width: Dp,
    selectedTab: Int,
    onToggle: () -> Unit,
    onTabSelected: (Int) -> Unit,
    success: PlayerUiState.Success?,
    commentState: CommentUiState,
    commentViewModel: VideoCommentViewModel,
    viewModel: PlayerViewModel,
    playerState: VideoPlayerState,
    onUpClick: (Long) -> Unit,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    context: android.content.Context
) {
    Row(
        modifier = Modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight()
                .padding(vertical = 26.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable { onToggle() },
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state == TabletSideCurtainState.OPEN) {
                        Icons.Outlined.KeyboardDoubleArrowRight
                    } else {
                        Icons.Outlined.KeyboardDoubleArrowLeft
                    },
                    contentDescription = "toggle curtain",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state != TabletSideCurtainState.HIDDEN) {
            Surface(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                if (state == TabletSideCurtainState.PEEK) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { onTabSelected(0) }) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "comments"
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        IconButton(onClick = { onTabSelected(1) }) {
                            Icon(
                                imageVector = Icons.Outlined.PlaylistPlay,
                                contentDescription = "related videos"
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { onTabSelected(0) },
                                text = {
                                    Text(
                                        text = "评论 ${if (commentState.replyCount > 0) "(${commentState.replyCount})" else ""}"
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { onTabSelected(1) },
                                text = { Text("相关推荐") }
                            )
                        }

                        if (selectedTab == 0 && success != null) {
                            CinemaCommentsPane(
                                success = success,
                                commentState = commentState,
                                commentViewModel = commentViewModel,
                                playerState = playerState,
                                onUpClick = onUpClick,
                                context = context
                            )
                        } else if (selectedTab == 1 && success != null) {
                            CinemaRelatedPane(
                                success = success,
                                onRelatedVideoClick = onRelatedVideoClick,
                                context = context
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoActivityIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CinemaCommentsPane(
    success: PlayerUiState.Success,
    commentState: CommentUiState,
    commentViewModel: VideoCommentViewModel,
    playerState: VideoPlayerState,
    onUpClick: (Long) -> Unit,
    context: android.content.Context
) {
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewInitialIndex by remember { mutableIntStateOf(0) }
    var sourceRect by remember { mutableStateOf<Rect?>(null) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3 && !commentState.isRepliesLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            commentViewModel.loadComments()
        }
    }

    if (showImagePreview && previewImages.isNotEmpty()) {
        ImagePreviewDialog(
            images = previewImages,
            initialIndex = previewInitialIndex,
            sourceRect = sourceRect,
            onDismiss = { showImagePreview = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 74.dp)
        ) {
            item {
                CommentSortFilterBar(
                    count = commentState.replyCount,
                    sortMode = commentState.sortMode,
                    onSortModeChange = { mode ->
                        commentViewModel.setSortMode(mode)
                        scope.launch {
                            SettingsManager.setCommentDefaultSortMode(context, mode.apiMode)
                        }
                    },
                    upOnly = commentState.upOnlyFilter,
                    onUpOnlyToggle = { commentViewModel.toggleUpOnly() }
                )
            }
            items(
                items = commentState.replies,
                key = { "curtain_reply_${it.rpid}" }
            ) { reply ->
                ReplyItemView(
                    item = reply,
                    upMid = success.info.owner.mid,
                    emoteMap = success.emoteMap,
                    onClick = {},
                    onSubClick = { commentViewModel.openSubReply(it) },
                    onTimestampClick = { positionMs ->
                        playerState.player.seekTo(positionMs)
                        playerState.player.play()
                    },
                    onImagePreview = { images, index, rect ->
                        previewImages = images
                        previewInitialIndex = index
                        sourceRect = rect
                        showImagePreview = true
                    },
                    onLikeClick = { commentViewModel.likeComment(reply.rpid) },
                    isLiked = reply.action == 1 || reply.rpid in commentState.likedComments,
                    onDeleteClick = if (
                        commentState.currentMid > 0 && reply.mid == commentState.currentMid
                    ) {
                        { commentViewModel.startDissolve(reply.rpid) }
                    } else {
                        null
                    },
                    onAvatarClick = { mid ->
                        mid.toLongOrNull()?.let(onUpClick)
                    }
                )
            }
            if (commentState.isRepliesLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CupertinoActivityIndicator()
                    }
                }
            }
            if (commentState.replies.isEmpty() && !commentState.isRepliesLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "还没有评论，先看看相关推荐",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { commentViewModel.toggleUpOnly() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            containerColor = if (commentState.upOnlyFilter) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (commentState.upOnlyFilter) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
            shape = CircleShape
        ) {
            Text(
                text = "UP",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CinemaRelatedPane(
    success: PlayerUiState.Success,
    onRelatedVideoClick: (String, android.os.Bundle?) -> Unit,
    context: android.content.Context
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = success.related,
            key = { "curtain_related_${it.bvid}" }
        ) { video ->
            RelatedVideoItem(
                video = video,
                isFollowed = video.owner.mid in success.followingMids,
                onClick = {
                    val activity = (context as? Activity)
                        ?: (context as? ContextWrapper)?.baseContext as? Activity
                    val options = activity?.let {
                        android.app.ActivityOptions.makeSceneTransitionAnimation(it).toBundle()
                    }
                    onRelatedVideoClick(video.bvid, options)
                }
            )
        }
        if (success.related.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂时没有推荐视频",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
