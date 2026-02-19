package com.android.purebilibili.core.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * 用于控制全局底栏可见性的 CompositionLocal
 * 子页面可以通过此 Local 调用函数来显式显示/隐藏底栏
 * 参数: visible (Boolean)
 */
val LocalSetBottomBarVisible = compositionLocalOf<(Boolean) -> Unit> { 
    error("No SetBottomBarVisible provided") 
}

/**
 * 用于获取当前全局底栏可见性的 CompositionLocal (可选)
 */
val LocalBottomBarVisible = compositionLocalOf<Boolean> { true }
