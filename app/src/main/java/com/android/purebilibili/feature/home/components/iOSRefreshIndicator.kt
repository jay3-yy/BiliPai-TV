// 文件路径: feature/home/components/iOSRefreshIndicator.kt
package com.android.purebilibili.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator

/**
 *  iOS 风格下拉刷新指示器
 * 
 * 特点：
 * - 下拉时显示"下拉刷新..."
 * - 达到阈值时显示"松手刷新"  
 * - 刷新中显示 iOS 风格旋转动画
 * - 刷新完成显示"刷新成功"
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun iOSRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    //  进度值（0.0 ~ 1.0+）
    val progress = state.distanceFraction
    
    //  是否达到刷新阈值
    val isOverThreshold = progress >= 1f
    
    //  提示文字
    val hintText = when {
        isRefreshing -> "正在刷新..."
        isOverThreshold -> "松手刷新"
        progress > 0f -> "下拉刷新..."
        else -> ""
    }
    
    //  箭头旋转角度（下拉超过阈值时翻转）- 使用低阻尼弹簧实现过冲
    val arrowRotation by animateFloatAsState(
        targetValue = if (isOverThreshold) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.4f,  // 低阻尼 = 过冲弹跳
            stiffness = 250f
        ),
        label = "arrow_rotation"
    )
    
    //  透明度动画
    val alpha by animateFloatAsState(
        targetValue = if (progress > 0.1f || isRefreshing) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "alpha"
    )
    
    //  缩放动画 - 增强弹性，释放时过冲
    val scale by animateFloatAsState(
        targetValue = when {
            isRefreshing -> 1f
            isOverThreshold -> 1.1f  // 超过阈值时放大
            else -> (progress.coerceIn(0f, 1f) * 0.4f + 0.6f).coerceAtMost(1f)
        },
        animationSpec = spring(
            dampingRatio = 0.45f,  // 低阻尼产生过冲
            stiffness = 300f
        ),
        label = "scale"
    )
    
    //  [新增] Y 轴弹跳偏移 - 模拟橡皮筋拉伸感
    val bounceY by animateFloatAsState(
        targetValue = when {
            isRefreshing -> 0f
            isOverThreshold -> -8f  // 超过阈值向上弹起
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = 0.35f,  // 强过冲
            stiffness = 350f
        ),
        label = "bounceY"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                translationY = bounceY  // Y 轴弹跳
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            if (isRefreshing) {
                //  iOS 风格转轮
                CupertinoActivityIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (progress > 0.1f) {
                //  箭头图标（旋转表示状态变化）
                Text(
                    text = "↓",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(arrowRotation)
                )
            }
            
            if (hintText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = hintText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
