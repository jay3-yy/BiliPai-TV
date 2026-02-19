package com.android.purebilibili.core.ui.animation.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class ParticleRenderer(
    private val textureBitmap: Bitmap?,
    private val onAnimationComplete: () -> Unit,
    private val onFirstFrame: () -> Unit,
    private val animationDurationSec: Float = 2.0f,
    private val particleStep: Int = 2,
    private val waveDurationSec: Float = 0.4f,
    private val waveRandomSec: Float = 0.1f
) : GLSurfaceView.Renderer {

    private var programHandle: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var velocityHandle: Int = 0
    private var startTimeHandle: Int = 0
    private var timeHandle: Int = 0
    private var canvasSizeHandle: Int = 0

    private var particleCount = 0
    private var startTime = 0L

    // Buffers
    private lateinit var positionBuffer: FloatBuffer
    private lateinit var colorBuffer: FloatBuffer
    private lateinit var velocityBuffer: FloatBuffer
    private lateinit var startTimeBuffer: FloatBuffer
    
    private var isFirstFrame = true

    private val vertexShaderCode = """
        uniform float u_Time;
        uniform vec2 u_CanvasSize;
        
        attribute vec2 a_Position;
        attribute vec4 a_Color;
        attribute vec2 a_Velocity;
        attribute float a_StartTime;
        
        varying vec4 v_Color;
        
        // Pseudo-random function
        float random(vec2 st) {
            return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
        }

        void main() {
            float t = u_Time - a_StartTime;
            
            // Calculate position based on time
            float effectiveT = max(t, 0.0);  // Clamp to 0 if not started yet
            
            // Physics Simulation
            float gravity = 500.0;  // Pixels per second squared
            float wind = -150.0;    // Pixels per second
            
            // Non-linear time factors
            float t2 = effectiveT * effectiveT;
            
            // Update Position
            // x = x0 + vx*t + wind*t
            // y = y0 + vy*t + 0.5*g*t^2
            
            float newX = a_Position.x + (a_Velocity.x + wind) * effectiveT;
            float newY = a_Position.y + a_Velocity.y * effectiveT + 0.5 * gravity * t2;
            
            // Add some noise/turbulence
            float noiseX = (random(vec2(newY * 0.01, u_Time)) - 0.5) * 20.0 * effectiveT;
            newX += noiseX;

            // Convert Logic Coords (0..W, 0..H) to NDC (-1..1, 1..-1)
            // Note: GL Origin is Bottom-Left, but Android Canvas is Top-Left usually.
            // We assume input Y is Top-Left based.
            
            float ndcX = (newX / u_CanvasSize.x) * 2.0 - 1.0;
            float ndcY = 1.0 - (newY / u_CanvasSize.y) * 2.0; // Flip Y for NDC
            
            gl_Position = vec4(ndcX, ndcY, 0.0, 1.0);
            
            // Size attenuation based on life
            gl_PointSize = 4.0 * (1.0 - effectiveT * 0.5); 
            
            // Color fading
            float alpha = 1.0 - effectiveT * 0.8;
            if (alpha < 0.0) alpha = 0.0;
            v_Color = vec4(a_Color.rgb, a_Color.a * alpha);
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        varying vec4 v_Color;
        
        void main() {
            if (v_Color.a <= 0.01) discard;
            gl_FragColor = v_Color;
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        
        programHandle = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode)
        if (programHandle == 0) return

        // Get Handles
        positionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color")
        velocityHandle = GLES20.glGetAttribLocation(programHandle, "a_Velocity")
        startTimeHandle = GLES20.glGetAttribLocation(programHandle, "a_StartTime")
        
        timeHandle = GLES20.glGetUniformLocation(programHandle, "u_Time")
        canvasSizeHandle = GLES20.glGetUniformLocation(programHandle, "u_CanvasSize")

        // Prepare Particles
        textureBitmap?.let { prepareParticles(it) }
        startTime = System.currentTimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(programHandle)
        GLES20.glUniform2f(canvasSizeHandle, width.toFloat(), height.toFloat())
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        if (particleCount == 0) return

        GLES20.glUseProgram(programHandle)
        
        val currentTime = (System.currentTimeMillis() - startTime) / 1000f
        GLES20.glUniform1f(timeHandle, currentTime)
        
        // Pass Data
        positionBuffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        colorBuffer.position(0)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)
        GLES20.glEnableVertexAttribArray(colorHandle)
        
        velocityBuffer.position(0)
        GLES20.glVertexAttribPointer(velocityHandle, 2, GLES20.GL_FLOAT, false, 0, velocityBuffer)
        GLES20.glEnableVertexAttribArray(velocityHandle)
        
        startTimeBuffer.position(0)
        GLES20.glVertexAttribPointer(startTimeHandle, 1, GLES20.GL_FLOAT, false, 0, startTimeBuffer)
        GLES20.glEnableVertexAttribArray(startTimeHandle)
        
        // Draw
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleCount)
        
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
        GLES20.glDisableVertexAttribArray(velocityHandle)
        GLES20.glDisableVertexAttribArray(startTimeHandle)
        
        if (isFirstFrame) {
            isFirstFrame = false
            onFirstFrame()
        }

        if (currentTime > animationDurationSec) {
             onAnimationComplete()
        }
    }
    
    // ... rest of class

    private fun prepareParticles(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val tempPos = ArrayList<Float>()
        val tempColor = ArrayList<Float>()
        val tempVel = ArrayList<Float>()
        val tempStart = ArrayList<Float>()
        
        // Sampling Step (Density)
        // Adjust this for performance vs quality. 2 = capture every 2nd pixel
        val step = particleStep.coerceIn(1, 6)
        
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val color = pixels[y * width + x]
                val alpha = (color ushr 24) and 0xFF
                
                if (alpha > 20) {
                    // Position
                    tempPos.add(x.toFloat())
                    tempPos.add(y.toFloat())
                    
                    // Color (Normalize to 0..1)
                    tempColor.add(((color ushr 16) and 0xFF) / 255f)
                    tempColor.add(((color ushr 8) and 0xFF) / 255f)
                    tempColor.add((color and 0xFF) / 255f)
                    tempColor.add(alpha / 255f)
                    
                    // Velocity (Random initial burst)
                    // Initial random velocity - particles fly towards top-left
                    val vx = (Random.nextFloat() - 0.7f) * 150f // Bias towards left
                    val vy = (Random.nextFloat() - 0.7f) * 100f  // Bias towards up
                    tempVel.add(vx)
                    tempVel.add(vy)
                    
                    // Start Time (Wave effect from Bottom-Right to Top-Left)
                    // Distance from bottom-right corner
                    val dx = width - x
                    val dy = height - y
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val maxDist = kotlin.math.sqrt((width * width + height * height).toDouble()).toFloat()
                    val normalizedDist = dist / maxDist
                    
                    // Particles start dissolving quickly (0.0s to 0.5s range)
                    // Bottom-right particles start first (normalizedDist is small for them)
                    tempStart.add(normalizedDist * waveDurationSec + Random.nextFloat() * waveRandomSec)
                }
            }
        }
        
        particleCount = tempPos.size / 2
        
        positionBuffer = ByteBuffer.allocateDirect(tempPos.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        positionBuffer.put(tempPos.toFloatArray()).position(0)
        
        colorBuffer = ByteBuffer.allocateDirect(tempColor.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        colorBuffer.put(tempColor.toFloatArray()).position(0)
        
        velocityBuffer = ByteBuffer.allocateDirect(tempVel.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        velocityBuffer.put(tempVel.toFloatArray()).position(0)
        
        startTimeBuffer = ByteBuffer.allocateDirect(tempStart.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        startTimeBuffer.put(tempStart.toFloatArray()).position(0)
    }
}
