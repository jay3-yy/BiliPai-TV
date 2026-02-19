// 文件路径: feature/video/player/PlaylistManager.kt
package com.android.purebilibili.feature.video.player

import android.content.Context
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "PlaylistManager"
private const val PREFS_NAME = "playlist_manager_state"
private const val KEY_SNAPSHOT = "snapshot_json"

/**
 * 播放列表项
 */
@Serializable
data class PlaylistItem(
    val bvid: String,
    val title: String,
    val cover: String,
    val owner: String,
    val duration: Long = 0L,
    // 番剧专用
    val isBangumi: Boolean = false,
    val seasonId: Long? = null,
    val epId: Long? = null
)

/**
 * 播放模式
 */
@Serializable
enum class PlayMode {
    SEQUENTIAL,   // 顺序播放
    SHUFFLE,      // 随机播放  
    REPEAT_ONE    // 单曲循环
}

/**
 *  播放列表管理器
 * 
 * 管理播放队列、播放模式和上下曲切换
 */
object PlaylistManager {
    @Serializable
    private data class PlaylistSnapshot(
        val playlist: List<PlaylistItem> = emptyList(),
        val currentIndex: Int = -1,
        val playMode: PlayMode = PlayMode.SEQUENTIAL,
        val isExternalPlaylist: Boolean = false
    )
    
    // ========== 状态 ==========
    
    private val _playlist = MutableStateFlow<List<PlaylistItem>>(emptyList())
    val playlist = _playlist.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex = _currentIndex.asStateFlow()
    
    private val _playMode = MutableStateFlow(PlayMode.SEQUENTIAL)
    val playMode = _playMode.asStateFlow()
    
    // 🔒 [新增] 外部播放列表标志 - 当为 true 时，不使用推荐视频覆盖
    // 适用于：稍后再看全部播放、UP主页全部播放、收藏夹播放等
    private val _isExternalPlaylist = MutableStateFlow(false)
    val isExternalPlaylist = _isExternalPlaylist.asStateFlow()
    
    // 已播放的随机索引（用于随机模式历史）
    private val shuffleHistory = mutableListOf<Int>()
    private var shuffleHistoryIndex = -1

    private var appContext: Context? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        appContext = context.applicationContext
        restoreState()
    }
    
    // ========== 公共 API ==========
    
    /**
     * 设置播放列表
     * @param items 播放列表
     * @param startIndex 开始播放的索引
     * 注意：此方法会重置外部播放列表标志
     */
    fun setPlaylist(items: List<PlaylistItem>, startIndex: Int = 0) {
        Logger.d(TAG, "🎵 设置播放列表: ${items.size} 项, 从索引 $startIndex 开始")
        _playlist.value = items
        _currentIndex.value = resolveStartIndex(items, startIndex)
        _isExternalPlaylist.value = false  // 重置外部播放列表标志
        
        resetShuffleHistoryForCurrentIndex()
        persistState()
    }
    
    /**
     * 🔒 [新增] 设置外部播放列表（稍后再看、UP主页、收藏夹等）
     * 外部播放列表不会被推荐视频覆盖
     * @param items 播放列表
     * @param startIndex 开始播放的索引
     */
    fun setExternalPlaylist(items: List<PlaylistItem>, startIndex: Int = 0) {
        Logger.d(TAG, "🔒 设置外部播放列表: ${items.size} 项, 从索引 $startIndex 开始")
        _playlist.value = items
        _currentIndex.value = resolveStartIndex(items, startIndex)
        _isExternalPlaylist.value = true  // 标记为外部播放列表
        
        resetShuffleHistoryForCurrentIndex()
        persistState()
    }
    
    /**
     * 添加到播放列表末尾
     */
    fun addToPlaylist(item: PlaylistItem) {
        if (_playlist.value.any { it.bvid == item.bvid }) {
            Logger.d(TAG, " ${item.bvid} 已在播放列表中")
            return
        }
        _playlist.value = _playlist.value + item
        Logger.d(TAG, "➕ 添加到播放列表: ${item.title}")
        persistState()
    }
    
    /**
     * 添加多个到播放列表
     */
    fun addAllToPlaylist(items: List<PlaylistItem>) {
        val existingBvids = _playlist.value.map { it.bvid }.toSet()
        val newItems = items.filter { it.bvid !in existingBvids }
        if (newItems.isNotEmpty()) {
            _playlist.value = _playlist.value + newItems
            Logger.d(TAG, "➕ 批量添加 ${newItems.size} 项到播放列表")
            persistState()
        }
    }
    
    /**
     * 从播放列表移除
     */
    fun removeFromPlaylist(bvid: String) {
        val index = _playlist.value.indexOfFirst { it.bvid == bvid }
        if (index >= 0) {
            _playlist.value = _playlist.value.toMutableList().apply { removeAt(index) }
            // 调整当前索引
            if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            } else if (index == _currentIndex.value && _currentIndex.value >= _playlist.value.size) {
                _currentIndex.value = _playlist.value.lastIndex.coerceAtLeast(0)
            }
            Logger.d(TAG, "➖ 从播放列表移除: $bvid")
            persistState()
        }
    }
    
    /**
     * 清空播放列表
     */
    fun clearPlaylist() {
        _playlist.value = emptyList()
        _currentIndex.value = -1
        _isExternalPlaylist.value = false
        shuffleHistory.clear()
        shuffleHistoryIndex = -1
        Logger.d(TAG, " 清空播放列表")
        persistState()
    }
    
    /**
     * 设置播放模式
     */
    fun setPlayMode(mode: PlayMode) {
        _playMode.value = mode
        Logger.d(TAG, " 播放模式: $mode")
        persistState()
    }
    
    /**
     * 切换播放模式（循环切换）
     */
    fun togglePlayMode(): PlayMode {
        val newMode = when (_playMode.value) {
            PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SEQUENTIAL
        }
        _playMode.value = newMode
        Logger.d(TAG, " 切换播放模式: $newMode")
        persistState()
        return newMode
    }
    
    /**
     * 获取当前播放项
     */
    fun getCurrentItem(): PlaylistItem? {
        val index = _currentIndex.value
        val list = _playlist.value
        return if (index in list.indices) list[index] else null
    }
    
    /**
     * 播放下一曲
     * @return 下一个播放项，如果没有则返回 null
     */
    fun playNext(): PlaylistItem? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        val currentIdx = _currentIndex.value
        
        val nextIndex = when (_playMode.value) {
            PlayMode.SEQUENTIAL -> {
                // 顺序播放：下一个，到末尾则停止
                if (currentIdx < list.lastIndex) currentIdx + 1 else null
            }
            PlayMode.SHUFFLE -> {
                // 随机播放
                if (shuffleHistoryIndex < shuffleHistory.lastIndex) {
                    // 在历史记录中有下一个
                    shuffleHistoryIndex++
                    shuffleHistory[shuffleHistoryIndex]
                } else {
                    // 生成新的随机索引
                    val remaining = list.indices.filter { it != currentIdx && it !in shuffleHistory.takeLast(minOf(5, list.size / 2)) }
                    if (remaining.isNotEmpty()) {
                        val next = remaining.random()
                        shuffleHistory.add(next)
                        shuffleHistoryIndex = shuffleHistory.lastIndex
                        next
                    } else if (list.size > 1) {
                        val next = list.indices.filter { it != currentIdx }.random()
                        shuffleHistory.add(next)
                        shuffleHistoryIndex = shuffleHistory.lastIndex
                        next
                    } else null
                }
            }
            PlayMode.REPEAT_ONE -> {
                // 单曲循环：保持当前
                currentIdx
            }
        }
        
        return if (nextIndex != null && nextIndex in list.indices) {
            _currentIndex.value = nextIndex
            Logger.d(TAG, " 播放下一曲: ${list[nextIndex].title} (索引: $nextIndex)")
            persistState()
            list[nextIndex]
        } else {
            Logger.d(TAG, "⏹️ 播放列表结束")
            null
        }
    }
    
    /**
     * 播放上一曲
     * @return 上一个播放项，如果没有则返回 null
     */
    fun playPrevious(): PlaylistItem? {
        val list = _playlist.value
        if (list.isEmpty()) return null
        
        val currentIdx = _currentIndex.value
        
        val prevIndex = when (_playMode.value) {
            PlayMode.SEQUENTIAL, PlayMode.REPEAT_ONE -> {
                // 顺序/单曲循环：上一个
                if (currentIdx > 0) currentIdx - 1 else null
            }
            PlayMode.SHUFFLE -> {
                // 随机播放：从历史记录返回
                if (shuffleHistoryIndex > 0) {
                    shuffleHistoryIndex--
                    shuffleHistory[shuffleHistoryIndex]
                } else null
            }
        }
        
        return if (prevIndex != null && prevIndex in list.indices) {
            _currentIndex.value = prevIndex
            Logger.d(TAG, "⏮️ 播放上一曲: ${list[prevIndex].title} (索引: $prevIndex)")
            persistState()
            list[prevIndex]
        } else {
            Logger.d(TAG, "⏹️ 已是第一曲")
            null
        }
    }
    
    /**
     * 跳转到指定索引
     */
    fun playAt(index: Int): PlaylistItem? {
        val list = _playlist.value
        if (index !in list.indices) return null
        
        _currentIndex.value = index
        
        // 添加到随机历史
        if (_playMode.value == PlayMode.SHUFFLE) {
            shuffleHistory.add(index)
            shuffleHistoryIndex = shuffleHistory.lastIndex
        }
        
        Logger.d(TAG, "🎯 跳转到: ${list[index].title} (索引: $index)")
        persistState()
        return list[index]
    }
    
    /**
     * 检查是否有下一曲
     */
    fun hasNext(): Boolean {
        val list = _playlist.value
        val currentIdx = _currentIndex.value
        
        return when (_playMode.value) {
            PlayMode.SEQUENTIAL -> currentIdx < list.lastIndex
            PlayMode.SHUFFLE -> list.size > 1
            PlayMode.REPEAT_ONE -> true
        }
    }
    
    /**
     * 检查是否有上一曲
     */
    fun hasPrevious(): Boolean {
        val currentIdx = _currentIndex.value
        
        return when (_playMode.value) {
            PlayMode.SEQUENTIAL, PlayMode.REPEAT_ONE -> currentIdx > 0
            PlayMode.SHUFFLE -> shuffleHistoryIndex > 0
        }
    }
    
    /**
     * 获取播放模式显示文本
     */
    fun getPlayModeText(): String {
        return when (_playMode.value) {
            PlayMode.SEQUENTIAL -> "顺序播放"
            PlayMode.SHUFFLE -> "随机播放"
            PlayMode.REPEAT_ONE -> "单曲循环"
        }
    }
    
    /**
     * 获取播放模式图标
     */
    fun getPlayModeIcon(): String {
        return when (_playMode.value) {
            PlayMode.SEQUENTIAL -> "🔂"
            PlayMode.SHUFFLE -> "🔀"
            PlayMode.REPEAT_ONE -> ""
        }
    }

    private fun resolveStartIndex(items: List<PlaylistItem>, requested: Int): Int {
        if (items.isEmpty()) return -1
        return requested.coerceIn(0, items.lastIndex)
    }

    private fun resetShuffleHistoryForCurrentIndex() {
        shuffleHistory.clear()
        val current = _currentIndex.value
        val list = _playlist.value
        if (current in list.indices) {
            shuffleHistory.add(current)
            shuffleHistoryIndex = 0
        } else {
            shuffleHistoryIndex = -1
        }
    }

    private fun persistState() {
        val context = appContext ?: return
        runCatching {
            val snapshot = PlaylistSnapshot(
                playlist = _playlist.value,
                currentIndex = _currentIndex.value,
                playMode = _playMode.value,
                isExternalPlaylist = _isExternalPlaylist.value
            )
            val raw = json.encodeToString(snapshot)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SNAPSHOT, raw)
                .apply()
        }.onFailure { e ->
            Logger.e(TAG, "⚠️ Failed to persist playlist state", e)
        }
    }

    private fun restoreState() {
        val context = appContext ?: return
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SNAPSHOT, null)
            .orEmpty()
        if (raw.isBlank()) return

        runCatching {
            json.decodeFromString<PlaylistSnapshot>(raw)
        }.onSuccess { snapshot ->
            _playlist.value = snapshot.playlist
            _playMode.value = snapshot.playMode
            _isExternalPlaylist.value = snapshot.isExternalPlaylist
            _currentIndex.value = resolveStartIndex(snapshot.playlist, snapshot.currentIndex)
            resetShuffleHistoryForCurrentIndex()
            Logger.d(
                TAG,
                "♻️ Restored playlist: size=${snapshot.playlist.size}, index=${_currentIndex.value}, external=${snapshot.isExternalPlaylist}"
            )
        }.onFailure { e ->
            Logger.e(TAG, "⚠️ Failed to restore playlist state, clearing cache", e)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_SNAPSHOT)
                .apply()
        }
    }
}
