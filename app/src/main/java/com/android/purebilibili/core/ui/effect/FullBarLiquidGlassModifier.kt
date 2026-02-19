package com.android.purebilibili.core.ui.effect

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language

/**
 * Shader that applies liquid glass refraction to the ENTIRE area,
 * creating a distortion effect for content sampled from behind the element.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object FullBarLiquidGlassShader {
    
    @Language("AGSL")
    const val SHADER = """
        uniform shader img;
        
        uniform float2 resolution;
        uniform float radius;          // Corner radius
        uniform float refract_intensity;
        uniform float2 scroll_offset;  // Current scroll position
        
        // Simplified refraction for full-area effect
        half4 main(in float2 fragCoord) {
            half2 uv = fragCoord;
            
            // Create a subtle wave distortion based on position
            // This simulates looking through a glass panel
            float wave = sin(fragCoord.x * 0.02 + scroll_offset.y * 0.01) * 
                        cos(fragCoord.y * 0.02 + scroll_offset.x * 0.01);
            
            // Apply refraction offset
            float2 offset = float2(wave, wave) * refract_intensity * 20.0;
            
            // Clamp to bounds
            half2 minUV = half2(0.0);
            half2 maxUV = half2(resolution.x - 1.0, resolution.y - 1.0);
            uv = clamp(uv + offset, minUV, maxUV);
            
            // Sample with slight chromatic aberration for glass feel
            float aberration = refract_intensity * 0.3;
            half r = img.eval(clamp(uv + offset * (1.0 + aberration), minUV, maxUV)).r;
            half g = img.eval(uv).g;
            half b = img.eval(clamp(uv + offset * (1.0 - aberration), minUV, maxUV)).b;
            half4 centerSamp = img.eval(uv);
            
            return half4(r, g, b, centerSamp.a);
        }
    """
}

/**
 * Modifier that applies liquid glass refraction effect to the entire element.
 * Unlike [liquidGlass], this doesn't define a specific "bubble" area -
 * the entire element gets the refraction treatment.
 *
 * @param refractIntensity Intensity of the refraction effect (0.0 - 1.0)
 * @param scrollOffset Current scroll offset to create dynamic distortion
 * @param cornerRadius Corner radius for the element
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.fullBarLiquidGlass(
    refractIntensity: Float = 0.3f,
    scrollOffset: Offset = Offset.Zero,
    cornerRadius: Dp = 24.dp
): Modifier = composed {
    val density = LocalDensity.current
    val radiusPx = with(density) { cornerRadius.toPx() }
    
    val shader = remember { RuntimeShader(FullBarLiquidGlassShader.SHADER) }
    
    this.graphicsLayer {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("radius", radiusPx)
        shader.setFloatUniform("refract_intensity", refractIntensity)
        shader.setFloatUniform("scroll_offset", scrollOffset.x, scrollOffset.y)
        
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "img")
            .asComposeRenderEffect()
    }
}

/**
 * A simpler approach: use transparent background and let the shader
 * sample whatever is rendered below in the graphics layer stack.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.liquidGlassBackground(
    refractIntensity: Float = 0.2f,
    scrollOffsetProvider: () -> Float, // Use lambda to avoid recomposition
    backgroundColor: Color = Color.Transparent
): Modifier = composed {
    val shader = remember { RuntimeShader(LiquidGlassBackgroundShader.SHADER) }
    
    this.graphicsLayer {
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("refract_intensity", refractIntensity)
        // Read the value inside the graphicsLayer block - this triggers redraw only, not recomposition
        shader.setFloatUniform("scroll_offset", scrollOffsetProvider())
        
        val bgColor = backgroundColor.toArgb()
        val a = android.graphics.Color.alpha(bgColor) / 255f
        val r = android.graphics.Color.red(bgColor) / 255f * a
        val g = android.graphics.Color.green(bgColor) / 255f * a
        val b = android.graphics.Color.blue(bgColor) / 255f * a
        shader.setFloatUniform("background_color", r, g, b, a)
        
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "img")
            .asComposeRenderEffect()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object LiquidGlassBackgroundShader {
    @Language("AGSL")
    const val SHADER = """
        uniform shader img;
        
        uniform float2 resolution;
        uniform float refract_intensity;
        uniform float scroll_offset;
        uniform float4 background_color;  // Premultiplied
        
        half4 main(in float2 fragCoord) {
            half2 uv = fragCoord;
            
            // Create vertical wave distortion based on scroll
            // Enhanced: stronger ripple effect when scrolling
            float scrollProgress = scroll_offset * 0.008;  // More responsive to scroll
            float waveX = sin(fragCoord.x * 0.025 + scrollProgress) * 0.6;
            float waveY = cos(fragCoord.y * 0.015 + scrollProgress * 0.5) * 0.8;
            float wave = waveX * waveY + sin(scrollProgress * 0.3) * 0.4;
            
            // Increased multiplier for more visible distortion
            float2 offset = float2(wave * 0.4, wave * 1.2) * refract_intensity * 50.0;
            
            half2 minUV = half2(0.0);
            half2 maxUV = half2(resolution.x - 1.0, resolution.y - 1.0);
            uv = clamp(uv + offset, minUV, maxUV);
            
            // Enhanced chromatic aberration for glass feel
            float aberration = refract_intensity * 0.35;
            half r = img.eval(clamp(uv + float2(aberration * 4.0, aberration * 1.5), minUV, maxUV)).r;
            half g = img.eval(uv).g;
            half b = img.eval(clamp(uv - float2(aberration * 4.0, aberration * 1.5), minUV, maxUV)).b;
            half4 sampled = half4(r, g, b, 1.0);
            
            // Blend with semi-transparent background for legibility
            half4 result = sampled * (1.0 - background_color.a) + background_color;
            
            return result;
        }
    """
}
