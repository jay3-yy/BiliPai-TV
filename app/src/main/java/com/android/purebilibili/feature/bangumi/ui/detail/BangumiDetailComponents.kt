// 文件路径: feature/bangumi/ui/detail/BangumiDetailComponents.kt
package com.android.purebilibili.feature.bangumi.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Star
import io.github.alexzhirkevich.cupertino.icons.outlined.Ellipsis
import io.github.alexzhirkevich.cupertino.icons.outlined.Plus
import io.github.alexzhirkevich.cupertino.icons.outlined.Checkmark
import com.android.purebilibili.core.theme.iOSYellow
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiEpisode
import com.android.purebilibili.data.model.response.SeasonInfo

/**
 * 番剧详情头部组件 - 手机端
 */
@Composable
fun BangumiDetailHeader(
    detail: BangumiDetail,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // 封面背景（模糊）
        AsyncImage(
            model = FormatUtils.fixImageUrl(detail.cover),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        // 信息区域
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 封面图
            AsyncImage(
                model = FormatUtils.fixImageUrl(detail.cover),
                contentDescription = detail.title,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 标题和信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 评分
                detail.rating?.let { rating ->
                    if (rating.score > 0) {
                        RatingRow(score = rating.score, count = rating.count)
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 更新状态
                detail.newEp?.desc?.let { desc ->
                    Text(
                        text = desc,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 播放量
                detail.stat?.let { stat ->
                    Text(
                        text = "${FormatUtils.formatStat(stat.views)}播放 · ${FormatUtils.formatStat(stat.favorites)}追番",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * 评分行组件
 */
@Composable
fun RatingRow(
    score: Float,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            CupertinoIcons.Default.Star,
            contentDescription = null,
            tint = iOSYellow,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format("%.1f", score),
            color = iOSYellow,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = " (${count}人评分)",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

/**
 * 追番按钮组件
 */
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onToggle,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            ),
            modifier = modifier
        ) {
            Icon(
                CupertinoIcons.Default.Checkmark,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("已追番")
        }
    } else {
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = modifier
        ) {
            Icon(
                CupertinoIcons.Default.Plus,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("追番")
        }
    }
}

/**
 * 季度切换选择器
 */
@Composable
fun SeasonSelector(
    seasons: List<SeasonInfo>,
    currentSeasonId: Long,
    onSeasonClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (seasons.size <= 1) return
    
    Column(modifier = modifier) {
        Text(
            text = "相关季度",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(seasons) { season ->
                val isCurrentSeason = season.seasonId == currentSeasonId
                Surface(
                    modifier = Modifier.clickable {
                        if (!isCurrentSeason) {
                            onSeasonClick(season.seasonId)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrentSeason) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = season.seasonTitle.ifEmpty { season.title },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        color = if (isCurrentSeason) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

/**
 * 选集 Chip 组件
 */
@Composable
fun EpisodeChip(
    episode: BangumiEpisode,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(140.dp)
            .aspectRatio(16f / 9f),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box {
            // 封面
            AsyncImage(
                model = FormatUtils.fixImageUrl(episode.cover),
                contentDescription = null,
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
                            startY = 30f
                        )
                    )
            )
            
            // 角标
            if (episode.badge.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    color = if (episode.badge.contains("会员")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = episode.badge,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
            }
            
            // 集数和标题
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = episode.title.ifEmpty { "第${episode.id}话" },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.longTitle.isNotEmpty() && episode.longTitle != episode.title) {
                    Text(
                        text = episode.longTitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 选集预览列表（显示前几集+更多按钮）
 */
@Composable
fun EpisodePreviewRow(
    episodes: List<BangumiEpisode>,
    maxPreviewCount: Int = 6,
    onEpisodeClick: (BangumiEpisode) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewEpisodes = episodes.take(maxPreviewCount)
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(previewEpisodes) { episode ->
            EpisodeChip(
                episode = episode,
                onClick = { onEpisodeClick(episode) }
            )
        }
        
        // 更多按钮
        if (episodes.size > maxPreviewCount) {
            item {
                Surface(
                    onClick = onShowAll,
                    modifier = Modifier
                        .width(80.dp)
                        .aspectRatio(16f / 9f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                CupertinoIcons.Default.Ellipsis,
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "全部${episodes.size}集",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
