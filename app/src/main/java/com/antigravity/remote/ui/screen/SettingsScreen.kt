package com.antigravity.remote.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.antigravity.remote.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToWeb: () -> Unit = {}
) {
    val webRemoteUrl by viewModel.webRemoteUrl.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val authToken by viewModel.authToken.collectAsState()
    val darkTheme by viewModel.darkTheme.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val immersiveMode by viewModel.immersiveMode.collectAsState()
    val fitScreen by viewModel.fitScreen.collectAsState()

    var editWebUrl by remember(webRemoteUrl) { mutableStateOf(webRemoteUrl) }
    var editServerUrl by remember(serverUrl) { mutableStateOf(serverUrl) }
    var editAuthToken by remember(authToken) { mutableStateOf(authToken) }
    var showToken by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        // 1. 官方网页版配置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "官方网页版远程控制 (推荐)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "在此粘贴您的 Antigravity 网页会话链接（例如 https://antigravity.google.com/u/3/r/...），将在内置安全浏览器中直接运行，支持 Google 账号登录与完整操控！",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = editWebUrl,
                    onValueChange = { editWebUrl = it },
                    label = { Text("Web Remote URL (网页链接)") },
                    placeholder = { Text("https://antigravity.google.com/...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.updateWebRemoteUrl(editWebUrl.trim())
                            onNavigateToWeb()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("保存并打开网页版")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 自建 WebSocket 配置
        Text(
            "自建 WebSocket 后端服务",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "适用于运行了本地 Node.js / Python 桥接服务（如 OmniAntigravityRemoteChat）的用户。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = editServerUrl,
            onValueChange = { editServerUrl = it },
            label = { Text("WebSocket Server 地址") },
            placeholder = { Text("如 192.168.1.100:3000") },
            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = editAuthToken,
            onValueChange = { editAuthToken = it },
            label = { Text("Auth Token (可选)") },
            placeholder = { Text("服务认证密钥") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "切换可见性"
                    )
                }
            },
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        FilledTonalButton(
            onClick = {
                viewModel.updateServerUrl(editServerUrl.trim())
                viewModel.updateAuthToken(editAuthToken.trim())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("保存 WebSocket 配置")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. 通用设置
        Text(
            "应用偏好",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("沉浸式全屏 App 体验 (推荐)") },
                    supportingContent = { Text("隐藏顶栏与底栏，像独立原生 App 一样全屏沉浸运行，消除黑边") },
                    leadingContent = { Icon(Icons.Filled.Home, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = immersiveMode,
                            onCheckedChange = { viewModel.updateImmersiveMode(it) }
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("网页自适应铺满消除黑边") },
                    supportingContent = { Text("自动注入移动端 Viewport 与自适应高度，解决桌面版上下留白") },
                    leadingContent = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = fitScreen,
                            onCheckedChange = { viewModel.updateFitScreen(it) }
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("暗色模式") },
                    supportingContent = { Text("适配深色界面主题") },
                    leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { viewModel.updateDarkTheme(it) }
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("状态提醒通知") },
                    supportingContent = { Text("Agent 需要输入或决策时震动通知") },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. 关于
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Antigravity Remote Client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("版本 v1.0.3 (统一永久签名·无缝升级版)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "双模架构：同时支持 Google Antigravity 官方网页版全屏沉浸容器（无黑边 App 体验）及自建 WebSocket 代理原生面板。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}