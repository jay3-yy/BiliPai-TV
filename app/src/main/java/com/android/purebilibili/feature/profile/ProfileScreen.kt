package com.android.purebilibili.feature.profile

import android.app.Activity
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.core.theme.DarkBackground
import com.android.purebilibili.core.theme.DarkSurface
import com.android.purebilibili.core.theme.DarkSurfaceVariant
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.home.UserState
import com.android.purebilibili.core.ui.LoadingAnimation
import com.android.purebilibili.core.ui.BiliGradientButton
import com.android.purebilibili.core.ui.AdaptiveSplitLayout
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.ui.components.IOSGroup
import com.android.purebilibili.core.ui.components.IOSClickableItem
import com.android.purebilibili.core.ui.components.IOSDivider
import com.android.purebilibili.core.ui.components.IOSSwitchItem
import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.components.IOSGridItem
import androidx.compose.ui.input.nestedscroll.nestedScroll

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.android.purebilibili.core.ui.blur.unifiedBlur

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.draw.blur
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onFollowingClick: (Long) -> Unit = {},  //  关注列表点击
    onDownloadClick: () -> Unit = {},  //  离线缓存点击
    onWatchLaterClick: () -> Unit = {}, // 稍后再看点击
    onInboxClick: () -> Unit = {},  //  [新增] 私信入口点击
    onVideoClick: (String) -> Unit = {}  // [新增] 视频点击（三连彩蛋跳转用）
    // [注意] 移除了 globalHazeState - 双 hazeSource 模式与 Haze 库冲突
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val windowSizeClass = LocalWindowSizeClass.current
    
    // [Blur] Haze State
    val hazeState = remember { HazeState() }

    //  设置沉浸式状态栏和导航栏（进入时修改，离开时恢复）
    DisposableEffect(state) {
        val window = (context as? Activity)?.window
        val insetsController = if (window != null) {
            WindowInsetsControllerCompat(window, view)
        } else null
        val isLoggedOut = state is ProfileUiState.LoggedOut
        
        // 保存原始配置
        val originalStatusBarColor = window?.statusBarColor ?: android.graphics.Color.TRANSPARENT
        val originalNavBarColor = window?.navigationBarColor ?: android.graphics.Color.TRANSPARENT
        val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
        val originalDecorFits = window?.decorView?.fitsSystemWindows ?: true
        
        if (isLoggedOut && window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            insetsController?.isAppearanceLightStatusBars = false
            insetsController?.isAppearanceLightNavigationBars = false
        }
        
        onDispose {
            // 离开时恢复原始配置
            if (isLoggedOut && window != null && insetsController != null) {
                WindowCompat.setDecorFitsSystemWindows(window, originalDecorFits)
                window.statusBarColor = originalStatusBarColor
                window.navigationBarColor = originalNavBarColor
                insetsController.isAppearanceLightStatusBars = originalLightStatusBars
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
        //  [埋点] 页面浏览追踪
        com.android.purebilibili.core.util.AnalyticsHelper.logScreenView("ProfileScreen")
    }

    //  未登录状态使用沉浸式全屏布局，已登录使用正常 Scaffold
    val currentUiState = state
    when (currentUiState) {
        is ProfileUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                LoadingAnimation(size = 80.dp)
            }
        }
        is ProfileUiState.LoggedOut -> {
            // [Modified] 游客模式：复用统一 UI，但使用虚拟游客数据
            val guestUser = UserState(
                isLogin = false,
                name = "点击登录/注册",
                face = "", // 空头像，UserInfoSection 会处理为默认或占位符
                mid = 0,
                level = 0,
                coin = 0.0,
                bcoin = 0.0,
                isVip = false,
                vipLabel = "",
                following = 0,
                follower = 0,

                dynamic = 0,
                topPhoto = currentUiState.topPhoto // [Modified] Use photo from state
            )
            
            
            Box(modifier = Modifier.fillMaxSize()) {
                ProfileBackground(user = guestUser, viewModel = viewModel)
                
                MobileProfileContent(
                    user = guestUser,
                    onLogout = onGoToLogin, // "退出登录" 变为 "登录"
                    onHistoryClick = onGoToLogin, // 游客点击功能需登录
                    onFavoriteClick = onGoToLogin,
                    onFollowingClick = { onGoToLogin() },
                    onDownloadClick = onGoToLogin,
                    onWatchLaterClick = onGoToLogin,
                    onInboxClick = onGoToLogin,  //  [新增] 游客点击需登录
                    onVideoClick = { },  // 游客模式不显示三连
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
                    onBack = onBack,
                    onSettingsClick = onSettingsClick,
                    hazeState = hazeState,
                    // [New] 传递点击头部去登录的回调 (需修改 MobileProfileContent 支持)
                    onHeaderClick = onGoToLogin,
                    paddingValues = PaddingValues(0.dp) // 全屏
                )
            }

        }
        is ProfileUiState.Error -> {
            // 🔧 [新增] 离线/错误状态 - 显示错误信息并提供重试和离线缓存入口
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("我的") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = onSettingsClick) {
                                Icon(CupertinoIcons.Default.Gearshape, contentDescription = "Settings")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 错误图标
                    Icon(
                        CupertinoIcons.Default.ExclamationmarkTriangle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = currentUiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 重试按钮
                    Button(
                        onClick = { viewModel.loadProfile() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(CupertinoIcons.Default.ArrowClockwise, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重试")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 离线缓存入口
                    OutlinedButton(onClick = onDownloadClick) {
                        Icon(CupertinoIcons.Default.ArrowDownCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("查看离线缓存")
                    }
                }
            }
        }
        is ProfileUiState.Success -> {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.background,
                // [Immersive] Mobile hides default TopBar, Tablet keeps it
                topBar = {
                    if (windowSizeClass.shouldUseSplitLayout) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .unifiedBlur(hazeState)
                        ) {
                            LargeTopAppBar(
                                title = { Text("我的", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    IconButton(onClick = onBack) {
                                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = onSettingsClick) {
                                        Icon(CupertinoIcons.Default.Gearshape, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                                colors = TopAppBarDefaults.largeTopAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                },
                contentWindowInsets = if (!windowSizeClass.shouldUseSplitLayout) WindowInsets(0.dp) else ScaffoldDefaults.contentWindowInsets
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    // [Refactor] Lift background to root
                    ProfileBackground(user = currentUiState.user, viewModel = viewModel)
                    
                    if (windowSizeClass.shouldUseSplitLayout) {
                        TabletProfileContent(
                            user = currentUiState.user,
                            onLogout = {
                                viewModel.logout()
                                onLogoutSuccess()
                            },
                            onHistoryClick = onHistoryClick,
                            onFavoriteClick = onFavoriteClick,
                            onFollowingClick = { onFollowingClick(currentUiState.user.mid) },
                            onDownloadClick = onDownloadClick,
                            onSettingsClick = onSettingsClick,
                            onBack = onBack,
                            onWatchLaterClick = onWatchLaterClick,
                            paddingValues = padding
                        )
                    } else {
                        MobileProfileContent(
                            viewModel = viewModel,
                            user = currentUiState.user,
                            onLogout = {
                                viewModel.logout()
                                onLogoutSuccess()
                            },
                            onHistoryClick = onHistoryClick,
                            onFavoriteClick = onFavoriteClick,
                            onFollowingClick = { onFollowingClick(currentUiState.user.mid) },
                            onDownloadClick = onDownloadClick,
                            onWatchLaterClick = onWatchLaterClick,
                            onInboxClick = onInboxClick,  //  [新增] 私信入口
                            onVideoClick = onVideoClick,  // [新增] 三连彩蛋跳转
                            // [Immersive] Pass ScrollBehavior and Navigation Actions
                            scrollBehavior = scrollBehavior,
                            onBack = onBack,
                            onSettingsClick = onSettingsClick,
                            hazeState = hazeState,
                            paddingValues = padding
                        )
                    }
                }
            }
        }
    }
}

// [New] Reusable Background Component
@Composable
private fun BoxScope.ProfileBackground(
    user: UserState,
    viewModel: ProfileViewModel
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isTablet = windowSizeClass.shouldUseSplitLayout
    val isImmersive = user.topPhoto.isNotEmpty()
    val bgAlignmentBias by viewModel.getProfileBgAlignment(isTablet).collectAsState(0f)

    if (isImmersive) {
        // 1. 底层：高斯模糊填充 (填补图片不够长的区域)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.topPhoto)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(60.dp) // Android 12+ 原生模糊
        )
        
        // 2. 顶层：清晰头部图 (Header Banner)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.topPhoto)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.BiasAlignment(0f, bgAlignmentBias), // [New] Apply user alignment
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp) // [Modified] 增加高度以适应沉浸式 (260 -> 320)
                .align(Alignment.TopCenter)
        )
        
        // 3. 遮罩：渐变黑遮罩 (增加缓动层级)
        val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val gradientColors = if (isDarkTheme) {
             listOf(
                Color.Black.copy(alpha = 0.6f),
                Color.Black.copy(alpha = 0.3f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.2f),
                Color.Black.copy(alpha = 0.8f)
            )
        } else {
             listOf(
                Color.Black.copy(alpha = 0.3f), // Lighter top
                Color.Black.copy(alpha = 0.1f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.05f),
                Color.Black.copy(alpha = 0.4f)  // Lighter bottom
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = 1200f
                    )
                )
        )
    } else {
         // 无背景图时使用默认渐变
         Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
         )
    }
}


@Composable
fun TabletProfileContent(
    user: UserState,
    onLogout: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBack: () -> Unit,
    onWatchLaterClick: () -> Unit,
    paddingValues: PaddingValues
) {
    AdaptiveSplitLayout(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        primaryRatio = 0.4f,
        primaryContent = {
            // Left Pane: User Info & Stats & VIP
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent) // [Modified] Transparent for immersive bg
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(CupertinoIcons.Default.Gearshape, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                UserInfoSection(user, centered = true)
                Spacer(modifier = Modifier.height(24.dp))
                UserStatsSection(user, onFollowingClick)
                Spacer(modifier = Modifier.height(24.dp))
                VipBannerSection(user)
                Spacer(modifier = Modifier.weight(1f))
            }
        },
        secondaryContent = {
            // Right Pane: Services
            // [Modified] Glassy Background for Readability
            // Detect theme via MaterialTheme properties
            val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val glassContainerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f)
            val glassBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.4f)
            val contentColor = if (isDarkTheme) Color.White else Color.Black

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Outer padding
                    .clip(RoundedCornerShape(32.dp))
                    .background(glassContainerColor)
                    .border(1.dp, glassBorderColor, RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "我的服务",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // [Modified] Use new grid layout
                    ServicesSection(
                        onHistoryClick = onHistoryClick, 
                        onFavoriteClick = onFavoriteClick, 
                        onDownloadClick = onDownloadClick, 
                        onWatchLaterClick = onWatchLaterClick,
                        isTablet = true, // Force tablet mode
                        containerColor = Color.Transparent, // Grid items handle bg
                        contentColor = contentColor
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth(0.5f) // Wide button
                    ) {
                        Text("退出登录")
                    }
                }
            }
        }
    )
}

// Imports moved to top

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileProfileContent(
    viewModel: ProfileViewModel = viewModel(),
    user: UserState,
    onLogout: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onWatchLaterClick: () -> Unit,
    onInboxClick: () -> Unit = {},  //  [新增] 私信入口
    onVideoClick: (String) -> Unit = {},  // [新增] 三连彩蛋跳转
    // [New] Params
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    hazeState: HazeState? = null,
    onHeaderClick: () -> Unit = {}, // [New] Support header click for guest login
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val windowSizeClass = LocalWindowSizeClass.current
    
    // 📸 图片选择器
    // [New] Adjustment Sheet State
    var showAdjustmentSheet by remember { mutableStateOf(false) }
    var tempSelectedUri by remember { mutableStateOf<Uri?>(null) }
    val initialMobileBias by viewModel.getProfileBgAlignment(false).collectAsState(0f)
    val initialTabletBias by viewModel.getProfileBgAlignment(true).collectAsState(0f)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // [Modified] Don't save immediately, show adjustment sheet
            tempSelectedUri = uri
            showAdjustmentSheet = true
        }
    }
    
    // [New] Adjustment Sheet
    if (showAdjustmentSheet && tempSelectedUri != null) {
        WallpaperAdjustmentSheet(
            imageUri = tempSelectedUri.toString(),
            initialMobileBias = initialMobileBias,
            initialTabletBias = initialTabletBias,
            onDismiss = { showAdjustmentSheet = false },
            onSave = { mBias, tBias ->
                showAdjustmentSheet = false
                tempSelectedUri?.let { uri ->
                    viewModel.updateCustomBackground(uri, mBias, tBias)
                }
            }
        )
    }
    
    // [New] State for Official Wallpaper Sheet
    var showWallpaperSheet by remember { mutableStateOf(false) }
    
    // [New] Sheet
    if (showWallpaperSheet) {
        OfficialWallpaperSheet(viewModel = viewModel, onDismiss = { showWallpaperSheet = false })
    }
    
    val isImmersive = user.topPhoto.isNotEmpty()
    val contentColor = if (isImmersive) Color.White else MaterialTheme.colorScheme.onSurface

        // [Modified] Background logic moved to ProfileBackground()
        // No need to duplicate here, but MobileProfileContent is called separately in Split Layout?
        // Ah, MobileProfileContent is independent. Let's keep it simple: 
        // Remove background rendering from here since it's now at root?
        // NO, MobileProfileContent is used in the "else" branch of Scaffold content.
        // If we move background to root, it covers everything.
        // So we should REMOVE the background logic from here.
        
        // However, ProfileBackground checks user.topPhoto.
        // MobileProfileContent already has `isImmersive`.
        // Let's remove the background rendering here.
        // BUT wait, MobileProfileContent is used in LoggedOut state too, where ProfileScreen wrapper might not have user info?
        // LoggedOut wrapper passes guestUser to MobileProfileContent.
        // So ProfileBackground at root should prefer `currentUiState.user` or fallback?
        // ProfileUiState.LoggedOut also has topPhoto. 
        // The root Scaffold logic handles Success state. 
        // LoggedOut state uses MobileProfileContent directly without Scaffold in "when (currentUiState)".
        // So for LoggedOut, we still need background here, OR wrap LoggedOut in ProfileBackground too.
        
        // Let's keep duplicate logic for now (safest) OR refactor LoggedOut to use ProfileBackground?
        // Refactoring LoggedOut is better but complex.
        // Let's simply hide the background here IF the parent is already rendering it?
        // No, ProfileScreen calls MobileProfileContent only when !shouldUseSplitLayout.
        // And we added ProfileBackground in the Success branch main Box.
        // So for Mobile + Success, we have double background if we don't remove it here.
        // YES, remove background here.


        // YES, remove background here.
        
        Box(modifier = Modifier.fillMaxSize()) {
            // 📜 滚动内容
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (hazeState != null) Modifier.hazeSource(hazeState) else Modifier),
            contentPadding = PaddingValues(
                // [Modified] 顶部留白，适配 CenterAlignedTopAppBar (64dp + Status Bar ~ 30-40dp)
                top = 120.dp, 
                bottom = paddingValues.calculateBottomPadding() + 120.dp
            )
        ) {
            item { 
                Column {
                    // [UI优化] 移除背景色，透明显示下方 Header 图
                    UserInfoSection(user, transparent = isImmersive) 
                    
                    // [Fixed] 壁纸选项行 - 独立于用户信息，避免重叠
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp), // Increased margin
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glassy Button Style Helper
                        val glassyModifier = Modifier
                            .clip(RoundedCornerShape(50)) // Capsule shape
                            .background(Color.Black.copy(alpha = 0.3f)) // Semi-transparent dark base
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)) // Subtle frost border
                            .padding(horizontal = 14.dp, vertical = 8.dp)

                        // 官方壁纸
                        Row(
                            modifier = glassyModifier.clickable { showWallpaperSheet = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Default.Photo,
                                contentDescription = "官方壁纸",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "官方壁纸",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 🖼️ 本地相册说明弹窗状态
                        var showPhotoPickerDialog by remember { mutableStateOf(false) }
                        
                        // 本地相册
                        Row(
                            modifier = glassyModifier.clickable {
                                showPhotoPickerDialog = true
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Default.Folder,
                                contentDescription = "本地相册",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "本地相册",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                        
                        // 📋 本地相册说明弹窗
                        if (showPhotoPickerDialog) {
                            AlertDialog(
                                onDismissRequest = { showPhotoPickerDialog = false },
                                icon = {
                                    Icon(
                                        CupertinoIcons.Default.Photo,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text("选择照片", fontWeight = FontWeight.Bold)
                                },
                                text = {
                                    Text(
                                        "将打开系统相册选择一张照片作为背景。\n\n" +
                                        "📸 仅获取您选中照片的访问权限\n" +
                                        "🔒 不会访问您的其他照片",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showPhotoPickerDialog = false
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Text("选择照片")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showPhotoPickerDialog = false }) {
                                        Text("取消")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            if (user.isLogin) {
                item { UserStatsSection(user, onFollowingClick, transparent = isImmersive) }
                // [Modified] 删除 VIP 横幅，改为三连图标入口
                item {
                    ProfileTripleActionEntry(
                        onVipClick = { onVideoClick("BV1GJ411x7h7") },
                        on4KClick = { onVideoClick("BV1JsK5eyEuB") }
                    )
                }
            } else {
                 // [Fix] Guest mode spacer to compensate for missing stats section
                 item { Spacer(modifier = Modifier.height(56.dp)) }
            }

            
            item { 

                // [Adaptive Frost] 自适应磨砂玻璃逻辑
                // [Fix] Detect theme via MaterialTheme properties
                val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                
                // 玻璃颜色：深色模式用黑透，浅色模式用白透
                val glassContainerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f)
                
                // 文字颜色：深色背景用白字，浅色背景用黑字
                val glassContentColor = if (isDarkTheme) Color.White else Color.Black
                
                // 边框颜色：深色用微白边框，浅色用稍明显白边框(增强质感)
                val glassBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.4f)

                ServicesSection(
                    onHistoryClick = onHistoryClick, 
                    onFavoriteClick = onFavoriteClick, 
                    onDownloadClick = onDownloadClick, 
                    onWatchLaterClick = onWatchLaterClick,
                    onInboxClick = onInboxClick,  //  [新增]
                    onLogout = onLogout,
                    containerColor = if (isImmersive) glassContainerColor else MaterialTheme.colorScheme.surface,
                    contentColor = if (isImmersive) glassContentColor else MaterialTheme.colorScheme.onSurface,
                    borderColor = if (isImmersive) glassBorderColor else null,
                    isLogin = user.isLogin // [New] Pass login status
                )
                
            }
            // item { Spacer(...) } // Removed
            // item { IOSGroup { ... } } // Removed
        }
        
        // 🏗️ 沉浸式 TopBar (Standard)
        CenterAlignedTopAppBar(
            title = { Text("我的", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back", tint = contentColor)
                }
            },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(CupertinoIcons.Default.Gearshape, contentDescription = "Settings", tint = contentColor)
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                // [Style] 滚动后变为半透明黑底 (配合白色文字)，或保持透明?
                // 建议使用深色背景以保证文字清晰
                scrolledContainerColor = if (isImmersive) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
                titleContentColor = contentColor,
                actionIconContentColor = contentColor,
                navigationIconContentColor = contentColor
            )
        )
    }
}

@Composable
fun GuestProfileContent(
    onGoToLogin: () -> Unit,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkSurfaceVariant,
                        DarkSurface,
                        DarkBackground
                    )
                )
            )
    ) {
        //  沉浸式顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back", tint = Color.White)
            }
            IconButton(onClick = onSettingsClick) {
                Icon(CupertinoIcons.Default.Gearshape, contentDescription = "Settings", tint = Color.White)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo - 使用 3D 蓝色图标
            Surface(
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 16.dp,
                modifier = Modifier.size(100.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(com.android.purebilibili.R.mipmap.ic_launcher_3d)
                        .crossfade(true)
                        .build(),
                    contentDescription = "BiliPai",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "欢迎使用 BiliPai",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "登录后享受完整的 B站 体验",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            //  登录按钮 - 使用现代化渐变按钮
            BiliGradientButton(
                text = "安全登录",
                onClick = onGoToLogin,
                leadingIcon = CupertinoIcons.Outlined.PersonCropCircleBadgePlus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 安全提示
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(0.5f)
            ) {
                Icon(
                    CupertinoIcons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "支持扫码登录和网页登录",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun UserInfoSection(
    user: UserState, 
    centered: Boolean = false, 
    transparent: Boolean = false,
    onClick: () -> Unit = {} // [New]
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            //  修复：背景色 (支持透明)
            .background(if (transparent) Color.Transparent else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick) // [New] Make it clickable
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(FormatUtils.fixImageUrl(user.face)).crossfade(true).placeholder(android.R.color.darker_gray).build(),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        if (!centered) {
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                UserInfoText(user, forceWhite = transparent)
            }
        }
    }
    if (centered) {
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
            UserInfoText(user, centered = true)
        }
    }
}

@Composable
fun UserInfoText(user: UserState, centered: Boolean = false, forceWhite: Boolean = false) {
    //  修复：用户名颜色 + 阴影
    val contentColor = if (forceWhite) Color.White else MaterialTheme.colorScheme.onSurface
    val shadow = if (forceWhite) Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f) else null
    
    Text(
        text = user.name,
        style = MaterialTheme.typography.titleLarge.copy(
            shadow = shadow
        ),
        fontWeight = FontWeight.Bold,
        color = contentColor
    )
    Spacer(modifier = Modifier.height(8.dp)) // Increased spacing
    Row(verticalAlignment = Alignment.CenterVertically) {
        LevelTag(level = user.level)
        Spacer(modifier = Modifier.width(8.dp))
        if (user.isVip) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp)) {
                Text(user.vipLabel.ifEmpty { "大会员" }, fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                Text("正式会员", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
fun LevelTag(level: Int) {
    Surface(color = if (level >= 5) iOSOrange else iOSSystemGray, shape = RoundedCornerShape(2.dp)) {
        Text("LV$level", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
    }
}

@Composable
fun UserStatsSection(user: UserState, onFollowingClick: () -> Unit = {}, transparent: Boolean = false) {
    // 如果背景透明，文字强制为白色
    val textColor = if (transparent) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = if (transparent) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            //  修复：背景色 (支持透明)
            .background(if (transparent) Color.Transparent else MaterialTheme.colorScheme.surface)
            .padding(bottom = 8.dp), // [Modified] 减少底部间距，使下方服务紧贴 (16.dp -> 8.dp)
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatItem(count = FormatUtils.formatStat(user.dynamic.toLong()), label = "动态", textColor = textColor, labelColor = labelColor)
        StatItem(count = FormatUtils.formatStat(user.following.toLong()), label = "关注", onClick = onFollowingClick, textColor = textColor, labelColor = labelColor)
        StatItem(count = FormatUtils.formatStat(user.follower.toLong()), label = "粉丝", textColor = textColor, labelColor = labelColor)
        StatItem(count = FormatUtils.formatStat(user.coin.toLong()), label = "硬币", textColor = textColor, labelColor = labelColor)
    }
}


@Composable
fun StatItem(
    count: String, 
    label: String, 
    onClick: (() -> Unit)? = null, 
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    // Detect if we need shadow (heuristic: if text is white)
    val useShadow = textColor == Color.White
    // Stronger shadow for better legibility against bright backgrounds
    val shadow = if (useShadow) Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 4f) else null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier.clickable { onClick() }
        } else Modifier
    ) {
        //  修复：数字和标签颜色 + 阴影
        Text(
            text = count, 
            fontWeight = FontWeight.Bold, 
            fontSize = 18.sp, 
            color = textColor,
            style = LocalTextStyle.current.copy(shadow = shadow)
        )
        Text(
            text = label, 
            fontSize = 12.sp, 
            color = if (useShadow) Color.White.copy(alpha = 0.9f) else labelColor, // Whiter label
            style = LocalTextStyle.current.copy(shadow = shadow) // Apply same shadow to label
        )
    }
}

@Composable
fun VipBannerSection(user: UserState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            //  保持 VIP 金色，因为这是品牌色，不需要随深色模式变黑
            .background(Brush.horizontalGradient(colors = listOf(Color(0xFFFFEECC), Color(0xFFFFCC99))))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(if (user.isVip) "尊贵的大会员" else "成为大会员", color = Color(0xFF8B5A2B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("硬币: ${user.coin}   B币: ${user.bcoin}", color = Color(0xFF8B5A2B).copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Text(if (user.isVip) "续费 >" else "开通 >", color = Color(0xFF8B5A2B), fontSize = 12.sp)
        }
    }
}

@Composable
fun ServicesSection(
    onHistoryClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit = {},
    onWatchLaterClick: () -> Unit = {},
    onInboxClick: () -> Unit = {},  //  [新增] 私信入口
    onLogout: () -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color? = null,
    isLogin: Boolean = true,
    isTablet: Boolean = false // [New]
) {
    if (isTablet) {
        // [New] Grid Layout for Tablet
        val items = listOf(
            Triple("离线缓存", CupertinoIcons.Default.ArrowDownCircle, onDownloadClick),
            Triple("历史记录", CupertinoIcons.Default.Clock, onHistoryClick),
            Triple("我的收藏", CupertinoIcons.Default.Bookmark, onFavoriteClick),
            Triple("稍后再看", CupertinoIcons.Default.Bookmark, onWatchLaterClick),
            Triple("我的私信", CupertinoIcons.Default.Envelope, onInboxClick)  //  [新增]
        )
        
        // Simple Grid implementation since LazyVerticalGrid might be overkill inside a Column if not scrolling?
        // But tablet right pane has plenty space.
        // Let's use FlowRow for auto-wrapping or a simple Row/Column combo.
        // Actually, since it's a fixed list, a hardcoded Row/Column grid is safer than LazyGrid inside Scrollable.
        
        // 2 columns x N rows
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { (title, icon, onClick) ->
                        IOSGridItem(
                            icon = icon,
                            title = title,
                            onClick = onClick,
                            iconTint = contentColor, // Use content color for icon in this mode?
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), // Slightly distinct background
                            contentColor = contentColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty space if odd number
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

    } else {
        // [Original] List Layout for Mobile
    // [Modified] 移除标题，纯净悬浮岛风格 (Option 3)
    // IOSSectionTitle("我的服务")
    
    // [Modified] Custom Surface implementation to avoid tonalElevation overlay causing "outer background" issue
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .then(
                if (borderColor != null) {
                    Modifier.border(
                        width = 0.5.dp, 
                        color = borderColor, 
                        shape = RoundedCornerShape(24.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(24.dp)),
        color = containerColor,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp // Ensure no extra overlay
    ) {
        Column {
            IOSClickableItem(
                icon = CupertinoIcons.Default.ArrowDownCircle,
                title = "离线缓存",
                onClick = onDownloadClick,
                iconTint = MaterialTheme.colorScheme.primary,
                textColor = contentColor
            )
            IOSClickableItem(
                icon = CupertinoIcons.Default.Clock,
                title = "历史记录",
                onClick = onHistoryClick,
                iconTint = iOSBlue,
                textColor = contentColor
            )
            IOSClickableItem(
                icon = CupertinoIcons.Default.Bookmark,
                title = "我的收藏",
                onClick = onFavoriteClick,
                iconTint = iOSYellow,
                textColor = contentColor
            )
            IOSClickableItem(
                icon = CupertinoIcons.Default.Bookmark,
                title = "稍后再看",
                onClick = onWatchLaterClick,
                iconTint = iOSGreen,
                textColor = contentColor
            )
            IOSClickableItem(
                icon = CupertinoIcons.Default.Envelope,
                title = "我的私信",
                onClick = onInboxClick,
                iconTint = com.android.purebilibili.core.theme.iOSPink,  //  粉色图标
                textColor = contentColor
            )
            
            // [Merged] 退出登录 / 立即登录
            IOSClickableItem(
                title = if (isLogin) "退出登录" else "立即登录", // [New] Dynamic text
                onClick = onLogout,
                textColor = if (isLogin) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, // Red for logout, Blue for login
                centered = true,
                showChevron = false
            )
        }
    }
    }
}

/**
 * 个人空间三连彩蛋入口 - 图三样式
 * 显示点赞+投币+收藏三个图标，长按大拇指触发三连动画后弹窗选择
 */
@Composable
fun ProfileTripleActionEntry(
    onVipClick: () -> Unit,
    on4KClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 引入动画和手势相关
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressProgress by remember { mutableFloatStateOf(0f) }
    var showDialog by remember { mutableStateOf(false) }
    val progressDuration = 1500 // 1.5 秒
    
    val haptic = com.android.purebilibili.core.util.rememberHapticFeedback()
    
    // 进度动画
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLongPressing) 1f else 0f,
        animationSpec = if (isLongPressing) {
            androidx.compose.animation.core.tween(durationMillis = progressDuration, easing = androidx.compose.animation.core.LinearEasing)
        } else {
            androidx.compose.animation.core.tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        },
        label = "tripleProgress",
        finishedListener = { progress ->
            if (progress >= 1f && isLongPressing) {
                haptic(com.android.purebilibili.core.util.HapticType.MEDIUM)
                showDialog = true
                isLongPressing = false
            }
        }
    )
    
    LaunchedEffect(animatedProgress) {
        longPressProgress = animatedProgress
    }

    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            haptic(com.android.purebilibili.core.util.HapticType.LIGHT)
        }
    }
    
    // 选择弹窗
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("🎉 三连成功！") },
            text = { Text("请选择你想解锁的功能：") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onVipClick()
                }) {
                    Text("解锁大会员")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    on4KClick()
                }) {
                    Text("4K 画质")
                }
            }
        )
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isLongPressing = true
                        val released = tryAwaitRelease()
                        isLongPressing = false
                    }
                )
            },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 点赞图标 (带进度环)
        com.android.purebilibili.feature.video.ui.section.TripleProgressIcon(
            icon = CupertinoIcons.Outlined.HandThumbsup,
            text = "149",
            progress = longPressProgress,
            progressColor = MaterialTheme.colorScheme.primary,
            isActive = false
        )
        
        // 投币图标
        com.android.purebilibili.feature.video.ui.section.TripleProgressIcon(
            icon = com.android.purebilibili.core.ui.AppIcons.BiliCoin,
            text = "25",
            progress = longPressProgress,
            progressColor = Color(0xFFFFB300),
            isActive = false
        )
        
        // 收藏图标
        com.android.purebilibili.feature.video.ui.section.TripleProgressIcon(
            icon = CupertinoIcons.Outlined.Bookmark,
            text = "7",
            progress = longPressProgress,
            progressColor = Color(0xFFFFC107),
            isActive = false
        )
    }
}
