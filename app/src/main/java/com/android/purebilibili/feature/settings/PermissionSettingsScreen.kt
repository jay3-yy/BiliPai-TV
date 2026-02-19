// 文件路径: feature/settings/PermissionSettingsScreen.kt
package com.android.purebilibili.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.filled.Tv
import io.github.alexzhirkevich.cupertino.icons.filled.Location
import io.github.alexzhirkevich.cupertino.icons.filled.XmarkCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.ui.animation.staggeredEntrance
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.core.theme.iOSPink  // 存储权限图标色
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal

/**
 *  权限管理页面
 * 显示应用所有权限的用途说明和当前状态
 */
/**
 *  权限管理页面内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSettingsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("权限管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        PermissionSettingsContent(
            modifier = Modifier.padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTvDevice = rememberIsTvDevice()
    val isTvPerformanceProfileEnabled by SettingsManager.getTvPerformanceProfileEnabled(context).collectAsState(
        initial = isTvDevice
    )
    val windowSizeClass = LocalWindowSizeClass.current
    val cardAnimationEnabled by SettingsManager.getCardAnimationEnabled(context).collectAsState(initial = false)
    val deviceUiProfile = remember(isTvDevice, windowSizeClass.widthSizeClass, isTvPerformanceProfileEnabled) {
        resolveDeviceUiProfile(
            isTv = isTvDevice,
            widthSizeClass = windowSizeClass.widthSizeClass,
            tvPerformanceProfileEnabled = isTvPerformanceProfileEnabled
        )
    }
    val effectiveMotionTier = remember(deviceUiProfile.motionTier, cardAnimationEnabled) {
        resolveEffectiveMotionTier(
            baseTier = deviceUiProfile.motionTier,
            animationEnabled = cardAnimationEnabled
        )
    }
    
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
    
    // 权限列表数据
    val permissions = remember {
        listOf(
            PermissionInfo(
                name = "网络访问",
                permission = Manifest.permission.INTERNET,
                description = "加载视频、图片和用户数据",
                icon = CupertinoIcons.Default.Wifi,
                iconTint = iOSBlue,
                isNormal = true,
                alwaysGranted = true
            ),
            PermissionInfo(
                name = "网络状态",
                permission = Manifest.permission.ACCESS_NETWORK_STATE,
                description = "检测网络连接状态，优化加载体验",
                icon = CupertinoIcons.Default.ChartBar,
                iconTint = iOSGreen,
                isNormal = true,
                alwaysGranted = true
            ),
            PermissionInfo(
                name = "通知权限",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    "android.permission.POST_NOTIFICATIONS"
                },
                description = "显示媒体播放控制通知，方便后台控制播放",
                icon = CupertinoIcons.Default.Bell,
                iconTint = iOSOrange,
                isNormal = false,
                alwaysGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ),
            PermissionInfo(
                name = "前台服务",
                permission = Manifest.permission.FOREGROUND_SERVICE,
                description = "支持后台播放视频时保持服务运行",
                icon = CupertinoIcons.Default.PlayCircle,
                iconTint = iOSPurple,
                isNormal = true,
                alwaysGranted = true
            ),
            PermissionInfo(
                name = "媒体播放服务",
                permission = "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                description = "允许应用在后台继续播放视频",
                icon = CupertinoIcons.Default.MusicNote,
                iconTint = iOSTeal,
                isNormal = true,
                alwaysGranted = true
            ),
            //  DLNA 投屏所需权限
            PermissionInfo(
                name = "设备发现 (DLNA)",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.NEARBY_WIFI_DEVICES
                } else {
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
                description = "用于扫描和连接附近的投屏设备 (DLNA)",
                icon = CupertinoIcons.Default.Tv,
                iconTint = iOSBlue,
                isNormal = false,
                alwaysGranted = false
            ),
             //  存储权限 (Android 11+ 所有文件访问 / Android 9- 读写 / Android 10 自动)
            PermissionInfo(
                name = "存储空间",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                } else {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                description = "用于下载视频和保存图片到自定义目录",
                icon = CupertinoIcons.Default.Folder,
                iconTint = iOSPink,
                isNormal = false,
                alwaysGranted = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q, // Android 10 也是 scoped storage，通常无需权限
                customCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    { Environment.isExternalStorageManager() }
                } else {
                    null
                }
            ),

        )
    }
    
    // 检查权限状态
    var permissionStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        permissionStates = permissions.associate { info ->
            info.permission to if (info.alwaysGranted) {
                true
            } else {
                info.customCheck?.invoke(context)
                    ?: (ContextCompat.checkSelfPermission(context, info.permission) == PackageManager.PERMISSION_GRANTED)
            }
        }
        isVisible = true
    }
    val grantedCount = permissions.count { info ->
        info.alwaysGranted || permissionStates[info.permission] == true
    }
    val permissionInteractionLevel = (
        0.2f + grantedCount.toFloat() / permissions.size.coerceAtLeast(1) * 0.8f
        ).coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        //  [修复] 添加底部导航栏内边距，确保沉浸式效果
        contentPadding = WindowInsets.navigationBars.asPaddingValues()
    ) {

            
            
            // 说明文字
            item {
                Box(modifier = Modifier.staggeredEntrance(0, isVisible, motionTier = effectiveMotionTier)) {
                    Text(
                        text = "以下是应用所需的权限及其用途说明。普通权限在安装时自动授予，无需手动操作。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
            
            // 需要运行时请求的权限
            item {
                Box(modifier = Modifier.staggeredEntrance(1, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("需要授权的权限")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(2, isVisible, motionTier = effectiveMotionTier)) {
                    IOSGroup {
                        permissions.filter { !it.isNormal }.forEachIndexed { index, info ->
                            if (index > 0) HorizontalDivider()
                            PermissionItem(
                                info = info,
                                isGranted = permissionStates[info.permission] ?: false,
                                onOpenSettings = {
                                    // Android 11+ 存储权限跳转到专属设置页
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                        info.permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE
                                    ) {
                                        try {
                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            openAppSettings(context)
                                        }
                                    } else {
                                        openAppSettings(context)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // 普通权限（自动授予）
            item {
                Box(modifier = Modifier.staggeredEntrance(3, isVisible, motionTier = effectiveMotionTier)) {
                    IOSSectionTitle("自动授予的权限")
                }
            }
            item {
                Box(modifier = Modifier.staggeredEntrance(4, isVisible, motionTier = effectiveMotionTier)) {
                    IOSGroup {
                        permissions.filter { it.isNormal }.forEachIndexed { index, info ->
                            if (index > 0) HorizontalDivider()
                            PermissionItem(
                                info = info,
                                isGranted = true,
                                onOpenSettings = null
                            )
                        }
                    }
                }
            }
            
            // 隐私说明
            item {
                Box(modifier = Modifier.staggeredEntrance(5, isVisible, motionTier = effectiveMotionTier)) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "BiliPai 仅在必要功能的前提下申请部分敏感权限。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }


/**
 * 权限信息数据类
 */
private data class PermissionInfo(
    val name: String,
    val permission: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val isNormal: Boolean,  // 是否是普通权限（自动授予）
    val alwaysGranted: Boolean = false,  // 是否总是被授予
    val customCheck: ((Context) -> Boolean)? = null // 自定义检查逻辑
)

/**
 * 单个权限项
 */
@Composable
private fun PermissionItem(
    info: PermissionInfo,
    isGranted: Boolean,
    onOpenSettings: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onOpenSettings != null) { onOpenSettings?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(info.iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                info.icon,
                contentDescription = null,
                tint = info.iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // 名称和描述
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 状态指示器
        if (isGranted) {
            Icon(
                CupertinoIcons.Default.CheckmarkCircle,
                contentDescription = "已授权",
                tint = iOSGreen,
                modifier = Modifier.size(22.dp)
            )
        } else {
            // 未授权时显示红色的 X
            Icon(
                CupertinoIcons.Default.XmarkCircle,
                contentDescription = "未授权",
                tint = com.android.purebilibili.core.theme.iOSRed,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * 打开应用设置页面
 */
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
