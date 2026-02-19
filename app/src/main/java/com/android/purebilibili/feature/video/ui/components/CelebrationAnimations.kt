package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🎉 纯 Compose 庆祝动画组件
 * 无需外部 Lottie JSON 文件
 */

// 爱心粒子
data class HeartParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val color: Color,
    val angle: Float,
    val speed: Float
)

/**
 *  点赞成功爆裂动画
 */
@Composable
fun LikeBurstAnimation(
    visible: Boolean,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return
    
    val colors = listOf(
        Color(0xFFFF2D55), // iOS Pink
        Color(0xFFFF6B9D),
        Color(0xFFFF9500), // iOS Orange
        Color(0xFFFFD60A), // iOS Yellow
        Color(0xFFAF52DE)  // iOS Purple
    )
    
    // 生成粒子
    val particles = remember {
        (0..12).map {
            HeartParticle(
                x = 0f,
                y = 0f,
                size = Random.nextFloat() * 8f + 6f,
                color = colors.random(),
                angle = (it * 30f) + Random.nextFloat() * 15f,
                speed = Random.nextFloat() * 80f + 60f
            )
        }
    }
    
    // 动画进度
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
            delay(100)
            onAnimationEnd()
        }
    }
    
    val progress = animatedProgress.value
    
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            particles.forEach { particle ->
                val radians = Math.toRadians(particle.angle.toDouble())
                val distance = particle.speed * progress
                val x = centerX + (cos(radians) * distance).toFloat()
                val y = centerY + (sin(radians) * distance).toFloat()
                
                // 透明度渐出
                val alpha = (1f - progress).coerceIn(0f, 1f)
                
                // 缩放
                val scale = 1f - progress * 0.5f
                
                // 绘制爱心形状 (简化为圆形)
                drawCircle(
                    color = particle.color.copy(alpha = alpha),
                    radius = particle.size * scale,
                    center = Offset(x, y)
                )
            }
        }
        
        // 中心放大的爱心
        val heartScale by animateFloatAsState(
            targetValue = if (progress < 0.3f) 1.5f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "heartScale"
        )
        
        Text(
            text = "👍",
            fontSize = (32 * heartScale).sp,
            modifier = Modifier.offset(y = (-4).dp)
        )
    }
}

/**
 *  三连成功庆祝动画
 */
@Composable
fun TripleSuccessAnimation(
    visible: Boolean,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return
    
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
            delay(200)
            onAnimationEnd()
        }
    }
    
    val progress = animatedProgress.value
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // 烟花粒子
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            val colors = listOf(
                Color(0xFFFF2D55),
                Color(0xFFFFD60A),
                Color(0xFFFF9500),
                Color(0xFFAF52DE),
                Color(0xFF5AC8FA)
            )
            
            // 绘制星星粒子
            for (i in 0..20) {
                val angle = (i * 18f) + (progress * 360f)
                val radians = Math.toRadians(angle.toDouble())
                val distance = 40f + (progress * 80f) + (i % 3) * 20f
                
                val x = centerX + (cos(radians) * distance).toFloat()
                val y = centerY + (sin(radians) * distance).toFloat()
                
                val alpha = (1f - progress * 0.8f).coerceIn(0f, 1f)
                val radius = 4f + (i % 4) * 2f
                
                drawCircle(
                    color = colors[i % colors.size].copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
        
        // 中心文字
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textScale by animateFloatAsState(
                targetValue = if (progress < 0.4f) 1.3f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "textScale"
            )
            
            Text(
                text = "🎉",
                fontSize = (40 * textScale).sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "三连成功！",
                fontSize = (18 * textScale).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF2D55)
            )
        }
    }
}

/**
 *  投币成功动画
 */
@Composable
fun CoinSuccessAnimation(
    visible: Boolean,
    coinCount: Int = 1,
    onAnimationEnd: () -> Unit = {}
) {
    if (!visible) return
    
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
            delay(100)
            onAnimationEnd()
        }
    }
    
    val progress = animatedProgress.value
    
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // 金币旋转
        val rotation by animateFloatAsState(
            targetValue = 360f * progress,
            animationSpec = tween(600),
            label = "coinRotation"
        )
        
        val scale by animateFloatAsState(
            targetValue = if (progress < 0.5f) 1.5f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "coinScale"
        )
        
        Text(
            text = if (coinCount >= 2) "🪙🪙" else "🪙",
            fontSize = (28 * scale).sp,
            modifier = Modifier.offset(y = (-progress * 20).dp)
        )
    }
}
