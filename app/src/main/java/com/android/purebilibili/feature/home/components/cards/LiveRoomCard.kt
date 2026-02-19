package com.android.purebilibili.feature.home.components.cards
/**
 * Shared Element Transition Imports
 */
import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.theme.iOSSystemGray
import com.android.purebilibili.data.model.response.LiveRoom
import com.android.purebilibili.core.util.iOSTapEffect
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.iOSCornerRadius
import androidx.compose.ui.input.key.onPreviewKeyEvent

/**
 *  iOS 风格直播间卡片
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LiveRoomCard(
    room: LiveRoom,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: (Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val isTvDevice = rememberIsTvDevice()
    
    // [新增] 获取圆角缩放比例
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val cardCornerRadius = iOSCornerRadius.Large * cornerRadiusScale  // 14.dp * scale
    val tagCornerRadius = iOSCornerRadius.Tiny * cornerRadiusScale   // 4.dp * scale
    
    // Shared Element Transition Scopes
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    
    val coverUrl = remember(room.roomid) {
        FormatUtils.fixImageUrl(room.cover.ifEmpty { room.keyframe.ifEmpty { room.userCover } })
    }
    val triggerCardClick = { onClick(room.roomid) }

    Column(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (
                    shouldTriggerHomeCardClickOnTvKey(
                        isTv = isTvDevice,
                        keyCode = event.nativeKeyEvent.keyCode,
                        action = event.nativeKeyEvent.action
                    )
                ) {
                    triggerCardClick()
                    true
                } else {
                    false
                }
            }
            //  iOS 点击动画
            .iOSTapEffect(
                scale = 0.97f,
                hapticEnabled = true
            ) {
                triggerCardClick()
            }
            .padding(bottom = 6.dp)  //  减少间距
    ) {
        //  封面容器 - iOS 风格
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(cardCornerRadius),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                    clip = true // [Optimization] Combine shadow and clip
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                sharedContentState = rememberSharedContentState(key = "live_cover_${room.roomid}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier
                )
        ) {
            // 封面图 -  优化
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(150)
                    .memoryCacheKey("live_cover_${room.roomid}")
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            // 🔴 直播标签 - 左上角
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(tagCornerRadius),
                color = Color(0xFFE02020)
            ) {
                Text(
                    text = "直播中",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            // 分区标签 - 右上角
            if (room.areaName.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(tagCornerRadius),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = room.areaName,
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // 观看人数 - 左下角
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👁",
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = FormatUtils.formatStat(room.online.toLong()),
                    color = Color.White.copy(0.95f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))  //  减少间距
        
        // 标题
        Text(
            text = room.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 主播信息
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主播头像
            if (room.face.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(FormatUtils.fixImageUrl(room.face))
                        .crossfade(150)
                        .size(72, 72)
                        .memoryCacheKey("live_avatar_${room.uid}")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            
            Text(
                text = room.uname,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
