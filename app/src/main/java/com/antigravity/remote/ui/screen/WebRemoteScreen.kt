package com.antigravity.remote.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.antigravity.remote.util.BlobDownloadInterface
import com.antigravity.remote.util.DownloadHelper

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

    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        fileChooserCallback?.onReceiveValue(uris)
        fileChooserCallback = null
    }

    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            showFabMenu = false
        }
    }

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
                        isFocusable = true
                        isFocusableInTouchMode = true

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
                            setSupportMultipleWindows(true)
                            javaScriptCanOpenWindowsAutomatically = true

                            // Chrome mobile UA without '; wv' for Google sign-in compatibility
                            val defaultUa = userAgentString
                            var ua = defaultUa.replace("; wv", "")
                            if (!ua.contains("Mobile")) {
                                ua = "$ua Mobile"
                            }
                            userAgentString = ua
                        }

                        // Register Javascript Interface for blob: and data: file downloads
                        addJavascriptInterface(BlobDownloadInterface(context), "AndroidBlobDownloader")

                        // Register DownloadListener for standard HTTP/HTTPS downloads and fallback handling
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            DownloadHelper.handleDownloadListener(
                                context = context,
                                webView = this,
                                url = url,
                                userAgent = userAgent,
                                contentDisposition = contentDisposition,
                                mimetype = mimetype
                            )
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

                                // 1. Inject download hook for intercepting <a> click, blob:, data: links
                                val jsDownloadHook = """
                                    (function() {
                                        if (window.__antigravityDownloadHooked) return;
                                        window.__antigravityDownloadHooked = true;

                                        function triggerBlobDownload(href, downloadName) {
                                            if (!href) return false;
                                            if (href.startsWith('data:')) {
                                                var comma = href.indexOf(',');
                                                var b64 = comma !== -1 ? href.substring(comma + 1) : href;
                                                var header = href.substring(0, comma);
                                                var mime = header.split(';')[0].replace('data:', '') || 'application/octet-stream';
                                                var fname = downloadName || ('download_' + Date.now());
                                                if (window.AndroidBlobDownloader) {
                                                    window.AndroidBlobDownloader.getBase64FromBlobData(b64, mime, fname);
                                                }
                                                return true;
                                            } else if (href.startsWith('blob:')) {
                                                fetch(href)
                                                    .then(function(res) {
                                                        var mime = res.headers.get('content-type') || 'application/octet-stream';
                                                        return res.blob().then(function(b) { return { blob: b, mime: mime }; });
                                                    })
                                                    .then(function(obj) {
                                                        var reader = new FileReader();
                                                        reader.onloadend = function() {
                                                            var res = reader.result;
                                                            var b64 = res.indexOf(',') !== -1 ? res.substring(res.indexOf(',') + 1) : res;
                                                            var fname = downloadName || ('download_' + Date.now());
                                                            if (window.AndroidBlobDownloader) {
                                                                window.AndroidBlobDownloader.getBase64FromBlobData(b64, obj.mime, fname);
                                                            }
                                                        };
                                                        reader.readAsDataURL(obj.blob);
                                                    })
                                                    .catch(function(e) {
                                                        console.error('Blob fetch failed: ' + e);
                                                    });
                                                return true;
                                            }
                                            return false;
                                        }

                                        document.addEventListener('click', function(e) {
                                            var el = e.target;
                                            while (el && el.tagName !== 'A') {
                                                el = el.parentElement;
                                            }
                                            if (el && el.tagName === 'A') {
                                                var href = el.getAttribute('href') || el.href;
                                                var download = el.getAttribute('download');
                                                if (download !== null || (href && (href.startsWith('blob:') || href.startsWith('data:')))) {
                                                    if (triggerBlobDownload(href, download || '')) {
                                                        e.preventDefault();
                                                        e.stopPropagation();
                                                    }
                                                }
                                            }
                                        }, true);

                                        var origClick = HTMLAnchorElement.prototype.click;
                                        HTMLAnchorElement.prototype.click = function() {
                                            var href = this.getAttribute('href') || this.href;
                                            var download = this.getAttribute('download');
                                            if (download !== null || (href && (href.startsWith('blob:') || href.startsWith('data:')))) {
                                                if (triggerBlobDownload(href, download || '')) {
                                                    return;
                                                }
                                            }
                                            origClick.apply(this, arguments);
                                        };
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(jsDownloadHook, null)

                                // 2. Inject CSS and viewport fixes to eliminate top/bottom black borders & auto scroll to input
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

                                            // Automatically scroll focused input/textarea into view when virtual keyboard appears
                                            function ensureInputVisible() {
                                                var active = document.activeElement;
                                                if (active && (
                                                    active.tagName === 'INPUT' || 
                                                    active.tagName === 'TEXTAREA' || 
                                                    active.isContentEditable || 
                                                    active.getAttribute('role') === 'textbox' ||
                                                    (active.className && typeof active.className === 'string' && active.className.indexOf('monaco') !== -1)
                                                )) {
                                                    setTimeout(function() {
                                                        active.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                                    }, 150);
                                                    setTimeout(function() {
                                                        active.scrollIntoView({ behavior: 'smooth', block: 'center' });
                                                    }, 350);
                                                }
                                            }

                                            window.addEventListener('focusin', ensureInputVisible, true);

                                            if (window.visualViewport) {
                                                window.visualViewport.addEventListener('resize', ensureInputVisible);
                                            } else {
                                                window.addEventListener('resize', ensureInputVisible);
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(jsInject, null)
                                }
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                canGoBack = view?.canGoBack() ?: false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                val urlStr = uri.toString()
                                val scheme = uri.scheme?.lowercase()

                                if (scheme == "http" || scheme == "https") {
                                    if (DownloadHelper.isDownloadableUrl(urlStr)) {
                                        DownloadHelper.downloadHttpUrl(
                                            context = context,
                                            url = urlStr,
                                            userAgent = view?.settings?.userAgentString ?: "",
                                            contentDisposition = null,
                                            mimeType = null
                                        )
                                        return true
                                    }
                                    return false
                                } else if (scheme != null && scheme != "about" && scheme != "data" && scheme != "blob" && scheme != "javascript") {
                                    return try {
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                return false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progress = newProgress / 100f
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                val newWebView = WebView(view!!.context).apply {
                                    settings.javaScriptEnabled = true
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                            val targetUrl = req?.url?.toString() ?: return false
                                            if (DownloadHelper.isDownloadableUrl(targetUrl)) {
                                                DownloadHelper.downloadHttpUrl(
                                                    context = context,
                                                    url = targetUrl,
                                                    userAgent = settings.userAgentString
                                                )
                                            } else {
                                                view.loadUrl(targetUrl)
                                            }
                                            return true
                                        }
                                    }
                                }
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = newWebView
                                resultMsg?.sendToTarget()
                                return true
                            }

                            override fun onShowFileChooser(
                                view: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback
                                return try {
                                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                    fileChooserLauncher.launch(intent)
                                    true
                                } catch (e: Exception) {
                                    fileChooserCallback?.onReceiveValue(null)
                                    fileChooserCallback = null
                                    false
                                }
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

        // Floating Action Button (FAB) - hide when virtual keyboard is open so it doesn't block inputs
        if (!isImeVisible) {
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
