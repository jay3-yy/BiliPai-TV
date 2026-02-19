package com.android.purebilibili.feature.settings

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 *  开源许可证数据类
 */
data class OpenSourceLibrary(
    val name: String,
    val license: String,
    val url: String,
    val description: String = ""
)

/**
 *  所有使用的开源库列表
 */
val openSourceLibraries = listOf(
    // Jetpack & AndroidX
    OpenSourceLibrary(
        name = "Jetpack Compose",
        license = "Apache 2.0",
        url = "https://developer.android.com/jetpack/compose",
        description = "现代化 Android UI 框架"
    ),
    OpenSourceLibrary(
        name = "AndroidX Core KTX",
        license = "Apache 2.0",
        url = "https://developer.android.com/kotlin/ktx",
        description = "Kotlin 扩展库"
    ),
    OpenSourceLibrary(
        name = "AndroidX Media3 (ExoPlayer)",
        license = "Apache 2.0",
        url = "https://developer.android.com/media/media3",
        description = "音视频播放器框架"
    ),
    OpenSourceLibrary(
        name = "AndroidX Room",
        license = "Apache 2.0",
        url = "https://developer.android.com/training/data-storage/room",
        description = "SQLite 数据库 ORM"
    ),
    OpenSourceLibrary(
        name = "AndroidX DataStore",
        license = "Apache 2.0",
        url = "https://developer.android.com/topic/libraries/architecture/datastore",
        description = "偏好设置存储"
    ),
    
    // 网络请求
    OpenSourceLibrary(
        name = "OkHttp",
        license = "Apache 2.0",
        url = "https://square.github.io/okhttp/",
        description = "HTTP 客户端"
    ),
    OpenSourceLibrary(
        name = "Retrofit",
        license = "Apache 2.0",
        url = "https://square.github.io/retrofit/",
        description = "类型安全 REST API 客户端"
    ),
    
    // 图片加载
    OpenSourceLibrary(
        name = "Coil",
        license = "Apache 2.0",
        url = "https://coil-kt.github.io/coil/",
        description = "Kotlin 优先的图片加载库"
    ),
    
    // 序列化
    OpenSourceLibrary(
        name = "Kotlinx Serialization",
        license = "Apache 2.0",
        url = "https://github.com/Kotlin/kotlinx.serialization",
        description = "Kotlin 多平台序列化"
    ),
    
    // 弹幕
    OpenSourceLibrary(
        name = "DanmakuFlameMaster",
        license = "Apache 2.0",
        url = "https://github.com/bilibili/DanmakuFlameMaster",
        description = "B站弹幕引擎"
    ),
    
    // 动画
    OpenSourceLibrary(
        name = "Lottie Compose",
        license = "Apache 2.0",
        url = "https://airbnb.io/lottie/",
        description = "矢量动画库"
    ),
    
    // UI 效果
    OpenSourceLibrary(
        name = "Haze",
        license = "Apache 2.0",
        url = "https://github.com/chrisbanes/haze",
        description = "毛玻璃效果"
    ),
    OpenSourceLibrary(
        name = "Compose Shimmer",
        license = "Apache 2.0",
        url = "https://github.com/valentinilk/compose-shimmer",
        description = "Shimmer 加载动画"
    ),
    
    // 工具
    OpenSourceLibrary(
        name = "Accompanist",
        license = "Apache 2.0",
        url = "https://google.github.io/accompanist/",
        description = "Compose 辅助库"
    )
)

/**
 *  开源许可证页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("开源许可证", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "Back")
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
        //  [修复] 禁用 Scaffold 默认的 WindowInsets 消耗，避免底部填充
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "本应用使用了以下开源组件，感谢所有开源贡献者！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(openSourceLibraries) { library ->
                LicenseCard(
                    library = library,
                    onClick = { uriHandler.openUri(library.url) }
                )
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

/**
 *  单个许可证卡片
 */
@Composable
fun LicenseCard(
    library: OpenSourceLibrary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (library.description.isNotEmpty()) {
                    Text(
                        text = library.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = library.license,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Icon(
                CupertinoIcons.Default.ChevronForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
