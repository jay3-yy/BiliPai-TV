package com.android.purebilibili.feature.download

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🔧 WorkManager Worker for background downloads
 * 
 * This worker handles video downloads in a way that survives app backgrounding
 * and process death. WorkManager automatically reschedules work if the process dies.
 */
class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val TAG_DOWNLOAD = "video_download"
        
        /**
         * 调度下载任务
         */
        fun enqueue(context: Context, taskId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val inputData = Data.Builder()
                .putString(KEY_TASK_ID, taskId)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(TAG_DOWNLOAD)
                .addTag(taskId) // 用于取消特定任务
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30_000L, // 30 秒初始退避
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    taskId,
                    ExistingWorkPolicy.KEEP, // 如果已存在则保留
                    workRequest
                )
            
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "📥 Enqueued download: $taskId")
        }
        
        /**
         * 取消下载任务
         */
        fun cancel(context: Context, taskId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(taskId)
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "⏹️ Cancelled download: $taskId")
        }
        
        /**
         * 取消所有下载任务
         */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG_DOWNLOAD)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) 
            ?: return@withContext Result.failure()
        
        com.android.purebilibili.core.util.Logger.d("DownloadWorker", "🚀 Starting download: $taskId")
        
        try {
            // 执行下载
            DownloadManager.executeDownload(taskId)
            
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "✅ Download completed: $taskId")
            Result.success()
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            com.android.purebilibili.core.util.Logger.d("DownloadWorker", "⏸️ Download paused: $taskId")
            // 用户主动取消，不重试
            Result.failure()
            
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e("DownloadWorker", "❌ Download failed: $taskId", e)
            
            // 更新任务状态
            DownloadManager.markFailed(taskId, e.message ?: "下载失败")
            
            // 网络错误时重试，其他错误直接失败
            if (e is java.net.UnknownHostException || 
                e is java.net.SocketTimeoutException ||
                e is java.net.ConnectException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    override suspend fun getForegroundInfo(): ForegroundInfo {
        // 创建前台通知（Android 12+ WorkManager 要求）
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext, 
            "download_channel"
        )
            .setContentTitle("下载中...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        
        return ForegroundInfo(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}
