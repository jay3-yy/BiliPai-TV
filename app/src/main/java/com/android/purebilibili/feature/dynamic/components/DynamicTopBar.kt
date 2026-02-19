// 文件路径: feature/dynamic/components/DynamicTopBar.kt
package com.android.purebilibili.feature.dynamic.components

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.blur.BlurStyles
import com.android.purebilibili.core.ui.blur.BlurIntensity
import dev.chrisbanes.haze.HazeState

//  动态页面布局模式
enum class DynamicDisplayMode {
    SIDEBAR,     // 侧边栏模式（默认，UP主列表在左侧）
    HORIZONTAL   // 横向模式（UP主列表在顶部，类似 Telegram）
}

/**
 *  带Tab的顶栏
 */
@Composable
fun DynamicTopBarWithTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    displayMode: DynamicDisplayMode = DynamicDisplayMode.SIDEBAR,
    onDisplayModeChange: (DynamicDisplayMode) -> Unit = {},
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } }
    
    //  读取当前模糊强度以确定背景透明度
    val blurIntensity by SettingsManager.getBlurIntensity(androidx.compose.ui.platform.LocalContext.current)
        .collectAsState(initial = BlurIntensity.THIN)
    val backgroundAlpha = BlurStyles.getBackgroundAlpha(blurIntensity)
    
    //  使用 blurIntensity 对应的背景透明度实现毛玻璃质感
    val headerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (hazeState != null) backgroundAlpha else 0f)

    //  [关键修复] 使用透明背景，让主界面的渐变透出来
    Box(
        modifier = modifier
            .fillMaxWidth()
            // 应用模糊效果
            .then(if (hazeState != null) Modifier.unifiedBlur(hazeState) else Modifier)
            .background(headerColor)
    ) {
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))
            
            //  标题行：标题 - 高度设为 44dp 以与左侧边栏返回按钮对齐
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp) // 固定高度 44dp
                    .padding(horizontal = 16.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题
                Text(
                    "动态",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground // 自适应颜色
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                //  布局模式切换按钮
                IconButton(
                    onClick = {
                        val newMode = if (displayMode == DynamicDisplayMode.SIDEBAR) 
                            DynamicDisplayMode.HORIZONTAL else DynamicDisplayMode.SIDEBAR
                        onDisplayModeChange(newMode)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (displayMode == DynamicDisplayMode.SIDEBAR)
                            CupertinoIcons.Default.ListBullet else CupertinoIcons.Default.RectangleStack,
                        contentDescription = "切换布局模式",
                        tint = MaterialTheme.colorScheme.onSurface, // 自适应颜色
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            // Tab栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onTabSelected(index) }
                            .padding(end = 24.dp)
                    ) {
                        Text(
                            tab,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
