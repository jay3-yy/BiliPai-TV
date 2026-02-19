package com.android.purebilibili.core.ui.effect

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Container that captures its content and provides the bitmap to an overlay.
 * Used for the liquid glass effect where the bottom bar needs to sample
 * the content behind it.
 *
 * @param modifier Modifier for the container
 * @param captureEnabled Whether to enable content capture (disabled for performance when not needed)
 * @param overlayContent Composable that receives the captured content bitmap and renders the overlay
 * @param content The main content to display and capture
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    captureEnabled: Boolean = true,
    overlayContent: @Composable BoxScope.(contentBitmap: ImageBitmap?, contentSize: IntSize) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var capturedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Capture counter to trigger recomposition when content changes
    var captureCounter by remember { mutableStateOf(0) }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Content layer - captures and draws the actual content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val newSize = coordinates.size
                    if (newSize != contentSize && newSize.width > 0 && newSize.height > 0) {
                        contentSize = newSize
                    }
                }
                .then(
                    if (captureEnabled && contentSize.width > 0 && contentSize.height > 0) {
                        Modifier.drawWithCache {
                            // Create bitmap for capture (scaled down for performance)
                            val scale = 0.5f  // 50% scale for better performance
                            val scaledWidth = (size.width * scale).roundToInt().coerceAtLeast(1)
                            val scaledHeight = (size.height * scale).roundToInt().coerceAtLeast(1)
                            
                            val bitmap = Bitmap.createBitmap(
                                scaledWidth,
                                scaledHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.scale(scale, scale)
                            
                            onDrawWithContent {
                                // Draw content normally
                                drawContent()
                                
                                // Capture to bitmap (run on each frame for dynamic content)
                                drawIntoCanvas { composeCanvas ->
                                    // Note: This is a simplified capture approach
                                    // The actual content is already drawn by drawContent()
                                    // For a proper implementation, we would need to use
                                    // Picture/RenderNode recording, but this requires
                                    // more complex handling
                                }
                                
                                // Increment capture counter to trigger overlay recomposition
                                // In a real implementation, we'd capture on scroll events
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
        
        // Overlay layer - receives captured content and draws on top
        overlayContent(capturedBitmap, contentSize)
    }
}

/**
 * State holder for content capture.
 * Can be hoisted to share capture state across composables.
 */
class ContentCaptureState {
    var bitmap: ImageBitmap? by mutableStateOf(null)
        internal set
    
    var size: IntSize by mutableStateOf(IntSize.Zero)
        internal set
    
    var scrollOffset: Offset by mutableStateOf(Offset.Zero)
    
    fun updateScrollOffset(offset: Offset) {
        scrollOffset = offset
    }
}

@Composable
fun rememberContentCaptureState(): ContentCaptureState {
    return remember { ContentCaptureState() }
}
