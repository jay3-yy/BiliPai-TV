package com.android.purebilibili.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.*
import com.android.purebilibili.core.util.EasterEggs
import com.android.purebilibili.core.ui.AppIcons
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import io.github.alexzhirkevich.cupertino.CupertinoSwitchDefaults
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import com.android.purebilibili.core.ui.common.copyOnLongPress

// ═══════════════════════════════════════════════════
//  UI 组件 (Stateless Components)
// ═══════════════════════════════════════════════════

// ═══════════════════════════════════════════════════
//  UI 组件 (Stateless Components)
// ═══════════════════════════════════════════════════

// Delegated to core/ui/components/iOSListComponents.kt
import com.android.purebilibili.core.ui.components.IOSSectionTitle as SettingsSectionTitle
import com.android.purebilibili.core.ui.components.IOSGroup as SettingsGroup
import com.android.purebilibili.core.ui.components.IOSSwitchItem as SettingSwitchItem
import com.android.purebilibili.core.ui.components.IOSClickableItem as SettingClickableItem
import com.android.purebilibili.core.ui.components.IOSDivider as SettingsDivider



// ═══════════════════════════════════════════════════
//  业务板块 (Business Sections)
// ═══════════════════════════════════════════════════

@Composable
fun FollowAuthorSection(
    onTelegramClick: () -> Unit,
    onTwitterClick: () -> Unit,
    onDonateClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            iconPainter = painterResource(R.drawable.ic_telegram_mono),
            title = "Telegram 频道",
            value = "@BiliPai",
            onClick = onTelegramClick,
            iconTint = Color(0xFF0088CC),
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = AppIcons.Twitter,
            title = "Twitter / X",
            value = "@YangY_0x00",
            onClick = onTwitterClick,
            iconTint = Color(0xFF1DA1F2),
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Filled.HandThumbsup,
            title = "打赏作者",
            value = "支持开发",
            onClick = onDonateClick,
            iconTint = Color(0xFFFF3B30),
            enableCopy = false
        )
    }
}

@Composable
fun GeneralSection(
    onAppearanceClick: () -> Unit,
    onPlaybackClick: () -> Unit,
    onBottomBarClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.Paintpalette,
            title = "外观设置",
            value = "主题、图标、模糊效果",
            onClick = onAppearanceClick,
            iconTint = iOSPink
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.Play,
            title = "播放设置",
            value = "解码、手势、后台播放",
            onClick = onPlaybackClick,
            iconTint = iOSGreen
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.RectangleStack,
            title = "底栏设置",
            value = "自定义底栏项目",
            onClick = onBottomBarClick,
            iconTint = iOSBlue
        )
    }
}

@Composable
fun SupportToolsSection(
    onTipsClick: () -> Unit,
    onOpenLinksClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.Lightbulb,
            title = "小贴士 & 隐藏操作",
            value = "探索更多功能",
            onClick = onTipsClick,
            iconTint = iOSOrange
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.Link,
            title = "默认打开链接",
            value = "设置应用链接支持",
            onClick = onOpenLinksClick,
            iconTint = iOSTeal
        )
    }
}

@Composable
fun ReleaseChannelPinnedCard(
    onGithubClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onDisclaimerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = CupertinoIcons.Default.Link,
                    contentDescription = null,
                    tint = iOSBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "官方发布渠道仅限 GitHub / Telegram",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "不存在其他官方发布渠道，请注意安装来源安全。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onGithubClick) {
                    Text("GitHub")
                }
                OutlinedButton(onClick = onTelegramClick) {
                    Text("Telegram")
                }
                TextButton(onClick = onDisclaimerClick) {
                    Text("完整声明")
                }
            }
        }
    }
}

@Composable
fun SettingsSubpageEntrySection(
    onContentAndStorageClick: () -> Unit,
    onPrivacyAndSecurityClick: () -> Unit,
    onExtensionsAndDebugClick: () -> Unit,
    onAboutAndSupportClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.FolderBadgePlus,
            title = "内容与存储",
            value = "推荐流、下载与缓存",
            onClick = onContentAndStorageClick,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.HandRaised,
            title = "隐私与安全",
            value = "无痕模式、权限与黑名单",
            onClick = onPrivacyAndSecurityClick,
            iconTint = iOSPurple
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.PuzzlepieceExtension,
            title = "扩展与调试",
            value = "插件、日志与数据采集",
            onClick = onExtensionsAndDebugClick,
            iconTint = iOSTeal
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.InfoCircle,
            title = "关于与支持",
            value = "版本、开源、帮助与作者",
            onClick = onAboutAndSupportClick,
            iconTint = iOSOrange
        )
    }
}

@Composable
fun FeedApiSection(
    feedApiType: com.android.purebilibili.core.store.SettingsManager.FeedApiType,
    onFeedApiTypeChange: (com.android.purebilibili.core.store.SettingsManager.FeedApiType) -> Unit,
    incrementalTimelineRefreshEnabled: Boolean,
    onIncrementalTimelineRefreshChange: (Boolean) -> Unit
) {
    SettingsGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.AntennaRadiowavesLeftAndRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iOSOrange
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "推荐流类型",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = feedApiType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 选项卡
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.android.purebilibili.core.store.SettingsManager.FeedApiType.entries.forEach { type ->
                    val isSelected = feedApiType == type
                    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color)
                            .clickable { onFeedApiTypeChange(type) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold 
                                        else androidx.compose.ui.text.font.FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
        SettingsDivider(startIndent = 66.dp)
        FeedSwitchItem(
            icon = CupertinoIcons.Default.ArrowTriangle2Circlepath,
            title = "动态增量刷新",
            subtitle = "下拉刷新时不重置列表，仅在顶部插入新内容",
            checked = incrementalTimelineRefreshEnabled,
            onCheckedChange = onIncrementalTimelineRefreshChange,
            iconTint = iOSGreen
        )
    }
}

@Composable
private fun FeedSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                CupertinoSwitch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CupertinoSwitchDefaults.colors(
                        thumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = Color(0xFFE9E9EA)
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PrivacySection(
    privacyModeEnabled: Boolean,
    onPrivacyModeChange: (Boolean) -> Unit,
    onPermissionClick: () -> Unit,
    onBlockedListClick: () -> Unit // [New]
) {
    SettingsGroup {
        SettingSwitchItem(
            icon = CupertinoIcons.Default.EyeSlash,
            title = "隐私无痕模式",
            subtitle = "启用后不记录播放历史和搜索历史",
            checked = privacyModeEnabled,
            onCheckedChange = onPrivacyModeChange,
            iconTint = iOSPurple
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.Lock,
            title = "权限管理",
            value = "查看应用权限",
            onClick = onPermissionClick,
            iconTint = iOSTeal
        )
         SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.PersonCropCircle, // Fallback icon
            title = "黑名单管理",
            value = "管理已屏蔽的 UP 主",
            onClick = onBlockedListClick,
            iconTint = iOSRed
        )
    }
}

@Composable
fun DataStorageSection(
    customDownloadPath: String?,
    cacheSize: String,
    onDownloadPathClick: () -> Unit,
    onClearCacheClick: () -> Unit
) {
    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.FolderBadgePlus,
            title = "下载位置",
            value = if (customDownloadPath != null) "自定义" else "默认",
            onClick = onDownloadPathClick,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.Trash,
            title = "清除缓存",
            value = cacheSize,
            onClick = onClearCacheClick,
            iconTint = iOSPink
        )
    }
}

@Composable
fun DeveloperSection(
    crashTrackingEnabled: Boolean,
    analyticsEnabled: Boolean,
    pluginCount: Int,
    onCrashTrackingChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onPluginsClick: () -> Unit,
    onExportLogsClick: () -> Unit
) {
    SettingsGroup {
        SettingSwitchItem(
            icon = CupertinoIcons.Default.ExclamationmarkTriangle,
            title = "崩溃追踪",
            subtitle = "帮助开发者发现和修复问题",
            checked = crashTrackingEnabled,
            onCheckedChange = onCrashTrackingChange,
            iconTint = iOSTeal
        )
        SettingsDivider(startIndent = 66.dp)
        SettingSwitchItem(
            icon = CupertinoIcons.Default.ChartBar,
            title = "使用情况统计",
            subtitle = "帮助改进应用体验，不收集个人信息",
            checked = analyticsEnabled,
            onCheckedChange = onAnalyticsChange,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.PuzzlepieceExtension,
            title = "插件中心",
            value = "$pluginCount 个已启用",
            onClick = onPluginsClick,
            iconTint = iOSPurple
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.DocTextMagnifyingglass,
            title = "导出日志",
            value = "用于反馈问题",
            onClick = onExportLogsClick,
            iconTint = iOSTeal
        )
    }
}

@Composable
fun AboutSection(
    versionName: String,
    easterEggEnabled: Boolean,
    onDisclaimerClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onGithubClick: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onVersionClick: () -> Unit,
    onReplayOnboardingClick: () -> Unit,
    onEasterEggChange: (Boolean) -> Unit,
    updateStatusText: String = "点击检查",
    isCheckingUpdate: Boolean = false,
    versionClickCount: Int = 0,
    versionClickThreshold: Int = EasterEggs.VERSION_EASTER_EGG_THRESHOLD
) {
    val safeThreshold = versionClickThreshold.coerceAtLeast(1)
    val normalizedClickCount = versionClickCount.coerceAtLeast(0)
    val versionProgress = normalizedClickCount.coerceAtMost(safeThreshold).toFloat() / safeThreshold
    val versionIconTint = animateColorAsState(
        targetValue = when {
            normalizedClickCount >= safeThreshold -> iOSGreen
            versionProgress >= 0.85f -> iOSOrange
            versionProgress >= 0.5f -> iOSYellow
            normalizedClickCount > 0 -> iOSBlue
            else -> iOSTeal
        },
        label = "versionIconTint"
    ).value
    val versionHint = when {
        normalizedClickCount <= 0 -> null
        normalizedClickCount >= safeThreshold -> "彩蛋已解锁"
        else -> "还差 ${safeThreshold - normalizedClickCount} 次"
    }
    val versionValue = buildString {
        append("v$versionName")
        versionHint?.let {
            append(" · ")
            append(it)
        }
    }

    SettingsGroup {
        SettingClickableItem(
            icon = CupertinoIcons.Default.Link,
            title = "发布渠道声明",
            value = "仅 GitHub / Telegram",
            onClick = onDisclaimerClick,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.DocText,
            title = "开源许可证",
            value = "License",
            onClick = onLicenseClick,
            iconTint = iOSOrange
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.Link,
            title = "开源主页",
            value = "GitHub",
            onClick = onGithubClick,
            iconTint = iOSPurple,
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.ArrowTriangle2Circlepath,
            title = "检查更新",
            value = if (isCheckingUpdate) "检查中..." else updateStatusText,
            onClick = onCheckUpdateClick,
            iconTint = iOSBlue
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.InfoCircle,
            title = "版本",
            value = versionValue,
            onClick = onVersionClick,
            iconTint = versionIconTint,
            enableCopy = true
        )
        SettingsDivider(startIndent = 66.dp)
        SettingClickableItem(
            icon = CupertinoIcons.Default.BookCircle,
            title = "重播新手引导",
            value = "了解应用功能",
            onClick = onReplayOnboardingClick,
            iconTint = iOSPink
        )
        SettingsDivider(startIndent = 66.dp)
        SettingSwitchItem(
            icon = CupertinoIcons.Default.Gift,
            title = "趣味彩蛋",
            subtitle = "刷新、点赞、投币、搜索时显示趣味提示",
            checked = easterEggEnabled,
            onCheckedChange = onEasterEggChange,
            iconTint = iOSYellow
        )
    }
}
