// 文件路径: feature/video/ui/overlay/BottomRightControls.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.feature.video.ui.components.*
import com.android.purebilibili.core.util.rememberIsTvDevice

/**
 *  横屏播放器底部右侧控制按钮组
 * 
 * 包含：倍速、画质、比例 三个按钮
 * 点击后显示对应的选择菜单
 */
@Composable
fun BottomRightControls(
    // 倍速
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    // 画质
    currentQualityText: String,
    onQualityClick: () -> Unit,
    // 比例
    currentRatio: VideoAspectRatio,
    onRatioChange: (VideoAspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showRatioMenu by remember { mutableStateOf(false) }
    val isTvDevice = rememberIsTvDevice()
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp, isTvDevice) {
        resolveBottomRightControlsLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isTv = isTvDevice
        )
    }
    
    Box(modifier = modifier) {
        // 控制按钮行
        Row(
            horizontalArrangement = Arrangement.spacedBy(layoutPolicy.rowSpacingDp.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 倍速按钮
            SpeedButton(
                currentSpeed = currentSpeed,
                onClick = { showSpeedMenu = !showSpeedMenu; showRatioMenu = false }
            )
            
            // 画质按钮
            QualityButton(
                qualityText = currentQualityText,
                layoutPolicy = layoutPolicy,
                onClick = { showSpeedMenu = false; showRatioMenu = false; onQualityClick() }
            )
            
            // 比例按钮
            AspectRatioButton(
                currentRatio = currentRatio,
                onClick = { showRatioMenu = !showRatioMenu; showSpeedMenu = false }
            )
        }
        
        // 倍速菜单（显示在按钮上方）
        AnimatedVisibility(
            visible = showSpeedMenu,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = layoutPolicy.menuOffsetYDp.dp)
        ) {
            SpeedSelectionMenu(
                currentSpeed = currentSpeed,
                onSpeedSelected = onSpeedChange,
                onDismiss = { showSpeedMenu = false }
            )
        }
        
        // 比例菜单（显示在按钮上方）
        AnimatedVisibility(
            visible = showRatioMenu,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = layoutPolicy.menuOffsetYDp.dp)
        ) {
            AspectRatioMenu(
                currentRatio = currentRatio,
                onRatioSelected = onRatioChange,
                onDismiss = { showRatioMenu = false }
            )
        }
    }
}

/**
 * 画质按钮（简化版，点击后由外部处理）
 */
@Composable
private fun QualityButton(
    qualityText: String,
    layoutPolicy: BottomRightControlsLayoutPolicy,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(layoutPolicy.chipCornerRadiusDp.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Text(
            text = qualityText,
            color = Color.White,
            fontSize = layoutPolicy.chipFontSp.sp,
            modifier = Modifier.padding(
                horizontal = layoutPolicy.chipHorizontalPaddingDp.dp,
                vertical = layoutPolicy.chipVerticalPaddingDp.dp
            )
        )
    }
}
