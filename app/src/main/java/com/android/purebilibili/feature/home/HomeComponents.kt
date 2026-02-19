// 文件路径: feature/home/HomeComponents.kt
// 此文件包含对话框和错误状态展示
// UserState 定义在 HomeViewModel.kt 中
package com.android.purebilibili.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 对话框组件
// ==========================================

/**
 * 欢迎对话框
 */
@Composable
fun WelcomeDialog(githubUrl: String, onConfirm: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = { },
        title = { Text("欢迎") },
        text = {
            Column {
                Text("本应用仅供学习使用。")
                TextButton(onClick = { uriHandler.openUri(githubUrl) }) {
                    Text("开源地址: $githubUrl", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Text("进入")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * 错误状态展示
 */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("重试")
            }
        }
    }
}

// ==========================================
// 直播子分类组件
// ==========================================

/**
 *  直播子分类行（关注/热门切换）
 */
@Composable
fun LiveSubCategoryRow(
    selectedSubCategory: LiveSubCategory,
    onSubCategorySelected: (LiveSubCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiveSubCategory.entries.forEach { subCategory ->
            val isSelected = selectedSubCategory == subCategory
            FilterChip(
                selected = isSelected,
                onClick = { onSubCategorySelected(subCategory) },
                label = { Text(subCategory.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,  //  使用主题色
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary     //  使用主题对应的前景色
                )
            )
        }
    }
}