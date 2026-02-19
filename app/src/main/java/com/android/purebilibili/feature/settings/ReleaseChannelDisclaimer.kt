package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

const val OFFICIAL_GITHUB_URL = "https://github.com/jay3-yy/BiliPai/"
const val OFFICIAL_TELEGRAM_URL = "https://t.me/BiliPai"
const val RELEASE_DISCLAIMER_ACK_KEY = "release_disclaimer_ack_v1"

@Composable
fun ReleaseChannelDisclaimerDialog(
    onDismiss: () -> Unit,
    onOpenGithub: () -> Unit,
    onOpenTelegram: () -> Unit,
    title: String = "免责声明"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "本应用仅用于学习与交流。\n\n" +
                    "官方发布渠道仅有：GitHub 与 Telegram。\n" +
                    "除上述渠道外，不存在任何其他官方发布途径。\n\n" +
                    "请勿安装来源不明的安装包，以避免账号与设备安全风险。"
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("我已知晓")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpenGithub) { Text("GitHub") }
                TextButton(onClick = onOpenTelegram) { Text("Telegram") }
            }
        }
    )
}
