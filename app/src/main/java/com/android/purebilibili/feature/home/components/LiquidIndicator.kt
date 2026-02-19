// 文件路径: feature/home/components/LiquidIndicator.kt
package com.android.purebilibili.feature.home.components



import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.blur
import com.android.purebilibili.core.store.LiquidGlassStyle

/**
 * 🌊 液态玻璃选中指示器
 * 
 * 实现类似 visionOS 的玻璃折射效果：
 * - 透镜折射效果 (Android 13+ 支持)
 * - 拖拽时放大形变
 * - 高光和内阴影
 * 
 * @param position 当前位置（浮点索引）
 * @param itemWidth 单个项目宽度
 * @param itemCount 项目数量
 * @param isDragging 是否正在拖拽
 * @param velocity 当前速度（用于形变）
 * @param hazeState HazeState 实例（用于模糊效果）
 * @param modifier Modifier
 */
@Composable
fun LiquidIndicator(
    position: Float,
    itemWidth: Dp,
    itemCount: Int,
    isDragging: Boolean,
    velocity: Float = 0f,
    startPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    isLiquidGlassEnabled: Boolean = false,
    clampToBounds: Boolean = false,
    edgeInset: Dp = 0.dp,
    viewportShiftPx: Float = 0f,
    indicatorWidthMultiplier: Float = 1.42f,
    indicatorMinWidth: Dp = 104.dp,
    indicatorMaxWidth: Dp = 136.dp,
    maxWidthToItemRatio: Float = Float.POSITIVE_INFINITY,
    indicatorHeight: Dp = 54.dp,
    lensIntensityBoost: Float = 1f,
    edgeWarpBoost: Float = 1f,
    chromaticBoost: Float = 1f,
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC, // [New]
    backdrop: LayerBackdrop? = null // [New] Backdrop for refraction
) {
    val density = LocalDensity.current
    val styleTuning = remember(liquidGlassStyle) { resolveLiquidStyleTuning(liquidGlassStyle) }
    val lensProfile = remember(
        isDragging,
        velocity,
        lensIntensityBoost,
        edgeWarpBoost,
        chromaticBoost,
        liquidGlassStyle
    ) {
        resolveLiquidLensProfile(
            isDragging = isDragging,
            velocityPxPerSecond = velocity,
            idleThresholdPxPerSecond = styleTuning.idleThresholdPxPerSecond,
            dragMotionFloor = styleTuning.dragMotionFloor,
            lensIntensityBoost = lensIntensityBoost * styleTuning.lensIntensityMultiplier,
            edgeWarpBoost = edgeWarpBoost * styleTuning.edgeWarpMultiplier,
            chromaticBoost = chromaticBoost * styleTuning.chromaticMultiplier
        )
    }
    
    val itemWidthPx = with(density) { itemWidth.toPx() }
    val indicatorWidthPx = resolveLiquidIndicatorWidthPx(
        itemWidthPx = itemWidthPx,
        widthMultiplier = indicatorWidthMultiplier,
        minWidthPx = with(density) { indicatorMinWidth.toPx() },
        maxWidthPx = with(density) { indicatorMaxWidth.toPx() },
        maxWidthToItemRatio = maxWidthToItemRatio
    )
    val indicatorWidth = with(density) { indicatorWidthPx.toDp() }

    // [优化] 使用 graphicsLayer 进行位移，避免 Layout 重排
    // 计算位置 (Px)
    val startPaddingPx = with(density) { startPadding.toPx() }
    val edgeInsetPx = with(density) { edgeInset.toPx() }
    // 居中偏移：(Item宽度 - 指示器宽度) / 2
    val centerOffsetPx = (itemWidthPx - indicatorWidthPx) / 2f
    
    // 速度形变
    val deformation = lensProfile.motionFraction * (0.34f * styleTuning.deformationMultiplier)
    
    val targetScaleX = 1f + deformation
    val targetScaleY = 1f - (deformation * 0.52f)
    
    val scaleX by animateFloatAsState(targetValue = targetScaleX, animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f), label = "scaleX")
    val scaleY by animateFloatAsState(targetValue = targetScaleY, animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f), label = "scaleY")
    val dragScale by animateFloatAsState(targetValue = if (isDragging) 1.0f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "dragScale")

    val finalScaleX = scaleX * dragScale
    val finalScaleY = scaleY * dragScale

    // 指示器形状
    val shape = RoundedCornerShape(indicatorHeight / 2)
    
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterStart
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
         Box(
            modifier = Modifier
                .graphicsLayer {
                    // [核心优化] 在绘制阶段计算位移
                    translationX = resolveIndicatorTranslationXPx(
                        position = position,
                        itemWidthPx = itemWidthPx,
                        indicatorWidthPx = indicatorWidthPx,
                        startPaddingPx = startPaddingPx,
                        containerWidthPx = containerWidthPx,
                        clampToBounds = clampToBounds,
                        edgeInsetPx = edgeInsetPx,
                        viewportShiftPx = viewportShiftPx
                    )
                    
                    this.scaleX = finalScaleX
                    this.scaleY = finalScaleY
                    shadowElevation = 0f
                }
                .size(indicatorWidth, indicatorHeight)
                .clip(shape)
                .run {
                    if (isLiquidGlassEnabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // [Effect] Strong refraction for the indicator (Magnifying Glass effect)
                        if (liquidGlassStyle == LiquidGlassStyle.SIMP_MUSIC) {
                            // [Style: SimpMusic] Frosted Glass (Blur Only, No Deformation)
                            val blurRadius = 36f
                            this.drawBackdrop(
                                backdrop = backdrop,
                                shape = { shape },
                                effects = {
                                    blur(blurRadius)
                                    // No lens effect here
                                },
                                onDrawSurface = {
                                    // More visible tint for frosted glass
                                    drawRect(color.copy(alpha = 0.18f))
                                    drawRect(Color.White.copy(alpha = 0.08f))
                                }
                            )
                        } else {
                            if (lensProfile.shouldRefract) {
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { shape },
                                    effects = {
                                        lens(
                                            refractionHeight = lensProfile.refractionHeight,
                                            refractionAmount = lensProfile.refractionAmount,
                                            depthEffect = styleTuning.depthEffectEnabled,
                                            chromaticAberration = styleTuning.allowChromaticAberration &&
                                                lensProfile.aberrationStrength > 0.01f
                                        )
                                    },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(
                                            baseColor = color,
                                            lensProfile = lensProfile,
                                            style = liquidGlassStyle
                                        )
                                    }
                                )
                            } else {
                                // 静止态仅保留玻璃感，不做折射
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { shape },
                                    effects = { blur(styleTuning.idleBlurRadius) },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(
                                            baseColor = color,
                                            lensProfile = lensProfile,
                                            style = liquidGlassStyle
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        // Fallback
                         this.background(color)
                    }
                }
        )
    }
}


/**
 * 简化版液态指示器（不依赖 Backdrop）
 * 
 * 使用标准 Compose 动画实现类似效果
 */
/**
 * 简化版液态指示器（适用于 TabRow 等变长场景）
 * 
 * 使用标准 Compose 动画实现类似效果
 */
@Composable
fun SimpleLiquidIndicator(
    position: Float, // [修复] 直接接受 Float 而非 State，简化 API
    itemWidthPx: Float, // [修复] 使用像素值计算
    isDragging: Boolean,
    velocityPxPerSecond: Float = 0f,
    isLiquidGlassEnabled: Boolean = false,
    liquidGlassStyle: LiquidGlassStyle = LiquidGlassStyle.CLASSIC,
    backdrop: LayerBackdrop? = null,
    indicatorColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    indicatorHeight: Dp = 34.dp,
    cornerRadius: Dp = 16.dp,
    widthRatio: Float = 0.78f,
    minWidth: Dp = 48.dp,
    horizontalInset: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val styleTuning = remember(liquidGlassStyle) { resolveLiquidStyleTuning(liquidGlassStyle) }
    val lensProfile = remember(isDragging, velocityPxPerSecond, liquidGlassStyle) {
        resolveLiquidLensProfile(
            isDragging = isDragging,
            velocityPxPerSecond = velocityPxPerSecond,
            idleThresholdPxPerSecond = styleTuning.idleThresholdPxPerSecond,
            dragMotionFloor = styleTuning.dragMotionFloor,
            lensIntensityBoost = styleTuning.lensIntensityMultiplier,
            edgeWarpBoost = styleTuning.edgeWarpMultiplier,
            chromaticBoost = styleTuning.chromaticMultiplier
        )
    }
    val minWidthPx = with(density) { minWidth.toPx() }
    val horizontalInsetPx = with(density) { horizontalInset.toPx() }
    val indicatorWidthPx = resolveTopTabIndicatorWidthPx(
        itemWidthPx = itemWidthPx,
        widthRatio = widthRatio,
        minWidthPx = minWidthPx,
        horizontalInsetPx = horizontalInsetPx
    )
    val indicatorWidth = with(density) { indicatorWidthPx.toDp() }
    val indicatorHeightPx = with(density) { indicatorHeight.toPx() }
    
    // [修复] 居中偏移：将指示器居中放置在每个 Tab 单元格内
    val centerOffsetPx = (itemWidthPx - indicatorWidthPx) / 2f
    
    val scale by animateFloatAsState(
        targetValue = 1f + lensProfile.motionFraction * (0.12f * styleTuning.deformationMultiplier),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )
    val indicatorAlphaScale by animateFloatAsState(
        targetValue = if (isLiquidGlassEnabled) 0.92f else 1f,
        animationSpec = tween(180),
        label = "indicatorAlphaScale"
    )
    val resolvedIndicatorColor = indicatorColor.copy(
        alpha = (indicatorColor.alpha * indicatorAlphaScale).coerceIn(0f, 1f)
    )
    
    // [修复] 使用 BoxWithConstraints 获取父容器高度来计算垂直居中
    BoxWithConstraints(
        modifier = modifier.fillMaxHeight()
    ) {
        val parentHeightPx = with(density) { maxHeight.toPx() }
        val verticalCenterOffsetPx = (parentHeightPx - indicatorHeightPx) / 2f
        
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = position * itemWidthPx + centerOffsetPx
                    translationY = verticalCenterOffsetPx
                    
                    this.scaleX = scale
                    this.scaleY = 1f - lensProfile.motionFraction * (0.08f * styleTuning.deformationMultiplier)
                }
                .size(indicatorWidth, indicatorHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .run {
                    if (isLiquidGlassEnabled && backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (liquidGlassStyle == LiquidGlassStyle.SIMP_MUSIC) {
                            this.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(cornerRadius) },
                                    effects = { blur(32f) },
                                    onDrawSurface = {
                                        drawRect(resolvedIndicatorColor.copy(alpha = 0.16f))
                                        drawRect(Color.White.copy(alpha = 0.06f))
                                    }
                                )
                        } else {
                            if (lensProfile.shouldRefract) {
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedCornerShape(cornerRadius) },
                                    effects = {
                                        lens(
                                            refractionHeight = lensProfile.refractionHeight,
                                            refractionAmount = lensProfile.refractionAmount,
                                            depthEffect = styleTuning.depthEffectEnabled,
                                            chromaticAberration = styleTuning.allowChromaticAberration &&
                                                lensProfile.aberrationStrength > 0.01f
                                        )
                                    },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(
                                            baseColor = resolvedIndicatorColor,
                                            lensProfile = lensProfile,
                                            style = liquidGlassStyle
                                        )
                                    }
                                )
                            } else {
                                this.drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedCornerShape(cornerRadius) },
                                    effects = { blur(styleTuning.idleBlurRadius) },
                                    onDrawSurface = {
                                        drawLiquidSphereSurface(
                                            baseColor = resolvedIndicatorColor,
                                            lensProfile = lensProfile,
                                            style = liquidGlassStyle
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        this.background(resolvedIndicatorColor)
                    }
                }
                .border(
                    width = 0.7.dp,
                    color = Color.White.copy(alpha = if (isLiquidGlassEnabled) 0.62f else 0.25f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
    }
}

internal fun resolveTopTabIndicatorWidthPx(
    itemWidthPx: Float,
    widthRatio: Float,
    minWidthPx: Float,
    horizontalInsetPx: Float
): Float {
    if (itemWidthPx <= 0f) return 0f
    val minBound = minWidthPx.coerceAtMost(itemWidthPx)
    val maxWidth = (itemWidthPx - horizontalInsetPx).coerceAtLeast(minBound)
    val desired = itemWidthPx * widthRatio
    return desired.coerceIn(minBound, maxWidth)
}

internal fun resolveLiquidIndicatorWidthPx(
    itemWidthPx: Float,
    widthMultiplier: Float,
    minWidthPx: Float,
    maxWidthPx: Float,
    maxWidthToItemRatio: Float = Float.POSITIVE_INFINITY
): Float {
    if (itemWidthPx <= 0f) return 0f

    val desiredWidth = itemWidthPx * widthMultiplier
    val designMaxWidth = maxWidthPx.coerceAtLeast(0f)
    val ratioCapWidth = if (maxWidthToItemRatio.isFinite() && maxWidthToItemRatio > 0f) {
        itemWidthPx * maxWidthToItemRatio
    } else {
        Float.POSITIVE_INFINITY
    }
    val effectiveMaxWidth = minOf(designMaxWidth, ratioCapWidth)
    val effectiveMinWidth = minWidthPx.coerceAtLeast(0f).coerceAtMost(effectiveMaxWidth)
    return desiredWidth.coerceIn(effectiveMinWidth, effectiveMaxWidth)
}

internal fun resolveIndicatorTranslationXPx(
    position: Float,
    itemWidthPx: Float,
    indicatorWidthPx: Float,
    startPaddingPx: Float,
    containerWidthPx: Float,
    clampToBounds: Boolean,
    edgeInsetPx: Float,
    viewportShiftPx: Float = 0f
): Float {
    val centerOffsetPx = (itemWidthPx - indicatorWidthPx) / 2f
    val raw = startPaddingPx + position * itemWidthPx + centerOffsetPx
    if (!clampToBounds) return raw

    val minX = edgeInsetPx.coerceAtLeast(0f) + viewportShiftPx
    val maxX = (containerWidthPx - indicatorWidthPx - edgeInsetPx + viewportShiftPx).coerceAtLeast(minX)
    return raw.coerceIn(minX, maxX)
}

internal data class LiquidLensProfile(
    val shouldRefract: Boolean,
    val motionFraction: Float,
    val refractionAmount: Float,
    val refractionHeight: Float,
    val centerHighlightAlpha: Float,
    val edgeCompressionAlpha: Float,
    val aberrationStrength: Float
)

internal data class LiquidStyleTuning(
    val idleThresholdPxPerSecond: Float,
    val dragMotionFloor: Float,
    val lensIntensityMultiplier: Float,
    val edgeWarpMultiplier: Float,
    val chromaticMultiplier: Float,
    val deformationMultiplier: Float,
    val idleBlurRadius: Float,
    val depthEffectEnabled: Boolean,
    val allowChromaticAberration: Boolean
)

internal fun resolveLiquidStyleTuning(style: LiquidGlassStyle): LiquidStyleTuning = when (style) {
    LiquidGlassStyle.CLASSIC -> LiquidStyleTuning(
        idleThresholdPxPerSecond = 110f,
        dragMotionFloor = 0.38f,
        lensIntensityMultiplier = 1f,
        edgeWarpMultiplier = 1f,
        chromaticMultiplier = 1f,
        deformationMultiplier = 1f,
        idleBlurRadius = 18f,
        depthEffectEnabled = true,
        allowChromaticAberration = true
    )
    LiquidGlassStyle.IOS26 -> LiquidStyleTuning(
        idleThresholdPxPerSecond = 135f,
        dragMotionFloor = 0.08f,
        lensIntensityMultiplier = 1.06f,
        edgeWarpMultiplier = 1.06f,
        chromaticMultiplier = 0.62f,
        deformationMultiplier = 0.72f,
        idleBlurRadius = 20f,
        depthEffectEnabled = true,
        allowChromaticAberration = true
    )
    LiquidGlassStyle.SIMP_MUSIC -> LiquidStyleTuning(
        idleThresholdPxPerSecond = 220f,
        dragMotionFloor = 0.08f,
        lensIntensityMultiplier = 0.6f,
        edgeWarpMultiplier = 0.55f,
        chromaticMultiplier = 0f,
        deformationMultiplier = 0.45f,
        idleBlurRadius = 32f,
        depthEffectEnabled = false,
        allowChromaticAberration = false
    )
}

internal fun resolveLiquidLensProfile(
    isDragging: Boolean,
    velocityPxPerSecond: Float,
    idleThresholdPxPerSecond: Float = 110f,
    dragMotionFloor: Float = 0.22f,
    lensIntensityBoost: Float = 1f,
    edgeWarpBoost: Float = 1f,
    chromaticBoost: Float = 1f
): LiquidLensProfile {
    val speed = abs(velocityPxPerSecond)
    val threshold = idleThresholdPxPerSecond
    val safeDragFloor = dragMotionFloor.coerceIn(0f, 0.8f)
    val safeLensBoost = lensIntensityBoost.coerceIn(0.8f, 2.2f)
    val safeEdgeWarpBoost = edgeWarpBoost.coerceIn(0.8f, 2.2f)
    val safeChromaBoost = chromaticBoost.coerceIn(0.8f, 2.2f)
    val baseMotion = if (isDragging) safeDragFloor else 0f
    val speedMotion = if (isDragging) {
        (speed / 2600f).coerceIn(0f, 1f)
    } else {
        ((speed - threshold).coerceAtLeast(0f) / 2600f).coerceIn(0f, 1f)
    }
    val motionFraction = (baseMotion + speedMotion * (1f - baseMotion)).coerceIn(0f, 1f)
    val shouldRefract = isDragging || speed > threshold

    if (!shouldRefract) {
        return LiquidLensProfile(
            shouldRefract = false,
            motionFraction = 0f,
            refractionAmount = 0f,
            refractionHeight = 0f,
            centerHighlightAlpha = 0f,
            edgeCompressionAlpha = 0f,
            aberrationStrength = 0f
        )
    }

    val eased = motionFraction * motionFraction * (3f - 2f * motionFraction)
    return LiquidLensProfile(
        shouldRefract = true,
        motionFraction = motionFraction,
        refractionAmount = (58f + eased * 54f) * safeLensBoost,
        refractionHeight = (84f + eased * 96f) * (0.9f + safeLensBoost * 0.1f),
        centerHighlightAlpha = 0.12f + eased * 0.16f,
        edgeCompressionAlpha = (0.06f + eased * 0.16f) * safeEdgeWarpBoost,
        aberrationStrength = ((0.008f + eased * 0.024f) * safeChromaBoost).coerceIn(0f, 0.06f)
    )
}

private fun DrawScope.drawLiquidSphereSurface(
    baseColor: Color,
    lensProfile: LiquidLensProfile,
    style: LiquidGlassStyle
) {
    val isMoving = lensProfile.shouldRefract

    if (style == LiquidGlassStyle.IOS26) {
        // iOS26: neutral gray capsule with slightly stronger visibility.
        drawRect(baseColor.copy(alpha = if (isMoving) 0.09f else 0.11f))
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isMoving) 0.13f else 0.10f),
                    Color.Transparent,
                    Color.Black.copy(alpha = if (isMoving) 0.04f else 0.03f)
                )
            )
        )

        val ringAlpha = if (isMoving) 0.22f else 0.16f
        val ringStroke = (size.minDimension * 0.05f).coerceAtLeast(1f)
        val ringHighlight = lerp(baseColor, Color.White, 0.48f).copy(alpha = ringAlpha)
        val ringMid = lerp(baseColor, Color.White, 0.22f).copy(alpha = ringAlpha * 0.86f)
        val ringShadow = lerp(baseColor, Color.Black, 0.24f).copy(alpha = ringAlpha * 0.70f)
        drawRoundRect(
            brush = Brush.sweepGradient(
                colors = listOf(
                    ringHighlight,
                    ringMid,
                    ringShadow,
                    ringMid,
                    ringHighlight
                ),
                center = Offset(size.width / 2f, size.height / 2f)
            ),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
            style = Stroke(width = ringStroke)
        )
        return
    }

    val centerGlowAlpha = if (isMoving) lensProfile.centerHighlightAlpha else 0.10f
    val edgeShadeAlpha = if (isMoving) lensProfile.edgeCompressionAlpha else 0.03f
    val baseAlpha = if (isMoving) 0.08f else 0.14f

    drawRect(baseColor.copy(alpha = baseAlpha))

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = centerGlowAlpha),
                Color.White.copy(alpha = centerGlowAlpha * 0.35f),
                Color.Transparent
            ),
            center = Offset(x = size.width / 2f, y = size.height * 0.54f),
            radius = size.minDimension * 0.9f
        )
    )

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Black.copy(alpha = edgeShadeAlpha),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = edgeShadeAlpha)
            )
        )
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isMoving) 0.10f else 0.06f),
                Color.Transparent,
                Color.Black.copy(alpha = if (isMoving) 0.09f else 0.04f)
            )
        )
    )

    if (isMoving && lensProfile.aberrationStrength > 0f) {
        val fringe = (lensProfile.aberrationStrength * 3.2f).coerceIn(0f, 0.18f)
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF3DA8FF).copy(alpha = fringe),
                    Color.Transparent,
                    Color.Transparent,
                    Color(0xFFFF4F8F).copy(alpha = fringe)
                )
            )
        )
    }
}
