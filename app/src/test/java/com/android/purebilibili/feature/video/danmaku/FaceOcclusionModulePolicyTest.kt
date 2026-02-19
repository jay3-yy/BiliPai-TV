package com.android.purebilibili.feature.video.danmaku

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FaceOcclusionModulePolicyTest {

    @Test
    fun gmsUnavailable_disablesDownloadAction() {
        val ui = resolveFaceOcclusionModuleUiState(FaceOcclusionModuleState.GmsUnavailable)

        assertFalse(ui.isReady)
        assertFalse(ui.isActionEnabled)
        assertEquals("当前设备不支持 Google Play 服务，无法使用人脸避挡", ui.statusText)
        assertEquals("不可用", ui.actionText)
    }

    @Test
    fun moduleNotInstalled_promptsManualDownload() {
        val ui = resolveFaceOcclusionModuleUiState(FaceOcclusionModuleState.NotInstalled)

        assertFalse(ui.isReady)
        assertTrue(ui.isActionEnabled)
        assertEquals("人脸模型未下载，点击后开始下载", ui.statusText)
        assertEquals("下载模型", ui.actionText)
    }

    @Test
    fun downloading_showsProgressAndBlocksRepeatedClick() {
        val ui = resolveFaceOcclusionModuleUiState(FaceOcclusionModuleState.Downloading)

        assertFalse(ui.isReady)
        assertFalse(ui.isActionEnabled)
        assertEquals("人脸模型下载中，请稍候…", ui.statusText)
        assertEquals("下载中", ui.actionText)
    }

    @Test
    fun downloading_withProgressPercent_showsPreciseStatus() {
        val ui = resolveFaceOcclusionModuleUiState(
            state = FaceOcclusionModuleState.Downloading,
            progressPercent = 45
        )

        assertEquals("人脸模型下载中：45%", ui.statusText)
        assertEquals("下载中", ui.actionText)
    }

    @Test
    fun readyState_enablesDetectionWithoutActionButton() {
        val ui = resolveFaceOcclusionModuleUiState(FaceOcclusionModuleState.Ready)

        assertTrue(ui.isReady)
        assertFalse(ui.showAction)
        assertEquals("人脸模型已就绪", ui.statusText)
    }
}
