// File: feature/video/ui/overlay/TopControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//  Cupertino Icons
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Cast
import com.android.purebilibili.core.util.rememberIsTvDevice
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top Control Bar Component
 * 
 * Redesigned to match official Bilibili landscape layout:
 * - Left: Back button, Title (Marquee), Online count
 * - Right: Action buttons (Like, Dislike, Coin, Share, Cast, More)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopControlBar(
    title: String,
    onlineCount: String = "",
    isFullscreen: Boolean,
    onBack: () -> Unit,
    // Interactions
    isLiked: Boolean = false,
    isCoined: Boolean = false,
    onLikeClick: () -> Unit = {},
    onDislikeClick: () -> Unit = {},
    onCoinClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCastClick: () -> Unit = {}, // Added Cast callback
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTvDevice = rememberIsTvDevice()
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveTopControlBarLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }
    val currentTimeText by produceState(initialValue = formatCurrentTime()) {
        while (true) {
            value = formatCurrentTime()
            val now = System.currentTimeMillis()
            val nextMinuteDelay = (60_000L - (now % 60_000L)).coerceAtLeast(1_000L)
            delay(nextMinuteDelay)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFullscreen) Modifier.statusBarsPadding() else Modifier)
            .padding(
                horizontal = layoutPolicy.horizontalPaddingDp.dp,
                vertical = layoutPolicy.verticalPaddingDp.dp
            )
    ) {
        // 顶部时间栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTimeText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = layoutPolicy.timeFontSp.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(layoutPolicy.timeBottomSpacingDp.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Left Section: Back & Info ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f) // Text takes remaining space
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
                ) {
                    Icon(
                        imageVector = CupertinoIcons.Default.ChevronBackward, 
                        contentDescription = "Back", 
                        tint = Color.White,
                        modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(layoutPolicy.backToTitleSpacingDp.dp))

                // 标题与右侧图标保持同一行
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = layoutPolicy.titleFontSp.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .basicMarquee() // Marquee effect for long text
                )
            }
            
            Spacer(modifier = Modifier.width(layoutPolicy.sectionGapDp.dp)) // Space between text and actions
            
            // --- Right Section: Actions ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layoutPolicy.actionSpacingDp.dp)
            ) {
                // Like
                ActionIcon(
                    icon = if (isLiked) Icons.Rounded.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "点赞",
                    isActive = isLiked,
                    onClick = onLikeClick,
                    buttonSizeDp = layoutPolicy.buttonSizeDp,
                    iconSizeDp = layoutPolicy.iconSizeDp
                )
                
                // Dislike
                ActionIcon(
                    icon = Icons.Outlined.ThumbDown,
                    contentDescription = "不喜欢",
                    isActive = false,
                    onClick = onDislikeClick,
                    buttonSizeDp = layoutPolicy.buttonSizeDp,
                    iconSizeDp = layoutPolicy.iconSizeDp
                )
                
                // Coin
                ActionIcon(
                    icon = if (isCoined) Icons.Rounded.MonetizationOn else Icons.Outlined.MonetizationOn,
                    contentDescription = "投币",
                    isActive = isCoined,
                    onClick = onCoinClick,
                    buttonSizeDp = layoutPolicy.buttonSizeDp,
                    iconSizeDp = layoutPolicy.iconSizeDp
                )
                
                // Share
                ActionIcon(
                    icon = Icons.Outlined.Share,
                    contentDescription = "分享",
                    isActive = false,
                    onClick = onShareClick,
                    buttonSizeDp = layoutPolicy.buttonSizeDp,
                    iconSizeDp = layoutPolicy.iconSizeDp
                )

                // Cast (Added back)
                ActionIcon(
                    icon = Icons.Outlined.Cast,
                    contentDescription = "投屏",
                    isActive = false,
                    onClick = onCastClick,
                    buttonSizeDp = layoutPolicy.buttonSizeDp,
                    iconSizeDp = layoutPolicy.iconSizeDp
                )
                
                // More (Three dots)
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(layoutPolicy.buttonSizeDp.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "更多",
                        tint = Color.White,
                        modifier = Modifier.size(layoutPolicy.iconSizeDp.dp)
                    )
                }
            }
        }

        // 观看人数放到下一行，避免影响标题与右侧图标对齐
        if (onlineCount.isNotEmpty()) {
            Text(
                text = onlineCount,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = layoutPolicy.onlineCountFontSp.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(
                    start = layoutPolicy.onlineCountStartPaddingDp.dp,
                    top = layoutPolicy.onlineCountTopPaddingDp.dp
                )
            )
        }
    }
}

private fun formatCurrentTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    buttonSizeDp: Int = 32,
    iconSizeDp: Int = 24
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSizeDp.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(iconSizeDp.dp)
        )
    }
}
