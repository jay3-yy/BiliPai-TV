package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.adaptive.MotionTier

@Composable
fun Modifier.tvFocusableJiggle(
    isTv: Boolean,
    screenWidthDp: Int,
    reducedMotion: Boolean,
    cardEmphasis: TvFocusCardEmphasis = TvFocusCardEmphasis.Standard,
    motionTier: MotionTier = MotionTier.Normal
): Modifier {
    if (!isTv) return this

    var isFocused by remember { mutableStateOf(false) }
    val spec = remember(isTv, screenWidthDp, reducedMotion, cardEmphasis, motionTier) {
        resolveTvFocusJiggleSpec(
            isTv = isTv,
            screenWidthDp = screenWidthDp,
            reducedMotion = reducedMotion,
            cardEmphasis = cardEmphasis,
            motionTier = motionTier
        )
    }
    val focusScaleAnim = remember { Animatable(1f) }
    LaunchedEffect(isFocused, spec.focusScale, spec.focusBumpScale) {
        if (isFocused) {
            if (spec.focusBumpScale > spec.focusScale) {
                focusScaleAnim.animateTo(
                    targetValue = spec.focusBumpScale,
                    animationSpec = tween(durationMillis = 110)
                )
            }
            focusScaleAnim.animateTo(
                targetValue = spec.focusScale,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 500f)
            )
        } else {
            focusScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 560f)
            )
        }
    }

    val jiggleTransition = if (isFocused && spec.rotationAmplitude > 0f) {
        rememberInfiniteTransition(label = "tvCardJiggle")
    } else {
        null
    }
    val rotation = jiggleTransition?.animateFloat(
        initialValue = -spec.rotationAmplitude,
        targetValue = spec.rotationAmplitude,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = spec.cycleMillis
                0f at spec.cycleMillis / 2
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "tvJiggleRotation"
    )?.value ?: 0f
    val translationOffsetPx = with(LocalDensity.current) {
        spec.translationAmplitudeDp.dp.toPx()
    }
    val jiggleTranslationXValue = jiggleTransition?.animateFloat(
        initialValue = -translationOffsetPx,
        targetValue = translationOffsetPx,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = spec.cycleMillis
                0f at spec.cycleMillis / 2
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "tvJiggleTranslationX"
    )?.value ?: 0f

    return this
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        }
        .focusable()
        .graphicsLayer {
            scaleX = focusScaleAnim.value
            scaleY = focusScaleAnim.value
            rotationZ = if (isFocused) rotation else 0f
            translationX = if (isFocused) jiggleTranslationXValue else 0f
        }
}
