package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply

// iOS Visual Styles
private val MenuBackground = Color(0xCC1C1C1E) // Translucent Black
private val SeparatorColor = Color(0xFF38383A)
private val DestructiveColor = Color(0xFFFF453A) // System Red
private val PrimaryColor = Color(0xFF0A84FF) // System Blue

@Composable
fun DanmakuContextMenu(
    text: String,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onRecall: () -> Unit,
    onReport: (reason: Int) -> Unit,
    voteCount: Int = 0,
    hasLiked: Boolean = false,
    voteLoading: Boolean = false,
    canVote: Boolean = false,
    onBlockUser: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    var showReportMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = showReportMenu,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 2 }).togetherWith(fadeOut() + slideOutVertically { it / 2 })
                },
                label = "MenuTransition"
            ) { isReport ->
                if (isReport) {
                    ReportReasonMenu(
                        onSelectReason = { reason ->
                            onReport(reason)
                            onDismiss()
                        },
                        onBack = { showReportMenu = false }
                    )
                } else {
                    MainMenu(
                        text = text,
                        voteCount = voteCount,
                        hasLiked = hasLiked,
                        voteLoading = voteLoading,
                        canVote = canVote,
                        onLike = {
                            onLike()
                            onDismiss()
                        },
                        onRecall = {
                            onRecall()
                            onDismiss()
                        },
                        onReportClick = { showReportMenu = true },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(text))
                            onDismiss()
                        },
                        onBlockUser = {
                            onBlockUser()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMenu(
    text: String,
    voteCount: Int,
    hasLiked: Boolean,
    voteLoading: Boolean,
    canVote: Boolean,
    onLike: () -> Unit,
    onRecall: () -> Unit,
    onReportClick: () -> Unit,
    onCopy: () -> Unit,
    onBlockUser: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MenuBackground)
            .padding(vertical = 0.dp), // iOS menus often have no outer padding inside the rounded container
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Preview Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "弹幕内容",
                color = Color.White.copy(0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        MenuSeparator()

        val voteLabel = when {
            voteLoading -> "加载投票状态..."
            !canVote -> "当前弹幕不支持投票"
            hasLiked -> "取消点赞 (${formatVoteCount(voteCount)})"
            else -> "点赞弹幕 (${formatVoteCount(voteCount)})"
        }
        MenuItem(
            label = voteLabel,
            icon = Icons.Filled.ThumbUp,
            enabled = canVote && !voteLoading,
            onClick = onLike
        )
        
        MenuSeparator()
        
        MenuItem(
            label = "复制内容",
            icon = Icons.Filled.ContentCopy,
            onClick = onCopy
        )

        MenuSeparator()

        // Distanced actions or specialized
        MenuItem(
            label = "撤回弹幕",
            icon = Icons.AutoMirrored.Filled.Reply, // Using Reply as Recall-like icon
            onClick = onRecall
        )

        MenuSeparator()
        
        MenuItem(
            label = "屏蔽发送者",
            icon = Icons.Filled.Block,
            onClick = onBlockUser
        )

        MenuSeparator()

        MenuItem(
            label = "举报弹幕",
            icon = Icons.Filled.Report,
            color = DestructiveColor, // Red for Report
            onClick = onReportClick
        )
    }
}

@Composable
private fun ReportReasonMenu(
    onSelectReason: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MenuBackground),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Back
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = PrimaryColor,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable(onClick = onBack)
            )
            Text(
                text = "举报原因",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        MenuSeparator()

        val reasons = listOf(
            Pair("违法违禁", 1),
            Pair("色情低俗", 2),
            Pair("赌博诈骗", 3), // mapped to 'Advertising' or similar in API usually? API doc said: 1=违法/2=色情/3=广告/4=引战/5=辱骂/6=剧透/7=刷屏/8=其他
            Pair("人身攻击", 5),
            Pair("引战", 4),
            Pair("剧透", 6),
            Pair("刷屏", 7),
            Pair("其他", 8)
        )

        reasons.forEachIndexed { index, (label, code) ->
            MenuItem(
                label = label,
                centered = true, // Center text for options
                onClick = { onSelectReason(code) }
            )
            if (index < reasons.lastIndex) {
                MenuSeparator()
            }
        }
    }
}

private fun formatVoteCount(rawCount: Int): String {
    val count = rawCount.coerceAtLeast(0)
    if (count < 10_000) return count.toString()
    val compact = ((count / 1000) / 10f).toString().removeSuffix(".0")
    return "${compact}万"
}

@Composable
private fun MenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color = Color.White,
    centered: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val clickModifier = if (enabled) Modifier.clickable(onClick = onClick) else Modifier
    val displayColor = if (enabled) color else color.copy(alpha = 0.45f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = displayColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = displayColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MenuSeparator() {
    Divider(
        color = SeparatorColor,
        thickness = 0.5.dp
    )
}
