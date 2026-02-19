package com.android.purebilibili.feature.video.danmaku

enum class FaceOcclusionModuleState {
    Checking,
    GmsUnavailable,
    NotInstalled,
    Downloading,
    Ready,
    Failed
}

data class FaceOcclusionModuleUiState(
    val statusText: String,
    val actionText: String,
    val showAction: Boolean,
    val isActionEnabled: Boolean,
    val isReady: Boolean
)

fun resolveFaceOcclusionModuleUiState(
    state: FaceOcclusionModuleState,
    progressPercent: Int? = null
): FaceOcclusionModuleUiState {
    return when (state) {
        FaceOcclusionModuleState.Checking -> FaceOcclusionModuleUiState(
            statusText = "正在检测模型状态…",
            actionText = "检测中",
            showAction = true,
            isActionEnabled = false,
            isReady = false
        )

        FaceOcclusionModuleState.GmsUnavailable -> FaceOcclusionModuleUiState(
            statusText = "当前设备不支持 Google Play 服务，无法使用人脸避挡",
            actionText = "不可用",
            showAction = true,
            isActionEnabled = false,
            isReady = false
        )

        FaceOcclusionModuleState.NotInstalled -> FaceOcclusionModuleUiState(
            statusText = "人脸模型未下载，点击后开始下载",
            actionText = "下载模型",
            showAction = true,
            isActionEnabled = true,
            isReady = false
        )

        FaceOcclusionModuleState.Downloading -> FaceOcclusionModuleUiState(
            statusText = progressPercent
                ?.coerceIn(0, 100)
                ?.let { "人脸模型下载中：$it%" }
                ?: "人脸模型下载中，请稍候…",
            actionText = "下载中",
            showAction = true,
            isActionEnabled = false,
            isReady = false
        )

        FaceOcclusionModuleState.Ready -> FaceOcclusionModuleUiState(
            statusText = "人脸模型已就绪",
            actionText = "",
            showAction = false,
            isActionEnabled = false,
            isReady = true
        )

        FaceOcclusionModuleState.Failed -> FaceOcclusionModuleUiState(
            statusText = "模型下载失败，请重试",
            actionText = "重试下载",
            showAction = true,
            isActionEnabled = true,
            isReady = false
        )
    }
}
