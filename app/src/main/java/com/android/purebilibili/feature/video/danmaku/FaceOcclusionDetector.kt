package com.android.purebilibili.feature.video.danmaku

import androidx.media3.ui.PlayerView
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.feature.video.util.captureVideoScreenshot
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class FaceOcclusionDetectionResult(
    val verticalRegions: List<FaceOcclusionRegion>,
    val maskRects: List<FaceOcclusionMaskRect>,
    val visualMasks: List<FaceOcclusionVisualMask>
)

internal suspend fun detectFaceOcclusionRegions(
    playerView: PlayerView,
    sampleWidth: Int,
    sampleHeight: Int,
    detector: FaceDetector
): FaceOcclusionDetectionResult {
    val bitmap = captureVideoScreenshot(
        playerView = playerView,
        videoWidth = sampleWidth.coerceAtLeast(1),
        videoHeight = sampleHeight.coerceAtLeast(1)
    ) ?: return FaceOcclusionDetectionResult(
        verticalRegions = emptyList(),
        maskRects = emptyList(),
        visualMasks = emptyList()
    )

    return try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).awaitResult(timeoutMs = 1_300L)
        val bitmapWidth = bitmap.width.toFloat().coerceAtLeast(1f)
        val bitmapHeight = bitmap.height.toFloat().coerceAtLeast(1f)

        val visualMasks = faces.mapNotNull { face ->
            val box = face.boundingBox
            if (box.height() <= 0 || box.width() <= 0) return@mapNotNull null

            if (!isReliableFaceCandidate(face, bitmapWidth, bitmapHeight)) {
                return@mapNotNull null
            }

            val rect = FaceOcclusionMaskRect(
                leftRatio = (box.left / bitmapWidth).coerceIn(0f, 1f),
                topRatio = (box.top / bitmapHeight).coerceIn(0f, 1f),
                rightRatio = (box.right / bitmapWidth).coerceIn(0f, 1f),
                bottomRatio = (box.bottom / bitmapHeight).coerceIn(0f, 1f)
            ).normalized()
            if (rect.rightRatio <= rect.leftRatio || rect.bottomRatio <= rect.topRatio) {
                return@mapNotNull null
            }

            val contourPoints = face.getContour(FaceContour.FACE)
                ?.points
                ?.map { point ->
                    NormalizedPoint(
                        xRatio = (point.x / bitmapWidth).coerceIn(0f, 1f),
                        yRatio = (point.y / bitmapHeight).coerceIn(0f, 1f)
                    )
                }
                .orEmpty()

            buildVisualMask(
                rect = rect,
                polygon = contourPoints
            )
        }
        val maskRects = resolveFaceOcclusionMasks(visualMasks.map { it.fallbackRect })
        val verticalRegions = maskRects.map { mask ->
            FaceOcclusionRegion(mask.topRatio, mask.bottomRatio)
        }
        FaceOcclusionDetectionResult(
            verticalRegions = verticalRegions,
            maskRects = maskRects,
            visualMasks = visualMasks
        )
    } catch (t: Throwable) {
        Logger.w("FaceOcclusion", "Face detection failed: ${t.message}")
        FaceOcclusionDetectionResult(
            verticalRegions = emptyList(),
            maskRects = emptyList(),
            visualMasks = emptyList()
        )
    } finally {
        bitmap.recycle()
    }
}

private fun isReliableFaceCandidate(
    face: com.google.mlkit.vision.face.Face,
    bitmapWidth: Float,
    bitmapHeight: Float
): Boolean {
    val box = face.boundingBox
    val widthRatio = (box.width() / bitmapWidth).coerceIn(0f, 1f)
    val heightRatio = (box.height() / bitmapHeight).coerceIn(0f, 1f)
    val areaRatio = widthRatio * heightRatio
    if (widthRatio < 0.06f || heightRatio < 0.08f) return false
    if (areaRatio < 0.008f || areaRatio > 0.45f) return false

    val aspect = (box.width().toFloat() / box.height().toFloat()).coerceAtLeast(0.01f)
    if (aspect < 0.5f || aspect > 1.7f) return false

    val faceContourCount = face.getContour(FaceContour.FACE)?.points?.size ?: 0
    if (faceContourCount < 20) return false

    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
    val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position

    val eyeCount = (if (leftEye != null) 1 else 0) + (if (rightEye != null) 1 else 0)
    if (eyeCount == 0 || noseBase == null) return false

    if (leftEye != null && rightEye != null) {
        val eyeDistanceRatio = kotlin.math.abs(leftEye.x - rightEye.x) / bitmapWidth
        if (eyeDistanceRatio < 0.025f) return false
    }
    return true
}

private suspend fun <T> Task<T>.awaitResult(timeoutMs: Long): T {
    return withTimeout(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            addOnFailureListener { throwable ->
                if (continuation.isActive) {
                    continuation.resumeWithException(throwable)
                }
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
    }
}
