// 私信数据仓库
package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 私信相关数据仓库
 * 负责处理会话列表、私信消息的获取和发送
 */
object MessageRepository {
    private val api = NetworkModule.messageApi
    
    // 设备ID缓存 (用于发送私信)
    private var deviceIdCache: String? = null
    
    /**
     * 获取或生成设备ID (UUID v4格式)
     */
    private fun getDeviceId(): String {
        return deviceIdCache ?: UUID.randomUUID().toString().also {
            deviceIdCache = it
        }
    }
    
    /**
     * 获取未读私信数量
     */
    suspend fun getUnreadCount(): Result<MessageUnreadData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUnreadCount()
            
            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    else -> response.message.ifEmpty { "获取未读数失败 (${response.code})" }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "getUnreadCount exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取会话列表
     * @param sessionType 会话类型: 1=用户与系统, 2=未关注人, 3=粉丝团, 4=所有, 9=关注的人与系统
     * @param size 返回数量
     */
    suspend fun getSessions(
        sessionType: Int = 4,
        size: Int = 50, //  keep size 50
        page: Int = 1,
        endTs: Long = 0
    ): Result<SessionListData> = withContext(Dispatchers.IO) {
        try {
            com.android.purebilibili.core.util.Logger.d("MessageRepo", "getSessions: type=$sessionType, size=$size, page=$page")
            
            val response = api.getSessions(
                sessionType = sessionType,
                size = size,
                pn = page, // Revert to using dynamic page
                endTs = endTs,
                unfollowFold = 0
            )
            
            com.android.purebilibili.core.util.Logger.d("MessageRepo", "getSessions response: code=${response.code}, sessions=${response.data?.session_list?.size ?: 0}")
            
            // [Debug] 打印第一个会话的详细信息以调试字段结构
            response.data?.session_list?.firstOrNull()?.let { first ->
                android.util.Log.d("MessageRepo", "First session debug: talker_id=${first.talker_id}, " +
                    "account_info=${first.account_info}, " +
                    "name=${first.account_info?.name}, " +
                    "pic_url=${first.account_info?.pic_url}, " +
                    "face=${first.account_info?.face}, " +
                    "avatarUrl=${first.account_info?.avatarUrl}")
            }
            
            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -400 -> "请求参数错误"
                    else -> response.message.ifEmpty { "获取会话列表失败 (${response.code})" }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "getSessions exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取私信消息记录
     * @param talkerId 聊天对象ID
     * @param sessionType 会话类型: 1=用户, 2=粉丝团
     * @param size 返回消息数量
     * @param endSeqno 结束序列号 (用于分页加载更早的消息)
     */
    suspend fun getMessages(
        talkerId: Long,
        sessionType: Int = 1,
        size: Int = 20,
        endSeqno: Long = 0
    ): Result<MessageHistoryData> = withContext(Dispatchers.IO) {
        try {
            com.android.purebilibili.core.util.Logger.d("MessageRepo", "getMessages: talkerId=$talkerId, type=$sessionType, size=$size")
            
            val response = api.fetchSessionMsgs(
                talkerId = talkerId,
                sessionType = sessionType,
                size = size,
                endSeqno = endSeqno
            )
            
            com.android.purebilibili.core.util.Logger.d("MessageRepo", "getMessages response: code=${response.code}, messages=${response.data?.messages?.size ?: 0}")
            
            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -400 -> "请求参数错误"
                    else -> response.message.ifEmpty { "获取消息记录失败 (${response.code})" }
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "getMessages exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 发送文字私信
     * @param receiverId 接收者ID
     * @param content 消息内容
     * @param receiverType 接收者类型: 1=用户, 2=粉丝团
     */
    suspend fun sendTextMessage(
        receiverId: Long,
        content: String,
        receiverType: Int = 1
    ): Result<SendMessageData> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            // 获取当前用户 mid
            val senderUid = TokenManager.midCache
            if (senderUid == null || senderUid <= 0) {
                return@withContext Result.failure(Exception("无法获取用户信息，请重新登录"))
            }
            
            // 构造消息内容 JSON
            val contentJson = """{"content":"${content.replace("\"", "\\\"").replace("\n", "\\n")}"}"""
            
            com.android.purebilibili.core.util.Logger.d("MessageRepo", "sendTextMessage: to=$receiverId, content=$content")
            
            val response = api.sendMsg(
                senderUid = senderUid,
                receiverId = receiverId,
                receiverType = receiverType,
                msgType = 1,  // 1=文字消息
                content = contentJson,
                timestamp = System.currentTimeMillis() / 1000,
                devId = getDeviceId(),
                csrf = csrf,
                csrfToken = csrf
            )
            
            if (response.code == 0 && response.data != null) {
                com.android.purebilibili.core.util.Logger.d("MessageRepo", "sendTextMessage success: msgKey=${response.data.msg_key}")
                Result.success(response.data)
            } else {
                val errorMsg = when (response.code) {
                    -101 -> "请先登录"
                    -400 -> "请求参数错误"
                    21007 -> "消息过长，无法发送"
                    21015 -> "需绑定手机号才能发送消息"
                    21020 -> "发送频率过快，请稍后再试"
                    21026 -> "不能给自己发消息"
                    21046 -> "发送过于频繁，请24小时后再试"
                    21047 -> "对方未关注你，最多发送1条消息"
                    25003 -> "对方隐私设置限制，无法发送"
                    25005 -> "已拉黑对方，请先解除拉黑"
                    else -> response.message.ifEmpty { "发送失败 (${response.code})" }
                }
                android.util.Log.e("MessageRepo", "sendTextMessage failed: ${response.code} - ${response.message}")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "sendTextMessage exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 标记会话为已读
     * @param talkerId 聊天对象ID
     * @param sessionType 会话类型
     * @param ackSeqno 已读消息的序列号
     */
    suspend fun markAsRead(
        talkerId: Long,
        sessionType: Int,
        ackSeqno: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            val response = api.updateAck(
                talkerId = talkerId,
                sessionType = sessionType,
                ackSeqno = ackSeqno,
                csrf = csrf,
                csrfToken = csrf
            )
            
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "标记已读失败" }))
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "markAsRead exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 置顶/取消置顶会话
     * @param talkerId 聊天对象ID
     * @param sessionType 会话类型
     * @param setTop true=置顶, false=取消置顶
     */
    suspend fun setSessionTop(
        talkerId: Long,
        sessionType: Int,
        setTop: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            val response = api.setTop(
                talkerId = talkerId,
                sessionType = sessionType,
                opType = if (setTop) 0 else 1,  // 0=置顶, 1=取消置顶
                csrf = csrf,
                csrfToken = csrf
            )
            
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "操作失败" }))
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "setSessionTop exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 移除会话
     * @param talkerId 聊天对象ID
     * @param sessionType 会话类型
     */
    suspend fun removeSession(
        talkerId: Long,
        sessionType: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val csrf = TokenManager.csrfCache
            if (csrf.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("请先登录"))
            }
            
            val response = api.removeSession(
                talkerId = talkerId,
                sessionType = sessionType,
                csrf = csrf,
                csrfToken = csrf
            )
            
            if (response.code == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message.ifEmpty { "移除会话失败" }))
            }
        } catch (e: Exception) {
            android.util.Log.e("MessageRepo", "removeSession exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
