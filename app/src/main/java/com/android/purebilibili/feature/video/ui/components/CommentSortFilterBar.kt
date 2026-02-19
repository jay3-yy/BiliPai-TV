package com.android.purebilibili.feature.video.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.filled.Person
import io.github.alexzhirkevich.cupertino.icons.outlined.Person

/**
 *  评论排序筛选栏 (iOS Style)
 *  Header: "评论 (123)"
 *  Controls: Segmented Control [按热度 | 按时间]
 */
@Composable
fun CommentSortFilterBar(
    count: Int,
    sortMode: CommentSortMode,
    onSortModeChange: (CommentSortMode) -> Unit,
    upOnly: Boolean = false,
    onUpOnlyToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sortModes = remember { CommentSortMode.entries.toList() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        //  Left: Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "评论",
                fontSize = 20.sp, // iOS Large Title style scale
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface 
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = FormatUtils.formatStat(count.toLong()),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Right: Sort Control + Only UP Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Only UP Toggle
            iOSToggleButton(
                isChecked = upOnly,
                onToggle = onUpOnlyToggle,
                icon = CupertinoIcons.Default.Person
            )

            // Segmented Control
            iOSSegmentedControl(
                items = sortModes.map { it.label },
                selectedIndex = sortModes.indexOf(sortMode).coerceAtLeast(0),
                onScaleChange = { index ->
                    sortModes.getOrNull(index)?.let(onSortModeChange)
                }
            )
        }
    }
}

/**
 * iOS Style Segmented Control
 */
@Composable
fun iOSSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onScaleChange: (Int) -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val indicatorColor = MaterialTheme.colorScheme.surface
    val selectedTextColor = MaterialTheme.colorScheme.onSurface
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cornerRadius = 8.dp
    val segmentWidth = if (items.size >= 4) 52.dp else 60.dp

    Box(
        modifier = Modifier
            .height(32.dp)
            .background(backgroundColor, RoundedCornerShape(cornerRadius))
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, text ->
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false) 
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(cornerRadius - 2.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // No ripple for iOS feel
                        ) { onScaleChange(index) }
                        .then(
                            if (selectedIndex == index) {
                                Modifier
                                    .background(indicatorColor, RoundedCornerShape(cornerRadius - 2.dp))
                                    .padding(vertical = 1.dp) // Subtle shadow inset simulation
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Shadow simulation for selected item
                    if (selectedIndex == index) {
                         // In a real iOS implementation this would have a shadow
                    }
                    
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selectedIndex == index) selectedTextColor else unselectedTextColor
                    )
                }
            }
        }
    }
}

/**
 * iOS Style Toggle Button (Optional usage)
 */
@Composable
fun iOSToggleButton(
    isChecked: Boolean,
    onToggle: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val backgroundColor = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isChecked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
