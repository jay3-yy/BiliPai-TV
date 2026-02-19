package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.data.model.response.HistoryData
import com.android.purebilibili.data.model.response.HistoryCursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 历史记录数据结果（包含列表和游标）
 */
data class HistoryResult(
    val list: List<HistoryData>,
    val cursor: HistoryCursor?
)

object HistoryRepository {
    private val api = NetworkModule.api

    /**
     * 获取历史记录列表（支持游标分页）
     * @param ps 每页数量
     * @param max 游标: 上一页最后一条的 oid (首次请求传 0)
     * @param viewAt 游标: 上一页最后一条的 view_at (首次请求传 0)
     */
    suspend fun getHistoryList(
        ps: Int = 30,
        max: Long = 0,
        viewAt: Long = 0
    ): Result<HistoryResult> {
        return withContext(Dispatchers.IO) {
            try {
                com.android.purebilibili.core.util.Logger.d("HistoryRepo", "🔴 Fetching history: ps=$ps, max=$max, viewAt=$viewAt")
                val response = api.getHistoryList(ps = ps, max = max, viewAt = viewAt)
                com.android.purebilibili.core.util.Logger.d("HistoryRepo", "🔴 Response code=${response.code}, items=${response.data?.list?.size ?: 0}")
                
                if (response.code == 0) {
                    val list = response.data?.list ?: emptyList()
                    val cursor = response.data?.cursor
                    com.android.purebilibili.core.util.Logger.d("HistoryRepo", "🔴 Cursor: max=${cursor?.max}, view_at=${cursor?.view_at}")
                    Result.success(HistoryResult(list, cursor))
                } else {
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                android.util.Log.e("HistoryRepo", " Error: ${e.message}")
                Result.failure(e)
            }
        }
    }
}
