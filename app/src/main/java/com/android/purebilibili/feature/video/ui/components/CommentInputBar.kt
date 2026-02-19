// 文件路径: feature/video/ui/components/CommentInputBar.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 * [新增] 评论输入栏组件
 * 固定在评论列表底部，支持发送评论和回复评论
 */
@Composable
fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean = false,
    replyToName: String? = null,  // 回复目标用户名
    onCancelReply: () -> Unit = {},
    onEmoteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // 回复提示条
        AnimatedVisibility(
            visible = replyToName != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "回复 @${replyToName ?: ""}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = CupertinoIcons.Default.Xmark,
                    contentDescription = "取消回复",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCancelReply() },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
        
        // 输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 表情按钮
            IconButton(
                onClick = onEmoteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = CupertinoIcons.Default.FaceSmiling,
                    contentDescription = "表情",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // 输入框
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = if (replyToName != null) "回复 @$replyToName" else "发一条友善的评论",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (value.isNotBlank() && !isSending) {
                                onSend()
                                focusManager.clearFocus()
                            }
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 发送按钮
            val canSend = value.isNotBlank() && !isSending
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(enabled = canSend) {
                        onSend()
                        focusManager.clearFocus()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = CupertinoIcons.Default.Paperplane,
                        contentDescription = "发送",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary 
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * [新增] 评论长按菜单
 */
@Composable
fun CommentContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onReply: () -> Unit,
    onDelete: (() -> Unit)? = null,  // 只有自己的评论才显示删除
    onReport: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("复制") },
            onClick = {
                onCopy()
                onDismiss()
            },
            leadingIcon = {
                Icon(CupertinoIcons.Default.DocOnDoc, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        DropdownMenuItem(
            text = { Text("回复") },
            onClick = {
                onReply()
                onDismiss()
            },
            leadingIcon = {
                Icon(CupertinoIcons.Default.ArrowshapeTurnUpLeft, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
        if (onDelete != null) {
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDelete()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(CupertinoIcons.Default.Trash, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            )
        }
        DropdownMenuItem(
            text = { Text("举报") },
            onClick = {
                onReport()
                onDismiss()
            },
            leadingIcon = {
                Icon(CupertinoIcons.Default.ExclamationmarkTriangle, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        )
    }
}

/**
 * [新增] 举报原因选择对话框
 */
@Composable
fun ReportReasonDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onReport: (Int) -> Unit
) {
    if (!visible) return
    
    val reasons = listOf(
        1 to "垃圾广告",
        2 to "色情",
        3 to "刷屏",
        4 to "引战",
        5 to "剧透",
        7 to "人身攻击",
        8 to "内容不相关",
        0 to "其他"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("举报原因", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                reasons.forEach { (code, label) ->
                    TextButton(
                        onClick = { onReport(code) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
