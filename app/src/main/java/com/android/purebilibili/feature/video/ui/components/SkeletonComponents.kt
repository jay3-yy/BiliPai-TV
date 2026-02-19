package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer

/**
 *  骨架屏组件 - iOS 风格加载占位
 */

// 基础 Shimmer 容器
@Composable
fun ShimmerContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.shimmer()) {
        content()
    }
}

// 骨架方块
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    )
}

// 圆形骨架 (头像)
@Composable
fun SkeletonCircle(size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    )
}

/**
 *  视频详情页内容骨架屏（不包含播放器区域）
 */
@Composable
fun VideoDetailSkeleton() {
    ShimmerContainer(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = 16.dp)
        ) {
            // UP主信息骨架
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonCircle(size = 46.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonBox(modifier = Modifier.width(100.dp), height = 16.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    SkeletonBox(modifier = Modifier.width(60.dp), height = 12.dp)
                }
                SkeletonBox(modifier = Modifier.width(72.dp), height = 36.dp, cornerRadius = 18.dp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题骨架
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 20.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f), height = 20.dp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮骨架
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SkeletonCircle(size = 32.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonBox(modifier = Modifier.width(40.dp), height = 12.dp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 推荐视频骨架
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SkeletonBox(modifier = Modifier.width(100.dp), height = 18.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                repeat(3) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SkeletonBox(
                            modifier = Modifier.width(160.dp).height(90.dp),
                            cornerRadius = 12.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 16.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f), height = 16.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            SkeletonBox(modifier = Modifier.width(80.dp), height = 12.dp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
