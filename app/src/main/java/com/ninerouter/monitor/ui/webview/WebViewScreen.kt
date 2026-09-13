package com.ninerouter.monitor.ui.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ninerouter.monitor.R

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    serverUrl: String,
    authToken: String?,
    onOpenSettings: () -> Unit,
    onLogoutClick: () -> Unit,
    onRequireReLogin: () -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var showQuickBar by remember { mutableStateOf(false) }

    val cleanBaseUrl = serverUrl.trim().removeSuffix("/")
    val targetDashboardUrl = "$cleanBaseUrl/dashboard"

    // Sinkronisasi cookie auth_token ke CookieManager sebelum WebView load
    LaunchedEffect(cleanBaseUrl, authToken) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (!authToken.isNullOrBlank()) {
            cookieManager.setCookie(cleanBaseUrl, "auth_token=$authToken; path=/; SameSite=Lax")
            cookieManager.flush()
        }
    }

    // Tangani back navigation fisik/gesture Android
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onOpenSettings()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT

                        // Penyesuaian viewport mobile & matikan zoom sentuh
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false

                        // Allow cleartext LAN resources
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            canGoBack = view?.canGoBack() == true

                            // Deteksi bila diarahkan balik ke login (sesi expired)
                            if (url != null && (url.contains("/login") || url.endsWith("/login"))) {
                                onRequireReLogin()
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            canGoBack = view?.canGoBack() == true

                            // Injeksi CSS & script mobile-friendly
                            view?.evaluateJavascript(MobileCss.getInjectionScript(), null)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val destUri = request?.url ?: return false
                            val destHost = destUri.host
                            val baseHost = Uri.parse(cleanBaseUrl).host

                            // Tetap di dalam WebView bila host sama
                            if (destHost == null || destHost.equals(baseHost, ignoreCase = true)) {
                                return false
                            }

                            // URL luar buka di browser default sistem (Chrome dll)
                            val intent = Intent(Intent.ACTION_VIEW, destUri)
                            ctx.startActivity(intent)
                            return true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (newProgress >= 100) {
                                isLoading = false
                            }
                        }
                    }

                    webViewInstance = this
                    loadUrl(targetDashboardUrl)
                }
            },
            update = { view ->
                webViewInstance = view
            }
        )

        // Progress bar pemuatan halaman di tepi atas
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // Tombol Mengambang (FAB) untuk membuka / menutup bilah Quick Actions
        FloatingActionButton(
            onClick = { showQuickBar = !showQuickBar },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = if (showQuickBar) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = "Quick Actions"
            )
        }

        // Bilah Aksi Cepat (Quick Actions Bar)
        AnimatedVisibility(
            visible = showQuickBar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            }
                        },
                        enabled = canGoBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }

                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_reload)
                        )
                    }

                    IconButton(onClick = {
                        val currentUrl = webViewInstance?.url ?: targetDashboardUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.action_open_browser)
                        )
                    }

                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }

                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = stringResource(R.string.action_logout)
                        )
                    }
                }
            }
        }
    }
}
