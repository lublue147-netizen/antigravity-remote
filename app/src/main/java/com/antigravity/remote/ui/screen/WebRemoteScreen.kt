package com.antigravity.remote.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.remote.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebRemoteScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (String) -> Unit = {}
) {
    val webRemoteUrl by viewModel.webRemoteUrl.collectAsState()
    val immersiveMode by viewModel.immersiveMode.collectAsState()
    val fitScreen by viewModel.fitScreen.collectAsState()
    val context = LocalContext.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Intercept hardware/gesture back to navigate inside webview history
    BackHandler(enabled = canGoBack) {
        webViewInstance?.goBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        if (webRemoteUrl.isBlank()) {
            // Empty state guidance
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "欢迎使用 Antigravity Remote",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "粘贴您的官方 Antigravity 远程会话链接（例如 https://antigravity.google.com/u/3/r/...），即可全屏无边框操控 AI Agent！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { showUrlDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("配置会话链接")
                        }
                    }
                }
            }
        } else {
            // Android WebView with 100% full screen stretch
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        setBackgroundColor(0xFF121212.toInt())
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        // Cookie persistence for Google authentication
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = false // Don't zoom out into desktop letterbox!
                            setSupportZoom(true)
                            builtInZoomControls = false
                            displayZoomControls = false
                            allowFileAccess = true
                            allowContentAccess = true
                            cacheMode = WebSettings.LOAD_DEFAULT

                            // Chrome mobile UA without '; wv' for Google sign-in compatibility
                            val defaultUa = userAgentString
                            var ua = defaultUa.replace("; wv", "")
                            if (!ua.contains("Mobile")) {
                                ua = "$ua Mobile"
                            }
                            userAgentString = ua
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                canGoBack = view?.canGoBack() ?: false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() ?: false

                                // Inject CSS and viewport fixes to eliminate top/bottom black borders
                                if (fitScreen) {
                                    val jsInject = """
                                        (function() {
                                            var meta = document.querySelector('meta[name="viewport"]');
                                            if (!meta) {
                                                meta = document.createElement('meta');
                                                meta.name = 'viewport';
                                                document.head.appendChild(meta);
                                            }
                                            meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover';

                                            var style = document.getElementById('antigravity-mobile-fit');
                                            if (!style) {
                                                style = document.createElement('style');
                                                style.id = 'antigravity-mobile-fit';
                                                style.innerHTML = `
                                                    html, body {
                                                        width: 100% !important;
                                                        min-height: 100% !important;
                                                        height: 100% !important;
                                                        margin: 0 !important;
                                                        padding: 0 !important;
                                                        background-color: #121212 !important;
                                                        overflow-x: hidden !important;
                                                    }
                                                    /* Expand main layout containers to full height without letterboxing */
                                                    main, #root, #app, [class*="layout"], [class*="container"] {
                                                        width: 100% !important;
                                                        max-width: 100% !important;
                                                        min-height: 100% !important;
                                                    }
                                                `;
                                                document.head.appendChild(style);
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(jsInject, null)
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progress = newProgress / 100f
                            }
                        }

                        loadUrl(webRemoteUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    if (webView.url != webRemoteUrl && webRemoteUrl.isNotBlank()) {
                        webView.loadUrl(webRemoteUrl)
                    }
                }
            )
        }

        // Top thin progress indicator (minimalistic, non-intrusive)
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // Floating Action Menu Overlay (when expanded)
        AnimatedVisibility(
            visible = showFabMenu,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 80.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ),
                modifier = Modifier.width(200.dp).shadow(12.dp, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "快捷操作",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // 1. 刷新
                    TextButton(
                        onClick = {
                            showFabMenu = false
                            webViewInstance?.reload()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("刷新网页", modifier = Modifier.weight(1f))
                    }

                    // 2. 后退
                    TextButton(
                        onClick = {
                            showFabMenu = false
                            webViewInstance?.goBack()
                        },
                        enabled = canGoBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("上一页", modifier = Modifier.weight(1f))
                    }

                    // 3. 修改链接
                    TextButton(
                        onClick = {
                            showFabMenu = false
                            showUrlDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("修改链接", modifier = Modifier.weight(1f))
                    }

                    // 4. 打开原生会话列表
                    TextButton(
                        onClick = {
                            showFabMenu = false
                            onNavigateToTab("dashboard")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("原生会话", modifier = Modifier.weight(1f))
                    }

                    // 5. 设置
                    TextButton(
                        onClick = {
                            showFabMenu = false
                            onNavigateToTab("settings")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("应用设置", modifier = Modifier.weight(1f))
                    }

                    // 6. 切换沉浸模式
                    TextButton(
                        onClick = {
                            viewModel.updateImmersiveMode(!immersiveMode)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (immersiveMode) "显示底栏" else "全屏沉浸", modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Floating Action Button (FAB)
        FloatingActionButton(
            onClick = { showFabMenu = !showFabMenu },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                if (showFabMenu) Icons.Filled.Close else Icons.Filled.Menu,
                contentDescription = "菜单",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Change URL Dialog
    if (showUrlDialog) {
        var inputUrl by remember { mutableStateOf(webRemoteUrl) }
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("配置 Antigravity 会话链接") },
            text = {
                Column {
                    Text(
                        "粘贴您的 Antigravity 网页会话链接（例如 https://antigravity.google.com/u/3/r/...）：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Web Remote URL") },
                        placeholder = { Text("https://antigravity.google.com/...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formatted = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                            "https://$inputUrl"
                        } else {
                            inputUrl
                        }.trim()
                        viewModel.updateWebRemoteUrl(formatted)
                        webViewInstance?.loadUrl(formatted)
                        showUrlDialog = false
                    },
                    enabled = inputUrl.isNotBlank()
                ) {
                    Text("加载运行")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
