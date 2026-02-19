package com.android.purebilibili.feature.following

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.FollowingUser
import io.github.alexzhirkevich.cupertino.CupertinoActivityIndicator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.android.purebilibili.core.util.PinyinUtils

// UI 状态
sealed class FollowingListUiState {
    object Loading : FollowingListUiState()
    data class Success(
        val users: List<FollowingUser>,
        val total: Int,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    ) : FollowingListUiState()
    data class Error(val message: String) : FollowingListUiState()
}

class FollowingListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FollowingListUiState>(FollowingListUiState.Loading)
    val uiState = _uiState.asStateFlow()
    
    private var currentPage = 1
    private var currentMid: Long = 0
    
    fun loadFollowingList(mid: Long) {
        if (mid <= 0) return
        currentMid = mid
        currentPage = 1
        
        viewModelScope.launch {
            _uiState.value = FollowingListUiState.Loading
            
            try {
                // 1. 加载第一页
                val response = NetworkModule.api.getFollowings(mid, pn = 1, ps = 50)
                if (response.code == 0 && response.data != null) {
                    val initialUsers = response.data.list ?: emptyList()
                    val total = response.data.total
                    
                    _uiState.value = FollowingListUiState.Success(
                        users = initialUsers,
                        total = total,
                        hasMore = initialUsers.size < total // 还有更多数据需要加载
                    )
                    
                    // 2. 如果还有更多数据，自动在后台加载剩余所有页面 (为了支持全量搜索)
                    if (initialUsers.size < total) {
                        loadAllRemainingPages(mid, total, initialUsers)
                    }
                } else {
                    _uiState.value = FollowingListUiState.Error("加载失败: ${response.message}")
                }
            } catch (e: Exception) {
                _uiState.value = FollowingListUiState.Error(e.message ?: "网络错误")
            }
        }
    }
    
    // 自动加载剩余所有页面
    private fun loadAllRemainingPages(mid: Long, total: Int, initialUsers: List<FollowingUser>) {
        viewModelScope.launch {
            try {
                var currentUsers = initialUsers.toMutableList()
                val pageSize = 50
                // 计算需要加载的总页数
                val totalPages = (total + pageSize - 1) / pageSize
                
                // 从第2页开始循环加载
                for (page in 2..totalPages) {
                    if (mid != currentMid) break // 如果用户切换了查看的 UP 主，停止加载
                    
                    // 延迟一点时间，避免请求过于频繁触发风控
                    kotlinx.coroutines.delay(300) 
                    
                    val response = NetworkModule.api.getFollowings(mid, pn = page, ps = pageSize)
                    if (response.code == 0 && response.data != null) {
                        val newUsers = response.data.list ?: emptyList()
                        if (newUsers.isNotEmpty()) {
                            currentUsers.addAll(newUsers)
                            
                            // 更新 UI 状态
                            _uiState.value = FollowingListUiState.Success(
                                users = currentUsers.toList(), // Create new list to trigger recomposition
                                total = total,
                                hasMore = page < totalPages,
                                isLoadingMore = true // 显示正在后台加载
                            )
                        }
                    } else {
                        break // 出错停止加载
                    }
                }
                
                // 加载完成
                val current = _uiState.value
                if (current is FollowingListUiState.Success) {
                    _uiState.value = current.copy(isLoadingMore = false, hasMore = false)
                }
            } catch (e: Exception) {
                // 后台加载失败暂不干扰主流程
                val current = _uiState.value
                if (current is FollowingListUiState.Success) {
                    _uiState.value = current.copy(isLoadingMore = false)
                }
            }
        }
    }
    
    // 手动加载更多 (已废弃，保留空实现兼容接口或删除)
    fun loadMore() { }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingListScreen(
    mid: Long,
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit,  // 点击跳转到 UP 主空间
    viewModel: FollowingListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(mid) {
        viewModel.loadFollowingList(mid)
    }
    
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的关注") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(CupertinoIcons.Default.ChevronBackward, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 🔍 搜索栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                com.android.purebilibili.core.ui.components.IOSSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "搜索 UP 主"
                )
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (val state = uiState) {
                    is FollowingListUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CupertinoActivityIndicator()
                        }
                    }
                    
                    is FollowingListUiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("😢", fontSize = 48.sp)
                                Spacer(Modifier.height(16.dp))
                                Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadFollowingList(mid) }) {
                                    Text("重试")
                                }
                            }
                        }
                    }
                    
                    is FollowingListUiState.Success -> {
                        // 🔍 过滤列表
                        val filteredUsers = remember(state.users, searchQuery) {
                            if (searchQuery.isBlank()) state.users
                            else {
                                state.users.filter { 
                                    PinyinUtils.matches(it.uname, searchQuery) ||
                                    PinyinUtils.matches(it.sign, searchQuery)
                                }
                            }
                        }

                        if (filteredUsers.isEmpty() && searchQuery.isNotEmpty()) {
                             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("没有找到相关 UP 主", color = MaterialTheme.colorScheme.onSurfaceVariant)
                             }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                // 统计信息
                                item {
                                    Text(
                                        text = if (searchQuery.isEmpty()) "共 ${state.total} 个关注" else "找到 ${filteredUsers.size} 个结果",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                                
                                items(filteredUsers, key = { it.mid }) { user ->
                                    FollowingUserItem(
                                        user = user,
                                        onClick = { onUserClick(user.mid) }
                                    )
                                }
                                
                                // 加载更多 (仅在未搜索时显示，因为搜索是本地过滤)
                                if (searchQuery.isEmpty()) {
                                    if (state.isLoadingMore) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CupertinoActivityIndicator()
                                            }
                                        }
                                    } else if (state.hasMore) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { viewModel.loadMore() }
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "加载更多",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingUserItem(
    user: FollowingUser,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(FormatUtils.fixImageUrl(user.face))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(Modifier.width(12.dp))
        
        // 用户信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.uname,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.sign.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.sign,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


