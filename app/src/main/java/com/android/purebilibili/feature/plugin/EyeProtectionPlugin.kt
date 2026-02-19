package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.ui.components.IOSSwitchItem
import com.android.purebilibili.core.util.Logger
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Moon
import io.github.alexzhirkevich.cupertino.icons.filled.SunMax
import io.github.alexzhirkevich.cupertino.icons.outlined.Clock
import io.github.alexzhirkevich.cupertino.icons.outlined.Heart
import io.github.alexzhirkevich.cupertino.icons.outlined.Lightbulb
import io.github.alexzhirkevich.cupertino.icons.outlined.Moon
import io.github.alexzhirkevich.cupertino.icons.outlined.Sparkles
import io.github.alexzhirkevich.cupertino.icons.outlined.SunMax
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.Calendar

private const val TAG = "EyeProtectionPlugin"

@Serializable
enum class EyeCarePreset {
    GENTLE,
    BALANCED,
    FOCUS,
    CUSTOM
}

@Serializable
data class EyeCareProfile(
    val brightnessLevel: Float,
    val warmFilterStrength: Float,
    val reminderIntervalMinutes: Int,
    val snoozeMinutes: Int
)

@Serializable
data class EyeCareReminder(
    val usageMinutes: Int,
    val title: String,
    val message: String,
    val suggestion: String
)

@Serializable
data class EyeProtectionConfig(
    // 定时护眼模式
    val nightModeEnabled: Boolean = true,
    val nightModeStartHour: Int = 22,
    val nightModeEndHour: Int = 7,

    // 使用时长提醒
    val usageReminderEnabled: Boolean = true,
    val usageDurationMinutes: Int = 30,
    val reminderSnoozeMinutes: Int = 10,
    val remindOnlyDuringNight: Boolean = true,

    // 显示调节
    val brightnessLevel: Float = 0.78f,
    val warmFilterStrength: Float = 0.22f,

    // 当前选中的模式（可 DIY）
    val carePreset: EyeCarePreset = EyeCarePreset.BALANCED,
    val profileGentle: EyeCareProfile = EyeCareProfile(
        brightnessLevel = 0.88f,
        warmFilterStrength = 0.12f,
        reminderIntervalMinutes = 45,
        snoozeMinutes = 10
    ),
    val profileBalanced: EyeCareProfile = EyeCareProfile(
        brightnessLevel = 0.78f,
        warmFilterStrength = 0.22f,
        reminderIntervalMinutes = 30,
        snoozeMinutes = 10
    ),
    val profileFocus: EyeCareProfile = EyeCareProfile(
        brightnessLevel = 0.65f,
        warmFilterStrength = 0.32f,
        reminderIntervalMinutes = 25,
        snoozeMinutes = 5
    ),

    // 手动强制开启
    val forceEnabled: Boolean = false
)

class EyeProtectionPlugin : Plugin {

    override val id = "eye_protection"
    override val name = "夜间护眼"
    override val description = "夜间护眼、休息提醒与温和关怀"
    override val version = "2.0.0"
    override val author = "YangY"
    override val icon: ImageVector = CupertinoIcons.Default.Moon

    private var config: EyeProtectionConfig = EyeProtectionConfig()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var usageTrackingJob: Job? = null

    // 使用时长（分钟）
    private var usageMinutes = 0
    private var snoozeUntilMinute: Int? = null
    private var lastReminderMinute: Int? = null

    private val _isNightModeActive = MutableStateFlow(false)
    val isNightModeActive: StateFlow<Boolean> = _isNightModeActive.asStateFlow()

    private val _brightnessLevel = MutableStateFlow(1.0f)
    val brightnessLevel: StateFlow<Float> = _brightnessLevel.asStateFlow()

    private val _warmFilterStrength = MutableStateFlow(0f)
    val warmFilterStrength: StateFlow<Float> = _warmFilterStrength.asStateFlow()

    private val _settingsPreviewEnabled = MutableStateFlow(false)
    val settingsPreviewEnabled: StateFlow<Boolean> = _settingsPreviewEnabled.asStateFlow()

    private val _careReminder = MutableStateFlow<EyeCareReminder?>(null)
    val careReminder: StateFlow<EyeCareReminder?> = _careReminder.asStateFlow()

    override suspend fun onEnable() {
        loadConfigSuspend()
        applyVisualState()
        startUsageTracking()
        Logger.d(TAG, "夜间护眼插件已启用")
    }

    override suspend fun onDisable() {
        usageTrackingJob?.cancel()
        usageMinutes = 0
        snoozeUntilMinute = null
        lastReminderMinute = null
        _careReminder.value = null
        _settingsPreviewEnabled.value = false
        _isNightModeActive.value = false
        _brightnessLevel.value = 1.0f
        _warmFilterStrength.value = 0f
        Logger.d(TAG, "夜间护眼插件已禁用")
    }

    fun dismissReminder() {
        _careReminder.value = null
    }

    fun snoozeReminder() {
        snoozeUntilMinute = usageMinutes + config.reminderSnoozeMinutes
        _careReminder.value = null
        Logger.d(TAG, "提醒已暂缓 ${config.reminderSnoozeMinutes} 分钟")
    }

    fun confirmRest() {
        usageMinutes = 0
        snoozeUntilMinute = null
        lastReminderMinute = null
        _careReminder.value = null
        Logger.d(TAG, "用户已确认休息，计时重置")
    }

    fun getSnoozeMinutes(): Int = config.reminderSnoozeMinutes

    fun setSettingsPreviewEnabled(enabled: Boolean) {
        _settingsPreviewEnabled.value = enabled
        applyVisualState()
    }

    private fun startUsageTracking() {
        usageTrackingJob?.cancel()
        usageTrackingJob = workerScope.launch {
            while (isActive) {
                delay(60_000)
                applyVisualState()

                if (!shouldCountUsageMinute()) continue

                usageMinutes++
                if (shouldTriggerCareReminder(
                        usageMinutes = usageMinutes,
                        intervalMinutes = config.usageDurationMinutes,
                        snoozeUntilMinute = snoozeUntilMinute,
                        lastReminderMinute = lastReminderMinute
                    )
                ) {
                    lastReminderMinute = usageMinutes
                    _careReminder.value = EyeCareReminder(
                        usageMinutes = usageMinutes,
                        title = "给眼睛一个小休息",
                        message = buildCareReminderMessage(usageMinutes),
                        suggestion = "试试 20-20-20：看向远处 20 秒"
                    )
                    Logger.d(TAG, "触发护眼提醒：$usageMinutes 分钟")
                }
            }
        }
    }

    private fun shouldCountUsageMinute(): Boolean {
        if (_settingsPreviewEnabled.value) return false
        if (!config.usageReminderEnabled) return false

        // 避免应用在后台也累计使用时长
        val appForeground = androidx.lifecycle.ProcessLifecycleOwner.get()
            .lifecycle.currentState
            .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        if (!appForeground) return false

        if (!config.remindOnlyDuringNight) return true
        return _isNightModeActive.value || config.forceEnabled
    }

    private fun applyVisualState() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val visualState = resolveEyeVisualState(
            settingsPreviewEnabled = _settingsPreviewEnabled.value,
            forceEnabled = config.forceEnabled,
            nightModeEnabled = config.nightModeEnabled,
            currentHour = currentHour,
            startHour = config.nightModeStartHour,
            endHour = config.nightModeEndHour,
            brightnessLevel = config.brightnessLevel,
            warmFilterStrength = config.warmFilterStrength
        )
        _isNightModeActive.value = visualState.isActive
        _brightnessLevel.value = visualState.brightnessLevel
        _warmFilterStrength.value = visualState.warmFilterStrength
    }

    private suspend fun loadConfigSuspend() {
        try {
            val context = PluginManager.getContext()
            val jsonStr = PluginStore.getConfigJson(context, id)
            if (!jsonStr.isNullOrBlank()) {
                config = Json.decodeFromString<EyeProtectionConfig>(jsonStr)
            }
            config = applyPresetConfig(config, config.carePreset)
        } catch (e: Exception) {
            Logger.e(TAG, "加载配置失败", e)
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

    private fun normalizePreset(preset: EyeCarePreset): EyeCarePreset {
        return if (preset == EyeCarePreset.CUSTOM) EyeCarePreset.BALANCED else preset
    }

    private fun profileForPreset(
        source: EyeProtectionConfig,
        preset: EyeCarePreset
    ): EyeCareProfile {
        return when (normalizePreset(preset)) {
            EyeCarePreset.GENTLE -> source.profileGentle
            EyeCarePreset.BALANCED -> source.profileBalanced
            EyeCarePreset.FOCUS -> source.profileFocus
            EyeCarePreset.CUSTOM -> source.profileBalanced
        }
    }

    private fun withPresetProfile(
        source: EyeProtectionConfig,
        preset: EyeCarePreset,
        profile: EyeCareProfile
    ): EyeProtectionConfig {
        return when (normalizePreset(preset)) {
            EyeCarePreset.GENTLE -> source.copy(profileGentle = profile)
            EyeCarePreset.BALANCED -> source.copy(profileBalanced = profile)
            EyeCarePreset.FOCUS -> source.copy(profileFocus = profile)
            EyeCarePreset.CUSTOM -> source.copy(profileBalanced = profile)
        }
    }

    private fun applyPresetConfig(
        source: EyeProtectionConfig,
        preset: EyeCarePreset
    ): EyeProtectionConfig {
        val normalizedPreset = normalizePreset(preset)
        val profile = profileForPreset(source, normalizedPreset)
        return source.copy(
            carePreset = normalizedPreset,
            brightnessLevel = profile.brightnessLevel.coerceIn(0.3f, 1.0f),
            warmFilterStrength = profile.warmFilterStrength.coerceIn(0f, 0.5f),
            usageDurationMinutes = profile.reminderIntervalMinutes.coerceAtLeast(1),
            reminderSnoozeMinutes = profile.snoozeMinutes.coerceAtLeast(1)
        )
    }

    private fun persistCurrentValuesToSelectedPreset(source: EyeProtectionConfig): EyeProtectionConfig {
        val normalizedPreset = normalizePreset(source.carePreset)
        val profile = EyeCareProfile(
            brightnessLevel = source.brightnessLevel.coerceIn(0.3f, 1.0f),
            warmFilterStrength = source.warmFilterStrength.coerceIn(0f, 0.5f),
            reminderIntervalMinutes = source.usageDurationMinutes.coerceAtLeast(1),
            snoozeMinutes = source.reminderSnoozeMinutes.coerceAtLeast(1)
        )
        return withPresetProfile(source.copy(carePreset = normalizedPreset), normalizedPreset, profile)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun SettingsContent() {
        var uiConfig by remember { mutableStateOf(config) }

        LaunchedEffect(Unit) {
            loadConfigSuspend()
            uiConfig = config
            applyVisualState()
        }
        DisposableEffect(Unit) {
            setSettingsPreviewEnabled(true)
            onDispose {
                setSettingsPreviewEnabled(false)
            }
        }

        fun updateConfig(newConfig: EyeProtectionConfig, refreshVisual: Boolean = true) {
            uiConfig = newConfig
            config = newConfig
            saveConfig()
            if (refreshVisual) applyVisualState()
        }

        val presets = listOf(
            EyeCarePreset.GENTLE to "轻柔",
            EyeCarePreset.BALANCED to "平衡",
            EyeCarePreset.FOCUS to "专注"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IOSSwitchItem(
                icon = CupertinoIcons.Outlined.Lightbulb,
                title = "立即开启护眼",
                subtitle = "手动强制开启，不受时间段限制",
                checked = uiConfig.forceEnabled,
                onCheckedChange = { enabled ->
                    updateConfig(uiConfig.copy(forceEnabled = enabled))
                },
                iconTint = Color(0xFFFFB74D)
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            IOSSwitchItem(
                icon = CupertinoIcons.Outlined.Moon,
                title = "定时护眼模式",
                subtitle = "${uiConfig.nightModeStartHour}:00 - ${uiConfig.nightModeEndHour}:00 自动开启",
                checked = uiConfig.nightModeEnabled,
                onCheckedChange = { enabled ->
                    updateConfig(uiConfig.copy(nightModeEnabled = enabled))
                },
                iconTint = Color(0xFF7E57C2)
            )

            if (uiConfig.nightModeEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 56.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimePickerDropdown(
                        modifier = Modifier.weight(1f),
                        selectedHour = uiConfig.nightModeStartHour,
                        onHourSelected = { hour ->
                            updateConfig(uiConfig.copy(nightModeStartHour = hour))
                        },
                        label = "开始"
                    )
                    TimePickerDropdown(
                        modifier = Modifier.weight(1f),
                        selectedHour = uiConfig.nightModeEndHour,
                        onHourSelected = { hour ->
                            updateConfig(uiConfig.copy(nightModeEndHour = hour))
                        },
                        label = "结束"
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp, top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            IOSSwitchItem(
                icon = CupertinoIcons.Outlined.Clock,
                title = "关怀提醒",
                subtitle = "定时提醒休息、看远处、放松肩颈",
                checked = uiConfig.usageReminderEnabled,
                onCheckedChange = { enabled ->
                    updateConfig(uiConfig.copy(usageReminderEnabled = enabled), refreshVisual = false)
                },
                iconTint = Color(0xFF42A5F5)
            )

            if (uiConfig.usageReminderEnabled) {
                Spacer(modifier = Modifier.height(10.dp))

                IOSSwitchItem(
                    icon = CupertinoIcons.Outlined.Sparkles,
                    title = "仅夜间提醒",
                    subtitle = "白天减少打扰，夜间更积极守护",
                    checked = uiConfig.remindOnlyDuringNight,
                    onCheckedChange = { enabled ->
                        updateConfig(uiConfig.copy(remindOnlyDuringNight = enabled), refreshVisual = false)
                    },
                    iconTint = Color(0xFF5C6BC0)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "提醒频率",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 56.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 56.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20, 30, 45, 60).forEach { minutes ->
                        FilterChip(
                            selected = uiConfig.usageDurationMinutes == minutes,
                            onClick = {
                                val changed = uiConfig.copy(usageDurationMinutes = minutes)
                                updateConfig(persistCurrentValuesToSelectedPreset(changed), refreshVisual = false)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 84.dp),
                            label = {
                                Text(
                                    text = "${minutes}分钟",
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "稍后提醒",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 56.dp)
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 56.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { minutes ->
                        FilterChip(
                            selected = uiConfig.reminderSnoozeMinutes == minutes,
                            onClick = {
                                val changed = uiConfig.copy(reminderSnoozeMinutes = minutes)
                                updateConfig(persistCurrentValuesToSelectedPreset(changed), refreshVisual = false)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 84.dp),
                            label = {
                                Text(
                                    text = "${minutes}分钟",
                                    softWrap = false,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "关怀强度预设",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "三种模式都可 DIY，当前模式下的调节会自动保存",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (preset, label) ->
                    FilterChip(
                        selected = uiConfig.carePreset == preset,
                        onClick = {
                            val changed = applyPresetConfig(uiConfig, preset)
                            updateConfig(changed)
                        },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "显示调节",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "当前页面是实时预览；离开设置后按“立即开启护眼/定时护眼”规则生效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        CupertinoIcons.Filled.SunMax,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Text("亮度", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = "${(uiConfig.brightnessLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = uiConfig.brightnessLevel,
                onValueChange = { value ->
                    val newConfig = persistCurrentValuesToSelectedPreset(
                        uiConfig.copy(brightnessLevel = value)
                    )
                    uiConfig = newConfig
                    config = newConfig
                    _brightnessLevel.value = value
                },
                onValueChangeFinished = {
                    saveConfig()
                    applyVisualState()
                },
                valueRange = 0.3f..1.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        CupertinoIcons.Outlined.SunMax,
                        contentDescription = null,
                        tint = Color(0xFFFF7043),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Text("暖色滤镜", style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    text = "${(uiConfig.warmFilterStrength * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = uiConfig.warmFilterStrength,
                onValueChange = { value ->
                    val newConfig = persistCurrentValuesToSelectedPreset(
                        uiConfig.copy(warmFilterStrength = value)
                    )
                    uiConfig = newConfig
                    config = newConfig
                    _warmFilterStrength.value = value
                },
                onValueChangeFinished = {
                    saveConfig()
                    applyVisualState()
                },
                valueRange = 0f..0.5f,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "护眼滤镜不影响触摸操作。建议搭配夜间模式与定时休息。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.Heart,
                    contentDescription = null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "照顾好自己，视频永远看得完。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    companion object {
        fun getInstance(): EyeProtectionPlugin? {
            return PluginManager.plugins.find { it.plugin.id == "eye_protection" }?.plugin as? EyeProtectionPlugin
        }
    }
}

@Composable
private fun TimePickerDropdown(
    modifier: Modifier = Modifier,
    selectedHour: Int,
    onHourSelected: (Int) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%02d:00", selectedHour),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = CupertinoIcons.Outlined.Clock,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            (0..23).forEach { hour ->
                DropdownMenuItem(
                    text = { Text(String.format("%02d:00", hour)) },
                    trailingIcon = {
                        if (hour == selectedHour) {
                            Text(
                                text = "当前",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        onHourSelected(hour)
                        expanded = false
                    }
                )
            }
        }
    }
}
