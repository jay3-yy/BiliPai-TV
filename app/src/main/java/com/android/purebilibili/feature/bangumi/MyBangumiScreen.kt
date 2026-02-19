// 文件路径: feature/bangumi/MyBangumiScreen.kt
package com.android.purebilibili.feature.bangumi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.FollowBangumiItem

/**
 * 我的追番列表组件
 */
@Composable
fun MyBangumiContent(
    myFollowState: MyFollowState,
    followStats: MyFollowStats,
    followType: Int,
    onFollowTypeChange: (Int) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onBangumiClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val followLabel = if (followType == MY_FOLLOW_TYPE_CINEMA) "追剧" else "追番"
    val loadedItems = (myFollowState as? MyFollowState.Success)?.items.orEmpty()
    val loadedCount = loadedItems.size
    val statsDetail = remember(followStats, followType, loadedCount) {
        buildMyFollowStatsDetail(
            stats = followStats,
            currentType = followType,
            loadedCount = loadedCount
        )
    }
    val watchInsight = remember(loadedItems) {
        buildMyFollowWatchInsight(loadedItems)
    }

    Column(modifier = modifier.fillMaxSize()) {
        MyFollowSummarySection(
            stats = followStats,
            currentType = followType,
            statsDetail = statsDetail,
            watchInsight = watchInsight
        )

        MyFollowTypeTabs(
            selectedType = followType,
            onTypeChange = onFollowTypeChange
        )

        when (myFollowState) {
            is MyFollowState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MyFollowState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = myFollowState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("重试")
                        }
                    }
                }
            }
            is MyFollowState.Success -> {
                if (myFollowState.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "还没有${followLabel}哦",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (followType == MY_FOLLOW_TYPE_CINEMA) "去发现喜欢的影视吧~" else "去发现喜欢的番剧吧~",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    MyFollowGrid(
                        items = myFollowState.items,
                        followType = followType,
                        hasMore = myFollowState.hasMore,
                        total = myFollowState.total,
                        onLoadMore = onLoadMore,
                        onItemClick = onBangumiClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MyFollowSummarySection(
    stats: MyFollowStats,
    currentType: Int,
    statsDetail: MyFollowStatsDetail,
    watchInsight: MyFollowWatchInsight
) {
    var showDetail by rememberSaveable(currentType) { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FollowSummaryCard(
                    title = "追番",
                    value = stats.bangumiTotal.toString(),
                    active = currentType == MY_FOLLOW_TYPE_BANGUMI,
                    modifier = Modifier.weight(1f)
                )
                FollowSummaryCard(
                    title = "追剧",
                    value = stats.cinemaTotal.toString(),
                    active = currentType == MY_FOLLOW_TYPE_CINEMA,
                    modifier = Modifier.weight(1f)
                )
                FollowSummaryCard(
                    title = "总计",
                    value = stats.total.toString(),
                    subtitle = "已加载 ${watchInsight.loadedCount}",
                    active = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                onClick = { showDetail = !showDetail }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showDetail) CupertinoIcons.Default.ChevronDown else CupertinoIcons.Default.ChevronForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "统计详情",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (currentType == MY_FOLLOW_TYPE_CINEMA) "追剧" else "追番",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = showDetail) {
                MyFollowStatsDetailPanel(
                    statsDetail = statsDetail,
                    watchInsight = watchInsight
                )
            }
        }
    }
}

@Composable
private fun FollowSummaryCard(
    title: String,
    value: String,
    subtitle: String? = null,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val cardColor = if (active) {
        activeColor.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }

    Surface(
        modifier = modifier.heightIn(min = 88.dp),
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (active) 1.4.dp else 1.dp,
            color = if (active) activeColor.copy(alpha = 0.38f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MyFollowStatsDetailPanel(
    statsDetail: MyFollowStatsDetail,
    watchInsight: MyFollowWatchInsight
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "已加载 ${statsDetail.loadedCount}/${statsDetail.currentTypeTotal}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { statsDetail.loadedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailMetricChip("在看", watchInsight.inProgressCount.toString(), Modifier.weight(1f))
                DetailMetricChip("已看完", watchInsight.completedCount.toString(), Modifier.weight(1f))
                DetailMetricChip("待开看", watchInsight.notStartedCount.toString(), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailMetricChip("更新中", watchInsight.updatedCount.toString(), Modifier.weight(1f))
                DetailMetricChip("会员专享", watchInsight.membershipOnlyCount.toString(), Modifier.weight(1f))
                DetailMetricChip("平均进度", "${watchInsight.averageProgressPercent}%", Modifier.weight(1f))
            }
            if (watchInsight.estimatedTotalEpisodes > 0) {
                Text(
                    text = "已追集数 ${watchInsight.estimatedWatchedEpisodes}/${watchInsight.estimatedTotalEpisodes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailMetricChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MyFollowGrid(
    items: List<FollowBangumiItem>,
    followType: Int,
    hasMore: Boolean,
    total: Int,
    onLoadMore: () -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    
    // 加载更多检测
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
    
    Column(modifier = modifier.fillMaxSize()) {
        // 统计信息
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (followType == MY_FOLLOW_TYPE_CINEMA) "共 $total 部追剧" else "共 $total 部追番",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 8.dp,
                end = 12.dp,
                bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()  //  添加导航栏内边距
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = items,
                key = { it.seasonId }
            ) { item ->
                MyFollowCard(
                    item = item,
                    onClick = { onItemClick(item.seasonId) }
                )
            }
            
            // 加载更多指示器
            if (hasMore) {
                item(span = { GridItemSpan(3) }) {
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
}

@Composable
private fun MyFollowTypeTabs(
    selectedType: Int,
    onTypeChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FollowTypeTab(
            text = "追番",
            selected = selectedType == MY_FOLLOW_TYPE_BANGUMI,
            onClick = { onTypeChange(MY_FOLLOW_TYPE_BANGUMI) },
            modifier = Modifier.weight(1f)
        )
        FollowTypeTab(
            text = "追剧",
            selected = selectedType == MY_FOLLOW_TYPE_CINEMA,
            onClick = { onTypeChange(MY_FOLLOW_TYPE_CINEMA) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FollowTypeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MyFollowCard(
    item: FollowBangumiItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = FormatUtils.fixImageUrl(item.cover),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 100f
                        )
                    )
            )
            
            // 角标（会员专享等）
            if (item.badge.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.badge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 更新状态标记
            if (item.newEp?.indexShow?.contains("更新") == true) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "更新",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 底部信息
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                // 观看进度
                if (item.progress.isNotEmpty()) {
                    Text(
                        text = item.progress,
                        color = iOSYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 更新状态
                item.newEp?.indexShow?.let { indexShow ->
                    Text(
                        text = indexShow,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            // 播放按钮 (悬浮)
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        CupertinoIcons.Default.Play,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // 标题
        Text(
            text = item.title,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}
