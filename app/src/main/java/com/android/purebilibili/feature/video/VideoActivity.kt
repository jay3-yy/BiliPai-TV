// 文件路径: feature/video/VideoActivity.kt
package com.android.purebilibili.feature.video

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import com.android.purebilibili.core.util.Logger
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Imports for moved classes
import com.android.purebilibili.feature.video.viewmodel.PlayerViewModel
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState


private const val TAG = "BiliPlayerActivity"


//  PiP 控制 Action 常量
private const val ACTION_PIP_CONTROL = "com.android.purebilibili.PIP_CONTROL"
private const val EXTRA_CONTROL_TYPE = "control_type"
private const val CONTROL_TYPE_PLAY = 1
private const val CONTROL_TYPE_PAUSE = 2

class VideoActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    private var isFullscreen by mutableStateOf(false)
    private var isInPipMode by mutableStateOf(false)
    
    //  PiP 广播接收器
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_CONTROL) {
                when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                    CONTROL_TYPE_PLAY -> {
                        Logger.d(TAG, "PiP: Play")
                        // 由 Compose 状态自动处理播放
                    }
                    CONTROL_TYPE_PAUSE -> {
                        Logger.d(TAG, "PiP: Pause")
                        // 由 Compose 状态自动处理暂停
                    }
                }
            }
        }
    }

    //  1. 权限回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) Logger.d(TAG, " 通知权限已授予") else com.android.purebilibili.core.util.Logger.w(TAG, " 通知权限被拒绝，媒体控件可能无法显示")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        //  2. 请求权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        //  注册 PiP 控制广播 (使用 ContextCompat 兼容所有版本)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter(ACTION_PIP_CONTROL)
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                pipReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        val bvid = intent.getStringExtra("bvid")
        if (bvid.isNullOrBlank()) {
            finish()
            return
        }

        updateStateFromConfig(resources.configuration)

        setContent {
            MaterialTheme {
                // VideoDetailScreen handles its own UI state and player initialization
                com.android.purebilibili.feature.video.screen.VideoDetailScreen(
                    bvid = bvid,
                    coverUrl = "", // Will be updated when video info loads
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onNavigateToAudioMode = {
                        viewModel.setAudioMode(true)
                    },
                    onVideoClick = { vid, options ->
                        VideoActivity.start(this, vid, options)
                    }
                    // We don't need to pass external player here as VideoDetailScreen manages it via VideoPlayerState
                    // But if we wanted to support smooth transition from notification (which might be playing), 
                    // VideoPlayerState's reuse logic handles checking MiniPlayerManager if applicable.
                    // For pure Activity launch, it creates/reuses logic internally.
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        //  注销广播接收器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                unregisterReceiver(pipReceiver)
            } catch (e: Exception) {
                com.android.purebilibili.core.util.Logger.w(TAG, "Failed to unregister PiP receiver", e)
            }
        }
    }

    // --- 配置与全屏逻辑保持不变 ---
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateStateFromConfig(newConfig)
    }

    private fun updateStateFromConfig(config: Configuration) {
        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        isFullscreen = isLandscape
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (isLandscape) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    //  构建 PiP 参数 (带播放控制按钮)
    private fun buildPipParams(isPlaying: Boolean = true): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val actions = mutableListOf<RemoteAction>()
            
            // 播放/暂停按钮
            val playPauseAction = if (isPlaying) {
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                    "暂停",
                    "暂停播放",
                    PendingIntent.getBroadcast(
                        this,
                        CONTROL_TYPE_PAUSE,
                        Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PAUSE),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                RemoteAction(
                    Icon.createWithResource(this, android.R.drawable.ic_media_play),
                    "播放",
                    "继续播放",
                    PendingIntent.getBroadcast(
                        this,
                        CONTROL_TYPE_PLAY,
                        Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_TYPE_PLAY),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
            actions.add(playPauseAction)
            
            builder.setActions(actions)
            
            // Android 12+: 自动进入 PiP 模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
                builder.setSeamlessResizeEnabled(true)
            }
        }
        
        return builder.build()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        //  [修复] 使用 SettingsManager 读取正确的小窗模式设置
        val mode = com.android.purebilibili.core.store.SettingsManager.getMiniPlayerModeSync(this)
        val shouldEnterPip = mode == com.android.purebilibili.core.store.SettingsManager.MiniPlayerMode.SYSTEM_PIP
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && shouldEnterPip) {
            val state = viewModel.uiState.value
            if (state is PlayerUiState.Success) {
                enterPictureInPictureMode(buildPipParams(true))
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
        Logger.d(TAG, "PiP mode changed: $isInPictureInPictureMode")
    }

    companion object {
        fun start(context: Context, bvid: String, options: android.os.Bundle? = null) {
            val intent = Intent(context, VideoActivity::class.java).apply {
                putExtra("bvid", bvid)
            }
            context.startActivity(intent, options)
        }
    }
}