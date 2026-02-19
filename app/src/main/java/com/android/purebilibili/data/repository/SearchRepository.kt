package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.HotItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.SearchUpItem
import com.android.purebilibili.data.model.response.LiveRoomSearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SearchRepository {
    private val api = NetworkModule.searchApi
    private val navApi = NetworkModule.api

    //  [新增] 搜索分页信息
    data class SearchPageInfo(
        val currentPage: Int,
        val totalPages: Int,
        val totalResults: Int,
        val hasMore: Boolean
    )

    //  视频搜索 - 支持排序、时长过滤和分页
    suspend fun search(
        keyword: String,
        order: SearchOrder = SearchOrder.TOTALRANK,
        duration: SearchDuration = SearchDuration.ALL,
        tids: Int = 0,
        page: Int = 1
    ): Result<Pair<List<VideoItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "video",
                "order" to order.value,
                "duration" to duration.value.toString(),
                "tids" to tids.toString(),
                "page" to page.toString(),
                "pagesize" to "20"
            )
            
            com.android.purebilibili.core.util.Logger.d(
                "SearchRepo",
                " search(video): keyword=$keyword, order=${order.value}, duration=${duration.value}, tids=$tids, page=$page"
            )

            val signedParams = signWithWbi(params)

            val response = api.search(signedParams)
            if (response.code != 0) {
                com.android.purebilibili.core.util.Logger.w(
                    "SearchRepo",
                    "search(video) primary api failed: code=${response.code}, msg=${response.message}, fallback=all/v2"
                )
                return@withContext searchVideoFallback(keyword = keyword, page = page)
            }
            
            val videoList = response.data?.result
                ?.map { it.toVideoItem() }
                ?: emptyList()
            
            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: videoList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )
            
            com.android.purebilibili.core.util.Logger.d(
                "SearchRepo",
                " search(video) result: size=${videoList.size}, page=${pageInfo.currentPage}/${pageInfo.totalPages}, hasMore=${pageInfo.hasMore}"
            )

            Result.success(Pair(videoList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            com.android.purebilibili.core.util.Logger.e(
                "SearchRepo",
                "search(video) primary api exception, fallback=all/v2",
                e
            )
            searchVideoFallback(keyword = keyword, page = page)
        }
    }
    
    //  UP主 搜索
    suspend fun searchUp(
        keyword: String,
        page: Int = 1,
        order: SearchUpOrder = SearchUpOrder.DEFAULT,
        orderSort: SearchOrderSort = SearchOrderSort.DESC,
        userType: SearchUserType = SearchUserType.ALL
    ): Result<Pair<List<SearchUpItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "bili_user",
                "order" to order.value,
                "order_sort" to orderSort.value.toString(),
                "user_type" to userType.value.toString(),
                "page" to page.toString(),
                "pagesize" to "20"
            )

            val signedParams = signWithWbi(params)

            val response = api.searchUp(signedParams)
            if (response.code != 0) {
                return@withContext Result.failure(createSearchError(response.code, response.message))
            }
            
            val upList = response.data?.result
                ?.map { it.cleanupFields() }
                ?: emptyList()

            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: upList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )
            
            com.android.purebilibili.core.util.Logger.d(
                "SearchRepo",
                " search(up): size=${upList.size}, page=${pageInfo.currentPage}/${pageInfo.totalPages}, hasMore=${pageInfo.hasMore}"
            )

            Result.success(Pair(upList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            com.android.purebilibili.core.util.Logger.e("SearchRepo", "UP Search failed", e)
            Result.failure(e)
        }
    }

    // 默认搜索占位词
    suspend fun getDefaultSearchHint(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val signedParams = signWithWbi(emptyMap())
            val wbiResp = api.getDefaultSearch(signedParams)
            if (wbiResp.code == 0) {
                val hint = wbiResp.data?.showName?.trim().orEmpty()
                if (hint.isNotEmpty()) {
                    return@withContext Result.success(hint)
                }
            }

            val legacyResp = api.getDefaultSearchLegacy()
            if (legacyResp.code == 0) {
                val hint = legacyResp.data?.showName?.trim().orEmpty()
                if (hint.isNotEmpty()) {
                    return@withContext Result.success(hint)
                }
            }

            Result.failure(Exception("获取默认搜索词失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //  热搜
    suspend fun getHotSearch(): Result<List<HotItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHotSearch()
            val list = response.data?.trending?.list ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  [新增] 番剧搜索
    suspend fun searchBangumi(
        keyword: String,
        page: Int = 1
    ): Result<Pair<List<com.android.purebilibili.data.model.response.BangumiSearchItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "media_bangumi",
                "page" to page.toString()
            )
            
            val signedParams = signWithWbi(params)

            val response = api.searchBangumi(signedParams)
            if (response.code != 0) {
                return@withContext Result.failure(createSearchError(response.code, response.message))
            }
            
            val bangumiList = response.data?.result?.map { item ->
                // 清理 HTML 标签
                item.copy(
                    title = item.title.replace(Regex("<.*?>"), ""),
                    cover = if (item.cover.startsWith("//")) "https:${item.cover}" else item.cover
                )
            } ?: emptyList()
            
            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: bangumiList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )
            
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 Bangumi search result: ${bangumiList.size} items, page ${pageInfo.currentPage}/${pageInfo.totalPages}")

            Result.success(Pair(bangumiList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    //  [新增] 影视搜索
    suspend fun searchMediaFt(
        keyword: String,
        page: Int = 1
    ): Result<Pair<List<com.android.purebilibili.data.model.response.BangumiSearchItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "media_ft",
                "page" to page.toString()
            )

            val signedParams = signWithWbi(params)
            val response = api.searchMediaFt(signedParams)
            if (response.code != 0) {
                return@withContext Result.failure(createSearchError(response.code, response.message))
            }

            val resultList = response.data?.result?.map { item ->
                item.copy(
                    title = item.title.replace(Regex("<.*?>"), ""),
                    cover = if (item.cover.startsWith("//")) "https:${item.cover}" else item.cover
                )
            } ?: emptyList()

            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: resultList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )

            com.android.purebilibili.core.util.Logger.d(
                "SearchRepo",
                "🔍 MediaFT search result: ${resultList.size} items, page ${pageInfo.currentPage}/${pageInfo.totalPages}"
            )

            Result.success(Pair(resultList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  [新增] 直播搜索
    suspend fun searchLive(
        keyword: String,
        page: Int = 1,
        order: SearchLiveOrder = SearchLiveOrder.ONLINE
    ): Result<Pair<List<LiveRoomSearchItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "live_room",
                "order" to order.value,
                "page" to page.toString()
            )
            
            val signedParams = signWithWbi(params)

            val response = api.searchLive(signedParams)
            if (response.code != 0) {
                return@withContext Result.failure(createSearchError(response.code, response.message))
            }
            
            val liveList = response.data?.result?.map { it.cleanupFields() } ?: emptyList()
            
            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: liveList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )
            
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 Live search result: ${liveList.size} rooms, page ${pageInfo.currentPage}/${pageInfo.totalPages}")

            Result.success(Pair(liveList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  搜索建议/联想
    suspend fun getSuggest(keyword: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (keyword.isBlank()) return@withContext Result.success(emptyList())
            
            val response = api.getSearchSuggest(keyword)
            if (response.code != 0) {
                return@withContext Result.failure(createSearchError(response.code, "搜索建议加载失败"))
            }
            val suggestions = response.result?.tag?.map { it.value } ?: emptyList()
            Result.success(suggestions)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    //  获取搜索发现 (个性化 + 官方热搜兜底)
    suspend fun getSearchDiscover(historyKeywords: List<String>): Result<Pair<String, List<String>>> = withContext(Dispatchers.IO) {
        try {
            // 1. 个性化推荐：尝试使用最近的搜索词进行联想
            if (historyKeywords.isNotEmpty()) {
                val lastKeyword = historyKeywords.firstOrNull()
                if (!lastKeyword.isNullOrBlank()) {
                    val response = api.getSearchSuggest(lastKeyword)
                    val suggestions = response.result?.tag?.map { it.value }?.filter { it != lastKeyword }?.take(10)
                    
                    if (!suggestions.isNullOrEmpty()) {
                        return@withContext Result.success("大家都在搜 \"$lastKeyword\" 相关" to suggestions)
                    }
                }
            }
            
            // 2. 官方推荐：使用热搜词乱序 (模拟官方推荐流)
            val hotResponse = api.getHotSearch()
            val hotList = hotResponse.data?.trending?.list?.map { it.show_name }?.shuffled()?.take(10) ?: emptyList()
            
            if (hotList.isNotEmpty()) {
                return@withContext Result.success(" 热门推荐" to hotList)
            }
            
            // 3. 静态兜底
            Result.success("搜索发现" to listOf("黑神话悟空", "原神", "初音未来", "JOJO", "罗翔说刑法", "何同学", "毕业季", "猫咪", "我的世界", "战鹰"))
        } catch (e: Exception) {
            e.printStackTrace()
            // 发生异常时的最后兜底
            Result.success("搜索发现" to listOf("黑神话悟空", "原神", "初音未来", "JOJO", "罗翔说刑法", "何同学", "毕业季", "猫咪", "我的世界", "战鹰"))
        }
    }

    private suspend fun signWithWbi(params: Map<String, String>): Map<String, String> {
        return try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            if (imgKey.isNotEmpty() && subKey.isNotEmpty()) {
                WbiUtils.sign(params, imgKey, subKey)
            } else {
                com.android.purebilibili.core.util.Logger.w(
                    "SearchRepo",
                    "signWithWbi: missing img/sub key, use unsigned params"
                )
                params
            }
        } catch (e: Exception) {
            com.android.purebilibili.core.util.Logger.e(
                "SearchRepo",
                "signWithWbi: failed to load nav/wbi keys, use unsigned params",
                e
            )
            params
        }
    }

    private suspend fun searchVideoFallback(
        keyword: String,
        page: Int
    ): Result<Pair<List<VideoItem>, SearchPageInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchAll(
                    mapOf(
                        "keyword" to keyword,
                        "page" to page.toString()
                    )
                )

                val videos = response.data?.result
                    ?.firstOrNull { it.result_type == "video" }
                    ?.data
                    ?.map { it.toVideoItem() }
                    ?: emptyList()

                val pageInfo = SearchPageInfo(
                    currentPage = page,
                    totalPages = if (videos.size >= 20) page + 1 else page,
                    totalResults = videos.size,
                    hasMore = videos.size >= 20
                )

                com.android.purebilibili.core.util.Logger.d(
                    "SearchRepo",
                    "search(video) fallback result: size=${videos.size}, page=${pageInfo.currentPage}, hasMore=${pageInfo.hasMore}"
                )

                Result.success(Pair(videos, pageInfo))
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun createSearchError(code: Int, message: String): Exception {
        val readable = when (code) {
            -412 -> "搜索请求被拦截，请稍后重试"
            -400 -> "搜索参数错误"
            -404 -> "搜索接口不存在"
            -1200 -> "搜索类型不存在或参数被降级过滤"
            else -> message.ifBlank { "搜索失败 ($code)" }
        }
        return Exception(readable)
    }
}

//  搜索排序选项
enum class SearchOrder(val value: String, val displayName: String) {
    TOTALRANK("totalrank", "综合排序"),
    PUBDATE("pubdate", "最新发布"),
    CLICK("click", "播放最多"),
    DM("dm", "弹幕最多"),
    STOW("stow", "收藏最多")
}

//  搜索时长筛选
enum class SearchDuration(val value: Int, val displayName: String) {
    ALL(0, "全部时长"),
    UNDER_10MIN(1, "10分钟以下"),
    TEN_TO_30MIN(2, "10-30分钟"),
    THIRTY_TO_60MIN(3, "30-60分钟"),
    OVER_60MIN(4, "60分钟以上")
}

enum class SearchUpOrder(val value: String, val displayName: String) {
    DEFAULT("0", "默认排序"),
    FANS("fans", "粉丝数"),
    LEVEL("level", "用户等级")
}

enum class SearchOrderSort(val value: Int, val displayName: String) {
    DESC(0, "从高到低"),
    ASC(1, "从低到高")
}

enum class SearchUserType(val value: Int, val displayName: String) {
    ALL(0, "全部用户"),
    UP(1, "UP主"),
    NORMAL(2, "普通用户"),
    VERIFIED(3, "认证用户")
}

enum class SearchLiveOrder(val value: String, val displayName: String) {
    ONLINE("online", "人气直播"),
    LIVE_TIME("live_time", "最新开播")
}
