package com.android.purebilibili.feature.web

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ChevronBackward

/**
 * WebViewScreen - 应用内浏览器
 * 
 * 支持拦截 Bilibili 链接并跳转到原生界面：
 * - 视频: bilibili.com/video/BV... 或 av...
 * - UP主空间: space.bilibili.com/{mid}
 * - 直播: live.bilibili.com/{roomId}
 * - 番剧: bilibili.com/bangumi/play/ss{id} 或 ep{id}
 * - 音乐: music.bilibili.com/h5/music-detail?music_id=...
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String? = null,
    onBack: () -> Unit,
    // [新增] 链接拦截回调
    onVideoClick: ((bvid: String) -> Unit)? = null,
    onSpaceClick: ((mid: Long) -> Unit)? = null,
    onLiveClick: ((roomId: Long) -> Unit)? = null,
    onBangumiClick: ((seasonId: Long, epId: Long) -> Unit)? = null,
    onMusicClick: ((musicId: String) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = title ?: "浏览器",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Outlined.ChevronBackward, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        
                        // [核心] 自定义 WebViewClient 拦截 Bilibili 链接
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val requestUrl = request?.url?.toString() ?: return false
                                return handleBilibiliUrl(view, requestUrl)
                            }
                            
                            // 兼容旧版 API
                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return url?.let { handleBilibiliUrl(view, it) } ?: false
                            }
                            
                            /**
                             * 处理 Bilibili URL 拦截
                             * @param webView WebView 实例，用于加载转换后的 URL
                             * @return true 表示已拦截处理，false 表示继续加载网页
                             */
                            private fun handleBilibiliUrl(webView: WebView?, urlString: String): Boolean {
                                android.util.Log.d("WebViewScreen", "🔗 Intercepting URL: $urlString")
                                try {
                                    val uri = Uri.parse(urlString)
                                    val scheme = uri.scheme ?: ""
                                    val host = uri.host ?: ""
                                    val path = uri.path ?: ""
                                    
                                    android.util.Log.d("WebViewScreen", "🔍 Scheme: $scheme, Host: $host, Path: $path")
                                    
                                    // ===== 0. 处理 bilibili:// Deep Link =====
                                    // 将自定义协议转换为 HTTPS URL 并在 WebView 中加载
                                    if (scheme == "bilibili") {
                                        val convertedUrl = convertDeepLinkToWebUrl(uri)
                                        if (convertedUrl != null) {
                                            android.util.Log.d("WebViewScreen", "🔄 Deep link -> $convertedUrl")
                                            // 在 WebView 中加载转换后的 URL
                                            webView?.loadUrl(convertedUrl)
                                            return true // 拦截原始 deep link
                                        }
                                        android.util.Log.w("WebViewScreen", "⚠️ Unknown deep link: $urlString")
                                        return true // 拦截，防止 ERR_UNKNOWN_URL_SCHEME
                                    }
                                    
                                    // 1. 视频链接: bilibili.com/video/BV... 或 av...
                                    // 支持: www.bilibili.com, m.bilibili.com, bilibili.com, b23.tv
                                    if (host.endsWith("bilibili.com") || host.contains("b23.tv")) {
                                        // BV 格式
                                        val bvMatch = Regex("(?:^|/)(BV[a-zA-Z0-9]{10})").find(path)
                                            ?: Regex("(?:^|/)(BV[a-zA-Z0-9]{10})").find(urlString) // fallback to full URL
                                        if (bvMatch != null) {
                                            val bvid = bvMatch.groupValues[1]
                                            android.util.Log.d("WebViewScreen", "✅ Found BV: $bvid")
                                            onVideoClick?.invoke(bvid)
                                            return true
                                        }
                                        
                                        // AV 格式
                                        val avMatch = Regex("/video/av(\\d+)").find(path)
                                            ?: Regex("av(\\d+)").find(urlString)
                                        if (avMatch != null) {
                                            val aid = avMatch.groupValues[1].toLongOrNull() ?: return false
                                            // [重要] 标准 B 站 AV 号通常小于 10 亿
                                            // 超大 AV 号可能是音乐页面的内部 ID，不应转换
                                            if (aid > 10_000_000_000L) {
                                                android.util.Log.w("WebViewScreen", "⚠️ AV ID too large, skipping: $aid")
                                                return false // 不拦截，让 WebView 继续加载
                                            }
                                            val bvid = avToBv(aid)
                                            android.util.Log.d("WebViewScreen", "✅ Found AV: $aid -> BV: $bvid")
                                            onVideoClick?.invoke(bvid)
                                            return true
                                        }
                                    }
                                    
                                    // 2. UP主空间: space.bilibili.com/{mid}
                                    if (host == "space.bilibili.com") {
                                        val midMatch = Regex("^/(\\d+)").find(path)
                                        if (midMatch != null) {
                                            val mid = midMatch.groupValues[1].toLongOrNull() ?: return false
                                            onSpaceClick?.invoke(mid)
                                            return true
                                        }
                                    }
                                    
                                    // 3. 直播: live.bilibili.com/{roomId}
                                    if (host == "live.bilibili.com") {
                                        val roomMatch = Regex("^/(\\d+)").find(path)
                                        if (roomMatch != null) {
                                            val roomId = roomMatch.groupValues[1].toLongOrNull() ?: return false
                                            onLiveClick?.invoke(roomId)
                                            return true
                                        }
                                    }
                                    
                                    // 4. 番剧: bilibili.com/bangumi/play/ss{id} 或 ep{id}
                                    if (host.contains("bilibili.com") && path.contains("/bangumi/play/")) {
                                        val ssMatch = Regex("/bangumi/play/ss(\\d+)").find(path)
                                        if (ssMatch != null) {
                                            val seasonId = ssMatch.groupValues[1].toLongOrNull() ?: return false
                                            onBangumiClick?.invoke(seasonId, 0)
                                            return true
                                        }
                                        val epMatch = Regex("/bangumi/play/ep(\\d+)").find(path)
                                        if (epMatch != null) {
                                            val epId = epMatch.groupValues[1].toLongOrNull() ?: return false
                                            onBangumiClick?.invoke(0, epId)
                                            return true
                                        }
                                    }
                                    
                                    // 5. 音乐详情: music.bilibili.com/h5/music-detail?music_id=...
                                    if (host == "music.bilibili.com" && path.contains("/music-detail")) {
                                        val musicId = uri.getQueryParameter("music_id")
                                        if (musicId != null) {
                                            onMusicClick?.invoke(musicId)
                                            return true
                                        }
                                    }
                                    
                                } catch (e: Exception) {
                                    android.util.Log.e("WebViewScreen", "URL parsing error: ${e.message}")
                                }
                                
                                return false // 不拦截，继续加载
                            }
                        }
                        
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    // Avoid reloading on recomposition if URL hasn't changed
                    if (webView.url != url) {
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * AV 号转 BV 号算法
 * 参考: https://www.zhihu.com/question/381784377
 * BV 格式: BV1__4_1_7__ (12 字符)
 * 固定位置: [0]='B', [1]='V', [2]='1', [5]='4', [7]='1', [9]='7'
 * 编码位置: s = [11, 10, 3, 8, 4, 6]
 */
private fun avToBv(aid: Long): String {
    val table = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
    val xorVal = 177451812L
    val addVal = 8728348608L
    val s = intArrayOf(11, 10, 3, 8, 4, 6)
    
    val av = (aid xor xorVal) + addVal
    // 初始化 BV 模板：BV1xx4x1x7xx
    val bv = charArrayOf('B', 'V', '1', ' ', ' ', '4', ' ', '1', ' ', '7', ' ', ' ')
    
    for (i in s.indices) {
        val index = ((av / Math.pow(58.0, i.toDouble()).toLong()) % 58).toInt()
        bv[s[i]] = table[index]
    }
    
    return String(bv)
}

/**
 * 将 bilibili:// 深链接转换为 HTTPS 网页 URL
 * 
 * 支持的格式:
 * - bilibili://video/{id} -> https://m.bilibili.com/video/av{id}
 * - bilibili://space/{mid} -> https://space.bilibili.com/{mid}
 * - bilibili://live/{roomId} -> https://live.bilibili.com/{roomId}
 * - bilibili://bangumi/season/{ssid} -> https://m.bilibili.com/bangumi/play/ss{ssid}
 */
private fun convertDeepLinkToWebUrl(uri: android.net.Uri): String? {
    val host = uri.host ?: uri.pathSegments?.getOrNull(0) ?: return null
    val pathSegments = uri.pathSegments ?: return null
    
    android.util.Log.d("WebViewScreen", "🔗 Converting deep link: host=$host, segments=$pathSegments")
    
    return when {
        // bilibili://video/123456 -> https://m.bilibili.com/video/av123456
        host == "video" || (pathSegments.isNotEmpty() && pathSegments[0] == "video") -> {
            val videoId = if (host == "video") {
                pathSegments.getOrNull(0) ?: uri.path?.removePrefix("/")
            } else {
                pathSegments.getOrNull(1)
            }
            if (videoId != null) {
                // 检查是否已经是 BV 格式
                if (videoId.startsWith("BV")) {
                    "https://m.bilibili.com/video/$videoId"
                } else {
                    // [重要] 检查 AV ID 是否有效
                    // 超大 ID (> 10B) 是音乐页面的内部 ID，不是真实视频
                    // 返回 null 阻止转换，防止无限循环
                    val numericId = videoId.toLongOrNull()
                    if (numericId != null && numericId > 10_000_000_000L) {
                        android.util.Log.w("WebViewScreen", "⚠️ Blocking invalid video ID: $numericId")
                        null // 不转换，直接阻止
                    } else {
                        "https://m.bilibili.com/video/av$videoId"
                    }
                }
            } else null
        }
        // bilibili://space/123456
        host == "space" -> {
            val mid = pathSegments.getOrNull(0)
            if (mid != null) "https://space.bilibili.com/$mid" else null
        }
        // bilibili://live/123456
        host == "live" -> {
            val roomId = pathSegments.getOrNull(0)
            if (roomId != null) "https://live.bilibili.com/$roomId" else null
        }
        // bilibili://bangumi/season/123456
        host == "bangumi" -> {
            if (pathSegments.getOrNull(0) == "season") {
                val ssid = pathSegments.getOrNull(1)
                if (ssid != null) "https://m.bilibili.com/bangumi/play/ss$ssid" else null
            } else null
        }
        else -> null
    }
}

