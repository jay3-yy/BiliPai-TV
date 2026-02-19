// 文件路径: feature/home/components/cards/StoryVideoCard.kt
package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.iOSCardTapEffect
import com.android.purebilibili.core.util.animateEnter
import com.android.purebilibili.core.util.CardPositionManager
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.core.util.rememberHapticFeedback
import com.android.purebilibili.core.util.rememberIsTvDevice
import com.android.purebilibili.core.util.HapticType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
//  共享元素过渡
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring

import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.theme.LocalCornerRadiusScale
import com.android.purebilibili.core.theme.iOSCornerRadius
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.animation.TvFocusCardEmphasis
import com.android.purebilibili.core.ui.animation.tvFocusableJiggle
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 *  故事卡片 - Apple TV+ 风格
 * 
 * 特点：
 * - 2:1 电影宽屏比例
 * - 大圆角 (24dp)
 * - 标题叠加在封面底部
 * - 沉浸电影感
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun StoryVideoCard(
    video: VideoItem,
    index: Int = 0,  //  [新增] 索引用于动画延迟
    animationEnabled: Boolean = true,  //  卡片动画开关
    motionTier: MotionTier = MotionTier.Normal,
    transitionEnabled: Boolean = false, //  卡片过渡动画开关
    onDismiss: (() -> Unit)? = null,    //  [新增] 删除/过滤回调（长按触发）
    onLongClick: ((VideoItem) -> Unit)? = null, // [修复] 长按预览回调
    onClick: (String, Long) -> Unit
) {
    val haptic = rememberHapticFeedback()
    
    // [新增] 获取圆角缩放比例
    val cornerRadiusScale = LocalCornerRadiusScale.current
    val cardCornerRadius = iOSCornerRadius.ExtraLarge * cornerRadiusScale  // 20.dp * scale
    val smallCornerRadius = iOSCornerRadius.Small * cornerRadiusScale - 2.dp  // 8.dp * scale
    
    //  [新增] 长按删除菜单状态
    var showDismissMenu by remember { mutableStateOf(false) }
    
    val coverUrl = remember(video.bvid) {
        FormatUtils.fixImageUrl(if (video.pic.startsWith("//")) "https:${video.pic}" else video.pic)
    }
    
    //  获取屏幕尺寸用于计算归一化坐标
    val isTvDevice = rememberIsTvDevice()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    //  记录卡片位置
    var cardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val triggerCardClick = {
        cardBounds?.let { bounds ->
            CardPositionManager.recordCardPosition(
                bounds,
                screenWidthPx,
                screenHeightPx,
                isSingleColumn = !transitionEnabled
            )
        }
        onClick(video.bvid, 0)
    }
    
    //  尝试获取共享元素作用域
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    
    val cardModifier = if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "video_cover_${video.bvid}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 300f
                        )
                    },
                    clipInOverlayDuringTransition = OverlayClip(
                        RoundedCornerShape(cardCornerRadius)
                    )
                )
        }
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusableJiggle(
                isTv = isTvDevice,
                screenWidthDp = configuration.screenWidthDp,
                reducedMotion = !animationEnabled,
                cardEmphasis = TvFocusCardEmphasis.Large,
                motionTier = motionTier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            //  [修复] 进场动画 - 使用 Unit 作为 key，避免分类切换时重新动画
            .animateEnter(
                index = index, 
                key = Unit, 
                animationEnabled = animationEnabled && !CardPositionManager.isReturningFromDetail && !CardPositionManager.isSwitchingCategory,
                motionTier = motionTier
            )
            //  [新增] 记录卡片位置
            .onGloballyPositioned { coordinates ->
                cardBounds = coordinates.boundsInRoot()
            }
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
            .pointerInput(onDismiss, onLongClick) {
                 val hasLongPressAction = onDismiss != null || onLongClick != null
                 if (hasLongPressAction) {
                     detectTapGestures(
                         onLongPress = {
                             if (onLongClick != null) {
                                 haptic(HapticType.HEAVY)
                                 onLongClick(video)
                             } else if (onDismiss != null) {
                                 haptic(HapticType.HEAVY)
                                 showDismissMenu = true
                             }
                         },
                         onTap = {
                             triggerCardClick()
                         }
                     )
                 }
            }
            .then(
                 if (onDismiss == null && onLongClick == null) {
                     Modifier.iOSCardTapEffect(
                         pressScale = 0.97f,
                         pressTranslationY = 10f,
                         hapticEnabled = true
                     ) {
                         triggerCardClick()
                     }
                 } else Modifier
            )
    ) {
        // 卡片容器 (封面)
        Box(
            modifier = cardModifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp, // 降低阴影使其更轻量
                    shape = RoundedCornerShape(cardCornerRadius),
                    ambientColor = Color.Black.copy(alpha = 0.1f),
                    spotColor = Color.Black.copy(alpha = 0.15f),
                    clip = true // [Optimization] Combine shadow and clip
                )
                .background(MaterialTheme.colorScheme.surfaceVariant) // 封面占位色
        ) {
            //  封面 - 2:1 电影宽屏
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverUrl)
                    .crossfade(150)
                    .memoryCacheKey("story_${video.bvid}")
                    .diskCacheKey("story_${video.bvid}")
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 1f)
                    .clip(RoundedCornerShape(cardCornerRadius)),
                contentScale = ContentScale.Crop
            )
            
            //  时长标签 (保留在封面上)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = FormatUtils.formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        //  标题
        // 🔗 [共享元素] 标题
        var titleModifier = Modifier.fillMaxWidth()
        if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                titleModifier = titleModifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "video_title_${video.bvid}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        spring(dampingRatio = 0.8f, stiffness = 200f)
                    }
                )
            }
        }
        
        Text(
            text = video.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp, // 比双列略大
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 23.sp,
            modifier = titleModifier
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // UP主信息 + 数据
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // UP主头像
            // 🔗 [共享元素] 头像
            var avatarModifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
            
            if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    avatarModifier = avatarModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_avatar_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        },
                         clipInOverlayDuringTransition = OverlayClip(CircleShape)
                    )
                }
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(FormatUtils.fixImageUrl(video.owner.face))
                    .crossfade(100)
                    .build(),
                contentDescription = null,
                modifier = avatarModifier,
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // UP主名称
            // 🔗 [共享元素] UP主名称
            var upNameModifier = Modifier.wrapContentSize()
            if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    upNameModifier = upNameModifier.sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "video_up_${video.bvid}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = 200f)
                        }
                    )
                }
            }
            
            Text(
                text = video.owner.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = upNameModifier
            )
            
            // 数据行 (Play & Danmaku)
             //  [重设计] 播放数据行 - 独立展示，精致风格
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 16.dp) // 与 UP 主信息分开
            ) {
                // 播放量
                if (video.stat.view > 0) {
                     // 🔗 [共享元素] 播放量
                    var viewsModifier = Modifier.wrapContentSize()
                    if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            viewsModifier = viewsModifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "video_views_${video.bvid}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { _, _ ->
                                    spring(dampingRatio = 0.8f, stiffness = 200f)
                                }
                            )
                        }
                    }
                    
                    Box(modifier = viewsModifier) {
                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FormatUtils.formatStat(video.stat.view.toLong()),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 弹幕数 (仅当有播放量时显示，保持逻辑一致)
                if (video.stat.view > 0 && video.stat.danmaku > 0) {
                     // 🔗 [共享元素] 弹幕数
                     var danmakuModifier = Modifier.wrapContentSize()
                     if (transitionEnabled && sharedTransitionScope != null && animatedVisibilityScope != null) {
                         with(sharedTransitionScope) {
                             danmakuModifier = danmakuModifier.sharedBounds(
                                 sharedContentState = rememberSharedContentState(key = "video_danmaku_${video.bvid}"),
                                 animatedVisibilityScope = animatedVisibilityScope,
                                 boundsTransform = { _, _ ->
                                     spring(dampingRatio = 0.8f, stiffness = 200f)
                                 }
                             )
                         }
                     }

                     Box(modifier = danmakuModifier) {
                         Row(
                             verticalAlignment = Alignment.CenterVertically,
                             horizontalArrangement = Arrangement.spacedBy(2.dp)
                         ) {
                             Icon(
                                 imageVector = CupertinoIcons.Outlined.BubbleLeft,
                                 contentDescription = null,
                                 modifier = Modifier.size(12.dp),
                                 tint = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                             Text(
                                 text = FormatUtils.formatStat(video.stat.danmaku.toLong()),
                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 fontSize = 11.sp,
                                 fontWeight = FontWeight.Medium
                             )
                         }
                     }
                }
            }
        }
    }
    
    //  [新增] 长按删除菜单
    DropdownMenu(
        expanded = showDismissMenu,
        onDismissRequest = { showDismissMenu = false }
    ) {
        DropdownMenuItem(
            text = { 
                Text(
                    "🚫 不感兴趣",
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            onClick = {
                showDismissMenu = false
                onDismiss?.invoke()
            }
        )
    }
}
