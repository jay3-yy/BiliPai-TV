package com.android.purebilibili.core.ui.animation

import com.android.purebilibili.core.ui.adaptive.MotionTier

enum class TvFocusCardEmphasis {
    Standard,
    Large
}

data class TvFocusJiggleSpec(
    val focusScale: Float,
    val focusBumpScale: Float,
    val rotationAmplitude: Float,
    val translationAmplitudeDp: Int,
    val cycleMillis: Int
)

fun resolveTvFocusJiggleSpec(
    isTv: Boolean,
    screenWidthDp: Int,
    reducedMotion: Boolean,
    cardEmphasis: TvFocusCardEmphasis = TvFocusCardEmphasis.Standard,
    motionTier: MotionTier = MotionTier.Normal
): TvFocusJiggleSpec {
    if (!isTv) {
        return TvFocusJiggleSpec(
            focusScale = 1f,
            focusBumpScale = 1f,
            rotationAmplitude = 0f,
            translationAmplitudeDp = 0,
            cycleMillis = 700
        )
    }

    val shouldReduceMotion = reducedMotion || motionTier == MotionTier.Reduced
    if (shouldReduceMotion) {
        return TvFocusJiggleSpec(
            focusScale = 1.03f,
            focusBumpScale = 1.03f,
            rotationAmplitude = 0f,
            translationAmplitudeDp = 0,
            cycleMillis = 700
        )
    }

    return when {
        screenWidthDp >= 1600 && cardEmphasis == TvFocusCardEmphasis.Large -> TvFocusJiggleSpec(
            focusScale = 1.055f,
            focusBumpScale = 1.068f,
            rotationAmplitude = 1.2f,
            translationAmplitudeDp = 2,
            cycleMillis = 620
        )
        screenWidthDp >= 1600 -> TvFocusJiggleSpec(
            focusScale = 1.06f,
            focusBumpScale = 1.075f,
            rotationAmplitude = 1.6f,
            translationAmplitudeDp = 3,
            cycleMillis = 560
        )
        cardEmphasis == TvFocusCardEmphasis.Large -> TvFocusJiggleSpec(
            focusScale = 1.045f,
            focusBumpScale = 1.058f,
            rotationAmplitude = 1.0f,
            translationAmplitudeDp = 1,
            cycleMillis = 680
        )
        else -> TvFocusJiggleSpec(
            focusScale = 1.05f,
            focusBumpScale = 1.065f,
            rotationAmplitude = 1.2f,
            translationAmplitudeDp = 2,
            cycleMillis = 620
        )
    }
}
