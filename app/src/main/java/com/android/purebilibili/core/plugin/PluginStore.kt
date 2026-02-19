// 文件路径: core/plugin/PluginStore.kt
package com.android.purebilibili.core.plugin

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.pluginDataStore by preferencesDataStore(name = "plugin_prefs")

/**
 * 🗄️ 插件配置持久化存储
 * 
 * 使用 DataStore 存储每个插件的启用状态和配置
 */
object PluginStore {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    /**
     * 获取插件启用状态 (Flow)
     */
    fun isEnabledFlow(context: Context, pluginId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("plugin_enabled_$pluginId")
        return context.pluginDataStore.data.map { prefs ->
            prefs[key] ?: false  // 默认禁用
        }
    }
    
    /**
     * 获取插件启用状态 (同步)
     */
    suspend fun isEnabled(context: Context, pluginId: String): Boolean {
        return isEnabledFlow(context, pluginId).first()
    }
    
    /**
     * 设置插件启用状态
     */
    suspend fun setEnabled(context: Context, pluginId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("plugin_enabled_$pluginId")
        context.pluginDataStore.edit { prefs ->
            prefs[key] = enabled
        }
    }
    
    /**
     * 获取插件配置 (JSON 字符串)
     */
    suspend fun getConfigJson(context: Context, pluginId: String): String? {
        val key = stringPreferencesKey("plugin_config_$pluginId")
        return context.pluginDataStore.data.map { prefs ->
            prefs[key]
        }.first()
    }
    
    /**
     * 设置插件配置 (JSON 字符串)
     */
    suspend fun setConfigJson(context: Context, pluginId: String, configJson: String) {
        val key = stringPreferencesKey("plugin_config_$pluginId")
        context.pluginDataStore.edit { prefs ->
            prefs[key] = configJson
        }
    }
}
