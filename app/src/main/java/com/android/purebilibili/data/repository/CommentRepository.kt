// 文件路径: data/repository/CommentRepository.kt
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.TreeMap

/**
 * 评论相关数据仓库
 * 从 VideoRepository 拆分出来，专注于评论功能
 */
object CommentRepository {
    private val api = NetworkModule.api
    private val commentJson = Json { ignoreUnknownKeys = true }

    // WBI Key 缓存
    private var wbiKeysCache: Pair<String, String>? = null
    private var wbiKeysTimestamp: Long = 0
    private const val WBI_CACHE_DURATION = 1000 * 60 * 30 // 30分钟缓存

    @Serializable
    private data class CommentPicturePayload(
        @SerialName("img_src") val imgSrc: String,
        @SerialName("img_width") val imgWidth: Int,
        @SerialName("img_height") val imgHeight: Int,
        @SerialName("img_size") val imgSize: Float
    )

    /**
     * 获取 WBI Keys（用于 WBI 签名）
     */
    private suspend fun getWbiKeys(): Pair<String, String> {
        val currentCheck = System.currentTimeMillis()
        val cached = wbiKeysCache
        if (cached != null && (currentCheck - wbiKeysTimestamp < WBI_CACHE_DURATION)) {
            return cached
        }

        val maxRetries = 3
        var lastError: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                val navResp = api.getNavInfo()
                val wbiImg = navResp.data?.wbi_img
                
                if (wbiImg != null) {
                    val imgKey = wbiImg.img_url.substringAfterLast("/").substringBefore(".")
                    val subKey = wbiImg.sub_url.substringAfterLast("/").substringBefore(".")
                    
                    wbiKeysCache = Pair(imgKey, subKey)
                    wbiKeysTimestamp = System.currentTimeMillis()
                    com.android.purebilibili.core.util.Logger.d("CommentRepo", " WBI Keys obtained successfully (attempt $attempt)")
                    return wbiKeysCache!!
                }
            } catch (e: Exception) {
                lastError = e
                android.util.Log.w("CommentRepo", "getWbiKeys attempt $attempt failed: ${e.message}")
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(200L * attempt) // 递增延迟
                }
            }
        }
        
        throw Exception("Wbi Keys Error after $maxRetries attempts: ${lastError?.message}")
    }

    /**
     * 获取评论列表
     * @param mode 排序模式:
     * 3=最热(WBI mode=3), 2=最新(legacy sort=0), 4=点赞(legacy sort=1), 1=回复(legacy sort=2)
     */
    suspend fun getComments(aid: Long, page: Int, ps: Int = 20, mode: Int = 3): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            // 确保 buvid3 已初始化
            VideoRepository.ensureBuvid3()
            
            val response = when (mode) {
                2 -> {
                    // 时间排序使用旧版 API
                    com.android.purebilibili.core.util.Logger.d("CommentRepo", " getComments (Legacy): aid=$aid, page=$page, sort=0 (时间)")
                    api.getReplyListLegacy(
                        oid = aid,
                        type = 1,
                        pn = page,
                        ps = ps,
                        sort = 0  // 旧版 API: 0=按时间
                    )
                }
                1 -> {
                    // 回复数排序使用旧版 API
                    com.android.purebilibili.core.util.Logger.d("CommentRepo", " getComments (Legacy): aid=$aid, page=$page, sort=2 (回复数)")
                    api.getReplyListLegacy(
                        oid = aid,
                        type = 1,
                        pn = page,
                        ps = ps,
                        sort = 2  // 旧版 API: 2=按回复数
                    )
                }
                4 -> {
                    // 点赞数排序使用旧版 API
                    com.android.purebilibili.core.util.Logger.d("CommentRepo", " getComments (Legacy): aid=$aid, page=$page, sort=1 (点赞数)")
                    api.getReplyListLegacy(
                        oid = aid,
                        type = 1,
                        pn = page,
                        ps = ps,
                        sort = 1  // 旧版 API: 1=按点赞数
                    )
                }
                else -> {
                    // 热度排序使用 WBI API (默认)
                    val (imgKey, subKey) = getWbiKeys()
                    com.android.purebilibili.core.util.Logger.d("CommentRepo", " getComments (WBI): aid=$aid, page=$page, mode=3 (热度)")
                    
                    val params = TreeMap<String, String>()
                    params["oid"] = aid.toString()
                    params["type"] = "1"
                    params["mode"] = "3"  // WBI API: 3=热度
                    params["next"] = page.toString()
                    params["ps"] = ps.toString()

                    val signedParams = WbiUtils.sign(params, imgKey, subKey)
                    api.getReplyList(signedParams)
                }
            }
            
            val sortLabel = when (mode) {
                2 -> "时间"
                4 -> "点赞数"
                1 -> "回复数"
                else -> "热度"
            }
            com.android.purebilibili.core.util.Logger.d("CommentRepo", " getComments result: mode=$mode($sortLabel), replies=${response.data?.replies?.size ?: 0}, code=${response.code}")

            if (response.code == 0) {
                Result.success(response.data ?: ReplyData())
            } else {
                val errorMsg = when (response.code) {
                    -352 -> "请求频率过高，请稍后再试"
                    -111 -> "签名验证失败"
                    -101 -> "需要登录后才能查看评论"
                    -400 -> "请求参数错误"
                    -412 -> "请求被拦截，请稍后再试"
                    12002 -> "评论区已关闭"
                    12009 -> "评论内容不存在"
                    else -> "加载评论失败 (${response.code})"
                }
                android.util.Log.e("CommentRepo", " getComments failed: ${response.code} - ${response.message}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepo", " getComments exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取二级评论（楼中楼）
     */
    suspend fun getSubComments(aid: Long, rootId: Long, page: Int, ps: Int = 20): Result<ReplyData> = withContext(Dispatchers.IO) {
        try {
            // 确保 buvid3 已初始化
            VideoRepository.ensureBuvid3()
            
            com.android.purebilibili.core.util.Logger.d("CommentRepo", " getSubComments: aid=$aid, rootId=$rootId, page=$page")
            
            val response = api.getReplyReply(
                oid = aid,
                root = rootId,
                pn = page,
                ps = ps
            )
            
            com.android.purebilibili.core.util.Logger.d("CommentRepo", " getSubComments response: code=${response.code}, replies=${response.data?.replies?.size ?: 0}")
            
            if (response.code == 0) {
                Result.success(response.data ?: ReplyData())
            } else {
                android.util.Log.e("CommentRepo", " getSubComments failed: ${response.code} - ${response.message}")
                Result.failure(Exception("加载回复失败 (${response.code})"))
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepo", " getSubComments exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取表情包映射
     */
    suspend fun getEmoteMap(): Map<String, String> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<String, String>()
        // 默认表情
        map["[doge]"] = "http://i0.hdslb.com/bfs/emote/6f8743c3c13009f4705307b2750e32f5068225e3.png"
        map["[笑哭]"] = "http://i0.hdslb.com/bfs/emote/500b63b2f293309a909403a746566fdd6104d498.png"
        map["[妙啊]"] = "http://i0.hdslb.com/bfs/emote/03c39c8eb009f63568971032b49c716259c72441.png"
        try {
            val params = mutableMapOf("business" to "reply")
            
            val response = api.getEmotes(params)
            val packages = response.data?.packages ?: response.data?.all_packages
            packages?.forEach { pkg ->
                pkg.emote?.forEach { emote -> map[emote.text] = emote.url }
            }
        } catch (e: Exception) { e.printStackTrace() }
        map
    }

    /**
     * 获取表情包列表 (用于UI展示)
     */
    suspend fun getEmotePackages(): Result<List<EmotePackage>> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf("business" to "reply")
            
            val response = api.getEmotes(params)
            if (response.code == 0) {
                val data = response.data
                val pkgs = data?.packages ?: data?.all_packages ?: emptyList()
                Result.success(pkgs)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * [新增] 发送评论
     * @param aid 视频 aid
     * @param message 评论内容
     * @param root 根评论 rpid（回复时需要）
     * @param parent 父评论 rpid
     * @return 新评论的 rpid
     */
    suspend fun addComment(
        aid: Long,
        message: String,
        root: Long = 0,
        parent: Long = 0,
        pictures: List<ReplyPicture> = emptyList()
    ): Result<ReplyItem?> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            val picturePayload = buildPicturesPayload(pictures)
            
            val response = api.addReply(
                oid = aid,
                type = 1,
                message = message,
                root = root.takeIf { it > 0L },
                parent = parent.takeIf { it > 0L },
                pictures = picturePayload,
                csrf = csrf
            )
            
            if (response.code == 0) {
                Result.success(response.data?.reply)
            } else {
                Logger.e(
                    "CommentRepo",
                    "addComment failed: aid=$aid, root=$root, parent=$parent, pictureCount=${pictures.size}, code=${response.code}, message=${response.message}"
                )
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -102 -> "账号被封禁"
                    -509 -> "请求过于频繁"
                    12002 -> "评论区已关闭"
                    12015 -> "需要评论验证码"
                    12016 -> "评论内容包含敏感信息"
                    12025 -> "评论字数过多"
                    12035 -> "您已被UP主拉黑"
                    12051 -> "重复评论，请勿刷屏"
                    else -> response.message.ifEmpty { "发送失败 (${response.code})" }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Logger.e("CommentRepo", "addComment exception: aid=$aid, root=$root, parent=$parent", e)
            Result.failure(e)
        }
    }

    /**
     * 上传评论图片，返回可用于评论 pictures 字段的元数据
     */
    suspend fun uploadCommentImage(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): Result<ReplyPicture> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }

            val mediaType = mimeType.toMediaType()
            val fileBody = bytes.toRequestBody(mediaType)
            val part = okhttp3.MultipartBody.Part.createFormData(
                "file_up",
                fileName.ifBlank { "comment_image.jpg" },
                fileBody
            )
            val textMedia = "text/plain".toMediaType()
            val categoryBody = "daily".toRequestBody(textMedia)
            val bizBody = "new_dyn".toRequestBody(textMedia)
            val csrfBody = csrf.toRequestBody(textMedia)

            val response = api.uploadCommentImage(
                fileUp = part,
                category = categoryBody,
                biz = bizBody,
                csrf = csrfBody
            )

            if (response.code == 0 && response.data != null) {
                val data = response.data
                Result.success(
                    ReplyPicture(
                        imgSrc = data.imageUrl,
                        imgWidth = data.imageWidth,
                        imgHeight = data.imageHeight,
                        imgSize = data.imgSize
                    )
                )
            } else {
                Logger.e(
                    "CommentRepo",
                    "uploadCommentImage failed: fileName=$fileName, mimeType=$mimeType, size=${bytes.size}, code=${response.code}, message=${response.message}"
                )
                Result.failure(Exception(response.message.ifEmpty { "图片上传失败 (${response.code})" }))
            }
        } catch (e: Exception) {
            Logger.e(
                "CommentRepo",
                "uploadCommentImage exception: fileName=$fileName, mimeType=$mimeType, size=${bytes.size}",
                e
            )
            Result.failure(e)
        }
    }

    internal fun buildPicturesPayload(pictures: List<ReplyPicture>): String? {
        if (pictures.isEmpty()) return null
        val payload = pictures.map { picture ->
            CommentPicturePayload(
                imgSrc = picture.imgSrc,
                imgWidth = picture.imgWidth,
                imgHeight = picture.imgHeight,
                imgSize = picture.imgSize
            )
        }
        return commentJson.encodeToString(payload)
    }
    
    /**
     * [新增] 点赞评论
     */
    suspend fun likeComment(aid: Long, rpid: Long, like: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            val response = api.likeReply(
                oid = aid,
                type = 1,
                rpid = rpid,
                action = if (like) 1 else 0,
                csrf = csrf
            )
            
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "操作失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * [新增] 点踩评论
     */
    suspend fun hateComment(aid: Long, rpid: Long, hate: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            val response = api.hateReply(
                oid = aid,
                type = 1,
                rpid = rpid,
                action = if (hate) 1 else 0,
                csrf = csrf
            )
            
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "操作失败" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * [新增] 删除评论
     */
    suspend fun deleteComment(aid: Long, rpid: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            val response = api.deleteReply(
                oid = aid,
                type = 1,
                rpid = rpid,
                csrf = csrf
            )
            
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code) {
                    -403 -> "无权删除此评论"
                    12022 -> "评论已被删除"
                    else -> response.message.ifEmpty { "删除失败" }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * [新增] 举报评论
     * @param reason 举报原因: 0=其他, 1=垃圾广告, 2=色情, 3=刷屏, 4=引战, 5=剧透, 6=政治, 7=人身攻击
     */
    suspend fun reportComment(aid: Long, rpid: Long, reason: Int, content: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = com.android.purebilibili.core.store.TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            val response = api.reportReply(
                oid = aid,
                type = 1,
                rpid = rpid,
                reason = reason,
                content = content,
                csrf = csrf
            )
            
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                val errorMsg = when (response.code) {
                    12008 -> "已经举报过了"
                    12019 -> "举报过于频繁"
                    else -> response.message.ifEmpty { "举报失败" }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
