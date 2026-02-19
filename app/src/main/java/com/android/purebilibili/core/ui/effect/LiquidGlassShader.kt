package com.android.purebilibili.core.ui.effect

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import org.intellij.lang.annotations.Language

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object LiquidGlassShader {

    // Ported from Telegram: TMessagesProj/src/main/res/raw/liquid_glass_shader.agsl
    @Language("AGSL")
    const val SHADER = """
        uniform shader img;
        
        uniform float2 resolution;
        uniform float2 center;
        uniform float2 size;
        uniform float4 radius;
        uniform float thickness;
        uniform float refract_index;
        uniform float refract_intensity;
        uniform float4 foreground_color_premultiplied;
        
        // [New] Dynamic Optical Parameters
        uniform float aberration_strength; // 0.0 (none) to ~0.02 (strong)
        uniform float specular_alpha;      // 0.0 to 1.0
        uniform float3 light_dir;          // Light direction vector (normalized)
        
        // [New] Background Tint (Applied OUTSIDE the glass)
        uniform float4 background_color;   // Premultiplied
        
        half sdfRect(half2 p, half4 r) {
            r.xy = (p.x > 0.0) ? r.xy : r.zw;
            r.x  = (p.y > 0.0) ? r.x  : r.y;
            half2 q = abs(p) - size + r.x;
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r.x;
        }
        
        half4 srcOver(half4 src, half4 dst) {
            return src + dst * (1.0 - src.a);
        }
        
        half4 main(in float2 fragCoord) {
            half2 p = fragCoord - center;
            half sd = sdfRect(p, radius);
            half2 uv = fragCoord;
            
            half spec = 0.0;
            half4 baseColor;
            
            if (sd < 0.0) {
                half sdX = sdfRect(p + half2(1.0, 0.0), radius);
                half sdY = sdfRect(p + half2(0.0, 1.0), radius);
                
                half n_cos = max(thickness + sd, 0.0) / thickness;
                half n_cos2 = n_cos * n_cos;
                half n_sin = sqrt(1.0 - n_cos2);
                half3 normal = normalize(half3((sdX - sd) * n_cos, (sdY - sd) * n_cos, n_sin));
                
                half3 refract_vec = refract(half3(0.0, 0.0, -1.0), normal, 1.0 / refract_index);
                
                half h = sd < -thickness ? thickness : sqrt(sd * (-2.0 * thickness - sd));
                
                float z_comp = -refract_vec.z;
                if (z_comp < 0.001) z_comp = 0.001; 
                half refract_length = (h + 8.0 * thickness) / z_comp;
                
                half2 offset = refract_vec.xy * refract_length * refract_intensity;
                
                // [New] Chromatic Aberration
                if (aberration_strength > 0.001) {
                    half2 minUV = half2(0.0);
                    half2 maxUV = half2(resolution.x - 1.0, resolution.y - 1.0);
                    
                    // Split RGB channels
                    half r = img.eval(clamp(uv + offset * (1.0 + aberration_strength * 5.0), minUV, maxUV)).r;
                    half g = img.eval(clamp(uv + offset, minUV, maxUV)).g;
                    half b = img.eval(clamp(uv + offset * (1.0 - aberration_strength * 5.0), minUV, maxUV)).b;
                    half4 centerSamp = img.eval(clamp(uv + offset, minUV, maxUV));
                    
                    // [Fix] Removed foreground tint overlay to prevent double-indicator visual
                    baseColor = half4(r, g, b, centerSamp.a);
                } else {
                    uv += offset;
                    // Fix bounds for clamp
                    half2 minUV = half2(0.0);
                    half2 maxUV = half2(resolution.x - 1.0, resolution.y - 1.0);
                    uv = clamp(uv, minUV, maxUV);
                    
                    // [Fix] Removed foreground tint overlay to prevent double-indicator visual
                    baseColor = img.eval(uv);
                }
                
                // [New] Specular Highlight (Blinn-Phong)
                half3 viewDir = half3(0.0, 0.0, 1.0);
                half3 halfVector = normalize(light_dir + viewDir);
                float NdotH = max(dot(normal, halfVector), 0.0);
                spec = pow(NdotH, 24.0) * specular_alpha;
            } else {
                // Outside liquid: Sample background directly (no tint overlay)
                // [Fix] Removed background_color overlay to prevent double-indicator visual
                baseColor = img.eval(uv);
            }
            
            return baseColor + half4(spec, spec, spec, 0.0);
        }
    """
}
