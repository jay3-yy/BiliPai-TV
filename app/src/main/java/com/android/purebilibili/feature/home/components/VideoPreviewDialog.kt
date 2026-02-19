package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoItem
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player

import androidx.compose.material.icons.rounded.Fullscreen
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.animation.DissolvableVideoCard
import com.android.purebilibili.core.ui.animation.DissolveAnimationPreset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.zIndex

/**
 * iOS 3D Touch style Preview Dialog
 */
@Composable
fun VideoPreviewDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onWatchLater: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit, // Navigate to Full Screen
    onNotInterested: (() -> Unit)? = null,
    onGetPreviewUrl: suspend (String, Long) -> String? = { _, _ -> null }, // [New] Fetch Url
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val haptic = rememberHapticFeedback()
    val context = LocalContext.current
    
    // Dissolve Animation State
    var isDissolving by remember { mutableStateOf(false) }
    
    // Playback State
    var isPlaying by remember { androidx.compose.runtime.mutableStateOf(false) }
    var videoUrl by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var isLoading by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    // Fetch URL when isPlaying becomes true
    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        if (isPlaying && videoUrl == null) {
            isLoading = true
            videoUrl = onGetPreviewUrl(video.bvid, video.cid)
            isLoading = false
            if (videoUrl == null) {
                // Failed to get URL, fallback to full screen navigation or toast?
                // For now, just reset isPlaying or show error
                android.widget.Toast.makeText(context, "无法获取预览地址", android.widget.Toast.LENGTH_SHORT).show()
                isPlaying = false 
            }
        }
    }

    // Handle Back Press manually since we are not in a Dialog anymore
    androidx.activity.compose.BackHandler(onBack = {
        if (isPlaying) isPlaying = false else onDismiss()
    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (hazeState != null) {
                    Modifier.unifiedBlur(hazeState = hazeState)
                } else {
                    Modifier
                }
            )
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = null, 
                indication = null, 
                onClick = { onDismiss() } // Always dismiss on background click
            ),
        contentAlignment = Alignment.Center
    ) {
        DissolvableVideoCard(
            isDissolving = isDissolving,
            onDissolveComplete = {
                onDismiss()
                onNotInterested?.invoke()
            },
            cardId = video.bvid
            ,
            preset = DissolveAnimationPreset.TELEGRAM_FAST
        ) {
            Column(
                modifier = Modifier
                    .width(300.dp) // Slightly wider than standard alert
                    // Remove padding between items by putting them in one Surface
                    .clip(RoundedCornerShape(16.dp)) // Clip the whole card
                    .clickable(enabled = false) {}, // Prevent clicks from passing through to background
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // 1. Media Area (Cover or Player)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.6f)
                                .background(Color.Black)
                                .clickable { // Toggle Play/Pause
                                    haptic(HapticType.MEDIUM)
                                    isPlaying = !isPlaying
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Cover Image (Always visible as background or placeholder)
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(FormatUtils.fixImageUrl(video.pic))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Video Player (Visible when playing)
                            if (isPlaying) {
                                if (videoUrl != null) {
                                    // Using a simpler composite:
                                    DisposableVideoPlayer(url = videoUrl!!)
                                }
                                
                                // Loading Indicator
                                if (isLoading) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            } else {
                                // Play Icon Overlay (Hint that it's clickable)
                                Icon(
                                    imageVector = CupertinoIcons.Filled.Play,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                                        .padding(12.dp)
                                )
                            }
                        }
                        
                        // 2. Title & Info
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${video.owner.name} · ${FormatUtils.formatStat(video.stat.view.toLong())}播放",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        MenuDivider()

                        // 3. Action Menu (Integrated)
                        // Play Immediately / Fullscreen
                        PreviewMenuItem(
                            text = if (isPlaying) "全屏播放" else "立即播放", 
                            icon = if (isPlaying) Icons.Rounded.Fullscreen else CupertinoIcons.Default.Play, 
                            onClick = {
                                haptic(HapticType.MEDIUM)
                                onPlay() // Go to Full Screen
                                onDismiss()
                            }
                        )

                        MenuDivider()

                        // Watch Later
                        PreviewMenuItem(
                            text = "稍后再看",
                            icon = CupertinoIcons.Default.Clock, // Clock or Time
                            onClick = {
                                haptic(HapticType.MEDIUM)
                                onWatchLater()
                                onDismiss()
                            }
                        )
                        
                        MenuDivider()
                        
                        // Share
                        PreviewMenuItem(
                            text = "分享",
                            icon = Icons.Rounded.Share, // Fallback to Material Share
                            onClick = {
                                haptic(HapticType.LIGHT)
                                onShare() // Use passed callback if needed, but logic is likely external? 
                                // Wait, the implementation passed in HomeScreen handles data. 
                                // But here we are inside VideoPreviewDialog which takes `onShare: () -> Unit`.
                                // Let's check HomeScreen's implementation of onShare.
                                onDismiss() // Closing dialog
                            }
                        )

                        if (onNotInterested != null) {
                            MenuDivider()
                            PreviewMenuItem(
                                text = "不感兴趣",
                                icon = CupertinoIcons.Outlined.Xmark, 
                                isDestructive = true,
                                onClick = {
                                    haptic(HapticType.HEAVY)
                                    isDissolving = true // Trigger Thanos snap animation
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisposableVideoPlayer(url: String) {
    val context = LocalContext.current
    
    // Create Player
    val player = androidx.compose.runtime.remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        }
    }

    // Set Media Item
    androidx.compose.runtime.LaunchedEffect(url) {
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.parse(url)))
        player.prepare()
    }

    // Cleanup
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PreviewMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MenuDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        thickness = 0.5.dp
    )
}
