// 文件路径: core/plugin/PluginManager.kt
package com.android.purebilibili.core.plugin

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private const val TAG = "PluginManager"

/**
 *  插件管理器
 * 
 * 负责管理所有插件的注册、启用/禁用、生命周期调用等。
 * 使用单例模式，在 Application 启动时初始化。
 */
object PluginManager {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /** 所有已注册插件 */
    private val _plugins = mutableStateListOf<PluginInfo>()
    val plugins: List<PluginInfo> get() = _plugins.toList()
    
    /** 插件列表状态流 (用于 Compose 监听) */
    private val _pluginsFlow = MutableStateFlow<List<PluginInfo>>(emptyList())
    val pluginsFlow: StateFlow<List<PluginInfo>> = _pluginsFlow.asStateFlow()

    /** 弹幕插件更新信号（用于播放中热刷新当前弹幕） */
    private val _danmakuPluginUpdateToken = MutableStateFlow(0L)
    val danmakuPluginUpdateToken: StateFlow<Long> = _danmakuPluginUpdateToken.asStateFlow()
    
    private var isInitialized = false
    private lateinit var appContext: Context
    
    /**
     * 初始化插件管理器
     * 应在 Application.onCreate() 中调用
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        isInitialized = true
        Logger.d(TAG, " PluginManager initialized")
    }
    
    /** 获取Application Context供插件使用 */
    fun getContext(): Context = appContext
    
    /**
     * 注册插件
     * 内置插件在 Application 中注册
     */
    fun register(plugin: Plugin) {
        if (_plugins.any { it.plugin.id == plugin.id }) {
            Logger.w(TAG, " Plugin already registered: ${plugin.id}")
            return
        }
        
        scope.launch {
            val enabled = PluginStore.isEnabled(appContext, plugin.id)
            val info = PluginInfo(plugin, enabled)
            _plugins.add(info)
            _pluginsFlow.value = _plugins.toList()
            
            if (enabled) {
                try {
                    plugin.onEnable()
                    Logger.d(TAG, " Plugin enabled on start: ${plugin.name}")
                } catch (e: Exception) {
                    Logger.e(TAG, " Failed to enable plugin: ${plugin.name}", e)
                }
            }
            
            Logger.d(TAG, " Plugin registered: ${plugin.name} (enabled=$enabled)")
        }
    }
    
    /**
     * 启用/禁用插件
     */
    suspend fun setEnabled(pluginId: String, enabled: Boolean) {
        val index = _plugins.indexOfFirst { it.plugin.id == pluginId }
        if (index == -1) {
            Logger.w(TAG, " Plugin not found: $pluginId")
            return
        }
        
        val info = _plugins[index]
        val plugin = info.plugin
        if (info.enabled == enabled) return
        
        try {
            if (enabled && !info.enabled) {
                plugin.onEnable()
                Logger.d(TAG, " Plugin enabled: ${plugin.name}")
            } else if (!enabled && info.enabled) {
                plugin.onDisable()
                Logger.d(TAG, "🔴 Plugin disabled: ${plugin.name}")
            }
            
            // 更新状态
            _plugins[index] = info.copy(enabled = enabled)
            _pluginsFlow.value = _plugins.toList()

            if (plugin is DanmakuPlugin) {
                notifyDanmakuPluginsUpdated()
            }
            
            // 持久化
            PluginStore.setEnabled(appContext, pluginId, enabled)
            
        } catch (e: Exception) {
            Logger.e(TAG, " Failed to toggle plugin: ${plugin.name}", e)
        }
    }
    
    /**
     * 获取指定类型的所有已启用插件
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Plugin> getEnabledPlugins(type: KClass<T>): List<T> {
        return _plugins
            .filter { it.enabled && type.isInstance(it.plugin) }
            .map { it.plugin as T }
    }
    
    /**
     * 获取所有 PlayerPlugin
     */
    fun getEnabledPlayerPlugins(): List<PlayerPlugin> = getEnabledPlugins(PlayerPlugin::class)
    
    /**
     * 获取所有 DanmakuPlugin
     */
    fun getEnabledDanmakuPlugins(): List<DanmakuPlugin> = getEnabledPlugins(DanmakuPlugin::class)
    
    /**
     * 获取所有 FeedPlugin
     */
    fun getEnabledFeedPlugins(): List<FeedPlugin> = getEnabledPlugins(FeedPlugin::class)
    
    /**
     *  使用所有启用的 FeedPlugin 过滤视频列表
     * 用于首页推荐和搜索结果
     */
    fun filterFeedItems(items: List<com.android.purebilibili.data.model.response.VideoItem>): List<com.android.purebilibili.data.model.response.VideoItem> {
        val feedPlugins = getEnabledFeedPlugins()
        if (feedPlugins.isEmpty()) return items
        
        return items.filter { item ->
            feedPlugins.all { plugin ->
                try {
                    plugin.shouldShowItem(item)
                } catch (e: Exception) {
                    Logger.e(TAG, " Feed plugin failed: ${plugin.name}", e)
                    true
                }
            }
        }
    }
    
    /**
     * 获取已启用插件数量
     */
    fun getEnabledCount(): Int = _plugins.count { it.enabled }

    fun notifyDanmakuPluginsUpdated() {
        _danmakuPluginUpdateToken.value = System.currentTimeMillis()
    }
}

/**
 * 插件信息包装类
 */
data class PluginInfo(
    val plugin: Plugin,
    val enabled: Boolean
)
