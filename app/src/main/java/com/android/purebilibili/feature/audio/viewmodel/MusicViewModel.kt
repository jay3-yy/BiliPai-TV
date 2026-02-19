package com.android.purebilibili.feature.audio.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.android.purebilibili.data.model.response.SongInfoData
import com.android.purebilibili.data.repository.AudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MusicUiState(
    val isLoading: Boolean = false,
    val songInfo: SongInfoData? = null,
    val lyrics: String? = null,
    val error: String? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val currentPositionMs: Long = 0,
    // [新增] MA 格式音乐的基本信息（没有完整的 songInfo）
    val musicTitle: String? = null,
    val musicCover: String? = null
)

@androidx.annotation.OptIn(UnstableApi::class)
class MusicViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null

    // Polling job for progress update is handled via LaunchedEffect in UI or a separate coroutine here
    // For simplicity, we expose player state and let UI trigger updates or we update frequently.
    
    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _uiState.value = _uiState.value.copy(durationMs = duration)
                        }
                    }
                })
            }
        }
    }

    fun loadMusic(sid: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 1. Get Info
            val infoResp = AudioRepository.getSongInfo(sid)
            if (infoResp.code != 0 || infoResp.data == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "加载歌曲信息失败: ${infoResp.msg}")
                return@launch
            }
            val songInfo = infoResp.data

            // 2. Get Stream URL
            val streamResp = AudioRepository.getSongStream(sid)
            if (streamResp.code != 0 || streamResp.data == null || streamResp.data.cdns.isEmpty()) {
                 _uiState.value = _uiState.value.copy(isLoading = false, error = "加载音频流失败: ${streamResp.msg}")
                 return@launch
            }
            val streamUrl = streamResp.data.cdns[0]

            // 3. Get Lyrics (Optional)
            val lyricResp = AudioRepository.getSongLyric(sid)
            val lyrics = lyricResp.data

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                songInfo = songInfo,
                lyrics = lyrics
            )

            // 4. Prepare Player
            playAudioUrl(streamUrl)
        }
    }
    
    /**
     * 加载 MA 格式音乐（从视频的 DASH 流获取音频）
     * 
     * @param musicTitle 音乐标题
     * @param bvid 关联视频的 BVID
     * @param cid 关联视频的分 P CID
     */
    fun loadMusicFromVideo(musicTitle: String, bvid: String, cid: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null,
                musicTitle = musicTitle,
                musicCover = null  // MA 格式暂时没有封面
            )
            
            // 从视频的 DASH 流获取音频 URL
            val audioUrl = AudioRepository.getAudioStreamFromVideo(bvid, cid)
            
            if (audioUrl.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = "无法获取音频流，请稍后重试"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
            
            // 播放音频
            playAudioUrl(audioUrl)
        }
    }
    
    private fun playAudioUrl(url: String) {
        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    fun updateProgress() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                 _uiState.value = _uiState.value.copy(
                     currentPositionMs = player.currentPosition,
                     durationMs = player.duration.coerceAtLeast(0)
                 )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}

