package com.android.purebilibili.feature.cast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListDialog(
    onDismissRequest: () -> Unit,
    onDeviceSelected: (CastDeviceInfo) -> Unit,
    onSsdpDeviceSelected: (SsdpDiscovery.SsdpDevice) -> Unit = {}
) {
    val devices by DlnaManager.devices.collectAsState()
    val isConnected by DlnaManager.isConnected.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 手动 SSDP 发现结果
    var ssdpDevices by remember { mutableStateOf<List<SsdpDiscovery.SsdpDevice>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // 启动时同时进行 Cling 和手动 SSDP 搜索
    LaunchedEffect(Unit) {
        if (isConnected) {
            DlnaManager.refresh()
        }
        // 同时进行手动 SSDP 发现
        isSearching = true
        scope.launch {
            ssdpDevices = SsdpDiscovery.discover(context, 5000)
            isSearching = false
        }
    }
    
    fun doRefresh() {
        DlnaManager.refresh()
        scope.launch {
            isSearching = true
            ssdpDevices = SsdpDiscovery.discover(context, 5000)
            isSearching = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Rounded.Cast, null) },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("选择投屏设备")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { if (!isSearching) doRefresh() }, enabled = !isSearching) {
                    Icon(Icons.Rounded.Refresh, "刷新")
                }
            }
        },
        text = {
            val hasDevices = devices.isNotEmpty() || ssdpDevices.isNotEmpty()
            
            if (!hasDevices && !isSearching) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    if (!isConnected) {
                        Text("连接服务中...", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("未找到设备", style = MaterialTheme.typography.bodyMedium)
                            Text("请确保在同一 WiFi 下", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (!hasDevices && isSearching) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("搜索设备中...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    // Cling 发现的设备
                    items(devices) { device ->
                        ListItem(
                            headlineContent = { Text(device.name.ifEmpty { "Unknown Device" }) },
                            supportingContent = { Text(device.description.ifEmpty { device.location ?: "Unknown" }) },
                            leadingContent = { Icon(Icons.Rounded.Tv, null) },
                            modifier = Modifier
                                .clickable { onDeviceSelected(device) }
                                .fillMaxWidth()
                        )
                    }
                    // 手动 SSDP 发现的设备（排除已被 Cling 发现的）
                    val clingLocations = devices.mapNotNull { 
                        it.location
                    }.toSet()
                    
                    val uniqueSsdpDevices = ssdpDevices.filter { ssdp ->
                        ssdp.location !in clingLocations
                    }
                    
                    items(uniqueSsdpDevices) { ssdpDevice ->
                        ListItem(
                            headlineContent = { 
                                Text(ssdpDevice.server.ifEmpty { "SSDP Device" })
                            },
                            supportingContent = { 
                                Text(ssdpDevice.st.substringAfterLast(":").ifEmpty { ssdpDevice.location })
                            },
                            leadingContent = { Icon(Icons.Rounded.Tv, null, tint = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier
                                .clickable { onSsdpDeviceSelected(ssdpDevice) }
                                .fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧导出日志按钮
                TextButton(
                    onClick = {
                        com.android.purebilibili.core.util.LogCollector.exportAndShare(context)
                    }
                ) {
                    Text("导出日志")
                }
                
                // 右侧取消按钮
                TextButton(onClick = onDismissRequest) {
                    Text("取消")
                }
            }
        }
    )
}
