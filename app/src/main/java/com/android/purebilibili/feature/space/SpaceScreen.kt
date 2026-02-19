package com.android.purebilibili.feature.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
//  已改用 MaterialTheme.colorScheme.primary
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.*
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import com.android.purebilibili.core.util.iOSTapEffect
import com.android.purebilibili.core.util.responsiveContentWidth

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.core.ui.blur.unifiedBlur
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SpaceScreen(
    mid: Long,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onDynamicWebClick: (String) -> Unit = {},
    onViewAllClick: (String, Long, Long, String) -> Unit = { _, _, _, _ -> }, // type, id, mid, title
    viewModel: SpaceViewModel = viewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // [Block] Repository & State
    val blockedUpRepository = remember { com.android.purebilibili.data.repository.BlockedUpRepository(context) }
    val isBlocked by blockedUpRepository.isBlocked(mid).collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var showBlockMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    
    // [Blur] Haze State
    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(mid) {
        viewModel.loadSpaceInfo(mid)
        //  [埋点] 页面浏览追踪
        com.android.purebilibili.core.util.AnalyticsHelper.logScreenView("SpaceScreen")
    }
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // [Blur] TopAppBar Container with Blur
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .unifiedBlur(hazeState)
            ) {
                TopAppBar(
                    title = { 
                        Text("空间", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = { showBlockMenu = true }) {
                            Icon(CupertinoIcons.Default.Ellipsis, contentDescription = "更多")
                        }
                        
                        DropdownMenu(
                            expanded = showBlockMenu,
                            onDismissRequest = { showBlockMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isBlocked) "解除屏蔽" else "屏蔽 UP 主") },
                                onClick = { 
                                    showBlockMenu = false
                                    showBlockConfirmDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        if (isBlocked) CupertinoIcons.Default.Eye else CupertinoIcons.Default.EyeSlash,
                                        contentDescription = null,
                                        tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = if (isBlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                    leadingIconColor = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp) // [Blur] Remove top padding to allow content behind TopBar
                .hazeSource(hazeState) // [Blur] Content Source
        ) {
            when (val state = uiState) {
                is SpaceUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CupertinoActivityIndicator()
                    }
                }
                
                is SpaceUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😢", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadSpaceInfo(mid) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                is SpaceUiState.Success -> {
                    SpaceContent(
                        state = state,
                        onVideoClick = onVideoClick,
                        onDynamicWebClick = onDynamicWebClick,
                        onLoadMore = { viewModel.loadMoreVideos() },
                        onCategoryClick = { viewModel.selectCategory(it) },
                        onSortOrderClick = { viewModel.selectSortOrder(it) },
                        onLoadHome = { viewModel.loadSpaceHome() },
                        onLoadDynamic = { viewModel.loadSpaceDynamic(refresh = true) },
                        onLoadMoreDynamic = { viewModel.loadSpaceDynamic(refresh = false) },
                        onSubTabSelected = { viewModel.selectSubTab(it) },
                        onViewAllClick = onViewAllClick,
                        // [Blur] Pass content padding to handle list top spacing
                        contentPadding = padding,

                        onFollowClick = { viewModel.toggleFollow() },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }
    }
    
    if (showBlockConfirmDialog) {
        val userName = (uiState as? SpaceUiState.Success)?.userInfo?.name ?: "该用户"
        val userFace = (uiState as? SpaceUiState.Success)?.userInfo?.face ?: ""
        
        com.android.purebilibili.core.ui.IOSAlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text(if (isBlocked) "解除屏蔽" else "屏蔽 UP 主") },
            text = { Text(if (isBlocked) "确定要解除对 $userName 的屏蔽吗？" else "屏蔽后，将不再推荐 $userName 的视频。\n确定要屏蔽吗？") },
            confirmButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(
                    onClick = {
                        scope.launch {
                            if (isBlocked) {
                                blockedUpRepository.unblockUp(mid)
                                android.widget.Toast.makeText(context, "已解除屏蔽", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                blockedUpRepository.blockUp(mid, userName, userFace)
                                android.widget.Toast.makeText(context, "已屏蔽该 UP 主", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            showBlockConfirmDialog = false
                        }
                    }
                ) {
                    Text(
                        text = if (isBlocked) "解除屏蔽" else "屏蔽",
                        color = if (!isBlocked) Color.Red else com.android.purebilibili.core.theme.iOSBlue
                    )
                }
            },
            dismissButton = {
                com.android.purebilibili.core.ui.IOSDialogAction(onClick = { showBlockConfirmDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SpaceContent(
    state: SpaceUiState.Success,
    onVideoClick: (String) -> Unit,
    onDynamicWebClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onCategoryClick: (Int) -> Unit,  //  分类点击回调
    onSortOrderClick: (VideoSortOrder) -> Unit,  //  排序点击回调
    onLoadHome: () -> Unit,  //  加载主页数据
    onLoadDynamic: () -> Unit,  //  加载动态数据
    onLoadMoreDynamic: () -> Unit,  //  加载更多动态
    onSubTabSelected: (SpaceSubTab) -> Unit,  // Uploads Sub-tab selection

    onViewAllClick: (String, Long, Long, String) -> Unit,
    contentPadding: PaddingValues, // [Blur] Receive padding from Scaffold
    onFollowClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    val context = LocalContext.current
    val playSpaceVideo: (String) -> Unit = { clickedBvid ->
        val externalPlaylist = buildExternalPlaylistFromSpaceVideos(
            videos = state.videos,
            clickedBvid = clickedBvid
        )
        if (externalPlaylist != null) {
            com.android.purebilibili.feature.video.player.PlaylistManager.setExternalPlaylist(
                externalPlaylist.playlistItems,
                externalPlaylist.startIndex
            )
        }
        onVideoClick(clickedBvid)
    }
    //  当前选中的 Tab（目前只实现投稿页）
    var selectedTab by remember { mutableIntStateOf(2) }  // 默认投稿
    
    val listState = rememberLazyGridState()
    
    //  自动加载更多：当滚动接近底部时触发
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 6 && !state.isLoadingMore && state.hasMoreVideos && selectedTab == 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = listState,
        modifier = Modifier.fillMaxSize().responsiveContentWidth(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(), // [Blur] Use top padding for first item
            bottom = contentPadding.calculateBottomPadding() + 16.dp // Add extra bottom padding
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 用户头部信息 (跨满列)
        item(span = { GridItemSpan(maxLineSpan) }) {
            SpaceHeader(

                userInfo = state.userInfo,
                relationStat = state.relationStat,
                upStat = state.upStat,
                onFollowClick = onFollowClick,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
        
        //  Tab 导航栏 (跨满列)
        item(span = { GridItemSpan(maxLineSpan) }) {
            SpaceTabRow(
                selectedTab = selectedTab,
                videoCount = state.totalVideos,
                collectionsCount = state.seasons.size + state.series.size,
                onTabSelected = { selectedTab = it }
            )
        }
        
        //  根据 Tab 显示不同内容
        when (selectedTab) {
            2 -> {  // 投稿
                // 投稿分类侧边栏 - 显示为水平标签（移动端适配）
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SpaceUploadsHeader(
                        selectedTab = state.selectedSubTab,
                        videoCount = state.totalVideos,
                        articleCount = state.articles.size,
                        audioCount = state.audios.size,
                        onTabSelected = onSubTabSelected
                    )
                }

                when (state.selectedSubTab) {
                    SpaceSubTab.VIDEO -> {
                        // 播放全部 + 排序按钮行 - 官方风格
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 播放全部按钮
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            state.videos.firstOrNull()?.let { playSpaceVideo(it.bvid) }
                                        }
                                        .padding(end = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        CupertinoIcons.Default.Play,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "播放全部",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                
                                Spacer(Modifier.weight(1f))
                                
                                // 排序下拉 - 简化显示当前排序方式
                                Row(
                                    modifier = Modifier.clickable { 
                                        // 切换排序
                                        val next = when (state.sortOrder) {
                                            VideoSortOrder.PUBDATE -> VideoSortOrder.CLICK
                                            VideoSortOrder.CLICK -> VideoSortOrder.STOW
                                            VideoSortOrder.STOW -> VideoSortOrder.PUBDATE
                                        }
                                        onSortOrderClick(next)
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (state.sortOrder) {
                                            VideoSortOrder.PUBDATE -> "最新发布"
                                            VideoSortOrder.CLICK -> "最多播放"
                                            VideoSortOrder.STOW -> "最多收藏"
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        CupertinoIcons.Default.ChevronDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // 视频列表 - 列表样式（非网格）
                        state.videos.forEach { video ->
                            item(key = "video_${video.bvid}", span = { GridItemSpan(maxLineSpan) }) {

                                SpaceVideoListItem(
                                    video = video,
                                    onClick = { playSpaceVideo(video.bvid) },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }

                        // Load More for Video
                         if (state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CupertinoActivityIndicator()
                                }
                            }
                        } else if (!state.hasMoreVideos && state.videos.isNotEmpty()) {
                             item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("—— 没有更多了 ——", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    SpaceSubTab.AUDIO -> {
                         items(state.audios, key = { it.id }) { audio ->
                             SpaceAudioCard(audio = audio, onClick = { /* TODO: Play Audio */ })
                         }
                         
                         // Load More for Audio
                        if (state.isLoadingAudios) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CupertinoActivityIndicator()
                                }
                            }
                        } else if (!state.hasMoreAudios && state.audios.isNotEmpty()) {
                             item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("—— 没有更多了 ——", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else if (state.audios.isEmpty() && !state.isLoadingAudios) {
                             item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("暂无音频", color = Color.Gray)
                                }
                            }
                        }
                    }
                    SpaceSubTab.ARTICLE -> {
                         items(state.articles, key = { it.id }) { article ->
                             SpaceArticleCard(article = article, onClick = { /* TODO: Open Article */ })
                         }
                         
                         // Load More for Articles
                        if (state.isLoadingArticles) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CupertinoActivityIndicator()
                                }
                            }
                        } else if (!state.hasMoreArticles && state.articles.isNotEmpty()) {
                             item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("—— 没有更多了 ——", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else if (state.articles.isEmpty() && !state.isLoadingArticles) {
                             item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("暂无专栏", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
            
            3 -> {  // 合集和系列 (跨满列)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "合集和系列",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                // 显示合集
                state.seasons.forEach { season ->
                    item(key = "season_${season.meta.season_id}", span = { GridItemSpan(maxLineSpan) }) {
                        SeasonSection(
                            season = season,
                            archives = state.seasonArchives[season.meta.season_id] ?: emptyList(),
                            onVideoClick = onVideoClick,
                            mid = state.userInfo.mid,
                            onMoreClick = {
                                onViewAllClick("season", season.meta.season_id, state.userInfo.mid, season.meta.name)
                            }
                        )
                    }
                }
                
                // 显示系列
                state.series.forEach { series ->
                    item(key = "series_${series.meta.series_id}", span = { GridItemSpan(maxLineSpan) }) {
                        SeriesSection(
                            series = series,
                            archives = state.seriesArchives[series.meta.series_id] ?: emptyList(),
                            onVideoClick = onVideoClick,
                            onMoreClick = {
                                onViewAllClick("series", series.meta.series_id, state.userInfo.mid, series.meta.name)
                            }
                        )
                    }
                }
                
                if (state.seasons.isEmpty() && state.series.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "该用户暂无合集和系列",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            
            0 -> {  //  主页 Tab - 官方客户端风格
                // 触发加载
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LaunchedEffect(Unit) { onLoadHome() }
                }
                
                // 视频区块 - "视频 xxxx" + "查看更多"
                if (state.videos.isNotEmpty() || state.totalVideos > 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceHomeSectionHeader(
                            title = "视频",
                            count = if (state.totalVideos > 0) state.totalVideos else state.videos.size,
                            onViewMore = { 
                                // 切换到投稿Tab (index 2)
                                selectedTab = 2
                                onSubTabSelected(SpaceSubTab.VIDEO)
                            }
                        )
                    }
                    
                    // 显示前4个视频 (2x2 网格)
                    val videosToShow = state.videos.take(4)
                    items(videosToShow, key = { "home_video_${it.bvid}" }) { video ->
                        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                            SpaceHomeVideoCard(
                                video = video,
                                onClick = { playSpaceVideo(video.bvid) }
                            )
                        }
                    }
                }
                
                // 置顶视频 (如果存在)
                if (state.topVideo != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceHomeTopVideo(
                            topVideo = state.topVideo,
                            onVideoClick = onVideoClick
                        )
                    }
                }
                
                // 图文区块 (如果有)
                if (state.articles.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceHomeSectionHeader(
                            title = "图文",
                            count = state.articles.size,
                            onViewMore = { 
                                // 切换到投稿Tab的图文分类
                                selectedTab = 2
                                onSubTabSelected(SpaceSubTab.ARTICLE)
                            }
                        )
                    }
                    
                    // 显示前2个图文 (列表样式)
                    state.articles.take(2).forEach { article ->
                        item(key = "home_article_${article.id}", span = { GridItemSpan(maxLineSpan) }) {
                            SpaceArticleCard(article = article, onClick = { /* TODO */ })
                        }
                    }
                }
                
                // 公告
                if (state.notice.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SpaceHomeNotice(notice = state.notice)
                    }
                }
                
                // 如果啥都没有
                if (state.videos.isEmpty() && state.topVideo == null && state.notice.isEmpty() && state.articles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无主页内容",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            
            1 -> {  //  动态 Tab
                // 触发加载
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LaunchedEffect(Unit) { onLoadDynamic() }
                }
                
                // 动态列表
                if (state.dynamics.isEmpty() && !state.isLoadingDynamics) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无动态",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    state.dynamics.forEachIndexed { index, dynamic ->
                        item(key = "dynamic_${dynamic.id_str}", span = { GridItemSpan(maxLineSpan) }) {
                            SpaceDynamicCard(
                                dynamic = dynamic,
                                onVideoClick = onVideoClick,
                                onDynamicWebClick = onDynamicWebClick
                            )
                            
                            // 触发加载更多
                            if (index == state.dynamics.size - 3 && state.hasMoreDynamics && !state.isLoadingDynamics) {
                                LaunchedEffect(index) { onLoadMoreDynamic() }
                            }
                        }
                    }
                    
                    // 加载中指示器
                    if (state.isLoadingDynamics) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CupertinoActivityIndicator()
                            }
                        }
                    } else if (!state.hasMoreDynamics) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("—— 没有更多了 ——", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceHeader(

    userInfo: SpaceUserInfo,
    relationStat: RelationStatData?,
    upStat: UpStatData?,
    onFollowClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        //  头图 Banner - 更紧凑的高度
        if (userInfo.topPhoto.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)  //  减少高度
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(userInfo.topPhoto))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // 渐变遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            }
        }
        
        //  头像和基本信息区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = if (userInfo.topPhoto.isNotEmpty()) (-20).dp else 4.dp),  //  减少 offset
            verticalAlignment = Alignment.Bottom
        ) {
            // 头像（带边框）
            Box {
                val avatarModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            rememberSharedContentState(key = "up_avatar_${userInfo.mid}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            clipInOverlayDuringTransition = OverlayClip(CircleShape)
                        )
                    }
                } else Modifier

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(userInfo.face))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .then(avatarModifier)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                
                //  直播状态标识（如果正在直播）
                if (userInfo.liveRoom?.liveStatus == 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .background(com.android.purebilibili.core.theme.iOSRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "播",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // 用户名和信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = userInfo.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(Modifier.width(6.dp))
                    
                    //  等级徽章
                    Surface(
                        color = when {
                            userInfo.level >= 6 -> Color(0xFFFF6699)  // 粉色高等级
                            userInfo.level >= 4 -> Color(0xFF00AEEC)  // 蓝色中等级
                            else -> Color(0xFF9E9E9E)  // 灰色低等级
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "LV${userInfo.level}",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    
                    //  性别图标
                    if (userInfo.sex == "男") {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "♂",
                            color = Color(0xFF00AEEC),  // 蓝色
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    } else if (userInfo.sex == "女") {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "♀",
                            color = Color(0xFFFF6699),  // 粉色
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                    }
                    
                    // VIP 标签
                    if (userInfo.vip.status == 1 && userInfo.vip.label.text.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFF6699),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = userInfo.vip.label.text,
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                softWrap = false
                            )
                        }
                    }
                }
            }

            
            // [新增] 关注按钮
            Spacer(Modifier.width(12.dp))
            val isFollowed = userInfo.isFollowed
            Button(
                onClick = onFollowClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFollowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .height(32.dp)
                    .padding(bottom = 8.dp) // 对齐底部
            ) {
                Text(
                    text = if (isFollowed) "已关注" else "关注",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // 签名
        if (userInfo.sign.isNotEmpty()) {
            Text(
                text = userInfo.sign,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = if (userInfo.topPhoto.isNotEmpty()) 0.dp else 8.dp)
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        // 数据统计
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 关注
            StatItem(label = "关注", value = relationStat?.following ?: 0)
            // 粉丝
            StatItem(label = "粉丝", value = relationStat?.follower ?: 0)
            // 获赞
            StatItem(label = "获赞", value = (upStat?.likes ?: 0).toInt())
            // 播放
            StatItem(label = "播放", value = (upStat?.archive?.view ?: 0).toInt())
        }
        
        Spacer(Modifier.height(12.dp))
        
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = FormatUtils.formatStat(value.toLong()),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SpaceVideoItem(video: SpaceVideoItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iOSTapEffect(scale = 0.98f) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(94.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(video.pic))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // 时长标签
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = video.length,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(Modifier.width(12.dp))
        
        // 信息
        Column(
            modifier = Modifier
                .weight(1f)
                .height(94.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = video.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    CupertinoIcons.Default.Play,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = FormatUtils.formatStat(video.play.toLong()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                Spacer(Modifier.width(12.dp))
                
                Icon(
                    CupertinoIcons.Default.Message,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = FormatUtils.formatStat(video.comment.toLong()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 投稿视频列表项 - 官方客户端风格
 * 左侧封面 + 右侧信息（标题、时间、播放/评论数）
 */
@Composable
private fun SpaceVideoListItem(
    video: SpaceVideoItem,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iOSTapEffect(scale = 0.98f) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 封面 - 16:9 比例
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val coverModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "video_cover_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(8.dp))
                    )
                }
            } else Modifier

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(video.pic))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(coverModifier)
            )
            
            // 时长标签
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = video.length,
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(Modifier.width(10.dp))
        
        // 右侧信息
        Column(
            modifier = Modifier
                .weight(1f)
                .height(80.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 标题
            var titleModifier = Modifier.fillMaxWidth()
            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    titleModifier = titleModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_title_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        }
                    )
                }
            }

            Text(
                text = video.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = titleModifier
            )
            
            // 底部信息行
            Column {
                // 时间
                Text(
                    text = FormatUtils.formatPublishTime(video.created.toLong()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                Spacer(Modifier.height(2.dp))
                
                // 播放和评论数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 🔗 [共享元素] 播放量
                    var viewsModifier = Modifier.wrapContentSize()
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            viewsModifier = viewsModifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "video_views_${video.bvid}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(dampingRatio = 0.8f, stiffness = 200f)
                                },
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    
                    Box(modifier = viewsModifier) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                CupertinoIcons.Default.Play,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = FormatUtils.formatStat(video.play.toLong()),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                   
                    Spacer(Modifier.width(12.dp))
                    
                    // 🔗 [共享元素] 评论数 (映射到详情页的弹幕数位置)
                    var danmakuModifier = Modifier.wrapContentSize()
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            danmakuModifier = danmakuModifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "video_danmaku_${video.bvid}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(dampingRatio = 0.8f, stiffness = 200f)
                                },
                                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    Box(modifier = danmakuModifier) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(
                                CupertinoIcons.Default.Message,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = FormatUtils.formatStat(video.comment.toLong()),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // 更多按钮
        IconButton(
            onClick = { /* TODO: 更多操作菜单 */ },
            modifier = Modifier.size(32.dp).align(Alignment.CenterVertically)
        ) {
            Icon(
                CupertinoIcons.Default.Ellipsis,
                contentDescription = "更多",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 *  分类标签行组件
 */
@Composable
private fun CategoryTabRow(
    categories: List<SpaceVideoCategory>,
    selectedTid: Int,
    onCategoryClick: (Int) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // 全部按钮
        item {
            CategoryChip(
                text = "全部",
                isSelected = selectedTid == 0,
                onClick = { onCategoryClick(0) }
            )
        }
        
        // 分类按钮
        items(categories, key = { it.tid }) { category ->
            CategoryChip(
                text = "${category.name} (${category.count})",
                isSelected = selectedTid == category.tid,
                onClick = { onCategoryClick(category.tid) }
            )
        }
    }
}

/**
 *  分类标签芯片
 */
@Composable
private fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) 
                Color.White 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 *  排序按钮行
 */
@Composable
private fun SortButtonRow(
    currentOrder: VideoSortOrder,
    onOrderClick: (VideoSortOrder) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoSortOrder.entries.forEach { order ->
            SortChip(
                text = order.displayName,
                isSelected = currentOrder == order,
                onClick = { onOrderClick(order) }
            )
        }
    }
}

/**
 *  排序芯片
 */
@Composable
private fun SortChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) 
                Color.White 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 *  Space Tab 导航栏
 */
@Composable
private fun SpaceTabRow(
    selectedTab: Int,
    videoCount: Int,
    collectionsCount: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        TabItem(0, "主页", CupertinoIcons.Default.House),
        TabItem(1, "动态", CupertinoIcons.Default.Bell),
        TabItem(2, "投稿", CupertinoIcons.Default.PlayCircle, if (videoCount > 999) "999+" else if (videoCount > 0) videoCount.toString() else null),
        TabItem(3, "合集和系列", CupertinoIcons.Default.Folder, if (collectionsCount > 0) collectionsCount.toString() else null)
    )
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                SpaceTab(
                    tab = tab,
                    isSelected = selectedTab == tab.index,
                    onClick = { onTabSelected(tab.index) }
                )
            }
        }
        
        // 下划线指示器
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

private data class TabItem(
    val index: Int,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badge: String? = null
)

@Composable
private fun SpaceTab(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = tab.title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 数量徽章
            if (tab.badge != null) {
                Spacer(Modifier.width(2.dp))
                Text(
                    text = tab.badge,
                    fontSize = 11.sp,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        
        // 选中指示条
        if (isSelected) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 *  合集区块 - 横向滚动
 */
@Composable
private fun SeasonSection(
    season: SeasonItem,
    archives: List<SeasonArchiveItem>,
    onVideoClick: (String) -> Unit,
    mid: Long = 0L,  // UP主的mid，用于构建分享链接
    onMoreClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "合集 · ${season.meta.name}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = " · ${season.meta.total}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            // 分享按钮
            IconButton(
                onClick = {
                    // 使用 space.bilibili.com 域名（www 域名会 404）
                    val shareUrl = "https://space.bilibili.com/$mid/lists/${season.meta.season_id}?type=season"
                    val shareText = "${season.meta.name}\n$shareUrl"
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "【合集】${season.meta.name}")
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "分享合集"))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    CupertinoIcons.Default.SquareAndArrowUp,
                    contentDescription = "分享合集",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.width(4.dp))
            
            // 查看全部按钮
            TextButton(
                onClick = onMoreClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "查看全部 >",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 横向视频列表
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(archives, key = { it.bvid }) { archive ->
                SeasonVideoCard(
                    archive = archive,
                    onClick = { onVideoClick(archive.bvid) }
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
    }
}


/**
 *  系列区块
 */
@Composable
private fun SeriesSection(
    series: SeriesItem,
    archives: List<SeriesArchiveItem>,
    onVideoClick: (String) -> Unit,
    onMoreClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "系列 · ${series.meta.name}",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = " · ${series.meta.total}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(Modifier.width(4.dp))
            
            // 查看全部按钮
            TextButton(
                onClick = onMoreClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "查看全部 >",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // 横向视频列表
        if (archives.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(archives, key = { it.bvid }) { archive ->
                    SeriesVideoCard(
                        archive = archive,
                        onClick = { onVideoClick(archive.bvid) }
                    )
                }
            }
        } else {
            Text(
                text = "暂无视频",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        Spacer(Modifier.height(12.dp))
    }
}

/**
 *  合集视频卡片 - 紧凑横向布局
 */
@Composable
private fun SeasonVideoCard(
    archive: SeasonArchiveItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(archive.pic))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // 时长标签
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = FormatUtils.formatDuration(archive.duration),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
        
        Spacer(Modifier.height(6.dp))
        
        // 标题
        Text(
            text = archive.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 播放量
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                CupertinoIcons.Default.Play,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = FormatUtils.formatStat(archive.stat.view),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 *  系列视频卡片 - 紧凑横向布局
 */
@Composable
private fun SeriesVideoCard(
    archive: SeriesArchiveItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(archive.pic))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // 时长标签
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = FormatUtils.formatDuration(archive.duration),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
        
        Spacer(Modifier.height(6.dp))
        
        // 标题
        Text(
            text = archive.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 播放量
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                CupertinoIcons.Default.Play,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = FormatUtils.formatStat(archive.stat.view),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ==========  主页 Tab 组件 ==========

/**
 * 主页区块标题 - "视频 xxxx" + "查看更多 >"
 */
@Composable
private fun SpaceHomeSectionHeader(
    title: String,
    count: Int,
    onViewMore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = count.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(Modifier.weight(1f))
        
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onViewMore() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "查看更多",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Icon(
                CupertinoIcons.Default.ChevronForward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 主页视频卡片 - 网格样式
 */
@Composable
private fun SpaceHomeVideoCard(
    video: SpaceVideoItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .iOSTapEffect(scale = 0.97f) { onClick() }
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(video.pic))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // 时长
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = video.length,
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        
        Spacer(Modifier.height(6.dp))
        
        // 标题
        Text(
            text = video.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(4.dp))
        
        // 播放量
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                CupertinoIcons.Default.Play,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = FormatUtils.formatStat(video.play.toLong()),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

/**
 *  置顶视频卡片
 */
@Composable
private fun SpaceHomeTopVideo(
    topVideo: SpaceTopArcData,
    onVideoClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "📌 置顶视频",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onVideoClick(topVideo.bvid) }
                .padding(12.dp)
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(topVideo.pic))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 时长
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(topVideo.duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topVideo.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(Modifier.height(8.dp))
                
                // 置顶理由
                if (topVideo.reason.isNotEmpty()) {
                    Text(
                        text = "「${topVideo.reason}」",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                
                Spacer(Modifier.weight(1f))
                
                // 统计
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        CupertinoIcons.Default.Play,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = " ${FormatUtils.formatStat(topVideo.stat.view)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        CupertinoIcons.Default.HandThumbsup,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = " ${FormatUtils.formatStat(topVideo.stat.like)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 *  公告卡片
 */
@Composable
private fun SpaceHomeNotice(notice: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "📢 公告",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = notice,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
                lineHeight = 20.sp
            )
        }
    }
}

// ==========  动态 Tab 组件 ==========

/**
 *  动态卡片（简化版，不复用 DynamicCard 以避免依赖问题）
 */
@Composable
private fun SpaceDynamicCard(
    dynamic: SpaceDynamicItem,
    onVideoClick: (String) -> Unit,
    onDynamicWebClick: (String) -> Unit
) {
    val author = dynamic.modules.module_author
    val content = dynamic.modules.module_dynamic
    val stat = dynamic.modules.module_stat
    
    val clickAction = remember(dynamic) { resolveSpaceDynamicClickAction(dynamic) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = clickAction != SpaceDynamicClickAction.None) {
                when (clickAction) {
                    is SpaceDynamicClickAction.OpenVideo -> onVideoClick(clickAction.bvid)
                    is SpaceDynamicClickAction.OpenWeb -> onDynamicWebClick(clickAction.url)
                    SpaceDynamicClickAction.None -> Unit
                }
            }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        // 发布时间
        if (author != null && author.pub_time.isNotEmpty()) {
            Text(
                text = author.pub_time,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // 文字内容
        val text = content?.desc?.text ?: content?.major?.opus?.summary?.text ?: ""
        if (text.isNotEmpty()) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // 视频类型
        content?.major?.archive?.let { archive ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onVideoClick(archive.bvid) }
                    .padding(8.dp)
            ) {
                // 封面
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(archive.cover))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .height(75.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                
                Spacer(Modifier.width(10.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = archive.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            CupertinoIcons.Default.Play,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = " ${archive.stat.play}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
        
        // 图片类型
        content?.major?.draw?.let { draw ->
            if (draw.items.isNotEmpty()) {
                val imageCount = draw.items.size
                val columns = when {
                    imageCount == 1 -> 1
                    imageCount <= 4 -> 2
                    else -> 3
                }
                
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((if (imageCount == 1) 200 else if (imageCount <= 4) 160 else 180).dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = false
                ) {
                    items(draw.items.take(9)) { item ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(FormatUtils.fixImageUrl(item.src))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
        
        // 统计
        if (stat != null) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        CupertinoIcons.Default.ArrowTurnUpRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = " ${stat.forward.count}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        CupertinoIcons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = " ${stat.comment.count}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        CupertinoIcons.Default.HandThumbsup,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = " ${stat.like.count}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ==========  Uploads Tab Sidebar Component (Official Style) ==========

/**
 * 投稿侧边栏 - 官方客户端风格
 * 左侧显示内容类型和数量
 */
@Composable
private fun SpaceUploadsSidebar(
    selectedTab: SpaceSubTab,
    videoCount: Int,
    articleCount: Int,
    audioCount: Int,
    onTabSelected: (SpaceSubTab) -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.95f))
            .padding(vertical = 8.dp)
    ) {
        SidebarItem(
            title = "视频",
            count = videoCount,
            isSelected = selectedTab == SpaceSubTab.VIDEO,
            onClick = { onTabSelected(SpaceSubTab.VIDEO) }
        )
        SidebarItem(
            title = "图文",
            count = articleCount,
            isSelected = selectedTab == SpaceSubTab.ARTICLE,
            onClick = { onTabSelected(SpaceSubTab.ARTICLE) }
        )
        SidebarItem(
            title = "音频",
            count = audioCount,
            isSelected = selectedTab == SpaceSubTab.AUDIO,
            onClick = { onTabSelected(SpaceSubTab.AUDIO) }
        )
    }
}

@Composable
private fun SidebarItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) 
        MaterialTheme.colorScheme.surface 
    else 
        Color.Transparent
    
    val textColor = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        Color.White.copy(alpha = 0.9f)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(backgroundColor)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (count > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 投稿分类头部 - 官方客户端风格水平标签
 * 显示视频/图文/音频分类及数量（可横向滚动）
 */
@Composable
private fun SpaceUploadsHeader(
    selectedTab: SpaceSubTab,
    videoCount: Int,
    articleCount: Int,
    audioCount: Int,
    onTabSelected: (SpaceSubTab) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            UploadsHeaderTab(
                title = "视频",
                count = videoCount,
                isSelected = selectedTab == SpaceSubTab.VIDEO,
                onClick = { onTabSelected(SpaceSubTab.VIDEO) }
            )
        }
        item {
            UploadsHeaderTab(
                title = "图文",
                count = articleCount,
                isSelected = selectedTab == SpaceSubTab.ARTICLE,
                onClick = { onTabSelected(SpaceSubTab.ARTICLE) }
            )
        }
        item {
            UploadsHeaderTab(
                title = "音频",
                count = audioCount,
                isSelected = selectedTab == SpaceSubTab.AUDIO,
                onClick = { onTabSelected(SpaceSubTab.AUDIO) }
            )
        }
    }
}

@Composable
private fun UploadsHeaderTab(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) 
        MaterialTheme.colorScheme.primary 
    else 
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    
    val textColor = if (isSelected) 
        Color.White 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant
    
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
            if (count > 0) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==========  Uploads Sub-Tab Components ==========

@Composable
private fun SpaceSubTabRow(
    selectedTab: SpaceSubTab,
    onTabSelected: (SpaceSubTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpaceSubTabChip("视频", selectedTab == SpaceSubTab.VIDEO) { onTabSelected(SpaceSubTab.VIDEO) }
        SpaceSubTabChip("音频", selectedTab == SpaceSubTab.AUDIO) { onTabSelected(SpaceSubTab.AUDIO) }
        SpaceSubTabChip("专栏", selectedTab == SpaceSubTab.ARTICLE) { onTabSelected(SpaceSubTab.ARTICLE) }
    }
}

@Composable
private fun SpaceSubTabChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
   Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    } 
}

@Composable
private fun SpaceAudioCard(
    audio: com.android.purebilibili.data.model.response.SpaceAudioItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
             AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(com.android.purebilibili.core.util.FormatUtils.fixImageUrl(audio.cover))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    CupertinoIcons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                 Spacer(Modifier.width(4.dp))
                Text(
                    text = "${com.android.purebilibili.core.util.FormatUtils.formatStat(audio.play_count.toLong())}播放 · ${com.android.purebilibili.core.util.FormatUtils.formatDuration(audio.duration)}",
                     fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        
        IconButton(onClick = onClick) {
             Icon(
                CupertinoIcons.Default.PlayCircle,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SpaceArticleCard(
    article: com.android.purebilibili.data.model.response.SpaceArticleItem,
    onClick: () -> Unit
) {
     Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Title
        Text(
            text = article.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (article.image_urls.isNotEmpty()) {
             Spacer(Modifier.height(8.dp))
             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                 article.image_urls.take(3).forEach { url ->
                     AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(com.android.purebilibili.core.util.FormatUtils.fixImageUrl(url))
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                 }
             }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Stats
        Row(verticalAlignment = Alignment.CenterVertically) {
             Text(
                text = if (article.category?.name?.isNotEmpty() == true) article.category.name else "专栏",
                 fontSize = 11.sp,
                 color = MaterialTheme.colorScheme.primary,
                 modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
            
            Spacer(Modifier.width(8.dp))
            
            Text(
                text = "${com.android.purebilibili.core.util.FormatUtils.formatStat(article.stats?.view?.toLong() ?: 0)}阅读 · ${com.android.purebilibili.core.util.FormatUtils.formatStat(article.stats?.like?.toLong() ?: 0)}点赞",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    }
}
