package com.android.purebilibili.feature.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.feature.home.UserState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.net.SocketTimeoutException

import android.net.Uri
import android.content.Context
import com.android.purebilibili.core.store.SettingsManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: UserState) : ProfileUiState()
    // LoggedOut 代表“当前是游客/未登录状态”，UI 应该显示“去登录”
    // [Modified] Support wallpaper in guest mode
    data class LoggedOut(val topPhoto: String = "") : ProfileUiState()
    // 🔧 [新增] 网络错误状态 — 保持登录但显示离线提示
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            var customBgUri = ""
            try {
                // 0. [New] 始终并行读取本地自定义背景设置 (即使未登录也需要背景)
                // 使用 first() 获取当前值
                customBgUri = try {
                    SettingsManager.getProfileBgUri(getApplication()).first() ?: ""
                } catch (e: Exception) { "" }

                // 1. 检查本地是否有 Token
                if (TokenManager.sessDataCache.isNullOrEmpty()) {
                    // [Fix] Add timestamp for LoggedOut state too
                    val finalUri = if (customBgUri.startsWith("file://")) {
                        try {
                            val file = File(Uri.parse(customBgUri).path ?: "")
                            if (file.exists()) "$customBgUri?t=${file.lastModified()}" else customBgUri
                        } catch (e: Exception) { customBgUri }
                    } else customBgUri
                    
                    _uiState.value = ProfileUiState.LoggedOut(topPhoto = finalUri)
                    return@launch
                }

                _uiState.value = ProfileUiState.Loading

                // 2. 使用 supervisorScope 进行并行请求，确保一个失败不会取消整个 scope
                // 这样可以在 await() 时捕获异常
                val (navResult, statResult) = kotlinx.coroutines.supervisorScope {
                    val navDeferred = async { runCatching { NetworkModule.api.getNavInfo() } }
                    val statDeferred = async { runCatching { NetworkModule.api.getNavStat() } }
                    Pair(navDeferred.await(), statDeferred.await())
                }

                // 检查网络请求结果
                val navResp = navResult.getOrElse { e ->
                    // 网络失败
                    if (isNetworkError(e as Exception)) {
                        _uiState.value = ProfileUiState.Error("网络不可用，请检查网络连接")
                    } else {
                        _uiState.value = ProfileUiState.Error("加载失败，点击重试")
                    }
                    return@launch
                }
                
                val statResp = statResult.getOrNull() // 统计信息失败不影响主流程

                val data = navResp.data
                val statData = statResp?.data

                // 3. 判断是否登录有效
                if (data != null && data.isLogin) {
                    // 优先使用本地自定义背景，否则使用 API 返回的 top_photo
                    val finalTopPhoto = if (customBgUri.isNotEmpty()) {
                         // [Fix] Add timestamp to bust Coil cache for local files
                        if (customBgUri.startsWith("file://")) {
                            try {
                                val uri = Uri.parse(customBgUri)
                                val file = File(uri.path ?: "")
                                if (file.exists()) {
                                    "$customBgUri?t=${file.lastModified()}"
                                } else customBgUri
                            } catch (e: Exception) {
                                customBgUri
                            }
                        } else {
                            customBgUri
                        }
                    } else {
                        data.top_photo
                    }
                    
                    _uiState.value = ProfileUiState.Success(
                        UserState(
                            isLogin = true,
                            face = data.face,
                            name = data.uname,
                            mid = data.mid,
                            level = data.level_info.current_level,
                            coin = data.money,
                            bcoin = data.wallet.bcoin_balance,
                            isVip = data.vip.status == 1,
                            vipLabel = data.vip.label.text,
                            // 绑定统计数据
                            following = statData?.following ?: 0,
                            follower = statData?.follower ?: 0,
                            dynamic = statData?.dynamic_count ?: 0,
                            // 绑定背景图
                            topPhoto = finalTopPhoto
                        )
                    )
                } else {
                    // Cookie 过期或无效
                    TokenManager.clear(getApplication())
                    _uiState.value = ProfileUiState.LoggedOut(topPhoto = customBgUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 🔧 [修复] 网络错误时不清除 Token，保持登录状态
                // 区分「无网络」和「真正的服务器错误」
                val hasToken = !TokenManager.sessDataCache.isNullOrEmpty()
                if (hasToken && isNetworkError(e)) {
                    // 有 Token 但网络不可用 → 显示离线提示，不退出登录
                    _uiState.value = ProfileUiState.Error("网络不可用，请检查网络连接")
                } else if (hasToken) {
                    // 有 Token 但其他错误 → 也显示错误，不清除登录
                    _uiState.value = ProfileUiState.Error("加载失败，点击重试")
                } else {
                    // 无 Token → 显示未登录
                    // [Fix] Add timestamp for LoggedOut state
                    val finalUri = if (customBgUri.startsWith("file://")) {
                        try {
                            val file = File(Uri.parse(customBgUri).path ?: "")
                            if (file.exists()) "$customBgUri?t=${file.lastModified()}" else customBgUri
                        } catch (e: Exception) { customBgUri }
                    } else customBgUri
                    _uiState.value = ProfileUiState.LoggedOut(topPhoto = finalUri) 
                }
            }
        }
    }
    
    /**
     * 更新自定义背景图
     * 将选中的图片复制到应用私有目录，并更新设置
     */
    fun updateCustomBackground(
        uri: Uri, 
        mobileBias: Float = 0f, 
        tabletBias: Float = 0f
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                
                SettingsManager.setProfileBgAlignment(context, false, mobileBias)
                SettingsManager.setProfileBgAlignment(context, true, tabletBias)
                
                // 1. 创建图片保存目录
                val imagesDir = File(context.filesDir, "images")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                
                // 2. 创建目标文件 (profile_bg.jpg)
                // 使用固定文件名，每次覆盖，节省空间
                val destFile = File(imagesDir, "profile_bg.jpg")
                
                // 3. 复制文件
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // 4. 保存文件路径到设置 (使用 file:// URI)
                val savedUri = Uri.fromFile(destFile).toString()
                SettingsManager.setProfileBgUri(context, savedUri)
                
                // 5. 刷新界面 (重新加载)
                loadProfile()
                
            } catch (e: Exception) {
                e.printStackTrace()
                // 可以增加一个 Toast 或 Error State 通知用户失败
            }
        }
    }
    
    /**
     * 判断是否为网络相关错误
     */
    private fun isNetworkError(e: Exception): Boolean {
        return e is UnknownHostException ||
               e is SocketTimeoutException ||
               e is java.net.ConnectException ||
               e.cause is UnknownHostException ||
               e.cause is SocketTimeoutException
    }

    fun logout() {
        viewModelScope.launch {
            // retain background
            val customBgUri = SettingsManager.getProfileBgUri(getApplication()).first() ?: ""
            TokenManager.clear(getApplication())
            _uiState.value = ProfileUiState.LoggedOut(topPhoto = customBgUri)
            com.android.purebilibili.core.util.AnalyticsHelper.syncUserContext(
                mid = null,
                isVip = false,
                privacyModeEnabled = SettingsManager.isPrivacyModeEnabledSync(getApplication())
            )
            //  记录登出事件
            com.android.purebilibili.core.util.AnalyticsHelper.logLogout()
        }
    }
    
    // [新增] 官方壁纸列表
    private val _officialWallpapers = MutableStateFlow<List<com.android.purebilibili.data.model.response.SplashItem>>(emptyList())
    val officialWallpapers = _officialWallpapers.asStateFlow()
    private val _officialWallpapersLoading = MutableStateFlow(false)
    val officialWallpapersLoading = _officialWallpapersLoading.asStateFlow()
    private val _officialWallpapersError = MutableStateFlow<String?>(null)
    val officialWallpapersError = _officialWallpapersError.asStateFlow()

    fun loadOfficialWallpapers() {
        viewModelScope.launch {
            _officialWallpapersLoading.value = true
            _officialWallpapersError.value = null
            val result = com.android.purebilibili.data.repository.SplashRepository.getOfficialWallpapers()
            if (result.isSuccess) {
                _officialWallpapers.value = result.getOrNull() ?: emptyList()
            } else {
                _officialWallpapersError.value = result.exceptionOrNull()?.message ?: "加载失败，点击重试"
            }
            _officialWallpapersLoading.value = false
        }
    }

    // [新增] 搜索壁纸
    private val _searchWallpapers = MutableStateFlow<List<com.android.purebilibili.data.model.response.SplashItem>>(emptyList())
    val searchWallpapers = _searchWallpapers.asStateFlow()
    private val _searchLoading = MutableStateFlow(false)
    val searchLoading = _searchLoading.asStateFlow()

    fun searchWallpapers(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) return@launch
            _searchLoading.value = true
            try {
                // 使用通用搜索接口搜索 "query + 壁纸"
                val searchApi = NetworkModule.searchApi
                // 这里调用 searchAll 或 searchType 接口，假设 searchAll 可用
                // 注意：B站搜索 API 比较复杂，这里简化处理，假设搜索 "壁纸" 相关内容
                // 实际可能需要解析 SearchResponse 并转换为 SplashItem
                
                // 构造搜索参数
                val params = mutableMapOf<String, String>()
                params["keyword"] = "$query 壁纸"
                
                // 模拟：由于没有直接的 searchWallpaper API，我们这里临时复用 searchAll
                // 真实场景下需解析 SearchResponse 中的 result.video 或 result.article
                // 为了演示，这里先留空或模拟一些数据，或者如果 SearchApi 返回结构匹配的话
                
                // [暂缓] 实际搜索逻辑需要详细解析 SearchResponse。
                // 鉴于 SearchResponse 结构较复杂，我们先模拟一个空列表或 TODO
                // 等待 SearchResponse 结构完全确认。
                
                // 既然用户想要 "搜索B站开屏壁纸"，通常这些资源不在标准搜索里直接以图片形式提供。
                // 我们可以搜 "垂直" 视频的封面? 
                // 让我们尝试搜 "draw" 栏目?
                
                // 简易方案：调用 searchAll，取 result.result 里的数据（需适配）
                // 暂时: 仅作为 UI 展示，不做真实网络请求以免崩溃，或者请求后打 Log
                
                // 真实实现：
                 val result = searchApi.searchAll(params)
                 // TODO: Parse result to SplashItem list
                 // _searchWallpapers.value = parsedList
                 
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _searchLoading.value = false
            }
        }
    }

    // [New] 壁纸保存状态
    private val _wallpaperSaveState = MutableStateFlow<WallpaperSaveState>(WallpaperSaveState.Idle)
    val wallpaperSaveState = _wallpaperSaveState.asStateFlow()

    /**
     * 保存壁纸 (下载并设置为背景)
     */
    // [New] Alignment State
    fun getProfileBgAlignment(isTablet: Boolean) = SettingsManager.getProfileBgAlignment(getApplication(), isTablet)

    /**
     * 保存壁纸 (下载并设置为背景)
     * 支持传入对齐参数
     */
    fun saveWallpaper(
        url: String, 
        mobileBias: Float = 0f, 
        tabletBias: Float = 0f,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _wallpaperSaveState.value = WallpaperSaveState.Loading
            try {
                // 保存对齐设置
                val context = getApplication<Application>()
                SettingsManager.setProfileBgAlignment(context, false, mobileBias)
                SettingsManager.setProfileBgAlignment(context, true, tabletBias)
                
                // 修复 URL 协议 (强制 HTTPS)
                var finalUrl = url
                if (finalUrl.startsWith("//")) {
                    finalUrl = "https:$finalUrl"
                } else if (finalUrl.startsWith("http://")) {
                    finalUrl = finalUrl.replace("http://", "https://")
                }
                
                val request = okhttp3.Request.Builder().url(finalUrl).build()
                val response = NetworkModule.okHttpClient.newCall(request).execute()
                
                if (response.isSuccessful && response.body != null) {
                    val imagesDir = File(context.filesDir, "images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()
                    val destFile = File(imagesDir, "profile_bg.jpg")
                    
                    FileOutputStream(destFile).use { output ->
                        response.body!!.byteStream().copyTo(output)
                    }
                    
                    val savedUri = Uri.fromFile(destFile).toString()
                    SettingsManager.setProfileBgUri(context, savedUri)
                    
                    loadProfile() // 刷新
                    
                    withContext(Dispatchers.Main) {
                        _wallpaperSaveState.value = WallpaperSaveState.Success
                        onComplete()
                    }
                } else {
                    _wallpaperSaveState.value = WallpaperSaveState.Error("下载失败: ${response.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _wallpaperSaveState.value = WallpaperSaveState.Error(e.message ?: "保存出错")
            } finally {
                // Delay reset to allow UI to show success checkmark if needed, but for now we rely on onDismiss
                if (_wallpaperSaveState.value is WallpaperSaveState.Success) {
                     _wallpaperSaveState.value = WallpaperSaveState.Idle
                }
            }
        }
    }

    fun selectOfficialWallpaper(url: String) {
        saveWallpaper(url)
    }
    
    // [New] Splash Wallpaper Logic
    private val _splashSaveState = MutableStateFlow<WallpaperSaveState>(WallpaperSaveState.Idle)
    val splashSaveState = _splashSaveState.asStateFlow()

    fun setAsSplashWallpaper(url: String, saveToGallery: Boolean = false, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            _splashSaveState.value = WallpaperSaveState.Loading
            try {
                val context = getApplication<Application>()
                var finalUrl = url
                if (finalUrl.startsWith("//")) {
                    finalUrl = "https:$finalUrl"
                } else if (finalUrl.startsWith("http://")) {
                    finalUrl = finalUrl.replace("http://", "https://")
                }

                val request = okhttp3.Request.Builder().url(finalUrl).build()
                val response = NetworkModule.okHttpClient.newCall(request).execute()

                if (response.isSuccessful && response.body != null) {
                    // Read bytes once
                    val bytes = response.body!!.bytes() 
                    
                    // 1. Save to internal splash directory
                    val splashDir = File(context.filesDir, "splash")
                    if (!splashDir.exists()) splashDir.mkdirs()
                    val destFile = File(splashDir, "splash_bg.jpg")

                    FileOutputStream(destFile).use { output ->
                        output.write(bytes)
                    }

                    // 2. Update Settings
                    val savedUri = Uri.fromFile(destFile).toString()
                    SettingsManager.setSplashWallpaperUri(context, savedUri)
                    SettingsManager.setSplashEnabled(context, true)

                    // 3. Save to Gallery if requested
                    if (saveToGallery) {
                         saveImageToGallery(context, bytes, "bili_splash_${System.currentTimeMillis()}.jpg")
                    }

                    withContext(Dispatchers.Main) {
                        _splashSaveState.value = WallpaperSaveState.Success
                        onComplete()
                    }
                } else {
                    _splashSaveState.value = WallpaperSaveState.Error("下载失败: ${response.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _splashSaveState.value = WallpaperSaveState.Error(e.message ?: "保存出错")
            } finally {
                // Delay reset slightly to let UI react if needed, or just reset logic
                 if (_splashSaveState.value is WallpaperSaveState.Success) {
                     _splashSaveState.value = WallpaperSaveState.Idle
                }
            }
        }
    }

    private fun saveImageToGallery(context: Context, bytes: ByteArray, fileName: String) {
        try {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                     put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/BiliPai")
                }
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { output: java.io.OutputStream ->
                    output.write(bytes)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // [New] Easter Egg: Triple Jump Setting
    val tripleJumpEnabled = SettingsManager.getTripleJumpEnabled(application).stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setTripleJumpEnabled(enabled: Boolean) {
        viewModelScope.launch {
            SettingsManager.setTripleJumpEnabled(getApplication(), enabled)
        }
    }
}

sealed class WallpaperSaveState {
    object Idle : WallpaperSaveState()
    object Loading : WallpaperSaveState()
    object Success : WallpaperSaveState()
    data class Error(val message: String) : WallpaperSaveState()
}
