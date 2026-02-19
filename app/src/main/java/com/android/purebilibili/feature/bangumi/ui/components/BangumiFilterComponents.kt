// 文件路径: feature/bangumi/ui/components/BangumiFilterComponents.kt
package com.android.purebilibili.feature.bangumi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronDown
import com.android.purebilibili.data.model.response.BangumiFilter
import com.android.purebilibili.feature.bangumi.BangumiDisplayMode
import com.android.purebilibili.feature.bangumi.resolveBangumiTopModes
import androidx.compose.ui.text.font.FontWeight

/**
 * 模式切换 Tabs (索引/时间表)
 */
@Composable
fun BangumiModeTabs(
    currentMode: BangumiDisplayMode,
    onModeChange: (BangumiDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = resolveBangumiTopModes().map { mode ->
        mode to when (mode) {
            BangumiDisplayMode.LIST -> "索引"
            BangumiDisplayMode.TIMELINE -> "时间表"
            else -> mode.name
        }
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            modes.forEach { (mode, label) ->
                val isSelected = currentMode == mode
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = { onModeChange(mode) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 筛选器面板
 */
@Composable
fun BangumiFilterPanel(
    filter: BangumiFilter,
    onFilterChange: (BangumiFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOrderMenu by remember { mutableStateOf(false) }
    var showAreaMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 排序
                FilterChip(
                    label = BangumiFilter.ORDER_OPTIONS.find { it.first == filter.order }?.second ?: "排序",
                    isActive = filter.order != 2,
                    onClick = { showOrderMenu = true },
                    expanded = showOrderMenu,
                    options = BangumiFilter.ORDER_OPTIONS.map { it.second },
                    onOptionSelected = { index ->
                        onFilterChange(filter.copy(order = BangumiFilter.ORDER_OPTIONS[index].first))
                        showOrderMenu = false
                    },
                    onDismiss = { showOrderMenu = false }
                )
                
                // 地区
                FilterChip(
                    label = BangumiFilter.AREA_OPTIONS.find { it.first == filter.area }?.second ?: "地区",
                    isActive = filter.area != -1,
                    onClick = { showAreaMenu = true },
                    expanded = showAreaMenu,
                    options = BangumiFilter.AREA_OPTIONS.map { it.second },
                    onOptionSelected = { index ->
                        onFilterChange(filter.copy(area = BangumiFilter.AREA_OPTIONS[index].first))
                        showAreaMenu = false
                    },
                    onDismiss = { showAreaMenu = false }
                )
                
                // 状态
                FilterChip(
                    label = BangumiFilter.STATUS_OPTIONS.find { it.first == filter.isFinish }?.second ?: "状态",
                    isActive = filter.isFinish != -1,
                    onClick = { showStatusMenu = true },
                    expanded = showStatusMenu,
                    options = BangumiFilter.STATUS_OPTIONS.map { it.second },
                    onOptionSelected = { index ->
                        onFilterChange(filter.copy(isFinish = BangumiFilter.STATUS_OPTIONS[index].first))
                        showStatusMenu = false
                    },
                    onDismiss = { showStatusMenu = false }
                )
                
                // 年份
                FilterChip(
                    label = BangumiFilter.YEAR_OPTIONS.find { it.first == filter.year }?.second ?: "年份",
                    isActive = filter.year != "-1",
                    onClick = { showYearMenu = true },
                    expanded = showYearMenu,
                    options = BangumiFilter.YEAR_OPTIONS.map { it.second },
                    onOptionSelected = { index ->
                        onFilterChange(filter.copy(year = BangumiFilter.YEAR_OPTIONS[index].first))
                        showYearMenu = false
                    },
                    onDismiss = { showYearMenu = false }
                )
            }
            
            // 重置按钮
            if (filter != BangumiFilter()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onFilterChange(BangumiFilter()) }
                ) {
                    Text("重置筛选", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * 筛选选项 Chip
 */
@Composable
fun FilterChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    expanded: Boolean,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    CupertinoIcons.Default.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 14.sp) },
                    onClick = { onOptionSelected(index) }
                )
            }
        }
    }
}
