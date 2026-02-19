package com.android.purebilibili.core.ui.animation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.core.ui.animation.gl.ParticleRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ==================== iOS 风格抖动效果系统 ====================

object DissolveAnimationManager {
    private val _isAnyCardDissolving = mutableStateOf(false)
    val isAnyCardDissolving: State<Boolean> = _isAnyCardDissolving
    
    private val _dissolvingCardId = mutableStateOf<String?>(null)
    val dissolvingCardId: State<String?> = _dissolvingCardId
    
    fun startDissolving(cardId: String) {
        _dissolvingCardId.value = cardId
        _isAnyCardDissolving.value = true
    }
    
    fun stopDissolving() {
        _dissolvingCardId.value = null
        _isAnyCardDissolving.value = false
    }
}

enum class DissolveAnimationPreset {
    CLASSIC,
    TELEGRAM_FAST
}

private data class DissolveAnimationParams(
    val durationSec: Float,
    val particleStep: Int,
    val waveDurationSec: Float,
    val waveRandomSec: Float,
    val collapseDurationMs: Int
)

private fun DissolveAnimationPreset.params(): DissolveAnimationParams {
    return when (this) {
        DissolveAnimationPreset.CLASSIC -> DissolveAnimationParams(
            durationSec = 1.8f,
            particleStep = 2,
            waveDurationSec = 0.35f,
            waveRandomSec = 0.08f,
            collapseDurationMs = 200
        )
        DissolveAnimationPreset.TELEGRAM_FAST -> DissolveAnimationParams(
            durationSec = 0.82f,
            particleStep = 3,
            waveDurationSec = 0.16f,
            waveRandomSec = 0.05f,
            collapseDurationMs = 135
        )
    }
}

@Composable
fun Modifier.jiggleOnDissolve(
    cardId: String,
    enabled: Boolean = true
): Modifier {
    // 🚀 [性能优化] 提前检查是否需要抖动，避免不必要的状态读取和动画创建
    if (!enabled) return this
    
    val isDissolving by DissolveAnimationManager.isAnyCardDissolving
    val dissolvingId by DissolveAnimationManager.dissolvingCardId
    val shouldJiggle = isDissolving && dissolvingId != cardId
    
    // 🚀 [关键优化] 不抖动时直接返回，不创建任何动画对象
    if (!shouldJiggle) return this
    
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleRotation"
    )
    
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(60, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleOffset"
    )
    
    return this.graphicsLayer {
        rotationZ = rotation
        translationX = offsetX
    }
}

// ==================== OpenGL 粒子消散动画 ====================

@Composable
fun DissolvableVideoCard(
    isDissolving: Boolean,
    onDissolveComplete: () -> Unit,
    modifier: Modifier = Modifier,
    cardId: String = "",
    preset: DissolveAnimationPreset = DissolveAnimationPreset.CLASSIC,
    content: @Composable () -> Unit
) {
    val animationParams = remember(preset) { preset.params() }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var shouldCollapse by remember { mutableStateOf(false) }
    
    val collapseSpec: AnimationSpec<Float> = remember(animationParams.collapseDurationMs, preset) {
        when (preset) {
            DissolveAnimationPreset.CLASSIC -> spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
            DissolveAnimationPreset.TELEGRAM_FAST -> tween(
                durationMillis = animationParams.collapseDurationMs,
                easing = FastOutSlowInEasing
            )
        }
    }

    // 动画：完成/收起
    val heightMultiplier by animateFloatAsState(
        targetValue = if (shouldCollapse) 0f else 1f,
        animationSpec = collapseSpec,
        label = "heightCollapse",
        finishedListener = {
             if (shouldCollapse) {
                 // Animation fully done, NOW we dismiss
                 onDissolveComplete()
                 DissolveAnimationManager.stopDissolving()
             }
        }
    )

    var captureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showGLView by remember { mutableStateOf(false) }
    var isGLContentReady by remember { mutableStateOf(false) } //  New: Wait for GL to draw first frame
    
    // Coordinates
    var cardWindowBounds by remember { mutableStateOf<Rect?>(null) }
    val context = LocalContext.current
    val composeView = LocalView.current
    
    LaunchedEffect(isDissolving) {
        if (isDissolving) {
            // Note: startDissolving is called in onFirstFrame callback to sync jiggle with actual animation
            // Trigger Capture
            if (cardWindowBounds != null) {
                val window = findWindow(context)
                if (window != null) {
                    val bitmap = Bitmap.createBitmap(
                        cardWindowBounds!!.width(), 
                        cardWindowBounds!!.height(), 
                        Bitmap.Config.ARGB_8888
                    )
                    
                    try {
                        // Use PixelCopy to capture the area
                        PixelCopy.request(
                            window,
                            cardWindowBounds!!,
                            bitmap,
                            { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    captureBitmap = bitmap
                                    showGLView = true
                                    // Don't hide content yet, wait for GL view to say "I'm drawing"
                                } else {
                                    // Fallback? Just collapse
                                    shouldCollapse = true
                                    // onDissolveComplete() // Let animation listener handle it
                                }
                            },
                            Handler(Looper.getMainLooper())
                        )
                    } catch (e: Exception) {
                         e.printStackTrace()
                         shouldCollapse = true
                    }
                } else {
                    shouldCollapse = true // No window found
                }
            } else {
                shouldCollapse = true // No bounds found
            }
        } else {
            captureBitmap = null
            showGLView = false
            isGLContentReady = false
            shouldCollapse = false
            if (cardId.isNotEmpty() && DissolveAnimationManager.dissolvingCardId.value == cardId) {
                DissolveAnimationManager.stopDissolving()
            }
        }
    }

    DisposableEffect(cardId) {
        onDispose {
            if (cardId.isNotEmpty() && DissolveAnimationManager.dissolvingCardId.value == cardId) {
                DissolveAnimationManager.stopDissolving()
            }
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { cardSize = it }
            .onGloballyPositioned { coordinates ->
                val boundsInWindow = coordinates.boundsInWindow()
                val location = IntArray(2)
                composeView.getLocationInWindow(location)
                
                val left = boundsInWindow.left.roundToInt()
                val top = boundsInWindow.top.roundToInt()
                val width = boundsInWindow.width.roundToInt()
                val height = boundsInWindow.height.roundToInt()
                
                cardWindowBounds = Rect(left, top, left + width, top + height)
            }
            .then(
                if (shouldCollapse) {
                    Modifier.height(
                        with(LocalDensity.current) {
                            (cardSize.height * heightMultiplier).toDp()
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // 1. Content Layer
        // Keep visible until GL view renders first frame (seamless transition)
        Box(
            modifier = Modifier.alpha(if (isGLContentReady) 0f else 1f)
        ) {
            content()
        }
        
        // 2. GL Overlay
        if (showGLView && captureBitmap != null) {
            AndroidView(
                factory = { ctx ->
                    GLParticleView(ctx).apply {
                        setZOrderOnTop(true)
                        holder.setFormat(PixelFormat.TRANSLUCENT)
                        
                        setBitmap(
                            bitmap = captureBitmap!!,
                            durationSec = animationParams.durationSec,
                            particleStep = animationParams.particleStep,
                            waveDurationSec = animationParams.waveDurationSec,
                            waveRandomSec = animationParams.waveRandomSec
                        )
                        setCallbacks(
                            complete = {
                                 // onDissolveComplete() // REMOVED: Don't dismiss yet
                                 // DissolveAnimationManager.stopDissolving() // REMOVED
                                 shouldCollapse = true // Trigger collapse animation
                            },
                            firstFrame = {
                                isGLContentReady = true
                                // Start jiggle effect on other cards now that animation is visible
                                if (cardId.isNotEmpty()) {
                                    DissolveAnimationManager.startDissolving(cardId)
                                }
                            }
                        )
                    }
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

private fun findWindow(context: Context): Window? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx.window
        ctx = ctx.baseContext
    }
    return null
}

class GLParticleView(context: Context) : GLSurfaceView(context) {
    private var renderer: ParticleRenderer? = null
    var onComplete: (() -> Unit)? = null
    var onFirstFrame: (() -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Enable Alpha
    }

    fun setBitmap(
        bitmap: Bitmap,
        durationSec: Float = 2.0f,
        particleStep: Int = 2,
        waveDurationSec: Float = 0.4f,
        waveRandomSec: Float = 0.1f
    ) {
        renderer = ParticleRenderer(
            textureBitmap = bitmap,
            onAnimationComplete = {
                post { onComplete?.invoke() }
            },
            onFirstFrame = {
                post { onFirstFrame?.invoke() }
            },
            animationDurationSec = durationSec,
            particleStep = particleStep,
            waveDurationSec = waveDurationSec,
            waveRandomSec = waveRandomSec
        )
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setCallbacks(complete: () -> Unit, firstFrame: () -> Unit) {
        this.onComplete = complete
        this.onFirstFrame = firstFrame
    }
}
// wait, I need to redefine GLParticleView properly to support both setup and logic
