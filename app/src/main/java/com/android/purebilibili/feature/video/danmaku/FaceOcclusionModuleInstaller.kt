package com.android.purebilibili.feature.video.danmaku

import android.content.Context
import androidx.core.content.ContextCompat
import com.android.purebilibili.core.util.Logger
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val FACE_MODULE_CHECK_TIMEOUT_MS = 4_000L
private const val FACE_MODULE_INSTALL_TIMEOUT_MS = 60_000L

internal fun createFaceOcclusionDetector(): FaceDetector {
    return FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.08f)
            .enableTracking()
            .build()
    )
}

internal fun isGooglePlayServicesReady(context: Context): Boolean {
    val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    return status == ConnectionResult.SUCCESS
}

internal suspend fun checkFaceOcclusionModuleState(
    context: Context,
    detector: FaceDetector
): FaceOcclusionModuleState {
    if (!isGooglePlayServicesReady(context)) {
        return FaceOcclusionModuleState.GmsUnavailable
    }

    return try {
        val response = withTimeout(FACE_MODULE_CHECK_TIMEOUT_MS) {
            ModuleInstall
                .getClient(context)
                .areModulesAvailable(detector)
                .awaitResult()
        }
        if (response.areModulesAvailable()) {
            FaceOcclusionModuleState.Ready
        } else {
            FaceOcclusionModuleState.NotInstalled
        }
    } catch (t: Throwable) {
        Logger.w("FaceOcclusion", "check module state failed: ${t.message}")
        FaceOcclusionModuleState.Failed
    }
}

internal suspend fun installFaceOcclusionModule(
    context: Context,
    detector: FaceDetector,
    onProgress: (Int) -> Unit = {}
): FaceOcclusionModuleState {
    if (!isGooglePlayServicesReady(context)) {
        return FaceOcclusionModuleState.GmsUnavailable
    }

    return try {
        onProgress(0)
        val client = ModuleInstall.getClient(context)
        val listener = InstallStatusListener { update ->
            when (update.installState) {
                ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> onProgress(100)
                else -> {
                    val progress = update.progressInfo?.let { info ->
                        val total = info.totalBytesToDownload
                        if (total <= 0L) {
                            null
                        } else {
                            ((info.bytesDownloaded * 100L) / total)
                                .toInt()
                                .coerceIn(0, 100)
                        }
                    }
                    if (progress != null) {
                        onProgress(progress)
                    }
                }
            }
        }

        try {
            val request = ModuleInstallRequest
                .newBuilder()
                .addApi(detector)
                .setListener(listener, ContextCompat.getMainExecutor(context))
                .build()

            withTimeout(FACE_MODULE_INSTALL_TIMEOUT_MS) {
                client.installModules(request).awaitResult()
            }
        } finally {
            runCatching { client.unregisterListener(listener) }
        }

        checkFaceOcclusionModuleState(context, detector).also { state ->
            if (state == FaceOcclusionModuleState.Ready) {
                onProgress(100)
            }
        }
    } catch (t: Throwable) {
        Logger.w("FaceOcclusion", "install module failed: ${t.message}")
        FaceOcclusionModuleState.Failed
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
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
