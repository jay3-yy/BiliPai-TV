package com.android.purebilibili.feature.plugin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.plugin.PluginManager
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Moon

@Composable
fun EyeProtectionOverlay() {
    val plugins by PluginManager.pluginsFlow.collectAsState()
    val pluginInfo = plugins.find { it.plugin.id == "eye_protection" } ?: return
    val plugin = pluginInfo.plugin as? EyeProtectionPlugin ?: return
    val pluginEnabled = pluginInfo.enabled
    val settingsPreviewEnabled by plugin.settingsPreviewEnabled.collectAsState()
    if (!pluginEnabled && !settingsPreviewEnabled) return

    val isNightModeActive by plugin.isNightModeActive.collectAsState()
    val brightnessLevel by plugin.brightnessLevel.collectAsState()
    val warmFilterStrength by plugin.warmFilterStrength.collectAsState()
    val careReminder by plugin.careReminder.collectAsState()

    val darknessAlpha by animateFloatAsState(
        targetValue = (1f - brightnessLevel).coerceIn(0f, 0.7f),
        label = "eye_darkness"
    )
    val warmTopAlpha by animateFloatAsState(
        targetValue = warmFilterStrength * 0.3f,
        label = "eye_warm_top"
    )
    val warmBottomAlpha by animateFloatAsState(
        targetValue = warmFilterStrength * 0.2f,
        label = "eye_warm_bottom"
    )

    AnimatedVisibility(
        visible = isNightModeActive || settingsPreviewEnabled,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black.copy(alpha = darknessAlpha))
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF9800).copy(alpha = warmTopAlpha),
                            Color(0xFFFF5722).copy(alpha = warmBottomAlpha)
                        )
                    )
                )
            }
        }
    }

    careReminder?.let { reminder ->
        RestReminderDialog(
            reminder = reminder,
            snoozeMinutes = plugin.getSnoozeMinutes(),
            onDismiss = { plugin.dismissReminder() },
            onSnooze = { plugin.snoozeReminder() },
            onRest = { plugin.confirmRest() }
        )
    }
}

@Composable
private fun RestReminderDialog(
    reminder: EyeCareReminder,
    snoozeMinutes: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onRest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = Color(0xFF7E57C2).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    CupertinoIcons.Filled.Moon,
                    contentDescription = null,
                    tint = Color(0xFF7E57C2),
                    modifier = Modifier.size(36.dp)
                )
            }
        },
        title = {
            Text(
                text = reminder.title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "你已连续观看 ${reminder.usageMinutes} 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminder.suggestion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("我去休息 20 秒", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${snoozeMinutes} 分钟后提醒")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "先继续观看",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
