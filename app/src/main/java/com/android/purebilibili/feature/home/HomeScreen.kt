// 文件路径: feature/home/HomeScreen.kt
package com.android.purebilibili.feature.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.ExperimentalFoundationApi //  Added
import androidx.compose.foundation.LocalOverscrollFactory // [Fix] Import for disabling overscroll (New API)
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.staggeredgrid.*  // 🌊 瀑布流布局
import com.kyant.backdrop.backdrops.layerBackdrop // [Fix] Import for modifier
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import com.android.purebilibili.feature.home.components.MineSideDrawer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.luminance  //  状态栏亮度计算
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.theme.BiliPink
import com.android.purebilibili.feature.settings.GITHUB_URL
import com.android.purebilibili.core.store.SettingsManager //  引入 SettingsManager
//  从 components 包导入拆分后的组件
import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.feature.home.components.FluidHomeTopBar
import com.android.purebilibili.feature.home.components.FrostedSideBar
import com.android.purebilibili.feature.home.components.CategoryTabRow
import com.android.purebilibili.feature.home.components.iOSHomeHeader  //  iOS 大标题头部
import com.android.purebilibili.feature.home.components.iOSRefreshIndicator  //  iOS 下拉刷新指示器
import com.android.purebilibili.feature.home.components.resolveHomeDrawerScrimAlpha
import com.android.purebilibili.feature.home.components.resolveTopTabStyle
//  从 cards 子包导入卡片组件
import com.android.purebilibili.feature.home.components.cards.ElegantVideoCard
import com.android.purebilibili.feature.home.components.cards.LiveRoomCard
import com.android.purebilibili.feature.home.components.cards.StoryVideoCard   //  故事卡片
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.VideoCardSkeleton
import com.android.purebilibili.core.ui.ErrorState as ModernErrorState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.core.ui.shimmer
import com.android.purebilibili.core.ui.LocalSharedTransitionScope  //  共享过渡
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard  //  粒子消散动画
import com.android.purebilibili.core.ui.animation.jiggleOnDissolve      // 📳 iOS 风格抖动效果
import com.android.purebilibili.core.util.responsiveContentWidth
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.core.util.resolveScrollToTopPlan
import com.android.purebilibili.core.util.resolveTvHomePerformanceConfig
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import coil.imageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged  //  性能优化：防止重复触发
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import androidx.compose.animation.ExperimentalSharedTransitionApi  //  共享过渡实验API
import com.android.purebilibili.core.ui.LocalSetBottomBarVisible
import com.android.purebilibili.core.ui.LocalBottomBarVisible

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.android.purebilibili.data.model.response.VideoItem // [Fix] Import VideoItem
import com.android.purebilibili.feature.home.components.VideoPreviewDialog // [Fix] Import VideoPreviewDialog

// [新增] 全局回顶事件通道
val LocalHomeScrollChannel = compositionLocalOf<Channel<Unit>?> { null }

// [New] Global Scroll Offset for Liquid Glass Effect
// Used to pass scroll position from HomeScreen to BottomBar without causing recomposition
val LocalHomeScrollOffset = compositionLocalOf { androidx.compose.runtime.mutableFloatStateOf(0f) }

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onVideoClick: (String, Long, String) -> Unit,
    onAvatarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    //  新增：动态页面回调
    onDynamicClick: () -> Unit = {},
    //  新增：历史记录回调
    onHistoryClick: () -> Unit = {},
    //  新增：分区回调
    onPartitionClick: () -> Unit = {},
    //  新增：直播点击回调
    onLiveClick: (Long, String, String) -> Unit = { _, _, _ -> },  // roomId, title, uname
    //  [修复] 番剧/影视回调，接受类型参数 (1=番剧 2=电影 等)
    onBangumiClick: (Int) -> Unit = {},
    //  新增：分类点击回调（用于游戏、知识、科技等分类，传入 tid 和 name）
    onCategoryClick: (Int, String) -> Unit = { _, _ -> },
    //  [新增] 底栏扩展项目导航回调
    onFavoriteClick: () -> Unit = {},  // 收藏页面
    onLiveListClick: () -> Unit = {},  // 直播列表页面
    onWatchLaterClick: () -> Unit = {},  // 稍后再看页面
    onDownloadClick: () -> Unit = {},  // 离线缓存页面
    onInboxClick: () -> Unit = {},  // 私信页面
    onStoryClick: () -> Unit = {},  //  [新增] 竖屏短视频
    globalHazeState: dev.chrisbanes.haze.HazeState? = null  //  [新增] 全局底栏模糊状态
) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
// val pullRefreshState = rememberPullToRefreshState() // [Removed] Moved inside HorizontalPager
    val context = LocalContext.current
    //  [Refactor] Use a map of grid states for each category to support HorizontalPager
    // [Refactor] Use a map of grid states for each category to support HorizontalPager
    val gridStates = remember { mutableMapOf<HomeCategory, LazyGridState>() }
    HomeCategory.entries.forEach { category ->
        gridStates[category] = rememberLazyGridState()
    }
    val staggeredGridState = rememberLazyStaggeredGridState() // 🌊 瀑布流状态
    val localHazeState = remember { HazeState(initialBlurEnabled = true) }
    // 首页使用独立 HazeState，避免命中外层全局 source 的祖先过滤规则导致无模糊。
    val hazeState = localHazeState


    // [Feature] Video Preview State (Global Scope)
    val targetVideoItemState = remember { mutableStateOf<VideoItem?>(null) }
    val homeBackdrop = rememberLayerBackdrop()

    val coroutineScope = rememberCoroutineScope() // 用于双击回顶动画
    // [Header] 首页重选/双击回顶时需要强制恢复顶部，避免自动收缩后残留空白区域
    var headerOffsetHeightPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var delayTopTabsUntilCardSettled by remember { mutableStateOf(false) }
    var hideTopTabsForForwardDetailNav by remember { mutableStateOf(false) }

    // [新增] 监听全局回顶事件
    val scrollChannel = LocalHomeScrollChannel.current
    LaunchedEffect(scrollChannel) {
        scrollChannel?.receiveAsFlow()?.collect {
            launch {
                // 双击首页回顶时强制展开顶部，避免收缩头部与回顶状态错位导致空白
                headerOffsetHeightPx = 0f
                val gridState = gridStates[state.currentCategory]
                val isAtTop = gridState == null || (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 50)

                if (isAtTop) {
                    viewModel.refresh()
                } else {
                    val currentIndex = gridState?.firstVisibleItemIndex ?: 0
                    val plan = resolveScrollToTopPlan(currentIndex)
                    plan.preJumpIndex?.let { preJump ->
                        if (currentIndex > preJump) {
                            gridState?.scrollToItem(preJump)
                        }
                    }
                    gridState?.animateScrollToItem(plan.animateTargetIndex)
                }
                headerOffsetHeightPx = 0f
            }
        }
    }

    // [P2] 顶栏自定义：顺序与可见项从设置读取
    val defaultTopTabIds = remember { resolveDefaultHomeTopTabIds() }
    val topTabOrderIds by SettingsManager.getTopTabOrder(context).collectAsState(
        initial = defaultTopTabIds
    )
    val topTabVisibleIds by SettingsManager.getTopTabVisibleTabs(context).collectAsState(
        initial = defaultTopTabIds.toSet()
    )
    // [Refactor] Hoist PagerState to be available for both Content and Header
    // 确保 pagerState 在所有作用域均可见，以便传给 iOSHomeHeader
    val topCategories = remember(topTabOrderIds, topTabVisibleIds) {
        resolveHomeTopCategories(
            customOrderIds = topTabOrderIds,
            visibleIds = topTabVisibleIds
        )
    }
    val initialPage = resolveHomeTopTabIndex(state.currentCategory, topCategories)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = initialPage) { topCategories.size }
    
    // [修复] 监听 Pager 滑动，同步更新 ViewModel 分类
    LaunchedEffect(pagerState.currentPage) {
        val targetCategory = resolveHomeCategoryForTopTab(
            index = pagerState.currentPage,
            topCategories = topCategories
        )
        viewModel.switchCategory(targetCategory)
    }

    // [P2] 当前分类被隐藏时，自动落到首个可见分类
    LaunchedEffect(topCategories) {
        val firstVisible = topCategories.firstOrNull() ?: return@LaunchedEffect
        if (state.currentCategory !in topCategories) {
            viewModel.updateDisplayedTabIndex(0)
            viewModel.switchCategory(firstVisible)
        }
    }

    // [CrashFix] 顶栏配置变化导致页数收缩时，先钳制 pager 当前页，避免越界
    LaunchedEffect(topCategories.size) {
        if (topCategories.isEmpty()) return@LaunchedEffect
        val lastIndex = topCategories.lastIndex
        if (pagerState.currentPage > lastIndex) {
            pagerState.scrollToPage(lastIndex)
        }
    }

    // [修复] 监听 ViewModel 状态变化，同步更新 Pager 位置
    // 当点击 Tab 导致 state.currentCategory 变化时，让 Pager 自动跟随滚动
    LaunchedEffect(state.currentCategory) {
        val targetPage = topCategories.indexOf(state.currentCategory)
        if (targetPage >= 0 && targetPage != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // [修复] 刷新时自动滚回顶部，防止下拉用力过猛导致内容偏移
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            gridStates[state.currentCategory]?.animateScrollToItem(0)
        }
    }

    // 从详情页返回时仅清理一次全局卡片状态，避免每个分页重复触发
    LaunchedEffect(Unit) {
        if (CardPositionManager.isReturningFromDetail) {
            delay(100)
            CardPositionManager.clearReturning()
        }
        if (CardPositionManager.isSwitchingCategory) {
            delay(300)
            CardPositionManager.isSwitchingCategory = false
        }
    }
    
    //  [新增] JSON 插件过滤提示
    val snackbarHostState = remember { SnackbarHostState() }
    val lastFilteredCount by com.android.purebilibili.core.plugin.json.JsonPluginManager.lastFilteredCount.collectAsState()
    
    //  当有视频被过滤时显示提示
    LaunchedEffect(lastFilteredCount) {
        if (lastFilteredCount > 0) {
            snackbarHostState.showSnackbar(
                message = " 已过滤 $lastFilteredCount 个视频",
                duration = SnackbarDuration.Short
            )
        }
    }
    
    //  [埋点] 页面浏览追踪
    LaunchedEffect(Unit) {
        com.android.purebilibili.core.util.AnalyticsHelper.logScreenView("HomeScreen")
    }
    
    //  [埋点] 分类切换追踪
    LaunchedEffect(state.currentCategory) {
        com.android.purebilibili.core.util.AnalyticsHelper.logCategoryView(
            categoryName = state.currentCategory.label,
            categoryId = state.currentCategory.tid
        )
    }

    // [New] Broadcast Scroll Offset for Liquid Glass Effect & Parallax
    // Create the state here and provide it

    
    //  [彩蛋] 彩蛋开关设置
    val easterEggEnabled by SettingsManager.getEasterEggEnabled(context).collectAsState(initial = true)
    var showEasterEggDialog by remember { mutableStateOf(false) }
    var refreshDeltaTipText by remember { mutableStateOf<String?>(null) }
    var dividerRevealRefreshKey by rememberSaveable { mutableLongStateOf(0L) }
    
    //  [彩蛋] 下拉刷新成功后显示趣味提示（仅在开关开启时）
    LaunchedEffect(state.refreshKey, easterEggEnabled) {
        val message = state.refreshMessage
        if (message != null && state.refreshKey > 0 && easterEggEnabled) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "关闭彩蛋",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                showEasterEggDialog = true
            }
        }
    }

    LaunchedEffect(state.refreshNewItemsKey) {
        val refreshKey = state.refreshNewItemsKey
        if (!shouldHandleRefreshNewItemsEvent(refreshKey, state.refreshNewItemsHandledKey)) {
            return@LaunchedEffect
        }
        val count = state.refreshNewItemsCount ?: return@LaunchedEffect
        if (state.currentCategory == HomeCategory.RECOMMEND && count > 0) {
            // 增量刷新插入新内容后强制回到顶部，避免被锚定在刷新前位置。
            gridStates[HomeCategory.RECOMMEND]?.scrollToItem(0)
        }
        refreshDeltaTipText = if (count > 0) "新增 $count 条内容" else "暂无新内容"
        // 分割线需等待用户发生下滑后再展示
        if (count > 0) dividerRevealRefreshKey = 0L
        delay(2200)
        refreshDeltaTipText = null
        viewModel.markRefreshNewItemsHandled(refreshKey)
    }

    // 仅在推荐页检测“刷新后是否已下滑”，用于激活旧内容分割线
    LaunchedEffect(
        state.currentCategory,
        state.refreshNewItemsKey,
        state.recommendOldContentAnchorBvid,
        state.categoryStates[HomeCategory.RECOMMEND]?.videos
    ) {
        if (state.currentCategory != HomeCategory.RECOMMEND) return@LaunchedEffect
        if ((state.refreshNewItemsCount ?: 0) <= 0) return@LaunchedEffect
        val targetKey = state.refreshNewItemsKey
        if (targetKey <= 0L || dividerRevealRefreshKey == targetKey) return@LaunchedEffect

        val anchorBvid = state.recommendOldContentAnchorBvid ?: return@LaunchedEffect
        val recommendVideos = state.categoryStates[HomeCategory.RECOMMEND]?.videos ?: return@LaunchedEffect
        val anchorIndex = recommendVideos.indexOfFirst { it.bvid == anchorBvid }
        if (anchorIndex <= 0) return@LaunchedEffect

        val recommendState = gridStates[HomeCategory.RECOMMEND] ?: return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = recommendState.layoutInfo
            val reachedByVisible = layoutInfo.visibleItemsInfo.any { it.index == anchorIndex }
            val reachedByIndex = recommendState.firstVisibleItemIndex >= anchorIndex
            reachedByVisible || reachedByIndex
        }.first { it }
        dividerRevealRefreshKey = targetKey
    }
    
    //  [彩蛋] 关闭确认对话框
    if (showEasterEggDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showEasterEggDialog = false },
            title = { 
                Text(
                    "关闭趣味提示？", 
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = { 
                Text(
                    "关闭后下拉刷新将不再显示趣味消息。\n\n你可以在「设置」中随时重新开启。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        coroutineScope.launch {
                            SettingsManager.setEasterEggEnabled(context, false)
                        }
                        showEasterEggDialog = false
                    }
                ) { Text("关闭彩蛋", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showEasterEggDialog = false }
                ) { Text("保留彩蛋", color = MaterialTheme.colorScheme.primary) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    //  [修复] 确保首页显示时 WindowInsets 配置正确，防止从视频页返回时布局跳动
    val view = androidx.compose.ui.platform.LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        // 保持边到边显示（与 VideoDetailScreen 一致）
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    //  [性能优化] 合并首页设置为单一 Flow，减少 6 个 collectAsState → 1 个
    val homeSettings by SettingsManager.getHomeSettings(context).collectAsState(
        initial = com.android.purebilibili.core.store.HomeSettings()
    )
    val isTvDevice = rememberIsTvDevice()
    val isTvPerformanceProfileEnabled by SettingsManager.getTvPerformanceProfileEnabled(context).collectAsState(
        initial = isTvDevice
    )
    
    // 解构设置值（避免每次访问都触发重组）
    val displayMode = homeSettings.displayMode
    val isBottomBarFloating = homeSettings.isBottomBarFloating
    val bottomBarLabelMode = homeSettings.bottomBarLabelMode
    // 顶部模糊开关直接读独立 Flow，避免聚合设置延迟/不同步导致首页状态错误。
    val baseIsHeaderBlurEnabled by SettingsManager.getHeaderBlurEnabled(context).collectAsState(initial = true)
    val baseIsBottomBarBlurEnabled = homeSettings.isBottomBarBlurEnabled
    val crashTrackingConsentShown = homeSettings.crashTrackingConsentShown
    val baseCardAnimationEnabled = homeSettings.cardAnimationEnabled      //  卡片进场动画开关
    val baseCardTransitionEnabled = homeSettings.cardTransitionEnabled    //  卡片过渡动画开关
    val baseIsLiquidGlassEnabled = homeSettings.isLiquidGlassEnabled      //  流体玻璃特效开关
    val baseIsDataSaverActive = remember(context) {
        com.android.purebilibili.core.store.SettingsManager.isDataSaverActive(context)
    }
    val tvHomePerformanceConfig = remember(
        isTvDevice,
        isTvPerformanceProfileEnabled,
        baseIsHeaderBlurEnabled,
        baseIsBottomBarBlurEnabled,
        baseIsLiquidGlassEnabled,
        baseCardAnimationEnabled,
        baseCardTransitionEnabled,
        baseIsDataSaverActive
    ) {
        resolveTvHomePerformanceConfig(
            isTvDevice = isTvDevice,
            isTvPerformanceProfileEnabled = isTvPerformanceProfileEnabled,
            headerBlurEnabled = baseIsHeaderBlurEnabled,
            bottomBarBlurEnabled = baseIsBottomBarBlurEnabled,
            liquidGlassEnabled = baseIsLiquidGlassEnabled,
            cardAnimationEnabled = baseCardAnimationEnabled,
            cardTransitionEnabled = baseCardTransitionEnabled,
            isDataSaverActive = baseIsDataSaverActive
        )
    }
    val isHeaderBlurEnabled = tvHomePerformanceConfig.headerBlurEnabled
    val isBottomBarBlurEnabled = tvHomePerformanceConfig.bottomBarBlurEnabled
    val cardAnimationEnabled = tvHomePerformanceConfig.cardAnimationEnabled
    val cardTransitionEnabled = tvHomePerformanceConfig.cardTransitionEnabled
    val isLiquidGlassEnabled = tvHomePerformanceConfig.liquidGlassEnabled
    val isDataSaverActive = tvHomePerformanceConfig.isDataSaverActive
    val preloadAheadCount = tvHomePerformanceConfig.preloadAheadCount

    //  [新增] 底栏可见项目配置
    val orderedVisibleTabIds by SettingsManager.getOrderedVisibleTabs(context).collectAsState(
        initial = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE")
    )
    // 将字符串 ID 转换为 BottomNavItem 枚举
    val visibleBottomBarItems = remember(orderedVisibleTabIds) {
        orderedVisibleTabIds.mapNotNull { id ->
            try { BottomNavItem.valueOf(id) } catch (e: Exception) { null }
        }
    }
    
    //  [新增] 底栏项目颜色配置
    val bottomBarItemColors by SettingsManager.getBottomBarItemColors(context).collectAsState(initial = emptyMap<String, Int>())

    
    //  📐 [平板适配] 根据屏幕尺寸和展示模式动态设置网格列数
    // 故事卡片(1)和沉浸模式(2)需要单列全宽，网格(0)使用双列
    val windowSizeClass = com.android.purebilibili.core.util.LocalWindowSizeClass.current
    val deviceUiProfile = remember(
        isTvDevice,
        windowSizeClass.widthSizeClass,
        isTvPerformanceProfileEnabled
    ) {
        resolveDeviceUiProfile(
            isTv = isTvDevice,
            widthSizeClass = windowSizeClass.widthSizeClass,
            tvPerformanceProfileEnabled = isTvPerformanceProfileEnabled
        )
    }
    val cardMotionTier = resolveEffectiveMotionTier(
        baseTier = deviceUiProfile.motionTier,
        animationEnabled = cardAnimationEnabled
    )
    val contentWidth = if (windowSizeClass.isExpandedScreen) {
        minOf(windowSizeClass.widthDp, 1280.dp)
    } else {
        windowSizeClass.widthDp
    }
    
    // 是否为单列模式 (Story or Cinematic)
    val isSingleColumnMode = displayMode == 1
    
    val adaptiveColumns = remember(contentWidth, displayMode, homeSettings.gridColumnCount) {
        // [新增] 如果用户自定义了列数 (且非单列模式)，优先使用用户设置
        if (!isSingleColumnMode && homeSettings.gridColumnCount > 0) {
            return@remember homeSettings.gridColumnCount
        }

        val minColumnWidth = if (isSingleColumnMode) 280.dp else 180.dp // 单列模式给更宽的基准
        val maxColumns = if (isSingleColumnMode) 2 else 6
        val columns = (contentWidth / minColumnWidth).toInt()
        columns.coerceIn(1, maxColumns)
    }
    
    val gridColumns = if (!isSingleColumnMode && homeSettings.gridColumnCount > 0) {
        homeSettings.gridColumnCount
    } else if (windowSizeClass.isExpandedScreen) {
        adaptiveColumns
    } else {
        com.android.purebilibili.core.util.rememberResponsiveValue(
            compact = if (isSingleColumnMode) 1 else 2,  // 手机：单列模式1列，其他2列
            medium = if (isSingleColumnMode) 2 else 3    // 中等宽度：单列模式2列，其它3列
        )
    }
    
    
    //   [平板导航切换] 用户偏好设置
    val tabletUseSidebar by SettingsManager.getTabletUseSidebar(context).collectAsState(initial = false)
    
    //  📐 [大屏适配] 平板导航模式：根据用户偏好决定
    // 仅在平板且用户选择了侧边栏时使用侧边导航
    val useSideNavigation = com.android.purebilibili.core.util.shouldUseSidebarNavigationForLayout(
        windowSizeClass = windowSizeClass,
        tabletUseSidebar = tabletUseSidebar
    )
    val isHomeDrawerEnabled = com.android.purebilibili.core.util.shouldEnableHomeDrawer(
        useSideNavigation = useSideNavigation
    )
    
    //  📱 [切换导航模式] 处理函数
    val onToggleNavigationMode: () -> Unit = {
        coroutineScope.launch {
            SettingsManager.setTabletUseSidebar(context, !tabletUseSidebar)
        }
    }

    //  [修复] 恢复状态栏样式：确保从视频详情页返回后状态栏正确
    // 当使用滑动动画时，Theme.kt 的 SideEffect 可能不会重新执行
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightBackground = remember(backgroundColor) { backgroundColor.luminance() > 0.5f }
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
            //  根据背景亮度设置状态栏图标颜色
            insetsController.isAppearanceLightStatusBars = isLightBackground
            //  [修复] 导航栏也需要根据背景亮度设置图标颜色
            insetsController.isAppearanceLightNavigationBars = isLightBackground
            //  确保状态栏可见且透明
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            //  [修复] 导航栏也设为透明，确保底栏隐藏时手势区域沉浸
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    val density = LocalDensity.current
    val navBarHeight = WindowInsets.navigationBars.getBottom(density).let { with(density) { it.toDp() } }

    //  [修复] 动态计算内容顶部边距，防止被头部遮挡
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listTopPadding = statusBarHeight + 120.dp  // [调整] 优化顶部间距 (110 -> 120) 增加呼吸感
    val homeStartupElapsedAt = remember { SystemClock.elapsedRealtime() }
    var todayWatchStartupRevealHandled by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    //  当前选中的导航项
    var currentNavItem by remember { mutableStateOf(BottomNavItem.HOME) }

    // 统一导航点击逻辑（底栏/侧栏复用）
    val handleNavItemClick: (BottomNavItem) -> Unit = { item ->
        currentNavItem = item
        when (item) {
            BottomNavItem.HOME -> {
                coroutineScope.launch { 
                    headerOffsetHeightPx = 0f
                    val gridState = gridStates[state.currentCategory]
                    val isAtTop = gridState == null || (gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 50)
                    
                    if (isAtTop) {
                        viewModel.refresh()
                    } else {
                        // [性能优化] 逻辑同上，如果太远先瞬移回来
                        if ((gridState?.firstVisibleItemIndex ?: 0) > 12) {
                            gridState?.scrollToItem(12)
                        }
                        gridState?.animateScrollToItem(0)
                    } 
                    headerOffsetHeightPx = 0f
                }
            }
            BottomNavItem.DYNAMIC -> onDynamicClick()
            BottomNavItem.HISTORY -> onHistoryClick()
            BottomNavItem.PROFILE -> onProfileClick()
            BottomNavItem.FAVORITE -> onFavoriteClick()
            BottomNavItem.LIVE -> onLiveListClick()
            BottomNavItem.WATCHLATER -> onWatchLaterClick()
            BottomNavItem.STORY -> onStoryClick()
            BottomNavItem.SETTINGS -> onSettingsClick()
        }
    }
    
    //  [新增] 底栏显示模式设置
    val bottomBarVisibilityMode by SettingsManager.getBottomBarVisibilityMode(context).collectAsState(
        initial = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE
    )
    
    //  [Refactor] 使用全局 CompositionLocal 控制底栏可见性
    val setBottomBarVisible = LocalSetBottomBarVisible.current
    val isGlobalBottomBarVisible = LocalBottomBarVisible.current
    // 兼容代码：为了最小化改动，将 bottomBarVisible 指向全局状态
    // 注意：这里的 bottomBarVisible 现在是只读的，修改必须通过 setBottomBarVisible
    val bottomBarVisible = isGlobalBottomBarVisible
    val bottomBarBodyHeight = when (bottomBarLabelMode) {
        0 -> if (windowSizeClass.isTablet) 76.dp else 70.dp
        2 -> if (windowSizeClass.isTablet) 56.dp else 54.dp
        else -> if (windowSizeClass.isTablet) 68.dp else 62.dp
    }
    val dockedBarBodyHeight = when (bottomBarLabelMode) {
        0 -> 72.dp
        2 -> if (windowSizeClass.isTablet) 52.dp else 56.dp
        else -> 64.dp
    }
    val bottomBarVerticalInset = if (isBottomBarFloating) {
        if (windowSizeClass.isTablet) 20.dp else 16.dp
    } else {
        0.dp
    }
    val homeListBottomPadding = when {
        useSideNavigation -> navBarHeight + 8.dp
        !bottomBarVisible -> navBarHeight + 8.dp
        isBottomBarFloating -> bottomBarBodyHeight + bottomBarVerticalInset + navBarHeight + 12.dp
        else -> dockedBarBodyHeight + navBarHeight + 12.dp
    }
    
    //  [修复] 跟踪是否正在导航到/从视频页 - 必须在 LaunchedEffect 之前声明
    var isVideoNavigating by remember { mutableStateOf(false) }
    
    //  [新增] 滚动方向检测状态（用于上滑隐藏模式）
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    var lastFirstVisibleItem by remember { mutableIntStateOf(0) }
    
    //  [新增] 滚动方向检测逻辑
    LaunchedEffect(state.currentCategory, bottomBarVisibilityMode, useSideNavigation) {
        if (useSideNavigation) {
            setBottomBarVisible(false)
            return@LaunchedEffect
        }
        if (bottomBarVisibilityMode != SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE) {
            // 非滚动隐藏模式时，根据设置决定底栏可见性
            setBottomBarVisible(bottomBarVisibilityMode == SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE)
            return@LaunchedEffect
        }
        
        // 上滑隐藏模式：监听滚动方向
        val currentGridState = gridStates[state.currentCategory] ?: return@LaunchedEffect
        snapshotFlow {
            Pair(currentGridState.firstVisibleItemIndex, currentGridState.firstVisibleItemScrollOffset)
        }
        .distinctUntilChanged()
        .collect { (firstVisibleItem, scrollOffset) ->
            // 视频导航期间不处理滚动隐藏
            if (isVideoNavigating) return@collect
            
            // 滚动到顶部时始终显示
            if (firstVisibleItem == 0 && scrollOffset < 100) {
                setBottomBarVisible(true)
            } else {
                // 计算滚动方向
                val isScrollingDown = when {
                    firstVisibleItem > lastFirstVisibleItem -> true
                    firstVisibleItem < lastFirstVisibleItem -> false
                    else -> scrollOffset > lastScrollOffset + 200 // [UX优化] 增大迟滞阈值 (30 -> 200) 防止微小抖动
                }
                val isScrollingUp = when {
                    firstVisibleItem < lastFirstVisibleItem -> true
                    firstVisibleItem > lastFirstVisibleItem -> false
                    else -> scrollOffset < lastScrollOffset - 200 // [UX优化] 增大迟滞阈值
                }
                
                if (isScrollingDown) setBottomBarVisible(false)
                if (isScrollingUp) setBottomBarVisible(true)
                
                // 更新上次位置（用于下次比较）
                lastScrollOffset = scrollOffset
                lastFirstVisibleItem = firstVisibleItem
            }
        }
    }

    // [New] State for side drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var bottomBarVisibleBeforeDrawer by remember { mutableStateOf<Boolean?>(null) }
    
    // 抽屉打开时隐藏全局底栏，避免覆盖侧边栏底部内容
    val isDrawerOpenOrOpening = drawerState.currentValue == DrawerValue.Open || drawerState.targetValue == DrawerValue.Open
    LaunchedEffect(isDrawerOpenOrOpening, isGlobalBottomBarVisible, useSideNavigation) {
        if (useSideNavigation) return@LaunchedEffect
        
        if (isDrawerOpenOrOpening) {
            if (bottomBarVisibleBeforeDrawer == null) {
                bottomBarVisibleBeforeDrawer = isGlobalBottomBarVisible
            }
            if (isGlobalBottomBarVisible) {
                setBottomBarVisible(false)
            }
        } else {
            bottomBarVisibleBeforeDrawer?.let { previousVisible ->
                setBottomBarVisible(previousVisible)
            }
            bottomBarVisibleBeforeDrawer = null
        }
    }
    
    // [P2] 优先按当前可见顶栏计算索引，避免自定义排序后高亮错位
    val currentCategoryIndex = topCategories.indexOf(state.currentCategory)
    val displayedTabIndex = if (currentCategoryIndex >= 0) {
        currentCategoryIndex
    } else {
        state.displayedTabIndex.coerceIn(0, (topCategories.size - 1).coerceAtLeast(0))
    }

    val tvSidebarFirstItemFocusRequester = remember { FocusRequester() }
    val tvPagerFocusRequester = remember { FocusRequester() }
    val tvGridEntryFocusRequester = remember { FocusRequester() }
    var tvFocusZone by remember(isTvDevice, useSideNavigation) {
        mutableStateOf(
            resolveInitialHomeTvFocusZone(
                isTv = isTvDevice,
                hasSidebar = useSideNavigation
            ) ?: HomeTvFocusZone.PAGER
        )
    }
    val tvSidebarFirstItemModifier = if (isTvDevice) {
        Modifier
            .focusRequester(tvSidebarFirstItemFocusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    tvFocusZone = HomeTvFocusZone.SIDEBAR
                }
            }
    } else {
        Modifier
    }
    val tvPagerContainerModifier = if (isTvDevice) {
        Modifier
            .focusRequester(tvPagerFocusRequester)
            .focusable(enabled = true)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    tvFocusZone = HomeTvFocusZone.PAGER
                }
            }
    } else {
        Modifier
    }
    val tvGridEntryModifier = if (isTvDevice) {
        Modifier
            .focusRequester(tvGridEntryFocusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    tvFocusZone = HomeTvFocusZone.GRID
                }
            }
            .onPreviewKeyEvent { event ->
                val transition = resolveHomeTvFocusTransition(
                    currentZone = HomeTvFocusZone.GRID,
                    keyCode = event.nativeKeyEvent.keyCode,
                    action = event.nativeKeyEvent.action,
                    hasSidebar = useSideNavigation,
                    isGridFirstRow = true,
                    isGridFirstColumn = true
                )
                if (!transition.consumeEvent) return@onPreviewKeyEvent false
                tvFocusZone = transition.nextZone
                when (transition.nextZone) {
                    HomeTvFocusZone.SIDEBAR -> tvSidebarFirstItemFocusRequester.requestFocus()
                    HomeTvFocusZone.PAGER -> tvPagerFocusRequester.requestFocus()
                    HomeTvFocusZone.GRID -> Unit
                }
                true
            }
    } else {
        Modifier
    }
    LaunchedEffect(isTvDevice, useSideNavigation) {
        if (!isTvDevice) return@LaunchedEffect
        val initialZone = resolveInitialHomeTvFocusZone(
            isTv = true,
            hasSidebar = useSideNavigation
        ) ?: HomeTvFocusZone.PAGER
        tvFocusZone = initialZone
        kotlinx.coroutines.yield()
        when (initialZone) {
            HomeTvFocusZone.SIDEBAR -> tvSidebarFirstItemFocusRequester.requestFocus()
            HomeTvFocusZone.PAGER -> tvPagerFocusRequester.requestFocus()
            HomeTvFocusZone.GRID -> tvGridEntryFocusRequester.requestFocus()
        }
    }

    //  根据滚动距离动态调整 BottomBar 可见性
    //  逻辑优化：使用 nestedScrollConnection 监听滚动
    var isHeaderVisible by rememberSaveable { mutableStateOf(true) }
    
    // Constants
    val topTabStyle = remember(isBottomBarFloating, isBottomBarBlurEnabled, isLiquidGlassEnabled) {
        resolveTopTabStyle(
            isBottomBarFloating = isBottomBarFloating,
            isBottomBarBlurEnabled = isBottomBarBlurEnabled,
            isLiquidGlassEnabled = isLiquidGlassEnabled
        )
    }
    val searchBarHeightDp = 52.dp 
    val tabRowHeightDp = if (topTabStyle.floating) 62.dp else 48.dp
    val headerHeightDp = searchBarHeightDp + tabRowHeightDp // Total height
    
    // Pixels
    val searchBarHeightPx = with(density) { searchBarHeightDp.toPx() }
    val tabRowHeightPx = with(density) { tabRowHeightDp.toPx() }
    val headerHeightPx = with(density) { headerHeightDp.toPx() }
    
    // Thresholds
    val searchCollapseThreshold = searchBarHeightPx // Collapse search bar first
    
    // [Feature] Sticky Header Options
    // If true, header will shrink but stay visible. If false, it scrolls away.
    val isHeaderCollapseEnabled = homeSettings.isHeaderCollapseEnabled // Enable shrinking based on settings
    
    // [Feature] Bottom Bar Auto-Hide (based on scroll hide mode)
    val isBottomBarAutoHideEnabled = bottomBarVisibilityMode == SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE
    val bottomBarVisibleState = LocalSetBottomBarVisible.current
    
    // [Feature] Global Scroll Offset for Liquid Glass
    val globalScrollOffset = LocalHomeScrollOffset.current

    val nestedScrollConnection = remember(isHeaderCollapseEnabled, isBottomBarAutoHideEnabled, useSideNavigation, isLiquidGlassEnabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                
                // Update Global Scroll Offset (accumulate)
                // [优化] 仅当开启流体玻璃特效时才更新全局滚动状态，避免不必要的重组开销
                if (isLiquidGlassEnabled) {
                    globalScrollOffset.value -= delta // Scroll down = positive offset
                }
                
                // Header Collapse Logic
                if (isHeaderCollapseEnabled) {
                    val newOffset = headerOffsetHeightPx + delta
                    // Min height: -searchBarHeightPx (Search bar collapsed) OR 0 (Full visible)?
                    // Actually, we want to collapse SearchBar (-52dp) but keep Tabs?
                    // Or collapse everything?
                    // Let's allow collapsing up to -searchBarHeightPx (hide search, keep tabs)
                    // If we also want to hide tabs, limit = -headerHeightPx
                    val minOffset = -headerHeightPx // Fully scroll away
                    headerOffsetHeightPx = newOffset.coerceIn(minOffset, 0f)
                } else {
                    headerOffsetHeightPx = 0f // Reset to full height if disabled
                }
                
                // Bottom Bar Logic
                if (isBottomBarAutoHideEnabled && !useSideNavigation) {
                    if (delta < -10) { //  向下滑动 (手指向上) -> 隐藏
                         bottomBarVisibleState(false)
                    } else if (delta > 10) { //  向上滑动 (手指向下) -> 显示
                         bottomBarVisibleState(true)
                    }
                }
                
                return Offset.Zero
            }
        }
    }
    var bottomBarRestoreJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var topTabsRevealJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    

    //  包装 onVideoClick：点击视频时先隐藏底栏再导航
    val wrappedOnVideoClick: (String, Long, String) -> Unit = remember(onVideoClick, setBottomBarVisible) {
        { bvid, aid, pic ->
             hideTopTabsForForwardDetailNav = true
             delayTopTabsUntilCardSettled = false
             setBottomBarVisible(false)
             isVideoNavigating = true
             onVideoClick(bvid, aid, pic)
        }
    }
    val onTodayWatchVideoClick: (VideoItem) -> Unit = remember(viewModel, wrappedOnVideoClick) {
        { video ->
            viewModel.markTodayWatchVideoOpened(video)
            wrappedOnVideoClick(video.bvid, video.cid, video.pic)
        }
    }

    // [TodayWatch首曝] 冷启动启动窗口内自动回顶一次，确保用户能看到今日推荐单卡片。
    LaunchedEffect(
        state.todayWatchPluginEnabled,
        state.todayWatchPlan?.generatedAt,
        state.currentCategory
    ) {
        if (todayWatchStartupRevealHandled) return@LaunchedEffect

        val recommendGridState = gridStates[HomeCategory.RECOMMEND] ?: return@LaunchedEffect
        val decision = decideTodayWatchStartupReveal(
            startupElapsedMs = SystemClock.elapsedRealtime() - homeStartupElapsedAt,
            isPluginEnabled = state.todayWatchPluginEnabled,
            currentCategory = state.currentCategory,
            hasTodayPlan = state.todayWatchPlan != null && !state.todayWatchCollapsed,
            firstVisibleItemIndex = recommendGridState.firstVisibleItemIndex,
            firstVisibleItemOffset = recommendGridState.firstVisibleItemScrollOffset
        )

        when (decision) {
            TodayWatchStartupRevealDecision.REVEAL -> {
                headerOffsetHeightPx = 0f
                if (recommendGridState.firstVisibleItemIndex > 12) {
                    recommendGridState.scrollToItem(12)
                }
                recommendGridState.animateScrollToItem(0)
                headerOffsetHeightPx = 0f
                todayWatchStartupRevealHandled = true
            }
            TodayWatchStartupRevealDecision.SKIP -> {
                todayWatchStartupRevealHandled = true
            }
            TodayWatchStartupRevealDecision.WAIT -> Unit
        }
    }

    //  Scaffold 内容封装 (用于 Panel 左右布局复用)
    val scaffoldLayout: @Composable () -> Unit = {
        Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                   // BottomBar logic handled by parent
                },
                contentWindowInsets = WindowInsets(0.dp)
            ) { padding ->
                   // [Refactor] Use Box to allow overlay and proper blur nesting
                   // [新增] Video Preview State (Long Press)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(homeBackdrop)
                            // 首页使用 Pager + Lazy 子层，source 挂在外层容器更稳定。
                            .hazeSource(state = hazeState)
                    ) {
                    // [Fix] Re-enabled default overscroll for better feedback
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 1, // [Optimization] Preload adjacent pages to prevent swipe lag
                            modifier = Modifier
                                .fillMaxSize()
                                .then(tvPagerContainerModifier),
                            key = { index -> resolveHomeTopCategoryKey(topCategories, index) }
                        ) { page ->
                        val category = resolveHomeTopCategoryOrNull(topCategories, page) ?: return@HorizontalPager
                        val categoryState = state.categoryStates[category] ?: com.android.purebilibili.feature.home.CategoryContent()
                        
                        //  独立的 PullToRefreshState，避免所有页面共享一个状态导致冲突
                        val pullRefreshState = rememberPullToRefreshState()

                        //  [新增] 下拉回弹物理动画状态 (Moved from outer scope)
                        val targetPullOffset = if (pullRefreshState.distanceFraction > 0) {
                            val fraction = pullRefreshState.distanceFraction.coerceAtMost(2f)
                            fraction * 0.5f 
                        } else 0f
                        
                        //  使用 animateFloatAsState 包装偏移量
                        val animatedDragOffsetFraction by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = targetPullOffset,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.5f,  // 0.5 = 明显的弹性 (Bouncy)
                                stiffness = 350f      // 350 = 中等刚度
                            ),
                            label = "pull_bounce"
                        )

                        //  Defers calculation to graphicsLayer
                        val calculateDragOffset: androidx.compose.ui.unit.Density.() -> Float = remember(animatedDragOffsetFraction) {
                            {
                                val maxPx = 140.dp.toPx()
                                maxPx * animatedDragOffsetFraction
                            }
                        }
                        
                        //  每个页面独立的 GridState
                        //  使用 saveable 记住滚动位置
                        val pageGridState = gridStates[category] ?: rememberLazyGridState()
                        
                        //  把 GridState 提升给父级用于控制 Header? 
                        
                        PullToRefreshBox(
                            isRefreshing = isRefreshing && state.currentCategory == category,
                            onRefresh = { viewModel.refresh() },
                            state = pullRefreshState,
                            modifier = Modifier.fillMaxSize(),
                             //  iOS 风格下拉刷新指示器 (位于内容上方)
                             indicator = {
                                iOSRefreshIndicator(
                                    state = pullRefreshState,
                                    isRefreshing = isRefreshing,
                                     modifier = Modifier
                                         .align(Alignment.TopCenter)
                                         // [物理优化] 指示器跟随拖拽移动，保持在 Gap 中央
                                         .padding(top = listTopPadding) 
                                         .graphicsLayer {
                                            val currentDragOffset = calculateDragOffset()
                                            // [物理优化] 始终保持在 Header (listTopPadding) 和 内容顶部 (listTopPadding + currentDragOffset) 之间
                                            // 公式： (currentDragOffset / 2) - (indicatorHeight / 2)
                                            // 假设指示器高度约 40dp (icon + text spacing)
                                            val indicatorHeight = 40.dp.toPx()
                                            translationY = (currentDragOffset / 2f) - (indicatorHeight / 2f)
                                         }
                                         .fillMaxWidth()
                                 )
                             }
                        ) {
                             // [物理优化] 内容容器应用下沉效果
                             Box(
                                 modifier = Modifier
                                     .fillMaxSize()
                                     .graphicsLayer {
                                         translationY = calculateDragOffset()
                                     }
                             ) {
                             if (categoryState.isLoading && categoryState.videos.isEmpty() && categoryState.liveRooms.isEmpty()) {
                                 // Loading Skeleton per page
                                 LazyVerticalGrid(
                                     columns = GridCells.Fixed(gridColumns),
                                     contentPadding = PaddingValues(
                                         bottom = homeListBottomPadding,
                                         start = 8.dp, end = 8.dp, top = listTopPadding // [Fix] Apply top padding to skeleton grid too
                                     ),
                                     horizontalArrangement = Arrangement.spacedBy(10.dp),
                                     verticalArrangement = Arrangement.spacedBy(10.dp),
                                     modifier = Modifier.fillMaxSize()
                                 ) {
                                     // [Fix] Dynamic skeleton count to fill tablet screens (at least 5 rows)
                                     val skeletonItemCount = gridColumns * 5
                                     // [Fix] Use modulo to prevent excessive delay for large item counts on tablet
                                     // Cap the animation wave to ~10 items (approx 800ms max delay) to ensure visibility
                                     items(skeletonItemCount) { index -> VideoCardSkeleton(index = index % 10) }
                                 }
                             } else if (categoryState.error != null && categoryState.videos.isEmpty()) {
                                 // Error State per page
                                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                     ModernErrorState(
                                         message = categoryState.error,
                                         onRetry = { viewModel.refresh() }
                                     )
                                 }
                             } else {
                                 // Data Content
                                 // [性能优化] Stabilize event callbacks to prevent recomposition on scroll
                                 val onLoadMoreCallback = remember(viewModel) { { viewModel.loadMore() } }
                                 val onDismissVideoCallback = remember(viewModel) { { bvid: String -> viewModel.startVideoDissolve(bvid) } }
                                 val onWatchLaterCallback = remember(viewModel) { { bvid: String, aid: Long -> viewModel.addToWatchLater(bvid, aid) } }
                                 val onDissolveCompleteCallback = remember(viewModel) { { bvid: String -> viewModel.completeVideoDissolve(bvid) } }
                                 val onLongPressCallback = remember(targetVideoItemState) { { item: VideoItem -> targetVideoItemState.value = item } }
                                 val onLiveClickCallback = remember(onLiveClick) { onLiveClick }
                                 val onTodayWatchModeChange = remember(viewModel) { { mode: TodayWatchMode -> viewModel.switchTodayWatchMode(mode) } }
                                 val onTodayWatchCollapsedChange = remember(viewModel) { { collapsed: Boolean -> viewModel.setTodayWatchCollapsed(collapsed) } }
                                 val onTodayWatchRefresh = remember(viewModel) { { viewModel.refreshTodayWatchOnly() } }

                                 HomeCategoryPageContent(
                                     category = category,
                                     categoryState = categoryState,
                                     gridState = pageGridState,
                                     gridColumns = gridColumns,
                                     contentPadding = PaddingValues(
                                         bottom = homeListBottomPadding,
                                         start = 8.dp, end = 8.dp, top = listTopPadding 
                                     ),
                                     dissolvingVideos = state.dissolvingVideos,
                                     followingMids = state.followingMids,
                                     onVideoClick = wrappedOnVideoClick,
                                     onLiveClick = onLiveClickCallback,
                                     onLoadMore = onLoadMoreCallback,
                                     onDismissVideo = onDismissVideoCallback,
                                     onWatchLater = onWatchLaterCallback,
                                     onDissolveComplete = onDissolveCompleteCallback,
                                     longPressCallback = onLongPressCallback, // [Feature] Pass callback
                                     displayMode = displayMode,
                                     cardAnimationEnabled = cardAnimationEnabled,
                                     cardMotionTier = cardMotionTier,
                                     cardTransitionEnabled = cardTransitionEnabled,
                                     isDataSaverActive = isDataSaverActive,
                                     oldContentAnchorBvid = if (category == HomeCategory.RECOMMEND &&
                                         dividerRevealRefreshKey == state.refreshNewItemsKey
                                     ) {
                                         state.recommendOldContentAnchorBvid
                                     } else {
                                         null
                                     },
                                     oldContentStartIndex = if (category == HomeCategory.RECOMMEND) {
                                         if (dividerRevealRefreshKey == state.refreshNewItemsKey) {
                                             state.recommendOldContentStartIndex
                                         } else {
                                             null
                                         }
                                     } else {
                                         null
                                     },
                                     todayWatchEnabled = category == HomeCategory.RECOMMEND && state.todayWatchPluginEnabled,
                                     todayWatchMode = state.todayWatchMode,
                                     todayWatchPlan = if (category == HomeCategory.RECOMMEND) state.todayWatchPlan else null,
                                     todayWatchLoading = category == HomeCategory.RECOMMEND && state.todayWatchLoading,
                                     todayWatchError = if (category == HomeCategory.RECOMMEND) state.todayWatchError else null,
                                     todayWatchCollapsed = category == HomeCategory.RECOMMEND && state.todayWatchCollapsed,
                                     todayWatchCardConfig = state.todayWatchCardConfig,
                                     onTodayWatchModeChange = onTodayWatchModeChange,
                                     onTodayWatchCollapsedChange = onTodayWatchCollapsedChange,
                                     onTodayWatchRefresh = onTodayWatchRefresh,
                                     onTodayWatchVideoClick = onTodayWatchVideoClick,
                                     firstGridItemModifier = tvGridEntryModifier,
                                     isTv = isTvDevice,
                                     hasSidebar = useSideNavigation,
                                     onTvGridBoundaryTransition = { nextZone ->
                                         tvFocusZone = nextZone
                                         when (nextZone) {
                                             HomeTvFocusZone.SIDEBAR -> tvSidebarFirstItemFocusRequester.requestFocus()
                                             HomeTvFocusZone.PAGER -> tvPagerFocusRequester.requestFocus()
                                             HomeTvFocusZone.GRID -> Unit
                                         }
                                     }
                                 )
                             }
                             } // Close Box wrapper
                        }
                } // Close HorizontalPager lambda
            } // Close Box wrapper
        } // Close Scaffold lambda
        
        //  ===== Header Overlay (毛玻璃效果) =====
        //  Header 现在在外层 Box 内、hazeSource 外部，可以正确模糊内层内容
        val isSkeletonState = state.isLoading && state.videos.isEmpty() && state.liveRooms.isEmpty()
        val isErrorState = state.error != null && 
            ((state.currentCategory == HomeCategory.LIVE && state.liveRooms.isEmpty()) ||
             (state.currentCategory != HomeCategory.LIVE && state.videos.isEmpty()))

        //  [Restored] Header 始终显示，不再随 Loading/Error 状态隐藏
        //  这保证了 Tab 指示器状态的连续性，防止消失或重置
        
        // Calculate parameters based on scroll
        // 1. Search Bar Collapse (First phase)
        iOSHomeHeader(
            headerOffsetProvider = { headerOffsetHeightPx }, // [Optimization] Pass lambda to defer state read
            isHeaderCollapseEnabled = isHeaderCollapseEnabled,
            user = state.user,
            onAvatarClick = { 
                if (state.user.isLogin && isHomeDrawerEnabled) {
                    coroutineScope.launch { drawerState.open() }
                } else {
                    onAvatarClick() 
                }
            },
            onSettingsClick = onSettingsClick,
            onSearchClick = onSearchClick,
            topCategories = topCategories.map { it.label },
            categoryIndex = displayedTabIndex,
            onCategorySelected = { index ->
                viewModel.updateDisplayedTabIndex(index)
                topCategories.getOrNull(index)?.let { selectedCategory ->
                    viewModel.switchCategory(selectedCategory)
                }
            },
            onPartitionClick = onPartitionClick,
            onLiveClick = onLiveListClick,  // [修复] 直播分区点击导航到独立页面
            // isScrollingUp = isHeaderVisible, // [Removed] logic moved to offset
            hazeState = if (isHeaderBlurEnabled) hazeState else null,
            onStatusBarDoubleTap = {
                coroutineScope.launch {
                    gridStates[state.currentCategory]?.animateScrollToItem(0)
                    headerOffsetHeightPx = 0f // [Refinement] Reset header on double tap
                }
            },
            isRefreshing = isRefreshing,
            pullProgress = 0f, // [Fix] Outer header doesn't track inner pull state
            pagerState = pagerState,
            backdrop = homeBackdrop,
            homeSettings = homeSettings,
            topTabsVisible = resolveHomeTopTabsVisible(
                isDelayedForCardSettle = delayTopTabsUntilCardSettled,
                isForwardNavigatingToDetail = hideTopTabsForForwardDetailNav
            )
        )

        AnimatedVisibility(
            visible = refreshDeltaTipText != null,
            enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
                animationSpec = tween(220),
                initialOffsetY = { -it / 2 }
            ),
            exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(
                animationSpec = tween(220),
                targetOffsetY = { -it / 2 }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = listTopPadding + 8.dp)
                .zIndex(90f)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    tonalElevation = 2.dp,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = refreshDeltaTipText.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
        // [Feature] Video Preview Overlay with Animation
        androidx.compose.animation.AnimatedVisibility(
            visible = targetVideoItemState.value != null,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
            modifier = Modifier.fillMaxSize().zIndex(100f) // Ensure on top
        ) {
            val item = targetVideoItemState.value
            if (item != null) {
                com.android.purebilibili.feature.home.components.VideoPreviewDialog(
                    video = item,
                    onDismiss = { targetVideoItemState.value = null },
                    onPlay = {
                     // 1. Log click
                     wrappedOnVideoClick(item.bvid, item.id, item.pic)
                     // 2. Clear preview state
                     targetVideoItemState.value = null
                },
                onWatchLater = {
                    viewModel.addToWatchLater(item.bvid, item.aid)
                    targetVideoItemState.value = null
                },
                onShare = {
                   val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "【${item.title}】 https://www.bilibili.com/video/${item.bvid}")
                    }
                    val chooser = android.content.Intent.createChooser(shareIntent, "分享视频")
                    if (context !is android.app.Activity) {
                        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    targetVideoItemState.value = null
                },
                onNotInterested = {
                    viewModel.markNotInterested(item.bvid)
                    targetVideoItemState.value = null
                },
                onGetPreviewUrl = { bvid, cid ->
                    viewModel.getPreviewVideoUrl(bvid, cid)
                },
                hazeState = hazeState
            )
            }
        }
    }

    val scaffoldContent: @Composable () -> Unit = {
        if (isHomeDrawerEnabled) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                scrimColor = Color.Black.copy(alpha = resolveHomeDrawerScrimAlpha(isHeaderBlurEnabled)),
                drawerContent = {
                    MineSideDrawer(
                        drawerState = drawerState,
                        user = state.user,
                        onLogout = { /* 登出后由 ProfileScreen 处理 */ },
                        onHistoryClick = onHistoryClick,
                        onFavoriteClick = onFavoriteClick,
                        onBangumiClick = { onBangumiClick(1) },
                        onDownloadClick = onDownloadClick,
                        onWatchLaterClick = onWatchLaterClick,
                        onInboxClick = onInboxClick,
                        onSettingsClick = onSettingsClick,
                        onProfileClick = onProfileClick,
                        hazeState = hazeState,
                        isBlurEnabled = isHeaderBlurEnabled
                    )
                }
            ) {
                scaffoldLayout()
            }
        } else {
            scaffoldLayout()
        }
    }

    
    //  [修复] 使用生命周期事件控制底栏可见性
    // ON_START: 恢复底栏（仅在从视频页返回时）
    // ON_STOP: 隐藏底栏（导航到其他页面时，避免影响导航栏区域）
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, useSideNavigation) {
        if (useSideNavigation) {
            return@DisposableEffect onDispose { }
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    topTabsRevealJob?.cancel()
                    val returningFromDetail = CardPositionManager.isReturningFromDetail
                    if (hideTopTabsForForwardDetailNav || returningFromDetail) {
                        hideTopTabsForForwardDetailNav = false
                        val revealDelayMs = resolveHomeTopTabsRevealDelayMs(
                            isReturningFromDetail = returningFromDetail,
                            cardTransitionEnabled = cardTransitionEnabled
                        )
                        if (revealDelayMs > 0L) {
                            delayTopTabsUntilCardSettled = true
                            topTabsRevealJob = coroutineScope.launch {
                                delay(revealDelayMs)
                                delayTopTabsUntilCardSettled = false
                            }
                        } else {
                            delayTopTabsUntilCardSettled = false
                        }
                    }
                    //  关键修复：只在底栏当前隐藏时才恢复可见
                    if (!bottomBarVisible && isVideoNavigating) {
                        //  [同步动画] 延迟后再显示底栏，让进入动画与卡片返回动画同步
                        //  [优化] 将延迟增加到 360ms (略大于转场动画 350ms)，防止在动画过程中修改 Padding 导致列表重排卡顿
                        bottomBarRestoreJob = kotlinx.coroutines.MainScope().launch {
                            kotlinx.coroutines.delay(360)  // 等待返回动画结束
                            setBottomBarVisible(true)
                            // 延迟重置导航状态，确保进入动画完成
                            kotlinx.coroutines.delay(200)
                            isVideoNavigating = false
                        }
                    } else if (!bottomBarVisible && !isVideoNavigating) {
                        //  [新增] 从设置等非视频页面返回时，立即显示底栏（无延迟）
                        setBottomBarVisible(true)
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    //  [修复] 移除此处隐藏底栏的逻辑
                    //  防止切换到其他Tab（如动态/历史）时底栏消失
                    bottomBarRestoreJob?.cancel()
                    bottomBarRestoreJob = null
                    // setBottomBarVisible(false) // REMOVED
                }
                else -> { /* 其他事件不处理 */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            bottomBarRestoreJob?.cancel()
            topTabsRevealJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    //  [修复] 使用 rememberSaveable 记住本次会话中是否已处理过弹窗（防止导航后重新显示）
    var consentDialogHandled by rememberSaveable { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    
    //  检查欢迎弹窗是否已显示过（确保弹窗顺序显示，不会同时出现）
    val welcomePrefs = remember { context.getSharedPreferences("app_welcome", Context.MODE_PRIVATE) }
    val welcomeAlreadyShown = welcomePrefs.getBoolean("first_launch_shown", false)
    
    // 检查是否需要显示弹窗（欢迎弹窗已显示过 且 同意弹窗尚未显示过 且 本次会话未处理过）
    LaunchedEffect(crashTrackingConsentShown) {
        if (welcomeAlreadyShown && !crashTrackingConsentShown && !consentDialogHandled) {
            showConsentDialog = true
        }
    }
    
    // 显示弹窗
    if (showConsentDialog) {
        com.android.purebilibili.feature.home.components.CrashTrackingConsentDialog(
            onDismiss = { 
                showConsentDialog = false
                consentDialogHandled = true  // 标记为已处理
            }
        )
    }
    
    //  计算滚动偏移量用于头部动画 -  优化：量化减少重组
    //  计算滚动偏移量用于头部动画 -  优化：量化减少重组
    val scrollOffset by remember {
        derivedStateOf {
            val currentGridState = gridStates[state.currentCategory]
            if (currentGridState == null) return@derivedStateOf 0f
            
            val firstVisibleItem = currentGridState.firstVisibleItemIndex
            if (firstVisibleItem == 0) {
                //  直接使用原始偏移量，避免量化导致的跳变
                currentGridState.firstVisibleItemScrollOffset.toFloat()
            } else 1000f
        }
    }
    
    //  滚动方向（简化版 - 不再需要复杂检测，因为标签页只在顶部显示）
    val isScrollingUp = true  // 保留参数兼容性

    //  [性能优化] 图片预加载 - 提前加载即将显示的视频封面
    // 📉 [省流量] 省流量模式下禁用预加载
    LaunchedEffect(state.currentCategory, isDataSaverActive, preloadAheadCount) {
        // 📉 省流量模式下跳过预加载
        if (isDataSaverActive) return@LaunchedEffect
        if (preloadAheadCount <= 0) return@LaunchedEffect
        
        val currentGridState = gridStates[state.currentCategory] ?: return@LaunchedEffect
        
        snapshotFlow { currentGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()  //  只在索引变化时触发
            .collect { lastVisibleIndex ->
                // Move heavy lifting to IO thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val videos = state.categoryStates[state.currentCategory]?.videos ?: state.videos
                    val preloadStart = (lastVisibleIndex + 1).coerceAtMost(videos.size)
                    val preloadEnd = (lastVisibleIndex + 1 + preloadAheadCount).coerceAtMost(videos.size)
                    
                    if (preloadStart < preloadEnd) {
                        for (i in preloadStart until preloadEnd) {
                            val imageUrl = videos.getOrNull(i)?.pic ?: continue
                            // [Optimization] Run validation and request building off main thread
                            val fixedUrl = com.android.purebilibili.core.util.FormatUtils.fixImageUrl(imageUrl)
                            
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(fixedUrl)
                                .size(360, 225)  //  预加载也使用限制尺寸
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build()
                            context.imageLoader.enqueue(request)
                        }
                    }
                }
            }
    }


    //  PullToRefreshBox 自动处理下拉刷新逻辑
    
    //  [已移除] 特殊分类（ANIME, MOVIE等）不再在首页切换，直接导航到独立页面
    
    //  [修复] 如果当前在直播-关注分类且列表为空，返回时先切换到热门，再切换到推荐
    val isEmptyLiveFollowed = state.currentCategory == HomeCategory.LIVE && 
                               state.liveSubCategory == LiveSubCategory.FOLLOWED &&
                               state.liveRooms.isEmpty() && 
                               !state.isLoading
    androidx.activity.compose.BackHandler(enabled = isEmptyLiveFollowed) {
        // 切换到热门直播
        viewModel.switchLiveSubCategory(LiveSubCategory.POPULAR)
    }

    //  [修复] 如果当前在直播分类（非关注空列表情况），返回时切换到推荐
    val isLiveCategoryNotHome = state.currentCategory == HomeCategory.LIVE && !isEmptyLiveFollowed
    androidx.activity.compose.BackHandler(enabled = isLiveCategoryNotHome) {
        viewModel.switchCategory(HomeCategory.RECOMMEND)
    }
    
// [Removed] Animation logic moved inside HorizontalPager where the active state exists
    
    // 指示器位置逻辑也移入 graphicsLayer
    
    // 📱 [平板适配] 导航模式切换动画
    // 始终使用 Row 布局，通过动画控制侧边栏的显示/隐藏
    Row(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (!isTvDevice) return@onPreviewKeyEvent false
                val command = resolveHomeTvRootNavigationCommand(
                    currentZone = tvFocusZone,
                    keyCode = event.nativeKeyEvent.keyCode,
                    action = event.nativeKeyEvent.action,
                    hasSidebar = useSideNavigation,
                    currentPage = pagerState.currentPage,
                    pageCount = topCategories.size
                ) ?: return@onPreviewKeyEvent false

                tvFocusZone = command.nextZone
                command.targetPage?.let { targetPage ->
                    if (targetPage != pagerState.currentPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                }
                when (command.nextZone) {
                    HomeTvFocusZone.SIDEBAR -> tvSidebarFirstItemFocusRequester.requestFocus()
                    HomeTvFocusZone.PAGER -> tvPagerFocusRequester.requestFocus()
                    HomeTvFocusZone.GRID -> tvGridEntryFocusRequester.requestFocus()
                }
                true
            }
    ) {
        AnimatedVisibility(
            visible = useSideNavigation,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = LinearOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(250, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            FrostedSideBar(
                currentItem = currentNavItem,
                onItemClick = handleNavItemClick,
                firstItemModifier = tvSidebarFirstItemModifier,
                onHomeDoubleTap = {
                    coroutineScope.launch {
                        headerOffsetHeightPx = 0f
                        gridStates[state.currentCategory]?.animateScrollToItem(0)
                        headerOffsetHeightPx = 0f
                    }
                },
                hazeState = if (isBottomBarBlurEnabled) hazeState else null,
                visibleItems = visibleBottomBarItems,
                itemColorIndices = bottomBarItemColors,
                onToggleSidebar = onToggleNavigationMode
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            scaffoldContent()
        }
    }
}
