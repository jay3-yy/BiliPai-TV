// 文件路径: navigation/AppNavigation.kt
package com.android.purebilibili.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState //  新增
import androidx.compose.runtime.getValue //  新增
import androidx.compose.runtime.LaunchedEffect // 新增
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.TransformOrigin
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.purebilibili.feature.home.HomeScreen
import com.android.purebilibili.feature.home.HomeViewModel
import com.android.purebilibili.feature.login.LoginScreen
import com.android.purebilibili.feature.profile.ProfileScreen
import com.android.purebilibili.feature.search.SearchScreen
import com.android.purebilibili.feature.settings.SettingsScreen
import com.android.purebilibili.feature.settings.AppearanceSettingsScreen
import com.android.purebilibili.feature.settings.PlaybackSettingsScreen
import com.android.purebilibili.feature.settings.OFFICIAL_GITHUB_URL
import com.android.purebilibili.feature.settings.OFFICIAL_TELEGRAM_URL
import com.android.purebilibili.feature.settings.RELEASE_DISCLAIMER_ACK_KEY
import com.android.purebilibili.feature.settings.ReleaseChannelDisclaimerDialog
import com.android.purebilibili.feature.list.CommonListScreen
import com.android.purebilibili.feature.list.HistoryViewModel
import com.android.purebilibili.feature.list.FavoriteViewModel
import com.android.purebilibili.feature.list.resolveHistoryPlaybackCid
import com.android.purebilibili.feature.video.screen.VideoDetailScreen
import com.android.purebilibili.feature.video.player.MiniPlayerManager
import com.android.purebilibili.feature.dynamic.DynamicScreen
import com.android.purebilibili.feature.dynamic.LocalDynamicScrollChannel
import com.android.purebilibili.feature.dynamic.components.ImagePreviewOverlayHost
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.core.ui.ProvideAnimatedVisibilityScope
import com.android.purebilibili.core.ui.SharedTransitionProvider
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.data.model.response.BgmInfo

import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import dev.chrisbanes.haze.hazeSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
// [LayerBackdrop] AndroidLiquidGlass for real background refraction
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.android.purebilibili.core.ui.LocalSetBottomBarVisible
import com.android.purebilibili.core.ui.LocalBottomBarVisible
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.shouldUseSidebarNavigationForLayout
// import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass (Removed)
// import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass (Removed)
// import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass (Removed)
// import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi (Removed)
import com.android.purebilibili.feature.home.components.FrostedBottomBar
import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.core.store.SettingsManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier // 确保 Modifier 被导入
import androidx.compose.foundation.layout.Box // 确保 Box 被导入
import androidx.compose.foundation.layout.fillMaxSize // 确保 fillMaxSize 被导入
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// 定义路由参数结构
object VideoRoute {
    const val base = "video"
    const val route = "$base/{bvid}?cid={cid}&cover={cover}"

    // 构建 helper
    fun createRoute(bvid: String, cid: Long, coverUrl: String): String {
        val encodedCover = Uri.encode(coverUrl)
        return "$base/$bvid?cid=$cid&cover=$encodedCover"
    }
}

@androidx.media3.common.util.UnstableApi
// @OptIn(ExperimentalMaterial3WindowSizeClassApi::class) (Removed)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    //  小窗管理器
    miniPlayerManager: MiniPlayerManager? = null,
    //  PiP 支持参数
    //  PiP 支持参数
    isInPipMode: Boolean = false,
    onVideoDetailEnter: () -> Unit = {},
    onVideoDetailExit: () -> Unit = {},
    mainHazeState: dev.chrisbanes.haze.HazeState? = null //  全局 Haze 状态
) {
    val homeViewModel: HomeViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    
    //  读取卡片过渡动画设置（在 Composable 作用域内）
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = LocalUriHandler.current
    val cardTransitionEnabled by com.android.purebilibili.core.store.SettingsManager
        .getCardTransitionEnabled(context).collectAsState(initial = true)

    // 🔒 [防抖] 全局导航防抖机制 - 防止快速点击导致页面重复加载
    val lastNavigationTime = androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val canNavigate: () -> Boolean = {
        val currentTime = System.currentTimeMillis()
        val canNav = currentTime - lastNavigationTime.longValue > 300 // 300ms 防抖
        if (canNav) lastNavigationTime.longValue = currentTime
        canNav
    }

    // 统一跳转逻辑
    fun navigateToVideo(bvid: String, cid: Long = 0L, coverUrl: String = "") {
        // 🔒 防抖检查
        if (!canNavigate()) return
        
        //  [修复] 设置导航标志，抑制小窗显示
        miniPlayerManager?.isNavigatingToVideo = true
        //  如果有小窗在播放，先退出小窗模式
        //  [修复] 点击新视频时，立即关闭小窗不播放退出动画，避免闪烁
        miniPlayerManager?.exitMiniMode(animate = false)
        navController.navigate(VideoRoute.createRoute(bvid, cid, coverUrl))
    }

    //  [修复] 通用单例跳转（防止重复打开相同页面）
    fun navigateTo(route: String) {
        if (!canNavigate()) return
        // 如果当前已经在目标页面，则不进行跳转
        if (navController.currentDestination?.route == route) return

        navController.navigate(route) {
            // [修复] 弹出到图表的起始目标，以避免在用户选择项目时
            // 在返回堆栈上堆积大量的目标
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            // 避免在重新选择同一项目时出现同一目标的多个副本
            launchSingleTop = true
            // 重新选择以前选择的项目时恢复状态
            restoreState = true
        }
    }

    // 动画时长
    val animDuration = 350

    // 🚀 [新手引导] 检查是否首次启动
    // 如果是首次启动，则进入 OnboardingScreen，否则进入 HomeScreen
    val welcomePrefs = androidx.compose.runtime.remember { context.getSharedPreferences("app_welcome", android.content.Context.MODE_PRIVATE) }
    // 注意：这里仅读取初始状态用于设置 startDestination
    // 后续状态更新由 OnboardingScreen 完成
    val firstLaunchShown = welcomePrefs.getBoolean("first_launch_shown", false)
    val launchDisclaimerAck = welcomePrefs.getBoolean(RELEASE_DISCLAIMER_ACK_KEY, false)
    var showLaunchDisclaimer by remember {
        mutableStateOf(!firstLaunchShown && !launchDisclaimerAck)
    }
    val startDestination = if (firstLaunchShown) ScreenRoutes.Home.route else ScreenRoutes.Onboarding.route

    SharedTransitionProvider {
        // [新增] 全局底栏状态管理
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val currentBottomNavItem = BottomNavItem.entries.find { it.route == currentRoute } ?: BottomNavItem.HOME

        // 设置状态
        val bottomBarVisibilityMode by SettingsManager.getBottomBarVisibilityMode(context).collectAsState(initial = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE)
        val isBottomBarBlurEnabled by SettingsManager.getBottomBarBlurEnabled(context).collectAsState(initial = true)
        val bottomBarLabelMode by SettingsManager.getBottomBarLabelMode(context).collectAsState(initial = SettingsManager.BottomBarLabelMode.SELECTED)
        val isBottomBarFloating by SettingsManager.getBottomBarFloating(context).collectAsState(initial = true)
        
        // [修复] 使用有序的可见项目列表
        val orderedVisibleTabIds by SettingsManager.getOrderedVisibleTabs(context).collectAsState(initial = listOf("HOME", "DYNAMIC", "HISTORY", "PROFILE"))
        val visibleBottomBarItems = remember(orderedVisibleTabIds) {
            orderedVisibleTabIds.mapNotNull { id -> 
                BottomNavItem.entries.find { it.name == id }
            }
        }
        val visibleBottomBarRoutes = remember(visibleBottomBarItems) {
            visibleBottomBarItems.map { it.route }.toSet()
        }
        
        val bottomBarItemColors by SettingsManager.getBottomBarItemColors(context).collectAsState(initial = emptyMap<String, Int>())
        
        // [新增] 获取首页设置 (包含 Liquid Glass 等视觉配置)
        val homeSettings by SettingsManager.getHomeSettings(context).collectAsState(initial = com.android.purebilibili.core.store.HomeSettings())

        // 平板侧边栏模式 (替代 WindowSizeClass)
        val windowSizeClass = LocalWindowSizeClass.current
        
        // [修复] 平板模式下，仅当用户开启侧边栏设置时才使用侧边导航
        val tabletUseSidebar by SettingsManager.getTabletUseSidebar(context).collectAsState(initial = false)
        
        // 统一侧边栏判定策略：600dp+ 且用户开启侧边栏
        val useSideNavigation = shouldUseSidebarNavigationForLayout(windowSizeClass, tabletUseSidebar)

        // [修复] 平板模式下(宽度>=600dp)，进入设置页(Settings.route)时隐藏底栏
        // 因为平板设置页使用 SplitLayout，已经有自己的内部导航结构，不需要底栏
        val isTabletLayout = windowSizeClass.isTablet
        val isSettingsScreen = currentRoute == ScreenRoutes.Settings.route
        val shouldHideBottomBarOnTablet = isTabletLayout && isSettingsScreen

        // [UX] 底栏仅在“用户配置为可见的一级入口”显示；Story 始终沉浸式隐藏。
        val isBottomBarDestination = currentRoute != ScreenRoutes.Story.route && currentRoute in visibleBottomBarRoutes
        val showBottomBar = isBottomBarDestination &&
            !useSideNavigation &&
            !shouldHideBottomBarOnTablet
        
        // 核心可见性逻辑：
        // 1. 永久隐藏模式 -> 始终隐藏
        // 2. 始终显示模式 -> 始终显示
        // 3. 上滑隐藏模式 -> 由子页面通过 LocalSetBottomBarVisible 控制，初始为 true
        var isBottomBarVisible by remember { mutableStateOf(true) }
        
        // 根据模式强制重置状态（防止模式切换后状态卡死）
        LaunchedEffect(bottomBarVisibilityMode) {
            isBottomBarVisible = true
        }

        // [New Fix] 切换到可显示底栏的主入口页面时，强制恢复底栏可见性
        LaunchedEffect(currentRoute) {
            if (isBottomBarDestination) {
                isBottomBarVisible = true
            }
        }
        
        // 最终决定是否显示：
        // - 必须是用户配置的可见主入口页面
        // - 不是侧边栏模式
        // - 不是故事模式
        // - 且 (模式为始终显示 OR (模式为上滑隐藏 AND 当前状态为可见))
        // - 且 模式不是永久隐藏
        val finalBottomBarVisible = showBottomBar && 
            bottomBarVisibilityMode != SettingsManager.BottomBarVisibilityMode.ALWAYS_HIDDEN &&
            (bottomBarVisibilityMode == SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE || isBottomBarVisible)

        val setBottomBarVisible: (Boolean) -> Unit = remember { { visible -> isBottomBarVisible = visible } }

        // [新增] 首页回顶事件通道 (Channel based event bus)
        val homeScrollChannel = remember { kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED) }
        val dynamicScrollChannel = remember { kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED) }
        // [New] Global Scroll Offset State
        val scrollOffsetState = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

        // [LayerBackdrop] Create backdrop for bottom bar refraction effect
        // This captures the NavHost content and allows the bottom bar to refract it
        val bottomBarBackdrop = rememberLayerBackdrop()

        CompositionLocalProvider(
            LocalSetBottomBarVisible provides setBottomBarVisible,
            LocalBottomBarVisible provides finalBottomBarVisible,
            com.android.purebilibili.feature.home.LocalHomeScrollChannel provides homeScrollChannel,
            LocalDynamicScrollChannel provides dynamicScrollChannel,
            com.android.purebilibili.feature.home.LocalHomeScrollOffset provides scrollOffsetState  // [新增] 提供回顶通道
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ===== 内容层 (hazeSource) =====
            // 这个 Box 包裹所有 NavHost 内容，作为底栏模糊的源
            // [LayerBackdrop] Apply layerBackdrop to capture content for bottom bar refraction
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(bottomBarBackdrop)
                    // [Fix] 将内容标记为全局底栏模糊的源
                    // 必须添加 hazeSource，否则底栏的 hazeEffect 无法获取背景内容，导致模糊失效
                    .then(if (mainHazeState != null) Modifier.hazeSource(mainHazeState) else Modifier)
            ) {
                NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        // --- 0. [新增] 新手引导页 ---
        composable(
            route = ScreenRoutes.Onboarding.route,
            exitTransition = { fadeOut(animationSpec = tween(400)) },
            popEnterTransition = { fadeIn(animationSpec = tween(400)) }
        ) {
            com.android.purebilibili.feature.onboarding.OnboardingScreen(
                onFinish = {
                    //  用户完成引导，写入标记
                    welcomePrefs.edit().putBoolean("first_launch_shown", true).apply()
                    //  跳转到首页，并清除引导页栈
                    navController.navigate(ScreenRoutes.Home.route) {
                         popUpTo(ScreenRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        // --- 1. 首页 ---
        composable(
            route = ScreenRoutes.Home.route,
            //  进入视频详情页时的退出动画
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            //  [修复] 从设置页返回时使用右滑动画
            popEnterTransition = { 
                val fromSettings = initialState.destination.route == ScreenRoutes.Settings.route
                if (fromSettings) {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration))
                } else {
                    fadeIn(animationSpec = tween(250))
                }
            }
        ) {
            //  提供 AnimatedVisibilityScope 给 HomeScreen 以支持共享元素过渡i l
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onVideoClick = { bvid, cid, cover -> navigateToVideo(bvid, cid, cover) },
                    onSearchClick = { navigateTo(ScreenRoutes.Search.route) },
                    onAvatarClick = { navigateTo(ScreenRoutes.Login.route) },
                    onProfileClick = { navigateTo(ScreenRoutes.Profile.route) },
                    onSettingsClick = { navigateTo(ScreenRoutes.Settings.route) },
                    // 🔒 [防抖 + SingleTop] 底栏导航优化
                    onDynamicClick = { navigateTo(ScreenRoutes.Dynamic.route) },
                    onHistoryClick = { navigateTo(ScreenRoutes.History.route) },
                    onPartitionClick = { navigateTo(ScreenRoutes.Partition.route) },  //  分区点击
                    onLiveClick = { roomId, title, uname ->
                        if (canNavigate()) navController.navigate(ScreenRoutes.Live.createRoute(roomId, title, uname))
                    },
                    //  [修复] 番剧点击导航，接受类型参数
                    onBangumiClick = { initialType ->
                        if (canNavigate()) navController.navigate(ScreenRoutes.Bangumi.createRoute(initialType))
                    },
                    //  分类点击：跳转到分类详情页面
                    onCategoryClick = { tid, name ->
                        if (canNavigate()) navController.navigate(ScreenRoutes.Category.createRoute(tid, name))
                    },
                    //  [新增] 底栏扩展项目导航
                    onFavoriteClick = { navigateTo(ScreenRoutes.Favorite.route) },
                    onLiveListClick = { navigateTo(ScreenRoutes.LiveList.route) },
                    onWatchLaterClick = { navigateTo(ScreenRoutes.WatchLater.route) },
                    onDownloadClick = { navigateTo(ScreenRoutes.DownloadList.route) },
                    onInboxClick = { navigateTo(ScreenRoutes.Inbox.route) },
                    onStoryClick = { navigateTo(ScreenRoutes.Story.route) },  //  [新增] 竖屏短视频
                    globalHazeState = mainHazeState  // [新增] 全局底栏模糊状态
                )
            }
        }

        // --- 2. 视频详情页 ---
        composable(
            route = VideoRoute.route,
            arguments = listOf(
                navArgument("bvid") { type = NavType.StringType },
                navArgument("cid") { type = NavType.LongType; defaultValue = 0L },
                navArgument("cover") { type = NavType.StringType; defaultValue = "" },
                navArgument("fullscreen") { type = NavType.BoolType; defaultValue = false }
            ),
            //  进入动画：当卡片过渡开启时用淡入（配合共享元素），关闭时用滑入
            //  进入动画：基于位置的扩散展开 (Scale + Fade)
            //  进入动画：基于位置的扩散展开 (Scale + Fade)
            enterTransition = { 
                // [Hero Animation] 如果启用了卡片过渡，使用简单的淡入，让 SharedElement 成为主角
                if (cardTransitionEnabled) {
                    fadeIn(animationSpec = tween(300))
                } else {
                    // 未启用卡片过渡时，使用常规的推入动画
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                }
            },
            //  返回动画：当卡片过渡开启时用淡出（配合共享元素），关闭时用滑出
            popExitTransition = { 
                if (cardTransitionEnabled) {
                    // 🔧 [修复] 使用简单淡出，避免与 sharedBounds 共享元素动画冲突
                    fadeOut(animationSpec = tween(250))
                } else {
                    //  位置感知滑出动画
                    if (CardPositionManager.isSingleColumnCard) {
                        //  单列卡片（故事卡片）：往下滑出
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration))
                    } else {
                        //  双列卡片：返回到原来卡片的方向
                        val isCardOnLeft = (CardPositionManager.lastClickedCardCenter?.x ?: 0.5f) < 0.5f
                        if (isCardOnLeft) {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration))
                        } else {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration))
                        }
                    }
                }
            },
            // [新增] 前进退出动画 (A -> B, A is exiting)
            exitTransition = {
                if (cardTransitionEnabled) {
                     fadeOut(animationSpec = tween(300))
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration))
                }
            },
            // [新增] 返回进入动画 (B -> A, A is re-entering)
            popEnterTransition = {
                if (cardTransitionEnabled) {
                     fadeIn(animationSpec = tween(300))
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) // Reverse of slideOutLeft? Or usually Right?
                    // Standard back nav usually slides from left to right (content entering from left) if we pushed from right.
                    // But here we slid OUT to Left. So we slide IN from Left?
                    // Actually standard Android is: Push: Enter Right, Exit Left. Pop: Enter Left, Exit Right.
                    // So popEnter should be SlideDirection.Right (content moving towards Right? No, coming FROM Left).
                    // SlideDirection.Right means "towards right".
                    // slideIntoContainer(Right) -> moves from Left edge towards Right. Correct.
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration))
                }
            }
        ) { backStackEntry ->
            val bvid = backStackEntry.arguments?.getString("bvid") ?: ""
            val coverUrl = android.net.Uri.decode(backStackEntry.arguments?.getString("cover") ?: "")
            val startFullscreen = backStackEntry.arguments?.getBoolean("fullscreen") ?: false
            
            //  使用顶层定义的 cardTransitionEnabled（已在 line 68 定义）

            //  进入视频详情页时通知 MainActivity
            //  [修复] 使用 Activity 引用检测配置变化（如旋转）
            val activity = context as? android.app.Activity
            
            //  [修复] 追踪是否导航到音频模式
            var isNavigatingToAudioMode by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                //  [修复] 重置导航标志，允许小窗在返回时显示
                miniPlayerManager?.isNavigatingToVideo = false
                // 🎯 [新增] 重置导航离开标志（进入视频页时）
                miniPlayerManager?.resetNavigationFlag()
                onVideoDetailEnter()
                onDispose {
                    // [关键修复] 从视频A切到视频B时，旧页面 onDispose 会晚于新页面 onEnter。
                    // 若此时仍在 video 路由，不能触发「退出视频页」状态，否则会导致 Home 后误暂停。
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    val stillInVideoRoute = currentRoute?.substringBefore("/") == VideoRoute.base

                    if (!stillInVideoRoute) {
                        onVideoDetailExit()
                    } else {
                        com.android.purebilibili.core.util.Logger.d(
                            "AppNavigation",
                            "Skip onVideoDetailExit because destination is still video route: $currentRoute"
                        )
                    }

                    //  [修复] 只有在真正退出页面时才进入小窗模式
                    // 配置变化（如旋转）不应触发小窗模式
                    //  [新增] 进入音频模式时也不应触发小窗（检查目标路由）
                    val currentDestination = navController.currentDestination?.route
                    // Update: use the state variable as a more reliable indicator
                    // val isNavigatingToAudioMode = currentDestination == ScreenRoutes.AudioMode.route
                    
                    if (!stillInVideoRoute && activity?.isChangingConfigurations != true && !isNavigatingToAudioMode) {
                        // [关键修复] 兜底处理：系统返回手势可能不会走 VideoDetailScreen.handleBack。
                        // 真正离开视频域时统一标记导航离开，避免后台播放状态残留。
                        miniPlayerManager?.markLeavingByNavigation()

                        //  [修复] 只有在"应用内小窗"模式下才进入小窗
                        // 后台模式只播放音频，不显示小窗
                        if (miniPlayerManager?.shouldShowInAppMiniPlayer() == true) {
                            miniPlayerManager.enterMiniMode()
                        }
                    }
                }
            }

            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                VideoDetailScreen(
                    bvid = bvid,
                    coverUrl = coverUrl,
                    // 传递 cid 参数
                    cid = backStackEntry.arguments?.getLong("cid") ?: 0L,
                    onUpClick = { mid -> navController.navigate(ScreenRoutes.Space.createRoute(mid)) },  //  点击UP跳转空间
                    miniPlayerManager = miniPlayerManager,
                    isInPipMode = isInPipMode,
                    isVisible = true,
                    startInFullscreen = startFullscreen,  //  传递全屏参数
                    transitionEnabled = cardTransitionEnabled,  //  传递过渡动画开关
                    onBack = { 
                        //  标记正在返回，跳过首页卡片入场动画
                        CardPositionManager.markReturning()
                        // 🎯 [新增] 标记通过导航离开，让播放器暂停
                        miniPlayerManager?.markLeavingByNavigation()
                        //  [修复] 不再在这里调用 enterMiniMode，由 onDispose 统一处理
                        navController.popBackStack() 
                    },
                    //  [新增] 导航到音频模式
                    onNavigateToAudioMode = { 
                        isNavigatingToAudioMode = true
                        navController.navigate(ScreenRoutes.AudioMode.route)
                    },
                    onNavigateToSearch = {
                        if (canNavigate()) navController.navigate(ScreenRoutes.Search.route)
                    },
                    // [修复] 传递视频点击导航回调
                    onVideoClick = { vid, _ -> 
                        navigateToVideo(vid, 0L, "")
                    },
                    onBgmClick = { bgm ->
                        // 获取当前视频的 cid（在闭包中捕获）
                        val videoCid = backStackEntry.arguments?.getLong("cid") ?: 0L
                        
                        android.util.Log.d("BGM_DEBUG", "🎵 musicId=${bgm.musicId}, title=${bgm.musicTitle}")
                        android.util.Log.d("BGM_DEBUG", "🎵 Using current video: bvid=$bvid, cid=$videoCid")
                        
                        // 尝试解析 au 格式 (如 au123456 或纯数字)
                        val auSid = bgm.musicId.removePrefix("au").toLongOrNull()
                        
                        if (auSid != null) {
                            // au 格式：使用原生音乐详情页
                            navController.navigate(ScreenRoutes.MusicDetail.createRoute(auSid))
                        } else if (bgm.musicId.startsWith("MA") && videoCid > 0) {
                            // MA 格式：使用当前视频的 bvid 和 cid 获取音频流
                            // jumpUrl 中的 aid/cid 是 B 站内部 ID，无法用于获取视频流
                            // 所以直接使用当前正在播放的视频来提取音频
                            val title = bgm.musicTitle.ifEmpty { "背景音乐" }
                            
                            android.util.Log.d("BGM_DEBUG", "🎵 Navigating with: bvid=$bvid, cid=$videoCid")
                            navController.navigate(ScreenRoutes.NativeMusic.createRoute(title, bvid, videoCid))
                        } else if (bgm.jumpUrl.isNotEmpty()) {
                            // 回退：使用 WebView
                            navController.navigate(ScreenRoutes.Web.createRoute(bgm.jumpUrl, "背景音乐"))
                        }
                    }
                )
            }
        }
        
        // --- 2.1  [新增] 音频模式页面 ---
        composable(
            route = ScreenRoutes.AudioMode.route,
            //  从底部滑入
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            //  向下滑出
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) { backStackEntry ->
            //  [关键] 共享 PlayerViewModel
            // 尝试获取前一个页面 (VideoDetailScreen) 的 ViewModel
            // 这样可以复用播放器实例，实现无缝切换
            val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                navController.previousBackStackEntry
            }
            
            // 如果能获取到 VideoDetail 的 entry，就使用它的 ViewModel
            // 否则创建一个新的（这不应该发生，除非直接深层链接进入）
            val viewModel: com.android.purebilibili.feature.video.viewmodel.PlayerViewModel = if (parentEntry != null) {
                viewModel(viewModelStoreOwner = parentEntry)
            } else {
                viewModel()
            }
            
            com.android.purebilibili.feature.video.screen.AudioModeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onVideoModeClick = { _ ->
                    //  [修复] 直接返回到 VideoDetailScreen
                    // 由于 ViewModel 是共享的，VideoDetailScreen 会自动显示当前正在播放的视频
                    // 不需要比较 bvid，因为播放器状态已同步
                    navController.popBackStack()
                }
            )
        }

        // --- 3. 个人中心 ---
        composable(
            route = ScreenRoutes.Profile.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onGoToLogin = { navController.navigate(ScreenRoutes.Login.route) },
                onLogoutSuccess = { homeViewModel.refresh() },
                onSettingsClick = { navController.navigate(ScreenRoutes.Settings.route) },
                onHistoryClick = { navController.navigate(ScreenRoutes.History.route) },
                onFavoriteClick = { navController.navigate(ScreenRoutes.Favorite.route) },
                onFollowingClick = { mid -> navController.navigate(ScreenRoutes.Following.createRoute(mid)) },
                onDownloadClick = { navController.navigate(ScreenRoutes.DownloadList.route) },
                onWatchLaterClick = { navController.navigate(ScreenRoutes.WatchLater.route) },
                onInboxClick = { navController.navigate(ScreenRoutes.Inbox.route) },  //  [新增] 私信入口
                onVideoClick = { bvid -> navigateToVideo(bvid, 0L, "") }  // [新增] 三连彩蛋跳转
            )
        }


        // --- 4. 历史记录 ---
        composable(
            route = ScreenRoutes.History.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            val historyViewModel: HistoryViewModel = viewModel()
            
            //  [修复] 每次进入历史记录页面时刷新数据
            androidx.compose.runtime.LaunchedEffect(Unit) {
                historyViewModel.loadData()
            }
            
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {

                CommonListScreen(
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() },
                    globalHazeState = mainHazeState, // [新增] 传入全局 HazeState
                    onVideoClick = { bvid, cid ->
                        // [修复] 根据历史记录类型导航到不同页面
                        val historyItem = historyViewModel.getHistoryItem(bvid)
                        val resolvedCid = resolveHistoryPlaybackCid(
                            clickedCid = cid,
                            historyItem = historyItem
                        )
                        when (historyItem?.business) {
                            com.android.purebilibili.data.model.response.HistoryBusiness.PGC -> {
                                // 番剧: 导航到番剧播放页
                                if (historyItem.epid > 0 && historyItem.seasonId > 0) {
                                    navController.navigate(ScreenRoutes.BangumiPlayer.createRoute(historyItem.seasonId, historyItem.epid))
                                } else if (historyItem.seasonId > 0 || historyItem.epid > 0) {
                                    // 有 seasonId (可能是 oid) 或 epid，进详情页
                                    // 注意：即使 seasonId 可能是错误的 (AVID)，只要有 epid，新的详情页逻辑也能正确加载
                                    navController.navigate(ScreenRoutes.BangumiDetail.createRoute(historyItem.seasonId, historyItem.epid))
                                } else {
                                    // 异常情况，尝试普通视频方式
                                    navigateToVideo(bvid, resolvedCid, "")
                                }
                            }
                            com.android.purebilibili.data.model.response.HistoryBusiness.LIVE -> {
                                // 直播: 导航到直播页
                                if (historyItem.roomId > 0) {
                                    navController.navigate(ScreenRoutes.Live.createRoute(
                                        historyItem.roomId,
                                        historyItem.videoItem.title,
                                        historyItem.videoItem.owner.name
                                    ))
                                } else {
                                    navigateToVideo(bvid, resolvedCid, "")
                                }
                            }
                            else -> {
                                // 普通视频 (archive) 或未知类型
                                navigateToVideo(bvid, resolvedCid, "")
                            }
                        }
                    }
                )
            }
        }

        // --- 5. 收藏 ---
        composable(
            route = ScreenRoutes.Favorite.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            val favoriteViewModel: FavoriteViewModel = viewModel()
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                CommonListScreen(
                    viewModel = favoriteViewModel,
                    onBack = { navController.popBackStack() },
                    globalHazeState = mainHazeState, // [新增] 传入全局 HazeState
                    onVideoClick = { bvid, cid -> navigateToVideo(bvid, cid, "") }
                )
            }
        }
        
        // --- 5.3  [新增] 稍后再看 ---
        composable(
            route = ScreenRoutes.WatchLater.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                com.android.purebilibili.feature.watchlater.WatchLaterScreen(
                    onBack = { navController.popBackStack() },
                    onVideoClick = { bvid, cid -> navigateToVideo(bvid, cid, "") },
                    globalHazeState = mainHazeState // [新增] 传入全局 HazeState (WatchLaterScreen 需支持)
                )
            }
        }
        
        // --- 5.4  [新增] 直播列表 ---
        composable(
            route = ScreenRoutes.LiveList.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                com.android.purebilibili.feature.live.LiveListScreen(
                    onBack = { navController.popBackStack() },
                    onLiveClick = { roomId, title, uname ->
                        navController.navigate(ScreenRoutes.Live.createRoute(roomId, title, uname))
                    },
                    globalHazeState = mainHazeState // [新增] 传入全局 HazeState (LiveListScreen 需支持)
                )
            }
        }
        
        // --- 5.5  关注列表 ---
        composable(
            route = ScreenRoutes.Following.route,
            arguments = listOf(
                navArgument("mid") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val mid = backStackEntry.arguments?.getLong("mid") ?: 0L
            com.android.purebilibili.feature.following.FollowingListScreen(
                mid = mid,
                onBack = { navController.popBackStack() },
                onUserClick = { userMid -> navController.navigate(ScreenRoutes.Space.createRoute(userMid)) }
            )
        }
        
        // --- 5.6  离线缓存列表 ---
        composable(
            route = ScreenRoutes.DownloadList.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.download.DownloadListScreen(
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid -> navigateToVideo(bvid, 0L, "") },
                // 🔧 [新增] 离线播放回调
                onOfflineVideoClick = { taskId ->
                    navController.navigate(ScreenRoutes.OfflineVideoPlayer.createRoute(taskId))
                }
            )
        }
        
        // --- 5.7 🔧 [新增] 离线视频播放 ---
        composable(
            route = ScreenRoutes.OfflineVideoPlayer.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val taskId = android.net.Uri.decode(backStackEntry.arguments?.getString("taskId") ?: "")
            com.android.purebilibili.feature.download.OfflineVideoPlayerScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() }
            )
        }


        // --- 6. 动态页面 ---
        composable(
            route = ScreenRoutes.Dynamic.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                DynamicScreen(
                    onVideoClick = { bvid -> navigateToVideo(bvid, 0L, "") },
                    onUserClick = { mid -> navController.navigate(ScreenRoutes.Space.createRoute(mid)) },
                    onLiveClick = { roomId, title, uname ->  //  直播点击
                        navController.navigate(ScreenRoutes.Live.createRoute(roomId, title, uname))
                    },
                    onBack = { navController.popBackStack() },
                    onLoginClick = { navController.navigate(ScreenRoutes.Login.route) },  //  跳转登录
                    onHomeClick = { navController.popBackStack() },  //  返回首页
                    globalHazeState = mainHazeState  // [新增] 全局底栏模糊状态
                )
            }
        }
        
        // --- 6.5  [新增] 竖屏短视频 (故事模式) ---
        composable(
            route = ScreenRoutes.Story.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.story.StoryScreen(
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid, aid, title -> navigateToVideo(bvid, 0L, "") }
            )
        }

        // --- 7. 搜索 (核心修复) ---
        composable(
            route = ScreenRoutes.Search.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            //  进入视频详情页时的退出动画（与首页一致）
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            //  从视频详情页返回时的动画（与首页一致，让卡片回到原位）
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            //  从 homeViewModel 获取最新的用户状态 (包括头像)
            val homeState by homeViewModel.uiState.collectAsState()

            //  提供 AnimatedVisibilityScope 给 SearchScreen 以支持共享元素过渡
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                SearchScreen(
                    userFace = homeState.user.face, // 传入头像 URL
                    onBack = { navController.popBackStack() },
                    onVideoClick = { bvid, cid -> navigateToVideo(bvid, cid, "") },
                    onUpClick = { mid -> navController.navigate(ScreenRoutes.Space.createRoute(mid)) },  //  点击UP主跳转到空间
                    onBangumiClick = { seasonId ->
                        if (canNavigate() && seasonId > 0) {
                            navController.navigate(ScreenRoutes.BangumiDetail.createRoute(seasonId))
                        }
                    },
                    onLiveClick = { roomId, title, uname ->
                        if (canNavigate()) navController.navigate(ScreenRoutes.Live.createRoute(roomId, title, uname))
                    },
                    onAvatarClick = {
                        // 如果已登录 -> 去个人中心，未登录 -> 去登录页
                        if (homeState.user.isLogin) {
                            navController.navigate(ScreenRoutes.Profile.route)
                        } else {
                            navController.navigate(ScreenRoutes.Login.route)
                        }
                    }
                )
            }
        }

        // --- Settings & Login ---
        composable(
            route = ScreenRoutes.Settings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSourceLicensesClick = { navController.navigate(ScreenRoutes.OpenSourceLicenses.route) },
                onAppearanceClick = { navController.navigate(ScreenRoutes.AppearanceSettings.route) },
                onPlaybackClick = { navController.navigate(ScreenRoutes.PlaybackSettings.route) },
                onPermissionClick = { navController.navigate(ScreenRoutes.PermissionSettings.route) },
                onPluginsClick = { navController.navigate(ScreenRoutes.PluginsSettings.route) },
                onNavigateToBottomBarSettings = { navController.navigate(ScreenRoutes.BottomBarSettings.route) },
                onTipsClick = { navController.navigate(ScreenRoutes.TipsSettings.route) }, // [Feature] Tips
                onReplayOnboardingClick = { navController.navigate(ScreenRoutes.Onboarding.route) },
                mainHazeState = mainHazeState //  传递全局 Haze 状态
            )
        }
        
        // [Feature] Tips Screen
        composable(
            route = ScreenRoutes.TipsSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.TipsSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ScreenRoutes.Login.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) {
            LoginScreen(
                onClose = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.popBackStack()
                    homeViewModel.refresh()
                }
            )
        }

        // --- 11. WebView ---
        composable(
            route = ScreenRoutes.Web.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) { backStackEntry ->
            val url = android.net.Uri.decode(backStackEntry.arguments?.getString("url") ?: "")
            val title = android.net.Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
            
            com.android.purebilibili.feature.web.WebViewScreen(
                url = url,
                title = title.ifEmpty { null },
                onBack = { navController.popBackStack() },
                // [新增] 链接拦截回调 - 跳转到应用内原生界面
                onVideoClick = { bvid -> 
                    navController.popBackStack()  // 先关闭 WebView
                    navigateToVideo(bvid, 0L, "") 
                },
                onSpaceClick = { mid -> 
                    navController.popBackStack()
                    navController.navigate(ScreenRoutes.Space.createRoute(mid)) 
                },
                onLiveClick = { roomId -> 
                    navController.popBackStack()
                    navController.navigate(ScreenRoutes.Live.createRoute(roomId, "", "")) 
                },
                onBangumiClick = { seasonId, epId ->
                    navController.popBackStack()
                    if (seasonId > 0) {
                        navController.navigate(ScreenRoutes.BangumiDetail.createRoute(seasonId, epId))
                    } else if (epId > 0) {
                        navController.navigate(ScreenRoutes.BangumiDetail.createRoute(0, epId))
                    }
                },
                onMusicClick = { musicId ->
                    navController.popBackStack()
                    // AU 格式：跳转到音乐详情页
                    val auSid = musicId.removePrefix("au").removePrefix("AU").toLongOrNull()
                    if (auSid != null) {
                        navController.navigate(ScreenRoutes.MusicDetail.createRoute(auSid))
                    }
                    // MA 格式目前无法在 WebView 内处理，因为缺少当前视频上下文
                    // 用户需要从视频页直接点击 BGM 按钮
                }
            )
        }

        // --- 8. 开源许可证 ---
        composable(
            route = ScreenRoutes.OpenSourceLicenses.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.OpenSourceLicensesScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // ---  外观设置二级页面 ---
        composable(
            route = ScreenRoutes.AppearanceSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            AppearanceSettingsScreen(
                onBack = { navController.popBackStack() },

                onNavigateToIconSettings = { navController.navigate(ScreenRoutes.IconSettings.route) },
                onNavigateToAnimationSettings = { navController.navigate(ScreenRoutes.AnimationSettings.route) }
            )
        }
        

        
        // ---  图标设置页面 ---
        composable(
            route = ScreenRoutes.IconSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.IconSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // ---  动画设置页面 ---
        composable(
            route = ScreenRoutes.AnimationSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.AnimationSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // ---  播放设置二级页面 ---
        composable(
            route = ScreenRoutes.PlaybackSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            PlaybackSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // ---  权限管理页面 ---
        composable(
            route = ScreenRoutes.PermissionSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.PermissionSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // ---  插件中心页面 ---
        composable(
            route = ScreenRoutes.PluginsSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.PluginsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // ---  底栏管理页面 ---
        composable(
            route = ScreenRoutes.BottomBarSettings.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.settings.BottomBarSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // --- 9.  [新增] UP主空间页面 ---
        composable(
            route = ScreenRoutes.Space.route,
            arguments = listOf(
                navArgument("mid") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val mid = backStackEntry.arguments?.getLong("mid") ?: 0L
            
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                com.android.purebilibili.feature.space.SpaceScreen(
                    mid = mid,
                    onBack = { navController.popBackStack() },
                    onVideoClick = { bvid -> navigateToVideo(bvid, 0L, "") },
                    onDynamicWebClick = { url ->
                        navController.navigate(ScreenRoutes.Web.createRoute(url, "动态详情"))
                    },
                    onViewAllClick = { type, id, mid, title ->
                        navController.navigate(ScreenRoutes.SeasonSeriesDetail.createRoute(type, id, mid, title))
                    },
                    sharedTransitionScope = LocalSharedTransitionScope.current,
                    animatedVisibilityScope = this
                )
            }
        }

        // --- 9.1 [新增] 合集/系列详情页面 ---
        composable(
            route = ScreenRoutes.SeasonSeriesDetail.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType },
                navArgument("mid") { type = NavType.LongType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: ""
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val mid = backStackEntry.arguments?.getLong("mid") ?: 0L
            val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
            
            val viewModel: com.android.purebilibili.feature.space.SeasonSeriesDetailViewModel = viewModel()
            
            // Initial load
            androidx.compose.runtime.LaunchedEffect(type, id) {
                viewModel.init(type, id, mid, title)
            }
            
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                CommonListScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onVideoClick = { bvid, cid -> navigateToVideo(bvid, cid, "") }
                )
            }
        }
        
        // --- 10.  [新增] 直播播放页面 ---
        composable(
            route = ScreenRoutes.Live.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.LongType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("uname") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { 
                if (cardTransitionEnabled) fadeIn(animationSpec = tween(300))
                else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration))
            },
            popExitTransition = { 
                if (cardTransitionEnabled) fadeOut(animationSpec = tween(300))
                else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration))
            }
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getLong("roomId") ?: 0L
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val uname = backStackEntry.arguments?.getString("uname") ?: ""
            
            ProvideAnimatedVisibilityScope(animatedVisibilityScope = this) {
                com.android.purebilibili.feature.live.LivePlayerScreen(
                    roomId = roomId,
                    title = Uri.decode(title),
                    uname = Uri.decode(uname),
                    onBack = { navController.popBackStack() },
                    onUserClick = { mid -> navController.navigate(ScreenRoutes.Space.createRoute(mid)) }
                )
            }
        }
        
        // --- 11.  [新增] 番剧/影视主页面 ---
        composable(
            route = ScreenRoutes.Bangumi.route,
            arguments = listOf(
                navArgument("type") { type = NavType.IntType; defaultValue = 1 }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val initialType = backStackEntry.arguments?.getInt("type") ?: 1
            com.android.purebilibili.feature.bangumi.BangumiScreen(
                onBack = { navController.popBackStack() },
                onBangumiClick = { seasonId ->
                    navController.navigate(ScreenRoutes.BangumiDetail.createRoute(seasonId))
                },
                initialType = initialType  //  [修复] 传入初始类型
            )
        }
        
        // --- 12.  [新增] 番剧/影视详情页面 ---
        composable(
            route = ScreenRoutes.BangumiDetail.route,
            arguments = listOf(
                navArgument("seasonId") { type = NavType.LongType },
                navArgument("epId") { type = NavType.LongType; defaultValue = 0L }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val seasonId = backStackEntry.arguments?.getLong("seasonId") ?: 0L
            val epId = backStackEntry.arguments?.getLong("epId") ?: 0L
            com.android.purebilibili.feature.bangumi.BangumiDetailScreen(
                seasonId = seasonId,
                epId = epId,
                onBack = { navController.popBackStack() },
                onEpisodeClick = { actionSeasonId, episode ->
                    //  [修改] 跳转到番剧播放页
                    navController.navigate(ScreenRoutes.BangumiPlayer.createRoute(actionSeasonId, episode.id))
                },
                onSeasonClick = { newSeasonId ->
                    //  切换到其他季度（替换当前页面）
                    navController.navigate(ScreenRoutes.BangumiDetail.createRoute(newSeasonId)) {
                        popUpTo(ScreenRoutes.BangumiDetail.createRoute(seasonId)) { inclusive = true }
                    }

                }
            )
        }
        
        // --- 13.  [新增] 番剧播放页面 ---
        composable(
            route = ScreenRoutes.BangumiPlayer.route,
            arguments = listOf(
                navArgument("seasonId") { type = NavType.LongType },
                navArgument("epId") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val seasonId = backStackEntry.arguments?.getLong("seasonId") ?: 0L
            val epId = backStackEntry.arguments?.getLong("epId") ?: 0L
            com.android.purebilibili.feature.bangumi.BangumiPlayerScreen(
                seasonId = seasonId,
                epId = epId,
                onBack = { navController.popBackStack() },
                onNavigateToLogin = { navController.navigate(ScreenRoutes.Login.route) }
            )
        }
        
        // --- 14.  分区页面 ---
        composable(
            route = ScreenRoutes.Partition.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.partition.PartitionScreen(
                onBack = { navController.popBackStack() },
                onPartitionClick = { id, name ->
                    //  点击分区后，跳转到分类详情页面
                    navController.navigate(ScreenRoutes.Category.createRoute(id, name))
                }
            )
        }
        
        // --- 15.  分类详情页面 ---
        composable(
            route = ScreenRoutes.Category.route,
            arguments = listOf(
                navArgument("tid") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val tid = backStackEntry.arguments?.getInt("tid") ?: 0
            val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
            com.android.purebilibili.feature.category.CategoryScreen(
                tid = tid,
                name = name,
                onBack = { navController.popBackStack() },
                onVideoClick = { bvid, cid, cover -> navigateToVideo(bvid, cid, cover) }
            )
        }
        
        // --- [新增] 私信收件箱 ---
        composable(
            route = ScreenRoutes.Inbox.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) {
            com.android.purebilibili.feature.message.InboxScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { talkerId, sessionType, userName ->
                    navController.navigate(ScreenRoutes.Chat.createRoute(talkerId, sessionType, userName))
                }
            )
        }
        
        // --- [新增] 私信聊天详情 ---
        composable(
            route = ScreenRoutes.Chat.route,
            arguments = listOf(
                navArgument("talkerId") { type = NavType.LongType },
                navArgument("sessionType") { type = NavType.IntType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(animDuration)) }
        ) { backStackEntry ->
            val talkerId = backStackEntry.arguments?.getLong("talkerId") ?: 0L
            val sessionType = backStackEntry.arguments?.getInt("sessionType") ?: 1
            val userName = Uri.decode(backStackEntry.arguments?.getString("name") ?: "用户$talkerId")
            com.android.purebilibili.feature.message.ChatScreen(
                talkerId = talkerId,
                sessionType = sessionType,
                userName = userName,
                onBack = { navController.popBackStack() },
                onNavigateToVideo = { bvid ->
                    navController.navigate(ScreenRoutes.VideoPlayer.createRoute(bvid))
                }
            )
        }
        
        // --- 16.  [新增] 音频详情页面 ---
        composable(
            route = ScreenRoutes.MusicDetail.route,
            arguments = listOf(
                navArgument("sid") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) { backStackEntry ->
            val sid = backStackEntry.arguments?.getLong("sid") ?: 0L
            com.android.purebilibili.feature.audio.screen.MusicDetailScreen(
                sid = sid,
                onBack = { navController.popBackStack() }
        )
        }
        
        // --- 17. [新增] 原生音乐播放页 (MA 格式 - 从视频 DASH 提取音频) ---
        composable(
            route = ScreenRoutes.NativeMusic.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
                navArgument("bvid") { type = NavType.StringType },
                navArgument("cid") { type = NavType.LongType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(animDuration)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(animDuration)) }
        ) { backStackEntry ->
            val title = android.net.Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
            val bvid = android.net.Uri.decode(backStackEntry.arguments?.getString("bvid") ?: "")
            val cid = backStackEntry.arguments?.getLong("cid") ?: 0L
            
            com.android.purebilibili.feature.audio.screen.MusicDetailScreen(
                musicTitle = title.ifEmpty { "背景音乐" },
                bvid = bvid,
                cid = cid,
                onBack = { navController.popBackStack() }
            )
        }
    } // End of NavHost
            } // End of Content Box

            // ===== 全局底栏 (Global Bottom Bar) =====
            // ===== 全局底栏 (Global Bottom Bar) =====
            // 依然保留 showBottomBar 作为外层判断，避免不必要的 AnimatedVisibility 挂载
            if (showBottomBar && bottomBarVisibilityMode != SettingsManager.BottomBarVisibilityMode.ALWAYS_HIDDEN) {
                // 用于处理底栏悬浮时的点击穿透问题，底栏自身处理点击
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f)
                ) {
                    AnimatedVisibility(
                        visible = finalBottomBarVisible,
                        enter = slideInVertically(
                            // [UX优化] 物理弹簧进场 (Spring Entrance)
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                            initialOffsetY = { it }
                        ) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(
                            // [UX优化] 物理弹簧出场 (Spring Exit)
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                            targetOffsetY = { it }
                        ) + fadeOut(animationSpec = tween(200))
                    ) {
                        if (isBottomBarFloating) {
                            // 悬浮式底栏
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                FrostedBottomBar(
                                    currentItem = currentBottomNavItem,
                                    onItemClick = { item -> navigateTo(item.route) },
                                    onHomeDoubleTap = { homeScrollChannel.trySend(Unit) },
                                    onDynamicDoubleTap = { dynamicScrollChannel.trySend(Unit) },
                                    hazeState = if (isBottomBarBlurEnabled) mainHazeState else null,
                                    isFloating = true,
                                    labelMode = bottomBarLabelMode,
                                    visibleItems = visibleBottomBarItems,
                                    itemColorIndices = bottomBarItemColors,
                                    homeSettings = homeSettings,
                                    backdrop = bottomBarBackdrop, // [LayerBackdrop] Real background refraction
                                    onToggleSidebar = {
                                        // [Tablet] Toggle sidebar mode
                                        coroutineScope.launch {
                                            SettingsManager.setTabletUseSidebar(context, true)
                                        }
                                    }
                                )
                            }
                        } else {
                            // 贴底式底栏
                            FrostedBottomBar(
                                currentItem = currentBottomNavItem,
                                onItemClick = { item -> navigateTo(item.route) },
                                onHomeDoubleTap = { homeScrollChannel.trySend(Unit) },
                                onDynamicDoubleTap = { dynamicScrollChannel.trySend(Unit) },
                                hazeState = if (isBottomBarBlurEnabled) mainHazeState else null,
                                isFloating = false,
                                labelMode = bottomBarLabelMode,
                                visibleItems = visibleBottomBarItems,
                                itemColorIndices = bottomBarItemColors,
                                homeSettings = homeSettings,
                                backdrop = bottomBarBackdrop, // [LayerBackdrop] Real background refraction
                                onToggleSidebar = {
                                    // [Tablet] Toggle sidebar mode
                                    coroutineScope.launch {
                                        SettingsManager.setTabletUseSidebar(context, true)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showLaunchDisclaimer) {
                ReleaseChannelDisclaimerDialog(
                    title = "首次使用声明",
                    onDismiss = {
                        showLaunchDisclaimer = false
                        welcomePrefs.edit().putBoolean(RELEASE_DISCLAIMER_ACK_KEY, true).apply()
                    },
                    onOpenGithub = { uriHandler.openUri(OFFICIAL_GITHUB_URL) },
                    onOpenTelegram = { uriHandler.openUri(OFFICIAL_TELEGRAM_URL) }
                )
            }

            ImagePreviewOverlayHost(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            )
        } // End of Main Box
        } // End of CompositionLocalProvider
    }
}
