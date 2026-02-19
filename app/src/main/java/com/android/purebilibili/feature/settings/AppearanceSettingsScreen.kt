@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.android.purebilibili.feature.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.core.store.SettingsManager
import coil.compose.AsyncImage
import com.android.purebilibili.core.theme.*
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.ui.blur.BlurIntensity
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.rememberIsTvDevice
import kotlinx.coroutines.launch
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.ui.animation.staggeredEntrance

/**
 *  外观设置二级页面
 * iOS 风格设计
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToBottomBarSettings: () -> Unit = {},  //  底栏设置导航

    onNavigateToIconSettings: () -> Unit = {},  //  [新增] 图标设置导航
    onNavigateToAnimationSettings: () -> Unit = {}  //  [新增] 动画设置导航
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    

    val displayLevel = when (state.displayMode) {
        0 -> 0.35f
        1 -> 0.6f
        else -> 0.85f
    }
    val appearanceInteractionLevel = (
        displayLevel +
            if (state.headerBlurEnabled) 0.1f else 0f +
            if (state.isBottomBarFloating) 0.1f else 0f
        ).coerceIn(0f, 1f)
    val appearanceAnimationSpeed = if (state.dynamicColor) 1.1f else 1f
    
    //  [修复] 设置导航栏透明，确保底部手势栏沉浸式效果
    val view = androidx.compose.ui.platform.LocalView.current
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
        topBar = {
            TopAppBar(
                title = { Text("外观设置", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        //  [修复] 禁用 Scaffold 默认的 WindowInsets 消耗，避免底部填充
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        AppearanceSettingsContent(
            modifier = Modifier.padding(padding),
            state = state,
            onNavigateToIconSettings = onNavigateToIconSettings,
            onNavigateToAnimationSettings = onNavigateToAnimationSettings,
            viewModel = viewModel,
            context = context
        )
    }
}



@Composable
fun AppearanceSettingsContent(
    modifier: Modifier = Modifier,
    state: SettingsUiState,
    onNavigateToIconSettings: () -> Unit,
    onNavigateToAnimationSettings: () -> Unit,
    viewModel: SettingsViewModel,
    context: android.content.Context
) {
    // Animation Trigger
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600 // Material Design 3 中型屏幕断点
    val isTvDevice = rememberIsTvDevice()
    val isTvPerformanceProfileEnabled by SettingsManager.getTvPerformanceProfileEnabled(context).collectAsState(
        initial = isTvDevice
    )
    val windowSizeClass = LocalWindowSizeClass.current
    val deviceUiProfile = remember(isTvDevice, windowSizeClass.widthSizeClass, isTvPerformanceProfileEnabled) {
        resolveDeviceUiProfile(
            isTv = isTvDevice,
            widthSizeClass = windowSizeClass.widthSizeClass,
            tvPerformanceProfileEnabled = isTvPerformanceProfileEnabled
        )
    }
    val effectiveMotionTier = remember(deviceUiProfile.motionTier, state.cardAnimationEnabled) {
        resolveEffectiveMotionTier(
            baseTier = deviceUiProfile.motionTier,
            animationEnabled = state.cardAnimationEnabled
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        //  [修复] 添加底部导航栏内边距，确保沉浸式效果
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {
        
        //  主题与颜色
        item { 
            Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                IOSSectionTitle("主题与颜色") 
            }
        }
        item {
            Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                IOSGroup {
                    // 主题模式选择 (横向卡片)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                val isSelected = state.themeMode == mode
                                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color)
                                        .clickable { viewModel.setThemeMode(mode) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // 动态取色开关
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                             IOSSwitchItem(
                                icon = CupertinoIcons.Default.PaintbrushPointed,
                                title = "Material You",
                                subtitle = "跟随系统壁纸变换应用主题色",
                                checked = state.dynamicColor,
                                onCheckedChange = { viewModel.toggleDynamicColor(it) },
                                iconTint = iOSPink
                            )
                        }

                        // 主题色选择 (仅当动态取色关闭时显示)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !state.dynamicColor,
                            enter =   androidx.compose.animation.expandVertically() +   androidx.compose.animation.fadeIn(),
                            exit =   androidx.compose.animation.shrinkVertically() +   androidx.compose.animation.fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                //  Theme Color Label
                                Text(
                                    "主题色", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                //  [新增] 实时主题色预览
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp)
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(
                                                    ThemeColors[state.themeColorIndex].copy(alpha = 0.15f),
                                                    ThemeColors[state.themeColorIndex].copy(alpha = 0.05f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = ThemeColors[state.themeColorIndex].copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // 模拟应用图标/Logo
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .padding(bottom = 12.dp)
                                                .background(
                                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                        colors = listOf(
                                                            ThemeColors[state.themeColorIndex],
                                                            ThemeColors[state.themeColorIndex].copy(alpha = 0.8f)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                CupertinoIcons.Filled.Play,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        
                                        // 当前选中颜色名称
                                        Text(
                                            text = ThemeColorNames.getOrElse(state.themeColorIndex) { "自定义" },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "正在预览当前主题色",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                //  [Redesign] Theme Color Grid - Strict 2 Rows x 5 Columns
                                val spacing = 12.dp
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp) // 增加行间距以容纳文字
                                ) {
                                    ThemeColors.chunked(5).forEach { rowColors ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(spacing)
                                        ) {
                                            rowColors.forEach { color ->
                                                val index = ThemeColors.indexOf(color)
                                                val isSelected = state.themeColorIndex == index
                                                
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    // 选中状态动画
                                                    val scale by androidx.compose.animation.core.animateFloatAsState(
                                                        targetValue = if (isSelected) 1.1f else 1.0f,
                                                        label = "scale",
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                    )
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .aspectRatio(1f) // Ensure square aspect ratio for perfect circles
                                                            .graphicsLayer {
                                                                scaleX = scale
                                                                scaleY = scale
                                                            }
                                                            // 选中时的外光环 (圆形)
                                                            .border(
                                                                width = if (isSelected) 2.dp else 0.dp,
                                                                color = if (isSelected) color.copy(alpha = 0.5f) else Color.Transparent,
                                                                shape = CircleShape
                                                            )
                                                            .padding(3.dp) // 光环与色块的间距
                                                            .clip(CircleShape) // 裁剪为圆形
                                                            .background(
                                                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                                                    colors = listOf(
                                                                        color.copy(alpha = 0.9f), // 中心稍亮
                                                                        color // 边缘原色
                                                                    ),
                                                                    center = androidx.compose.ui.geometry.Offset.Unspecified,
                                                                    radius = Float.POSITIVE_INFINITY
                                                                )
                                                            )
                                                            // 添加个内部高光，增加球体质感
                                                            .background(
                                                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                                                    colors = listOf(
                                                                        Color.White.copy(alpha = 0.2f),
                                                                        Color.Transparent
                                                                    ),
                                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                                    end = androidx.compose.ui.geometry.Offset(100f, 100f)
                                                                )
                                                            )
                                                            .clickable { 
                                                                viewModel.setThemeColorIndex(index)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.animation.AnimatedVisibility(
                                                            visible = isSelected,
                                                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                                                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                                                        ) {
                                                            Icon(
                                                                CupertinoIcons.Default.Checkmark,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    // 颜色名称
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = ThemeColorNames.getOrElse(index) { "" },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            
                                            // Fill empty spots if last row has fewer than 5 items
                                            if (rowColors.size < 5) {
                                                repeat(5 - rowColors.size) {
                                                     Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        //  启动画面
        item { 
            Box(modifier = Modifier.staggeredEntrance(2, isVisible, motionTier = effectiveMotionTier)) {
                IOSSectionTitle("启动画面") 
            }
        }
        item {
            Box(modifier = Modifier.staggeredEntrance(3, isVisible, motionTier = effectiveMotionTier)) {
                IOSGroup {
                    val isSplashEnabled by com.android.purebilibili.core.store.SettingsManager.isSplashEnabled(context).collectAsState(initial = false)
                    val splashWallpaperUri by com.android.purebilibili.core.store.SettingsManager.getSplashWallpaperUri(context).collectAsState(initial = null)
                    
                    // 开关项
                    IOSSwitchItem(
                        icon = CupertinoIcons.Default.Photo,
                        title = "使用开屏壁纸",
                        subtitle = "应用启动时显示所选官方壁纸",
                        checked = isSplashEnabled,
                        onCheckedChange = { viewModel.toggleSplashEnabled(it) },
                        iconTint = com.android.purebilibili.core.theme.iOSBlue
                    )
                    
                    // 当开启时，显示选择壁纸入口
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSplashEnabled,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column {
                            Divider()
                            
                            var showWallpaperPicker by remember { mutableStateOf(false) }
                            
                            // 选择壁纸按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWallpaperPicker = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 壁纸缩略图预览
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (splashWallpaperUri != null) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(context)
                                                .data(splashWallpaperUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                CupertinoIcons.Default.Photo,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "选择开屏壁纸",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (splashWallpaperUri != null) "已设置自定义壁纸" else "从官方壁纸库中选择",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Icon(
                                    CupertinoIcons.Default.ChevronForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            // 壁纸选择 Sheet
                            if (showWallpaperPicker) {
                                com.android.purebilibili.feature.profile.SplashWallpaperPickerSheet(
                                    onDismiss = { showWallpaperPicker = false }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        //  个性化
        item { 
            Box(modifier = Modifier.staggeredEntrance(4, isVisible, motionTier = effectiveMotionTier)) {
                IOSSectionTitle("个性化") 
            }
        }
        item {
            Box(modifier = Modifier.staggeredEntrance(5, isVisible, motionTier = effectiveMotionTier)) {
                IOSGroup {
                    // 图标设置
                    IOSClickableItem(
                        icon = CupertinoIcons.Default.SquareStack3dUp,
                        title = "应用图标",
                        value = when(state.appIcon) {
                            // 🎀 二次元少女系列
                            "Yuki" -> "比心少女"
                            "Anime", "icon_anime" -> "蓝发电视"
                            "Tv" -> "双马尾"
                            "Headphone" -> "耳机少女"
                            // 经典系列
                            "3D", "icon_3d" -> "3D立体"
                            "Blue", "icon_blue" -> "经典蓝"
                            "Retro", "icon_retro" -> "复古怀旧"
                            "Flat", "icon_flat" -> "扁平现代"
                            "Flat Material" -> "扁平材质"
                            "Neon", "icon_neon" -> "霓虹"
                            "Telegram Blue", "icon_telegram_blue" -> "纸飞机蓝"
                            "Pink", "icon_telegram_pink" -> "樱花粉"
                            "Purple", "icon_telegram_purple" -> "香芋紫"
                            "Green", "icon_telegram_green" -> "薄荷绿"
                            "Dark", "icon_telegram_dark" -> "暗夜蓝"
                            else -> "3D立体"  // 默认显示 3D立体 (对应默认 icon_3d)
                        },
                        onClick = onNavigateToIconSettings,
                        iconTint = iOSPurple
                    )
                    Divider()
                    // 动画设置
                    IOSClickableItem(
                        icon = CupertinoIcons.Default.WandAndStars,
                        title = "动画与效果",
                        value = if (state.cardAnimationEnabled) "已开启" else "已关闭",
                        onClick = onNavigateToAnimationSettings,
                        iconTint = iOSPink
                    )

                    Divider()
                    // 触感反馈
                    IOSSwitchItem(
                        icon = CupertinoIcons.Default.HandTap,
                        title = "触感反馈",
                        checked = state.hapticFeedbackEnabled,
                        onCheckedChange = { viewModel.toggleHapticFeedback(it) },
                        iconTint = iOSBlue
                    )
                }
            }
        } // End of Personalization item

            //  [新增] 平板设置 (仅平板显示)
            if (isTablet) {
                item {
                    Box(modifier = Modifier.staggeredEntrance(8, isVisible, motionTier = effectiveMotionTier)) {
                        IOSSectionTitle("平板布局")
                    }
                }
                item {
                    Box(modifier = Modifier.staggeredEntrance(9, isVisible, motionTier = effectiveMotionTier)) {
                        IOSGroup {
                            IOSSwitchItem(
                                icon = CupertinoIcons.Outlined.SidebarLeft,
                                title = "侧边导航栏",
                                subtitle = "开启后使用侧边栏代替底部导航",
                                checked = state.tabletUseSidebar,
                                onCheckedChange = { viewModel.toggleTabletUseSidebar(it) },
                                iconTint = iOSBlue
                            )
                        }
                    }
                }
            }
        
            //  首页展示 - 抽屉式选择
            item { 
                Box(modifier = Modifier.staggeredEntrance(6, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("首页展示") 
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(7, isVisible, motionTier = effectiveMotionTier)) {
                    IOSGroup {
                        val displayMode = state.displayMode
                        var isExpanded by remember { mutableStateOf(false) }
                        
                        // 当前选中模式的名称
                        val currentModeName = DisplayMode.entries.find { it.value == displayMode }?.title ?: "双列网格"
                        
                        Column(modifier = Modifier.padding(16.dp)) {
                            // 标题行 - 可点击展开/收起
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    CupertinoIcons.Default.SquareOnSquare,
                                    contentDescription = null,
                                    tint = iOSBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "展示样式",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentModeName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            // 展开后的选项 - 带动画
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isExpanded,
                                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DisplayMode.entries.forEach { mode ->
                                        val isSelected = displayMode == mode.value
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .clickable {
                                                    viewModel.setDisplayMode(mode.value)
                                                    isExpanded = false
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    mode.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                            else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    mode.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    CupertinoIcons.Default.Checkmark,
                                                    contentDescription = "已选择",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(start = 16.dp))
                        IOSSwitchItem(
                            icon = CupertinoIcons.Default.ChevronUp,
                            title = "顶部栏自动收缩",
                            subtitle = "上滑时自动收起推荐分类",
                            checked = state.isHeaderCollapseEnabled,
                            onCheckedChange = { viewModel.toggleHeaderCollapse(it) },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue
                        )
                        
                        // 网格列数设置 (仅在双列网格模式下显示)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isTablet && state.displayMode == 0,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column {
                                Divider(modifier = Modifier.padding(start = 16.dp))
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    ) {
                                        Icon(
                                            CupertinoIcons.Default.ListBullet,
                                            contentDescription = null,
                                            tint = com.android.purebilibili.core.theme.iOSBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "网格列数",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (state.gridColumnCount == 0) "自适应 (默认)" else "固定 ${state.gridColumnCount} 列",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    // 列数选择器
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        item {
                                            // 自动
                                            val isSelected = state.gridColumnCount == 0
                                            Box(
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { viewModel.setGridColumnCount(0) }
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "自动",
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                        items(6) { i ->
                                            val count = i + 1
                                            val isSelected = state.gridColumnCount == count
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp) // Square for numbers
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { viewModel.setGridColumnCount(count) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        

    }
}


/**
 *  动态取色预览组件
 * 显示从壁纸提取的 Material You 颜色
 */


@Composable
fun DynamicColorPreview() {
    val colorScheme = MaterialTheme.colorScheme
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "当前取色预览",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Primary
            ColorPreviewItem(
                color = colorScheme.primary,
                label = "主色",
                modifier = Modifier.weight(1f)
            )
            // Secondary
            ColorPreviewItem(
                color = colorScheme.secondary,
                label = "辅色",
                modifier = Modifier.weight(1f)
            )
            // Tertiary
            ColorPreviewItem(
                color = colorScheme.tertiary,
                label = "第三色",
                modifier = Modifier.weight(1f)
            )
            // Primary Container
            ColorPreviewItem(
                color = colorScheme.primaryContainer,
                label = "容器",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ColorPreviewItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
