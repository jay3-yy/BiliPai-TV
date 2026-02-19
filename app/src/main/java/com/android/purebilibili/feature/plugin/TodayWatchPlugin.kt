package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.store.TodayWatchFeedbackStore
import com.android.purebilibili.core.store.TodayWatchProfileStore
import com.android.purebilibili.core.ui.components.IOSSwitchItem
import com.android.purebilibili.core.util.Logger
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.Lightbulb
import io.github.alexzhirkevich.cupertino.icons.outlined.ListBullet
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "TodayWatchPlugin"

@Serializable
enum class TodayWatchPluginMode {
    RELAX,
    LEARN
}

@Serializable
data class TodayWatchPluginConfig(
    val currentMode: TodayWatchPluginMode = TodayWatchPluginMode.RELAX,
    val upRankLimit: Int = 5,
    val queueBuildLimit: Int = 20,
    val queuePreviewLimit: Int = 6,
    val historySampleLimit: Int = 80,
    val linkEyeCareSignal: Boolean = true,
    val showUpRank: Boolean = true,
    val showReasonHint: Boolean = true,
    val enableWaterfallAnimation: Boolean = true,
    val waterfallExponent: Float = 1.38f,
    val collapsed: Boolean = false,
    val refreshTriggerToken: Long = 0L
)

class TodayWatchPlugin : Plugin {

    override val id: String = PLUGIN_ID
    override val name: String = "今日推荐单"
    override val description: String = "本地分析观看历史，生成可定制推荐队列"
    override val version: String = "1.0.0"
    override val author: String = "YangY"
    override val icon: ImageVector = CupertinoIcons.Default.ListBullet

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var config: TodayWatchPluginConfig = TodayWatchPluginConfig()
    private val _configState = MutableStateFlow(config)
    val configState: StateFlow<TodayWatchPluginConfig> = _configState.asStateFlow()

    override suspend fun onEnable() {
        loadConfigSuspend()
        Logger.d(TAG, "今日推荐单插件已启用")
    }

    override suspend fun onDisable() {
        Logger.d(TAG, "今日推荐单插件已禁用")
    }

    fun updateConfig(transform: (TodayWatchPluginConfig) -> TodayWatchPluginConfig) {
        val updated = normalizeConfig(transform(config))
        if (updated == config) return
        config = updated
        _configState.value = updated
        saveConfig()
    }

    fun setCurrentMode(mode: TodayWatchPluginMode) {
        updateConfig { it.copy(currentMode = mode) }
    }

    fun clearPersonalizationData() {
        ioScope.launch {
            try {
                val context = PluginManager.getContext()
                TodayWatchProfileStore.clear(context)
                TodayWatchFeedbackStore.clear(context)
                updateConfig { current ->
                    current.copy(refreshTriggerToken = System.currentTimeMillis())
                }
                Logger.d(TAG, "已清空今日推荐画像与反馈缓存")
            } catch (e: Exception) {
                Logger.e(TAG, "清空画像失败", e)
            }
        }
    }

    private suspend fun loadConfigSuspend() {
        try {
            val context = PluginManager.getContext()
            val json = PluginStore.getConfigJson(context, id)
            config = if (json.isNullOrBlank()) {
                TodayWatchPluginConfig()
            } else {
                normalizeConfig(Json.decodeFromString(json))
            }
            _configState.value = config
        } catch (e: Exception) {
            Logger.e(TAG, "加载配置失败", e)
            config = TodayWatchPluginConfig()
            _configState.value = config
        }
    }

    private fun saveConfig() {
        ioScope.launch {
            try {
                val context = PluginManager.getContext()
                PluginStore.setConfigJson(context, id, Json.encodeToString(config))
            } catch (e: Exception) {
                Logger.e(TAG, "保存配置失败", e)
            }
        }
    }

    private fun normalizeConfig(source: TodayWatchPluginConfig): TodayWatchPluginConfig {
        val upRankLimit = source.upRankLimit.coerceIn(1, 12)
        val queueBuildLimit = source.queueBuildLimit.coerceIn(6, 40)
        val queuePreviewLimit = source.queuePreviewLimit.coerceIn(3, 12).coerceAtMost(queueBuildLimit)
        return source.copy(
            upRankLimit = upRankLimit,
            queueBuildLimit = queueBuildLimit,
            queuePreviewLimit = queuePreviewLimit,
            historySampleLimit = source.historySampleLimit.coerceIn(20, 120),
            waterfallExponent = source.waterfallExponent.coerceIn(1.0f, 2.2f)
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun SettingsContent() {
        val configSnapshot by configState.collectAsState()
        var uiConfig by remember { mutableStateOf(configSnapshot) }
        var showResetDialog by remember { mutableStateOf(false) }
        var resetMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            loadConfigSuspend()
        }
        LaunchedEffect(configSnapshot) {
            uiConfig = configSnapshot
        }

        fun commit(next: TodayWatchPluginConfig) {
            updateConfig { next }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "默认模式",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TodayWatchPluginMode.entries.forEach { mode ->
                    val label = if (mode == TodayWatchPluginMode.RELAX) "今晚轻松看" else "深度学习看"
                    FilterChip(
                        selected = uiConfig.currentMode == mode,
                        onClick = { commit(uiConfig.copy(currentMode = mode)) },
                        label = { Text(label) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text(
                text = "推荐规模",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text("UP主榜数量", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 8, 10).forEach { value ->
                    FilterChip(
                        selected = uiConfig.upRankLimit == value,
                        onClick = { commit(uiConfig.copy(upRankLimit = value)) },
                        label = { Text("$value 个") }
                    )
                }
            }

            Text("队列生成长度", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(12, 20, 30, 40).forEach { value ->
                    FilterChip(
                        selected = uiConfig.queueBuildLimit == value,
                        onClick = {
                            val preview = uiConfig.queuePreviewLimit.coerceAtMost(value)
                            commit(uiConfig.copy(queueBuildLimit = value, queuePreviewLimit = preview))
                        },
                        label = { Text("$value 条") }
                    )
                }
            }

            Text("卡片展示条数", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 6, 8, 10).forEach { value ->
                    val clamped = value.coerceAtMost(uiConfig.queueBuildLimit)
                    FilterChip(
                        selected = uiConfig.queuePreviewLimit == clamped,
                        onClick = { commit(uiConfig.copy(queuePreviewLimit = clamped)) },
                        label = { Text("$clamped 条") }
                    )
                }
            }

            Text("历史样本量", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(40, 80, 120).forEach { value ->
                    FilterChip(
                        selected = uiConfig.historySampleLimit == value,
                        onClick = { commit(uiConfig.copy(historySampleLimit = value)) },
                        label = { Text("$value 条") }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            IOSSwitchItem(
                icon = CupertinoIcons.Default.Sparkles,
                title = "联动护眼信号",
                subtitle = "夜间优先短时长、低刺激内容",
                checked = uiConfig.linkEyeCareSignal,
                onCheckedChange = { enabled -> commit(uiConfig.copy(linkEyeCareSignal = enabled)) }
            )

            IOSSwitchItem(
                icon = CupertinoIcons.Default.Lightbulb,
                title = "显示模式说明",
                subtitle = "显示“已结合护眼状态”等提示文案",
                checked = uiConfig.showReasonHint,
                onCheckedChange = { enabled -> commit(uiConfig.copy(showReasonHint = enabled)) }
            )

            IOSSwitchItem(
                icon = CupertinoIcons.Default.ListBullet,
                title = "显示 UP 主榜",
                subtitle = "在卡片中展示你近期偏好的创作者",
                checked = uiConfig.showUpRank,
                onCheckedChange = { enabled -> commit(uiConfig.copy(showUpRank = enabled)) }
            )

            IOSSwitchItem(
                icon = CupertinoIcons.Default.Sparkles,
                title = "瀑布展开动画",
                subtitle = "卡片内容按非线性节奏依次展开",
                checked = uiConfig.enableWaterfallAnimation,
                onCheckedChange = { enabled -> commit(uiConfig.copy(enableWaterfallAnimation = enabled)) }
            )

            if (uiConfig.enableWaterfallAnimation) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("动画曲率", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        1.2f to "柔和",
                        1.38f to "标准",
                        1.6f to "明显",
                        1.9f to "强烈"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(uiConfig.waterfallExponent - value) < 0.01f,
                            onClick = { commit(uiConfig.copy(waterfallExponent = value)) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = "推荐画像维护",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "会清空本地学习到的偏好与不感兴趣反馈，推荐会回到冷启动状态。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { showResetDialog = true }
            ) {
                Text("清空本地推荐画像与反馈")
            }

            if (!resetMessage.isNullOrBlank()) {
                Text(
                    text = resetMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "所有设置仅在本地生效，不上传你的历史记录。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("清空推荐画像") },
                text = { Text("确定清空本地推荐画像与不感兴趣反馈吗？该操作不可恢复。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            clearPersonalizationData()
                            resetMessage = "已清空，本地推荐将重新学习"
                            showResetDialog = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }

    companion object {
        const val PLUGIN_ID: String = "today_watch"

        fun getInstance(): TodayWatchPlugin? {
            return PluginManager.plugins
                .find { it.plugin.id == PLUGIN_ID }
                ?.plugin as? TodayWatchPlugin
        }
    }
}
