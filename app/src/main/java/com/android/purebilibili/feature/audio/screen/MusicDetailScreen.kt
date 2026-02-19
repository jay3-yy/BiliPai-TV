package com.android.purebilibili.feature.audio.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.purebilibili.feature.audio.viewmodel.MusicViewModel
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.BackwardEnd
import io.github.alexzhirkevich.cupertino.icons.filled.ForwardEnd
import io.github.alexzhirkevich.cupertino.icons.filled.Pause
import io.github.alexzhirkevich.cupertino.icons.filled.Play
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronDown
import io.github.alexzhirkevich.cupertino.icons.outlined.MusicNote
import kotlinx.coroutines.delay

/**
 * 原生音乐播放页 - au 格式 (传统 sid)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicDetailScreen(
    sid: Long,
    onBack: () -> Unit,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initPlayer(context)
        viewModel.loadMusic(sid)
    }

    MusicDetailContent(
        state = state,
        onBack = onBack,
        onPlayPause = { viewModel.togglePlayPause() },
        onSeek = { viewModel.seekTo(it) },
        onUpdateProgress = { viewModel.updateProgress() }
    )
}

/**
 * 原生音乐播放页 - MA 格式 (从视频 DASH 流提取音频)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicDetailScreen(
    musicTitle: String,
    bvid: String,
    cid: Long,
    onBack: () -> Unit,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initPlayer(context)
        viewModel.loadMusicFromVideo(musicTitle, bvid, cid)
    }

    MusicDetailContent(
        state = state,
        onBack = onBack,
        onPlayPause = { viewModel.togglePlayPause() },
        onSeek = { viewModel.seekTo(it) },
        onUpdateProgress = { viewModel.updateProgress() }
    )
}

/**
 * 共享的音乐详情页 UI 内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicDetailContent(
    state: com.android.purebilibili.feature.audio.viewmodel.MusicUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onUpdateProgress: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying) {
            while (true) {
                onUpdateProgress()
                delay(500)
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Outlined.ChevronDown, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Blur (Simplified as dark gradient or single color for now)
             Box(modifier = Modifier
                 .fillMaxSize()
                 .background(Color(0xFF1E1E1E)))

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            } else if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(20.dp))
                    
                    // Cover Art (or placeholder for MA format)
                    val coverUrl = state.songInfo?.cover ?: state.musicCover
                    
                    if (coverUrl != null) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context).data(coverUrl).crossfade(true).build()
                            ),
                            contentDescription = "Cover",
                            modifier = Modifier
                                .size(300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // MA 格式没有封面，显示占位符
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF667eea),
                                            Color(0xFF764ba2)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CupertinoIcons.Outlined.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Title & Author
                    Text(
                        text = state.songInfo?.title ?: state.musicTitle ?: "未知歌曲",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.songInfo?.author ?: "背景音乐",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                         textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(32.dp))

                    // Lyrics Preview (Just scrollable text for now)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(16.dp)
                    ) {
                        LazyColumn(state = rememberLazyListState()) {
                             item {
                                 Text(
                                     text = state.lyrics ?: "暂无歌词",
                                     color = Color.White.copy(alpha = 0.8f),
                                     lineHeight = 24.sp,
                                     textAlign = TextAlign.Center,
                                     modifier = Modifier.fillMaxWidth()
                                 )
                             }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Progress Bar
                    Slider(
                        value = state.currentPositionMs.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(state.currentPositionMs),
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = formatTime(state.durationMs),
                             color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         IconButton(onClick = { /* Prev */ }, modifier = Modifier.size(48.dp)) {
                             Icon(CupertinoIcons.Filled.BackwardEnd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                         }
                         Spacer(Modifier.width(32.dp))
                         
                         FloatingActionButton(
                             onClick = onPlayPause,
                             containerColor = Color.White,
                             contentColor = Color.Black,
                             shape = CircleShape,
                             modifier = Modifier.size(64.dp)
                         ) {
                             Icon(
                                 imageVector = if (state.isPlaying) CupertinoIcons.Filled.Pause else CupertinoIcons.Filled.Play,
                                 contentDescription = if (state.isPlaying) "Pause" else "Play",
                                 modifier = Modifier.size(32.dp)
                             )
                         }

                         Spacer(Modifier.width(32.dp))
                         IconButton(onClick = { /* Next */ }, modifier = Modifier.size(48.dp)) {
                             Icon(CupertinoIcons.Filled.ForwardEnd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                         }
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

