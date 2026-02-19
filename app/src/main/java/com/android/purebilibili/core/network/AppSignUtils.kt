// 文件路径: core/network/AppSignUtils.kt
package com.android.purebilibili.core.network

import java.security.MessageDigest

/**
 *  APP 签名工具类
 * 用于 TV 端登录和 APP API 调用的签名计算
 * 
 * 参考: https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/misc/sign/APPKey.md
 */
object AppSignUtils {
    
    //  TV 端 appkey 和 appsec (云视听小电视)
    // 注意：通过某一组 APPKEY/APPSEC 获取到的 access_token，之后的 API 调用也必须使用同一组
    const val TV_APP_KEY = "4409e2ce8ffd12b8"
    private const val TV_APP_SEC = "59b43e04ad6965f34319062b478f83dd"
    
    //  Android 客户端 appkey 和 appsec (用于获取高画质视频)
    const val ANDROID_APP_KEY = "1d8b6e7d45233436"
    private const val ANDROID_APP_SEC = "560c52ccd288fed045859ed18bffd973"
    
    /**
     * 计算 APP 签名
     * 签名规则：将参数按 key 排序后拼接成 query string，末尾加上 appsec，然后 MD5
     * 
     * @param params 请求参数 (不含 sign)
     * @param appSec 使用的 appsec
     * @return 签名后的完整参数 Map (含 sign)
     */
    fun sign(params: Map<String, String>, appSec: String = TV_APP_SEC): Map<String, String> {
        val sortedParams = params.toSortedMap()
        
        // 构建 query string
        val queryString = sortedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        
        // 计算 MD5
        val signStr = queryString + appSec
        val sign = md5(signStr)
        
        // 返回包含 sign 的完整参数
        return sortedParams + ("sign" to sign)
    }
    
    /**
     * 为 TV 端登录生成签名
     */
    fun signForTvLogin(params: Map<String, String>): Map<String, String> {
        return sign(params, TV_APP_SEC)
    }
    
    /**
     * 为 Android APP API 生成签名 (用于 playurl 等)
     */
    fun signForAndroidApi(params: Map<String, String>): Map<String, String> {
        return sign(params, ANDROID_APP_SEC)
    }
    
    /**
     * 获取当前时间戳 (秒)
     */
    fun getTimestamp(): Long = System.currentTimeMillis() / 1000
    
    /**
     * MD5 计算
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
