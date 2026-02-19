package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.HandThumbsup
import io.github.alexzhirkevich.cupertino.icons.outlined.Star
import dev.chrisbanes.haze.HazeState
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.store.SettingsManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.feature.home.components.resolveBottomBarSurfaceColor

@Composable
fun BottomInputBar(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    isFavorited: Boolean,
    isCoined: Boolean,
    onLikeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit,
    onShareClick: () -> Unit,
    onCommentClick: () -> Unit,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val blurIntensity by SettingsManager.getBlurIntensity(context)
        .collectAsState(initial = com.android.purebilibili.core.ui.blur.BlurIntensity.THIN)
    
    val barColor = resolveBottomBarSurfaceColor(
        surfaceColor = MaterialTheme.colorScheme.surface,
        blurEnabled = hazeState != null,
        blurIntensity = blurIntensity
    )

    Surface(
        color = barColor,
        tonalElevation = 8.dp,
        shadowElevation = if (hazeState != null) 0.dp else 8.dp, // Reduce shadow if blur is on for cleaner glass look
        modifier = modifier
            .fillMaxWidth()
            .let { m ->
                if (hazeState != null) m.unifiedBlur(hazeState = hazeState) else m
            }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding(), // fit system windows
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input Field Placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onCommentClick() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "发个友善的评论...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                IconActionButton(
                    icon = if (isLiked) CupertinoIcons.Filled.HandThumbsup else CupertinoIcons.Outlined.HandThumbsup,
                    label = "点赞",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = onLikeClick,
                    showLabel = false
                )
                
                // Coin (Using generic circle icon if specific coin icon not available, or reusing star for now as place holder if needed, but let's try to find a coin icon or text)
                // For simplicity, using a custom composable or text for Coin if icon missing. 
                // Assuming we have a coin icon or similar. Let's use a placeholder Circle/Money icon.
                // Using "C" text circle for Coin as fallback matching Bilibili style often.
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isCoined) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onCoinClick() },
                    contentAlignment = Alignment.Center
                ) {
                   if (isCoined) {
                       Text("币", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                   } else {
                       Text("币", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                   }
                }

                // Favorite
                IconActionButton(
                    icon = if (isFavorited) CupertinoIcons.Filled.Star else CupertinoIcons.Outlined.Star,
                    label = "收藏",
                    tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = onFavoriteClick,
                    showLabel = false
                )
                
                // Share
                IconActionButton(
                    icon = CupertinoIcons.Filled.SquareAndArrowUp,
                    label = "分享",
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onShareClick,
                    showLabel = false
                )
            }
        }
    }
}

@Composable
private fun IconActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    showLabel: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = tint
            )
        }
    }
}
