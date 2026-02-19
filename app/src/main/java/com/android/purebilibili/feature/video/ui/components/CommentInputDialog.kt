// 文件路径: feature/video/ui/components/CommentInputDialog.kt
package com.android.purebilibili.feature.video.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.delay

/**
 * 评论输入对话框
 * 
 * 提供评论输入功能，支持回复指定评论
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInputDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSend: (String, List<Uri>) -> Unit,
    isSending: Boolean = false,
    replyToName: String? = null,
    inputHint: String = "进来唠会嗑呗~",
    canUploadImage: Boolean = true,
    canInputComment: Boolean = true,
    modifier: Modifier = Modifier,
    emotePackages: List<com.android.purebilibili.data.model.response.EmotePackage> = emptyList() // [新增] 表情包列表
) {
    // 状态
    var text by remember { mutableStateOf("") }
    var isForwardToDynamic by remember { mutableStateOf(false) } // 转发到动态
    var showEmojiPanel by remember { mutableStateOf(false) }    // 表情面板
    var currentTab by remember { mutableStateOf(0) } // 0=Kaomoji, 1=Emoji, 2+=API Packages
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris = (selectedImageUris + uris)
                .distinct()
                .take(9)
        }
    }
    
    // 重置状态
    LaunchedEffect(visible) {
        if (visible) {
            text = ""
            isForwardToDynamic = false
            showEmojiPanel = false
            selectedImageUris = emptyList()
            delay(100)
            if (canInputComment) {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }
    
    // 监听 emoji 面板开关，控制键盘
    LaunchedEffect(showEmojiPanel, canInputComment) {
        if (!canInputComment) return@LaunchedEffect
        if (showEmojiPanel) {
            keyboardController?.hide()
        } else if (visible) {
            // 切回键盘
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false, // 允许全宽
                decorFitsSystemWindows = false   // 沉浸式：内容延伸到状态栏/导航栏下
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(), // 避让软键盘
                verticalArrangement = Arrangement.Bottom // 底部对齐
            ) {
                // 点击上半部分空白区域关闭
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onDismiss() }
                        )
                )
                
                // 输入区域
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding() // 避让底部导航栏(手势条)
                    ) {
                        // 1. 顶部：输入框
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 180.dp) // 最小/最大高度
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = text,
                                onValueChange = { if (it.length <= 1000) text = it },
                                enabled = canInputComment && !isSending,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight() // 填满 Box
                                    .focusRequester(focusRequester),
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 24.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (text.isEmpty()) {
                                            val fallbackHint = "进来唠会嗑呗~"
                                            val resolvedHint = inputHint.ifBlank { fallbackHint }
                                            Text(
                                                text = if (replyToName != null) "回复 @$replyToName: $resolvedHint" else resolvedHint,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            
                            // 右上角全屏图标 (装饰)
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Expand",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .alpha(0.5f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (selectedImageUris.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "已选 ${selectedImageUris.size}/9 张",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedImageUris, key = { it.toString() }) { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "已选图片",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .clickable {
                                                    selectedImageUris = selectedImageUris.filterNot { it == uri }
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "移除",
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .padding(1.dp),
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 2. 底部工具栏
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 转发到动态
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = canInputComment && !isSending) { isForwardToDynamic = !isForwardToDynamic }
                                    .padding(4.dp)
                            ) {
                                // 模拟 RadioButton/Checkbox
                                Icon(
                                    imageVector = if (isForwardToDynamic) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isForwardToDynamic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "转发到动态",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // 图标栏: 表情 @ 图片
                            IconButton(
                                onClick = { showEmojiPanel = !showEmojiPanel },
                                enabled = canInputComment && !isSending
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Face,
                                    contentDescription = "Emoji",
                                    tint = if (showEmojiPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    text += "@"
                                    // 切换回键盘
                                    showEmojiPanel = false
                                },
                                enabled = canInputComment && !isSending
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = "At",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = canUploadImage && canInputComment && !isSending
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = "Add",
                                    tint = if (canUploadImage) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    },
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // 发送按钮
                            Button(
                                onClick = {
                                    if (text.isNotBlank() && !isSending && canInputComment) {
                                        android.util.Log.d("CommentInputDialog", "📤 Sending comment: $text")
                                        onSend(text.trim(), selectedImageUris)
                                    }
                                },
                                enabled = text.isNotBlank() && !isSending && canInputComment,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary, // 应该是粉色
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        text = "发布",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!canInputComment) {
                            Text(
                                text = "当前评论区暂不可评论",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        } else if (!canUploadImage) {
                            Text(
                                text = "当前评论区不支持图片评论",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        
                        // 3. 表情面板區域
                        AnimatedVisibility(
                            visible = showEmojiPanel,
                            enter = androidx.compose.animation.expandVertically() + fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp) // 增加高度以容纳 Tab
                                    .padding(top = 8.dp)
                            ) {
                                // 顶部标签栏 (可滚动)
                                ScrollableTabRow(
                                    selectedTabIndex = currentTab,
                                    edgePadding = 16.dp,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    indicator = { tabPositions ->
                                        if (currentTab < tabPositions.size) {
                                            TabRowDefaults.SecondaryIndicator(
                                                Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    // Tab 0: 颜文字
                                    Tab(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        text = { Text("颜文字") }
                                    )
                                    // Tab 1: Emoji
                                    Tab(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        text = { Text("Emoji") }
                                    )
                                    // API Packages (Tab 2+)
                                    emotePackages.forEachIndexed { index, pkg ->
                                        Tab(
                                            selected = currentTab == index + 2,
                                            onClick = { currentTab = index + 2 },
                                            text = { 
                                                // 尝试显示图标，没有则显示文字
                                                if (pkg.url.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = pkg.url,
                                                        contentDescription = pkg.text,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Text(pkg.text) 
                                                }
                                            }
                                        )
                                    }
                                }

                                // 内容区域
                                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                                    when (currentTab) {
                                        0 -> { // 颜文字
                                            val kaomojis = listOf(
                                                "(⌒▽⌒)", "（￣▽￣）", "(=・ω・=)", "(｀・ω・´)", 
                                                "(〜￣△￣)〜", "(･∀･)", "(°∀°)ﾉ", "(￣3￣)", 
                                                "╮(￣▽￣)╭", "( ´_ゝ｀)", "_(:3」∠)_", "(;¬_¬)",
                                                "(ﾟДﾟ≡ﾟДﾟ)", "(ノ=Д=)ノ┻━┻", "Σ( ￣□￣||)", "(´；ω；`)",
                                                "（/TДT)/", "(^・ω・^ )", "(●￣(ｴ)￣●)", "ε=ε=(ノ≧∇≦)ノ",
                                                "( >﹏<。)", "( *・ω・)✄╰ひ╯", "(╬￣皿￣)凸", "⊙__⊙"
                                            )
                                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(80.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(kaomojis.size) { i ->
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .height(36.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .clickable { text += kaomojis[i] }
                                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f))
                                                    ) {
                                                        Text(kaomojis[i], fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                        1 -> { // Emoji
                                            val emojis = listOf(
                                                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
                                                "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
                                                "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
                                                "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏",
                                                "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
                                                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼"
                                            )
                                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(40.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(emojis.size) { i ->
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clickable { text += emojis[i] }
                                                    ) {
                                                        Text(emojis[i], fontSize = 24.sp)
                                                    }
                                                }
                                            }
                                        }
                                        else -> { // API Package
                                            val pkgIndex = currentTab - 2
                                            if (pkgIndex < emotePackages.size) {
                                                val pkg = emotePackages[pkgIndex]
                                                val emotes = pkg.emote ?: emptyList()
                                                
                                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(60.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    items(emotes.size) { i ->
                                                        val emote = emotes[i]
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier.clickable { text += emote.text }
                                                        ) {
                                                            AsyncImage(
                                                                model = emote.url,
                                                                contentDescription = emote.text,
                                                                modifier = Modifier.size(50.dp)
                                                            )
                                                            Text(
                                                                text = emote.text.replace("[", "").replace("]", ""),
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
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
        }
    }
}
