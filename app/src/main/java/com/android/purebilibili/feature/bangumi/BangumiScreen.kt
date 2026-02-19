// 文件路径: feature/bangumi/BangumiScreen.kt
package com.android.purebilibili.feature.bangumi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.data.model.response.BangumiFilter
import com.android.purebilibili.data.model.response.BangumiItem
import com.android.purebilibili.data.model.response.BangumiSearchItem
import com.android.purebilibili.data.model.response.BangumiType
// [重构] 使用提取的可复用组件
import com.android.purebilibili.feature.bangumi.ui.components.BangumiModeTabs
import com.android.purebilibili.feature.bangumi.ui.components.BangumiFilterPanel
import com.android.purebilibili.feature.bangumi.ui.list.BangumiCard
import com.android.purebilibili.feature.bangumi.ui.list.BangumiGrid
import com.android.purebilibili.feature.bangumi.ui.list.BangumiSearchCard
import com.android.purebilibili.feature.bangumi.ui.list.BangumiSearchCardGrid
import com.android.purebilibili.feature.bangumi.ui.components.FilterChip

/**
 * 番剧主页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BangumiScreen(
    onBack: () -> Unit,
    onBangumiClick: (Long) -> Unit,  // 点击番剧 -> seasonId
    initialType: Int = 1,  // 初始类型：1=番剧 2=电影 等
    viewModel: BangumiViewModel = viewModel()
) {
    val displayMode by viewModel.displayMode.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val listState by viewModel.listState.collectAsState()
    val timelineState by viewModel.timelineState.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val myFollowState by viewModel.myFollowState.collectAsState()
    val myFollowType by viewModel.myFollowType.collectAsState()
    val myFollowStats by viewModel.myFollowStats.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    
    // 搜索状态
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 筛选器展开状态
    var showFilter by remember { mutableStateOf(false) }
    
    // 初始类型切换
    LaunchedEffect(initialType) {
        if (initialType != 1) {
            viewModel.selectType(initialType)
        }
    }
    
    // 番剧类型列表
    val types = listOf(
        BangumiType.ANIME,
        BangumiType.GUOCHUANG,
        BangumiType.MOVIE,
        BangumiType.TV_SHOW,
        BangumiType.DOCUMENTARY,
        BangumiType.VARIETY
    )
    
    //  [修复] 设置导航栏透明，确保底部手势栏沉浸式效果
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        
        onDispose {
            if (window != null) {
                window.navigationBarColor = originalNavBarColor
            }
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        topBar = {
            if (showSearchBar) {
                // 搜索模式顶栏
                BangumiSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        if (it.isNotBlank()) {
                            viewModel.searchBangumi(it)
                            keyboardController?.hide()
                        }
                    },
                    onBack = {
                        showSearchBar = false
                        searchQuery = ""
                        viewModel.clearSearch()
                    }
                )
            } else {
                BangumiNavigationBar(
                    title = if (displayMode == BangumiDisplayMode.MY_FOLLOW) "我的追番/追剧" else "番剧影视",
                    filterActive = filter != BangumiFilter(),
                    isMyFollowMode = displayMode == BangumiDisplayMode.MY_FOLLOW,
                    onBack = {
                        if (displayMode == BangumiDisplayMode.MY_FOLLOW) {
                            viewModel.setDisplayMode(BangumiDisplayMode.LIST)
                        } else {
                            onBack()
                        }
                    },
                    onSearch = { showSearchBar = true },
                    onOpenMyFollow = { viewModel.openMyFollowEntry() },
                    onToggleFilter = { showFilter = !showFilter }
                )
            }
        },
        //  [修复] 禁用 Scaffold 默认的 WindowInsets 消耗，让内容区域自行处理
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .responsiveContentWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
                //  [修复] 移除这里的底部内边距，让内容区域自己处理（如 LazyVerticalGrid 的 contentPadding）
        ) {
            // 模式切换 Tabs (时间表/索引)
            if (displayMode == BangumiDisplayMode.LIST || displayMode == BangumiDisplayMode.TIMELINE) {
                BangumiModeTabs(
                    currentMode = displayMode,
                    onModeChange = { viewModel.setDisplayMode(it) }
                )
            }
            
            // 类型选择 Tabs (仅列表模式显示)
            if (displayMode == BangumiDisplayMode.LIST) {
                BangumiTypeTabs(
                    types = types,
                    selectedType = selectedType,
                    onTypeSelected = { viewModel.selectType(it) }
                )
                
                // 筛选器
                if (showFilter) {
                    BangumiFilterPanel(
                        filter = filter,
                        onFilterChange = { viewModel.updateFilter(it) },
                        onDismiss = { showFilter = false }
                    )
                }
            }
            
            // 内容区域
            when (displayMode) {
                BangumiDisplayMode.LIST -> {
                    BangumiListContent(
                        listState = listState,
                        onRetry = { viewModel.loadBangumiList() },
                        onLoadMore = { viewModel.loadMore() },
                        onItemClick = onBangumiClick
                    )
                }
                BangumiDisplayMode.TIMELINE -> {
                    BangumiTimelineContent(
                        timelineState = timelineState,
                        onRetry = { viewModel.loadTimeline() },
                        onBangumiClick = onBangumiClick
                    )
                }
                BangumiDisplayMode.MY_FOLLOW -> {
                    MyBangumiContent(
                        myFollowState = myFollowState,
                        followStats = myFollowStats,
                        followType = myFollowType,
                        onFollowTypeChange = { viewModel.selectMyFollowType(it) },
                        onRetry = { viewModel.loadMyFollowBangumi(myFollowType) },
                        onLoadMore = { viewModel.loadMoreMyFollow() },
                        onBangumiClick = onBangumiClick
                    )
                }
                BangumiDisplayMode.SEARCH -> {
                    BangumiSearchContent(
                        searchState = searchState,
                        onRetry = { viewModel.searchBangumi(searchKeyword) },
                        onLoadMore = { viewModel.loadMoreSearchResults() },
                        onItemClick = onBangumiClick
                    )
                }
            }
        }
    }
}


/**
 * 搜索顶栏
 */
@Composable
private fun BangumiSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().responsiveContentWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp
    ) {
        Column {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // 搜索框
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        CupertinoIcons.Default.MagnifyingGlass,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        "搜索番剧名称、声优...",
                                        style = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                            fontSize = 15.sp
                                        )
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                CupertinoIcons.Default.XmarkCircle,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                TextButton(
                    onClick = { onSearch(query) },
                    enabled = query.isNotEmpty()
                ) {
                    Text(
                        "搜索",
                        color = if (query.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BangumiNavigationBar(
    title: String,
    filterActive: Boolean,
    isMyFollowMode: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpenMyFollow: () -> Unit,
    onToggleFilter: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val titleFontSize = resolveBangumiNavigationTitleFontSizeSp(configuration.screenWidthDp).sp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 2.dp
    ) {
        Column {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                }
                Text(
                    text = title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSearch) {
                    Icon(CupertinoIcons.Default.MagnifyingGlass, contentDescription = "搜索")
                }
                IconButton(onClick = onOpenMyFollow) {
                    Icon(
                        CupertinoIcons.Default.Bookmark,
                        contentDescription = "我的追番",
                        tint = if (isMyFollowMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                if (!isMyFollowMode) {
                    IconButton(onClick = onToggleFilter) {
                        Icon(
                            CupertinoIcons.Default.ListBullet,
                            contentDescription = "筛选",
                            tint = if (filterActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    }
}

@Composable
private fun BangumiTypeTabs(
    types: List<BangumiType>,
    selectedType: Int,
    onTypeSelected: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val tabFontSize = resolveBangumiTypeTabFontSizeSp(configuration.screenWidthDp).sp

    Surface(color = MaterialTheme.colorScheme.surface) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(types, key = { it.value }) { type ->
                val selected = type.value == selectedType
                Column(
                    modifier = Modifier
                        .clickable { onTypeSelected(type.value) }
                        .padding(top = 10.dp, bottom = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = type.label,
                        fontSize = tabFontSize,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

/**
 * 番剧列表内容
 */
@Composable
private fun BangumiListContent(
    listState: BangumiListState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    when (listState) {
        is BangumiListState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is BangumiListState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = listState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        }
        is BangumiListState.Success -> {
            BangumiGrid(
                items = listState.items,
                hasMore = listState.hasMore,
                onLoadMore = onLoadMore,
                onItemClick = { onItemClick(it.seasonId) }
            )
        }
    }
}

/**
 * 番剧搜索结果内容
 */
@Composable
private fun BangumiSearchContent(
    searchState: BangumiSearchState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    when (searchState) {
        is BangumiSearchState.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "输入关键词搜索番剧",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is BangumiSearchState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is BangumiSearchState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = searchState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                }
            }
        }
        is BangumiSearchState.Success -> {
            if (searchState.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "未找到相关番剧",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                BangumiSearchGrid(
                    items = searchState.items,
                    hasMore = searchState.hasMore,
                    onLoadMore = onLoadMore,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun BangumiSearchGrid(
    items: List<BangumiSearchItem>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onItemClick: (Long) -> Unit
) {
    val gridState = rememberLazyGridState()
    
    LaunchedEffect(gridState) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= layoutInfo.totalItemsCount - 4
        }.collect { shouldLoad ->
            if (shouldLoad && hasMore) {
                onLoadMore()
            }
        }
    }
    
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        state = gridState,
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 12.dp,
            end = 12.dp,
            bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = items,
            key = { it.seasonId }
        ) { item ->
            BangumiSearchCardGrid(
                item = item,
                onClick = { onItemClick(item.seasonId) }
            )
        }
        
        if (hasMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
