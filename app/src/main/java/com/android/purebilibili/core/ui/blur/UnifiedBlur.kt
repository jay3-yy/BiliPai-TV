// 文件路径: core/ui/blur/UnifiedBlur.kt
package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.core.store.SettingsManager
import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 *  统一的模糊Modifier
 * 
 * 自动根据用户设置选择模糊强度
 * 
 * @param hazeState Haze状态
 * @param enabled 是否启用模糊
 * @return 应用了用户偏好模糊的Modifier
 */
@Composable
fun Modifier.unifiedBlur(
    hazeState: HazeState,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape? = null
): Modifier = composed {
    if (!enabled) return@composed this
    
    val context = LocalContext.current
    
    //  读取用户设置的模糊强度
    val blurIntensity by SettingsManager.getBlurIntensity(context)
        .collectAsState(initial = BlurIntensity.THIN)
    
    // 根据用户选择获取对应的模糊样式
    val blurStyle = BlurStyles.getBlurStyle(blurIntensity)
    
    //  [修复] HazeEffect 不支持 shape 参数，需使用 clip 修饰符
    //  仅当提供了 shape 时才应用 clip，避免破坏现有圆角组件 (如 BottomBar)
    if (shape != null) {
        this.clip(shape)
    } else {
        this
    }.hazeEffect(
        state = hazeState,
        style = blurStyle
    ) {
        blurEnabled = true
    }
}
