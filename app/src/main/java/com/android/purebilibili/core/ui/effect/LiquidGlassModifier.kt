package com.android.purebilibili.core.ui.effect

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.composed

/**
 * Applies the Liquid Glass refraction effect to the content.
 *
 * @param rectCenter The logical center of the glass/liquid rectangle, relative to this component.
 * @param rectSize The half-size (w/2, h/2) of the liquid rectangle.
 * @param radius Corner radius of the liquid rectangle.
 * @param refractIndex Refraction index (e.g., 1.5 for glass).
 * @param refractIntensity Intensity of the distortion.
 * @param intensity Global intensity/opacity of the effect.
 * @param color Tint color for the liquid.
 */
fun Modifier.liquidGlass(
    rectCenter: androidx.compose.ui.geometry.Offset,
    rectSize: androidx.compose.ui.geometry.Size,
    radius: Dp,
    refractIndex: Float = 1.05f,
    refractIntensity: Float = 0.5f,
    aberrationStrength: Float = 0f,
    specularAlpha: Float = 0.4f,
    backgroundColor: Color = Color.Transparent, // [New] Color to mask outside area
    color: Color = Color.White.copy(alpha = 0.15f)
): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // Fallback for Android 12 and below: just a simple background or skip
        Modifier
    } else {
        val density = LocalDensity.current
        
        this.graphicsLayer {
            val shader = RuntimeShader(LiquidGlassShader.SHADER)
            
            // Uniforms
            // uniform float2 resolution;
            shader.setFloatUniform("resolution", size.width, size.height)
            
            // uniform float2 center;
            shader.setFloatUniform("center", rectCenter.x, rectCenter.y)
            
            // uniform float2 size; (Note: AGSL sdfRect expects HALF size)
            shader.setFloatUniform("size", rectSize.width / 2f, rectSize.height / 2f)
            
            // uniform float4 radius; (xy: top-right, zw: bottom-right? AGSL logic is custom)
            // Telegram's SDF logic uses r.xy for right side, r.zw for left side somehow?
            // Actually sdfRect logic:
            // r.xy = (p.x > 0.0) ? r.xy : r.zw; -> if x>0 use xy (right), else zw (left)
            // r.x  = (p.y > 0.0) ? r.x  : r.y; -> if y>0 use x (bottom?), else y (top?)
            // Let's assume uniform radius for now
            val rPx = with(density) { radius.toPx() }
            shader.setFloatUniform("radius", rPx, rPx, rPx, rPx)
            
            // uniform float thickness;
            // Controls the bevel/edge thickness
            // Telegram uses dp(11).
            val thicknessPx = with(density) { 11.dp.toPx() }
            shader.setFloatUniform("thickness", thicknessPx)
            
            // uniform float refract_index;
            shader.setFloatUniform("refract_index", refractIndex)
            
            // uniform float refract_intensity;
            // Telegram passes intensity directly. 0.5f might be too strong for px-based UVs if not scaled?
            // Telegram's shader: uv += refract_vec.xy * refract_length * refract_intensity;
            // refract_length is in pixels (based on size/thickness).
            // So intensity should be small scalar. Default 0.5f is okay.
            shader.setFloatUniform("refract_intensity", refractIntensity)
            
            // [New] Optical Uniforms
            shader.setFloatUniform("aberration_strength", aberrationStrength)
            shader.setFloatUniform("specular_alpha", specularAlpha)
            // Fixed top-left light source direction (normalized)
            shader.setFloatUniform("light_dir", -0.35f, -0.6f, 0.72f)
            
            // [New] Background Tint
            val bgArgb = backgroundColor.toArgb()
            val bgA = android.graphics.Color.alpha(bgArgb) / 255f
            val bgR = android.graphics.Color.red(bgArgb) / 255f * bgA
            val bgG = android.graphics.Color.green(bgArgb) / 255f * bgA
            val bgB = android.graphics.Color.blue(bgArgb) / 255f * bgA
            shader.setFloatUniform("background_color", bgR, bgG, bgB, bgA)
             
            // uniform float4 foreground_color_premultiplied;
            val argb = color.toArgb()
            val a = android.graphics.Color.alpha(argb) / 255f
            val r = android.graphics.Color.red(argb) / 255f * a
            val g = android.graphics.Color.green(argb) / 255f * a
            val b = android.graphics.Color.blue(argb) / 255f * a
            shader.setFloatUniform("foreground_color_premultiplied", r, g, b, a)

            // Apply as RenderEffect
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "img")
                .asComposeRenderEffect()
        }
    }
}
