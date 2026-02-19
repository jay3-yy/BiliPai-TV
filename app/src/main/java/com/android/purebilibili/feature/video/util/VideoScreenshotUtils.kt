package com.android.purebilibili.feature.video.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

private val SCREENSHOT_FILE_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

private val ILLEGAL_FILE_CHAR_REGEX = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

fun resolveScreenshotDimensions(
    videoWidth: Int,
    videoHeight: Int,
    surfaceWidth: Int,
    surfaceHeight: Int,
): Pair<Int, Int> {
    if (videoWidth > 0 && videoHeight > 0) {
        return videoWidth to videoHeight
    }
    if (surfaceWidth > 0 && surfaceHeight > 0) {
        return surfaceWidth to surfaceHeight
    }
    return 1 to 1
}

fun buildScreenshotFileName(videoTitle: String, timestampMs: Long = System.currentTimeMillis()): String {
    val safeTitle = videoTitle
        .replace(ILLEGAL_FILE_CHAR_REGEX, "_")
        .trim()
        .ifEmpty { "video" }
        .take(64)

    val timePart = SCREENSHOT_FILE_TIME_FORMAT.format(
        Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
    )
    return "${safeTitle}_$timePart.png"
}

suspend fun captureAndSaveVideoScreenshot(
    context: Context,
    playerView: PlayerView,
    videoWidth: Int,
    videoHeight: Int,
    videoTitle: String,
    timestampMs: Long = System.currentTimeMillis(),
): Boolean {
    val bitmap = captureVideoScreenshot(
        playerView = playerView,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
    ) ?: return false

    val fileName = buildScreenshotFileName(videoTitle = videoTitle, timestampMs = timestampMs)
    return try {
        saveScreenshotToGallery(context = context, bitmap = bitmap, fileName = fileName)
    } finally {
        bitmap.recycle()
    }
}

suspend fun captureVideoScreenshot(
    playerView: PlayerView,
    videoWidth: Int,
    videoHeight: Int,
): Bitmap? = withContext(Dispatchers.Main.immediate) {
    val videoSurface = playerView.videoSurfaceView
    val (targetWidth, targetHeight) = resolveScreenshotDimensions(
        videoWidth = videoWidth,
        videoHeight = videoHeight,
        surfaceWidth = videoSurface?.width ?: playerView.width,
        surfaceHeight = videoSurface?.height ?: playerView.height,
    )

    when (videoSurface) {
        is TextureView -> {
            val bitmap = runCatching { videoSurface.getBitmap(targetWidth, targetHeight) }.getOrNull()
            if (bitmap == null) {
                Logger.w("VideoScreenshot", "TextureView getBitmap returned null")
                null
            } else {
                resizeBitmapIfNeeded(bitmap = bitmap, targetWidth = targetWidth, targetHeight = targetHeight)
            }
        }

        is SurfaceView -> captureSurfaceViewBitmap(
            surfaceView = videoSurface,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
        )

        else -> {
            val fallbackWidth = maxOf(playerView.width, 1)
            val fallbackHeight = maxOf(playerView.height, 1)
            val fallbackBitmap = runCatching {
                Bitmap.createBitmap(fallbackWidth, fallbackHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
                    val canvas = Canvas(bitmap)
                    playerView.draw(canvas)
                }
            }.getOrNull()

            if (fallbackBitmap == null) {
                Logger.w("VideoScreenshot", "Fallback draw capture failed")
                null
            } else {
                resizeBitmapIfNeeded(
                    bitmap = fallbackBitmap,
                    targetWidth = targetWidth,
                    targetHeight = targetHeight,
                )
            }
        }
    }
}

suspend fun saveScreenshotToGallery(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
): Boolean = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BiliPai/Screenshots")
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return@withContext false

    try {
        val wrote = resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: false

        if (!wrote) {
            resolver.delete(uri, null, null)
            return@withContext false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        true
    } catch (e: Exception) {
        Logger.e("VideoScreenshot", "Failed to save screenshot", e)
        resolver.delete(uri, null, null)
        false
    }
}

private suspend fun captureSurfaceViewBitmap(
    surfaceView: SurfaceView,
    targetWidth: Int,
    targetHeight: Int,
): Bitmap? = suspendCancellableCoroutine { continuation ->
    val copyWidth = maxOf(surfaceView.width, 1)
    val copyHeight = maxOf(surfaceView.height, 1)
    val sourceBitmap = Bitmap.createBitmap(copyWidth, copyHeight, Bitmap.Config.ARGB_8888)

    try {
        PixelCopy.request(
            surfaceView,
            sourceBitmap,
            { result ->
                if (!continuation.isActive) {
                    sourceBitmap.recycle()
                    return@request
                }
                if (result == PixelCopy.SUCCESS) {
                    continuation.resume(
                        resizeBitmapIfNeeded(
                            bitmap = sourceBitmap,
                            targetWidth = targetWidth,
                            targetHeight = targetHeight,
                        )
                    )
                } else {
                    Logger.w("VideoScreenshot", "PixelCopy failed with code: $result")
                    sourceBitmap.recycle()
                    continuation.resume(null)
                }
            },
            Handler(Looper.getMainLooper()),
        )
    } catch (e: Exception) {
        Logger.e("VideoScreenshot", "PixelCopy exception", e)
        sourceBitmap.recycle()
        if (continuation.isActive) {
            continuation.resume(null)
        }
    }

    continuation.invokeOnCancellation {
        sourceBitmap.recycle()
    }
}

private fun resizeBitmapIfNeeded(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
    val safeWidth = maxOf(targetWidth, 1)
    val safeHeight = maxOf(targetHeight, 1)
    if (bitmap.width == safeWidth && bitmap.height == safeHeight) {
        return bitmap
    }
    return Bitmap.createScaledBitmap(bitmap, safeWidth, safeHeight, true).also { scaled ->
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
    }
}
