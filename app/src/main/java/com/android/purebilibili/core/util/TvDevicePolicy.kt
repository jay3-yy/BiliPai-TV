package com.android.purebilibili.core.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal data class TvHomePerformanceConfig(
    val headerBlurEnabled: Boolean,
    val bottomBarBlurEnabled: Boolean,
    val liquidGlassEnabled: Boolean,
    val cardAnimationEnabled: Boolean,
    val cardTransitionEnabled: Boolean,
    val isDataSaverActive: Boolean,
    val preloadAheadCount: Int
)

internal fun resolveTvHomePerformanceConfig(
    isTvDevice: Boolean,
    isTvPerformanceProfileEnabled: Boolean,
    headerBlurEnabled: Boolean,
    bottomBarBlurEnabled: Boolean,
    liquidGlassEnabled: Boolean,
    cardAnimationEnabled: Boolean,
    cardTransitionEnabled: Boolean,
    isDataSaverActive: Boolean,
    normalPreloadAheadCount: Int = 5
): TvHomePerformanceConfig {
    val isTvPerformanceProfileActive = isTvDevice && isTvPerformanceProfileEnabled
    if (isTvPerformanceProfileActive) {
        return TvHomePerformanceConfig(
            headerBlurEnabled = false,
            bottomBarBlurEnabled = false,
            liquidGlassEnabled = false,
            cardAnimationEnabled = false,
            cardTransitionEnabled = false,
            isDataSaverActive = true,
            preloadAheadCount = 0
        )
    }

    return TvHomePerformanceConfig(
        headerBlurEnabled = headerBlurEnabled,
        bottomBarBlurEnabled = bottomBarBlurEnabled,
        liquidGlassEnabled = liquidGlassEnabled,
        cardAnimationEnabled = cardAnimationEnabled,
        cardTransitionEnabled = cardTransitionEnabled,
        isDataSaverActive = isDataSaverActive,
        preloadAheadCount = if (isDataSaverActive) 0 else normalPreloadAheadCount.coerceAtLeast(0)
    )
}

internal fun shouldTreatAsTvDevice(
    hasLeanbackFeature: Boolean,
    uiModeType: Int
): Boolean {
    return hasLeanbackFeature || uiModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

internal fun shouldHandleTvSelectKey(
    keyCode: Int,
    action: Int
): Boolean {
    if (action != KeyEvent.ACTION_UP) return false
    return keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
        keyCode == KeyEvent.KEYCODE_ENTER ||
        keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        keyCode == KeyEvent.KEYCODE_BUTTON_A
}

internal fun resolveTvPagerTargetPage(
    keyCode: Int,
    action: Int,
    currentPage: Int,
    pageCount: Int
): Int? {
    if (action != KeyEvent.ACTION_DOWN || pageCount <= 0) return null
    return when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> (currentPage - 1).takeIf { it >= 0 }
        KeyEvent.KEYCODE_DPAD_RIGHT -> (currentPage + 1).takeIf { it < pageCount }
        else -> null
    }
}

internal fun shouldHandleTvPlayPauseKey(
    keyCode: Int,
    action: Int
): Boolean {
    if (action != KeyEvent.ACTION_UP) return false
    return keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
        keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
}

internal fun shouldHandleTvBackKey(
    keyCode: Int,
    action: Int
): Boolean {
    return action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK
}

internal fun shouldHandleTvMenuKey(
    keyCode: Int,
    action: Int
): Boolean {
    return action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU
}

internal fun shouldHandleTvMoveFocusDownKey(
    keyCode: Int,
    action: Int
): Boolean {
    return action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN
}

fun isTvDevice(context: Context): Boolean {
    val packageManager = context.packageManager
    val hasLeanbackFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val uiModeManager = context.getSystemService(UiModeManager::class.java)
    val uiModeType = uiModeManager?.currentModeType
        ?: (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK)

    return shouldTreatAsTvDevice(hasLeanbackFeature = hasLeanbackFeature, uiModeType = uiModeType)
}

@Composable
fun rememberIsTvDevice(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        isTvDevice(context)
    }
}
