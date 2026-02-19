package com.android.purebilibili.feature.download

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 *  离线缓存列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,  // bvid - 在线播放
    onOfflineVideoClick: (String) -> Unit = {}  // 🔧 [新增] taskId - 离线播放
) {
    val context = LocalContext.current
    val tasks by DownloadManager.tasks.collectAsState()
    val taskList = tasks.values.toList().sortedByDescending { it.createdAt }
    
    // 🔧 检测网络状态
    val connectivityManager = remember {
        context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    }
    
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("离线缓存") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (taskList.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无缓存视频",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "在视频详情页点击「缓存」按钮下载",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(taskList, key = { it.id }) { task ->
                    DownloadTaskItem(
                        task = task,
                        onClick = { 
                            if (task.isComplete) {
                                // 🔧 [修复] 根据网络状态选择播放方式
                                if (isNetworkAvailable()) {
                                    // 有网络：打开在线视频详情（可以加载评论等）
                                    onVideoClick(task.bvid)
                                } else {
                                    // 无网络：直接播放本地文件
                                    onOfflineVideoClick(task.id)
                                }
                            }
                        },
                        onPauseResume = {
                            if (task.isDownloading) {
                                DownloadManager.pauseDownload(task.id)
                            } else if (task.canResume) {
                                DownloadManager.startDownload(task.id)
                            }
                        },
                        onDelete = {
                            DownloadManager.removeTask(task.id)
                        }
                    )
                }
                
                // [新增] 显示当前存储路径
                item {
                    val currentDir = remember { DownloadManager.getDownloadDir().absolutePath }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "存储位置",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentDir,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun DownloadTaskItem(
    task: DownloadTask,
    onClick: () -> Unit,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // 🖼️ [修复] 优先使用本地封面（无网络时也能显示）
                val localCoverFile = task.localCoverPath?.let { java.io.File(it) }
                val coverSource = if (localCoverFile?.exists() == true) {
                    // 使用本地缓存的封面
                    localCoverFile
                } else {
                    // Fallback 到网络URL
                    val coverUrl = task.cover.let { url ->
                        if (url.startsWith("http://")) url.replace("http://", "https://")
                        else url
                    }
                    coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(coverUrl)
                        .addHeader("Referer", "https://www.bilibili.com")
                        .crossfade(true)
                        .build()
                }
                
                AsyncImage(
                    model = coverSource,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 进度/状态覆盖层
                if (!task.isComplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (task.status) {
                            DownloadStatus.DOWNLOADING, DownloadStatus.MERGING -> {
                                CircularProgressIndicator(
                                    progress = { task.progress },
                                    modifier = Modifier.size(32.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                            DownloadStatus.PAUSED -> {
                                Text("已暂停", color = Color.White, fontSize = 12.sp)
                            }
                            DownloadStatus.FAILED -> {
                                Text("失败", color = com.android.purebilibili.core.theme.iOSRed, fontSize = 12.sp)
                            }
                            else -> {}
                        }
                    }
                }
                
                // 画质标签
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.qualityDesc,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = task.ownerName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 状态文字
                val statusText = when (task.status) {
                    DownloadStatus.PENDING -> "等待中..."
                    DownloadStatus.DOWNLOADING -> "下载中 ${(task.progress * 100).toInt()}%"
                    DownloadStatus.MERGING -> "处理中..."
                    DownloadStatus.COMPLETED -> "已完成"
                    DownloadStatus.PAUSED -> "已暂停"
                    DownloadStatus.FAILED -> task.errorMessage ?: "下载失败"
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = when (task.status) {
                        DownloadStatus.COMPLETED -> Color(0xFF4CAF50)
                        DownloadStatus.FAILED -> com.android.purebilibili.core.theme.iOSRed
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            
            // 操作按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 暂停/继续
                if (task.isDownloading || task.canResume) {
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            imageVector = if (task.isDownloading) CupertinoIcons.Default.Pause else CupertinoIcons.Default.Play,
                            contentDescription = if (task.isDownloading) "暂停" else "继续",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // 删除
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = CupertinoIcons.Default.Trash,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
