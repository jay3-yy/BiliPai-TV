package com.android.purebilibili.feature.video.ui.section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.data.model.response.AiSummaryData
import com.android.purebilibili.data.model.response.AiOutline
import com.android.purebilibili.data.model.response.AiPartOutline
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.*
import io.github.alexzhirkevich.cupertino.icons.outlined.*

/**
 * AI Video Summary Card
 */
@Composable
fun AiSummaryCard(
    aiSummary: AiSummaryData?,
    onTimestampClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (aiSummary == null || aiSummary.code != 0 || aiSummary.modelResult == null) return

    val hasContent = aiSummary.modelResult.summary.isNotEmpty() ||
            aiSummary.modelResult.outline.isNotEmpty()

    if (!hasContent) return

    var expanded by remember { mutableStateOf(false) } // Default collapsed or expanded? Let's say collapsed if long.
    // However, usually AI summary is a feature user wants to see. Let's start collapsed to save space?
    // Bilibili official might show a preview.
    // Let's keep it collapsed by default but show a "AI Generated Summary" header.

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .animateContentSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CupertinoIcons.Default.Sparkles,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI 视频总结",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) CupertinoIcons.Default.ChevronUp else CupertinoIcons.Default.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                
                // Summary Text
                if (aiSummary.modelResult.summary.isNotEmpty()) {
                    Text(
                        text = aiSummary.modelResult.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Outline
                if (aiSummary.modelResult.outline.isNotEmpty()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    aiSummary.modelResult.outline.forEach { outlineItem ->
                        OutlineItemRow(
                            title = outlineItem.title,
                            timestamp = outlineItem.timestamp,
                            onClick = { onTimestampClick?.invoke(outlineItem.timestamp * 1000L) }
                        )
                        
                        // Part Outlines (if any)
                        outlineItem.partOutline.forEach { part ->
                             OutlineItemRow(
                                title = part.content,
                                timestamp = part.timestamp,
                                isSubItem = true,
                                onClick = { onTimestampClick?.invoke(part.timestamp * 1000L) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OutlineItemRow(
    title: String,
    timestamp: Long,
    isSubItem: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = if (isSubItem) 16.dp else 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!isSubItem) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
             Spacer(modifier = Modifier.width(4.dp)) // Indent for sub items aligned with bullet?
        }
        
        Column(modifier = Modifier.weight(1f)) {
             Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Time Button
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.clickable(onClick = onClick) // extra click area
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CupertinoIcons.Outlined.Clock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTimestamp(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatTimestamp(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
