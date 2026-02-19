package com.android.purebilibili.feature.video.ui.overlay

import android.graphics.Color as AndroidColor
import android.os.SystemClock
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.feature.live.LiveDanmakuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bytedance.danmaku.render.engine.control.DanmakuController
import com.bytedance.danmaku.render.engine.data.DanmakuData
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_BOTTOM_CENTER
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_TOP_CENTER
import com.bytedance.danmaku.render.engine.DanmakuView
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 直播弹幕图层
 * 使用 ByteDance DanmakuRenderEngine 渲染
 * 
 * 修复记录:
 * - 使用 mutableStateOf 替代 object 管理状态
 * - 添加 isActive 检查防止协程泄漏
 * - 添加 try-catch 防止崩溃
 */
@Composable
fun LiveDanmakuOverlay(
    danmakuFlow: SharedFlow<LiveDanmakuItem>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 使用稳定的状态管理
    var controller by remember { mutableStateOf<DanmakuController?>(null) }
    var startTime by remember { mutableLongStateOf(0L) }
    var isStarted by remember { mutableStateOf(false) }
    val danmakuList = remember { mutableListOf<DanmakuData>() }
    val pendingDanmaku = remember { mutableListOf<DanmakuData>() }

    AndroidView(
        factory = { ctx ->
            DanmakuView(ctx).apply {
                try {
                    // 设置透明背景
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    
                    // 设置布局参数
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // 保存引用
                    controller = this.controller
                    startTime = SystemClock.elapsedRealtime()
                    
                    android.util.Log.d("LiveDanmakuOverlay", "DanmakuView created")
                    
                    // 启动渲染引擎
                    this.controller.start(0)
                    isStarted = true
                } catch (e: Exception) {
                    android.util.Log.e("LiveDanmakuOverlay", "DanmakuView init failed: ${e.message}")
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = {
            try {
                // 确保控制器正在运行
                val ctrl = controller
                if (ctrl != null && !isStarted) {
                    val currentTime = SystemClock.elapsedRealtime() - startTime
                    ctrl.start(currentTime)
                    isStarted = true
                }
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "Update failed: ${e.message}")
            }
        }
    )

    // 批量合并弹幕更新，避免每条弹幕都全量 setData 导致卡顿
    LaunchedEffect(controller, isStarted) {
        var tick = 0
        while (isActive) {
            try {
                val ctrl = controller
                if (ctrl != null && isStarted) {
                    val currentTime = SystemClock.elapsedRealtime() - startTime
                    var dataChanged = false

                    if (pendingDanmaku.isNotEmpty()) {
                        danmakuList.addAll(pendingDanmaku)
                        pendingDanmaku.clear()
                        dataChanged = true
                    }

                    if (dataChanged || tick % 10 == 0) {
                        val expireBefore = currentTime - 20_000
                        val beforeSize = danmakuList.size
                        danmakuList.removeAll { it.showAtTime < expireBefore }
                        if (beforeSize != danmakuList.size) {
                            dataChanged = true
                        }
                    }

                    if (dataChanged) {
                        danmakuList.sortBy { it.showAtTime }
                        ctrl.setData(danmakuList.toList(), 0)
                        ctrl.invalidateView()
                    }

                    // 保持渲染时钟前进，但降频到 10fps 减轻主线程压力
                    ctrl.start(currentTime)
                    tick++
                }
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "Render loop error: ${e.message}")
            }
            delay(100)
        }
    }
    
    // 监听弹幕流
    LaunchedEffect(danmakuFlow) {
        danmakuFlow.collect { item ->
            try {
                if (!isStarted) return@collect

                // 计算当前相对时间（使用单调时钟，避免系统时间调整导致漂移）
                val currentTime = SystemClock.elapsedRealtime() - startTime
                val danmakuData = createDanmakuData(item, currentTime, context, controller)
                pendingDanmaku.add(danmakuData)
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "Danmaku collect error: ${e.message}")
            }
        }
    }
    
    // 清理
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("LiveDanmakuOverlay", "Disposing DanmakuView")
            try {
                controller?.stop()
                danmakuList.clear()
                pendingDanmaku.clear()
                isStarted = false
                controller = null
            } catch (e: Exception) {
                android.util.Log.e("LiveDanmakuOverlay", "Dispose error: ${e.message}")
            }
        }
    }
}


private fun createDanmakuData(
    item: LiveDanmakuItem, 
    currentTime: Long, 
    context: android.content.Context,
    controller: DanmakuController?
): DanmakuData {
    val textSize = 42f
    val layerType = when (item.mode) {
        4 -> LAYER_TYPE_BOTTOM_CENTER
        5 -> LAYER_TYPE_TOP_CENTER
        else -> LAYER_TYPE_SCROLL
    }
    
    val textColor = if (item.color == 0) {
        AndroidColor.WHITE
    } else {
        (0xFF000000 or item.color.toLong()).toInt()
    }

    return com.android.purebilibili.feature.video.danmaku.createBitmapDanmaku(
        context = context,
        text = item.text,
        textColor = textColor,
        textSize = textSize,
        layerType = layerType,
        showAtTime = currentTime + 50L,
        onUpdate = {
            // 当图片加载完成后刷新视图
            controller?.invalidateView()
        }
    )
}
