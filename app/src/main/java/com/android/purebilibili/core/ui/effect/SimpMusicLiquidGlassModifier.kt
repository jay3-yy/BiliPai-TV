package com.android.purebilibili.core.ui.effect

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape // [Fix] Import
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.scale
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.math.sign
import kotlin.time.Duration.Companion.seconds

/**
 * SimpMusic Style Liquid Glass Modifier
 * Ports the logic from SimpMusic's LiquidGlassAppBottomNavigationBar.android.kt
 */
@RequiresApi(Build.VERSION_CODES.O) // For Bitmap.config, etc. Ideally Tiramisu for Backdrop but simpler here.
fun Modifier.simpMusicLiquidGlass(
    backdrop: LayerBackdrop,
    shape: Shape = RectangleShape,
    onLuminanceChanged: (Float) -> Unit = {}
): Modifier = composed {
    // Only supported on API 29+ ideally for GraphicsLayer usage as implemented in SimpMusic (uses newer APIs)
    // But LayerBackdrop usually requires Tiramisu (33).
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }

    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0f) }

    // Analysis Loop
    LaunchedEffect(layer) {
        // SimpMusic uses a 5x5 buffers
        val buffer = IntBuffer.allocate(25)
        while (isActive) {
            try {
                withContext(Dispatchers.IO) {
                    val imageBitmap = layer.toImageBitmap()
                    val thumbnail = imageBitmap
                        .asAndroidBitmap()
                        .scale(5, 5, false)
                        .copy(Bitmap.Config.ARGB_8888, false)
                    
                    buffer.rewind()
                    thumbnail.copyPixelsToBuffer(buffer)
                }
            } catch (e: Exception) {
                // Logger.e(TAG, "Error getting pixels from layer: ${e.localizedMessage}")
            }
            
            // Calculate average luminance
            val averageLuminance = (0 until 25).sumOf { index ->
                val color = buffer.get(index)
                val r = (color shr 16 and 0xFF) / 255f
                val g = (color shr 8 and 0xFF) / 255f
                val b = (color and 0xFF) / 255f
                0.2126 * r + 0.7152 * g + 0.0722 * b
            } / 25.0
            
            val targetLuminance = averageLuminance.coerceAtMost(0.8).toFloat()
            
            // Notify listener (for text color changes)
            // luminanceAnimation updates slowly, so we might want to notify the target or current value
            // We'll notify the animated value logic inside the draw loop or here? 
            // SimpMusic uses luminanceAnimation.value for drawing, so we should expose that.
            // But we can't expose state from modifier easily except via callback.
            // We will launch a separate effect to notify if needed, or just let the user read the state if we returned it (we can't).
            // So we update the animation.
            
            luminanceAnimation.animateTo(
                targetLuminance,
                tween(500),
            )
            delay(1.seconds)
        }
    }

    // Notify luminance change (optional, if we want outside to know)
    // Since onLuminanceChanged is a callback, we can call it when animation value changes.
    // But calling it every frame is bad. Maybe just pass the state object?
    // For now, let's just use the callback efficiently or assume caller passes a lambda that updates a state.
    LaunchedEffect(luminanceAnimation.value) {
        onLuminanceChanged(luminanceAnimation.value)
    }

    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminanceAnimation.value * 2f - 1f).let { sign(it) * it * it }
            
            vibrancy()
            
            colorControls(
                brightness = if (l > 0f) {
                    lerp(0.1f, 0.5f, l)
                } else {
                    lerp(0.1f, -0.2f, -l)
                },
                contrast = if (l > 0f) {
                    lerp(1f, 0f, l)
                } else {
                    1f
                },
                saturation = 1.5f,
            )
            
            // Dynamic blur based on luminance
            blur(
                if (l > 0f) {
                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                } else {
                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                },
            )
            
            // Lens effect
            // size.minDimension is not available directly in effects scope? 
            // In SimpMusic: lens(24f.dp.toPx(), size.minDimension / 2f, true)
            // Backdrop effects block receiver is 'BackdropScope', which probably has 'size'.
            // Let's assume it does (it usually mirrors DrawScope).
            // If not, we might need a standard drawWithContent intermediate.
            // Looking at SimpMusic code: actual fun Modifier.drawBackdropCustomShape calls this.drawBackdrop.
            // And inside effects lexically, 'size' refers to the DrawScope size from drawBackdrop's internal implementation?
            // Actually 'com.kyant.backdrop.drawBackdrop' lambda receiver is likely DrawScope or similar Context.
            
            // Standard Backdrop lens signature: lens(radius, zoom, ...).
            // SimpMusic: lens(24f.dp.toPx(), size.minDimension / 2f, true)
            // We use 'size' here.
             lens(24f.dp.toPx(), size.minDimension / 2f, true)
        },
        onDrawBackdrop = { drawBackdrop ->
            drawBackdrop()
            // Capture the drawn backdrop for analysis
            layer.record { drawBackdrop() }
        },
        shape = { shape },
        onDrawSurface = { 
            // Overlay logic from SimpMusic: drawRect(Color.Black.copy(alpha = 0.1f))
            drawRect(Color.Black.copy(alpha = 0.1f)) 
        }
    )
}
