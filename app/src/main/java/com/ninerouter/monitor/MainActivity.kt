package com.ninerouter.monitor

import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.network.NineRouterApiClient
import com.ninerouter.monitor.ui.setup.SetupScreen
import com.ninerouter.monitor.ui.theme.NineRouterTheme
import com.ninerouter.monitor.ui.webview.WebViewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var apiClient: NineRouterApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = SessionManager(applicationContext)
        apiClient = NineRouterApiClient()

        setContent {
            NineRouterTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
                var authToken by remember { mutableStateOf<String?>(null) }
                var isLoggingIn by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf<String?>(null) }

                fun performLogin(url: String, pass: String, onComplete: ((Boolean) -> Unit)? = null) {
                    isLoggingIn = true
                    loginError = null

                    lifecycleScope.launch(Dispatchers.IO) {
                        val cleanUrl = url.trim().removeSuffix("/")
                        val result = apiClient.login(cleanUrl, pass)

                        withContext(Dispatchers.Main) {
                            isLoggingIn = false
                            result.onSuccess { resp ->
                                if (resp.success) {
                                    sessionManager.saveServerConfig(cleanUrl, pass, true)
                                    val token = apiClient.cookieJar.getCookiesForHost(android.net.Uri.parse(cleanUrl).host ?: "")
                                        .firstOrNull { it.name == "auth_token" }?.value

                                    authToken = token
                                    currentScreen = Screen.WebView(cleanUrl)
                                    onComplete?.invoke(true)
                                } else {
                                    val msg = when {
                                        resp.mustChangePassword -> getString(R.string.error_must_change_password)
                                        resp.remainingBeforeLock != null -> getString(R.string.error_invalid_password_remaining, resp.remainingBeforeLock)
                                        resp.retryAfter != null -> getString(R.string.error_rate_limited, resp.retryAfter)
                                        !resp.error.isNullOrBlank() -> resp.error
                                        else -> getString(R.string.error_login_failed)
                                    }
                                    loginError = msg
                                    currentScreen = Screen.Setup
                                    onComplete?.invoke(false)
                                }
                            }.onFailure { err ->
                                loginError = err.localizedMessage ?: getString(R.string.error_connection_failed)
                                currentScreen = Screen.Setup
                                onComplete?.invoke(false)
                            }
                        }
                    }
                }

                // Cek sesi awal saat aplikasi dibuka
                LaunchedEffect(Unit) {
                    val savedUrl = sessionManager.getServerUrl()
                    val savedPass = sessionManager.getSavedPassword()

                    if (!savedUrl.isNullOrBlank() && !savedPass.isNullOrBlank()) {
                        performLogin(savedUrl, savedPass)
                    } else {
                        currentScreen = Screen.Setup
                    }
                }

                when (val screen = currentScreen) {
                    is Screen.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Screen.Setup -> {
                        SetupScreen(
                            initialUrl = sessionManager.getServerUrl().orEmpty(),
                            isLoading = isLoggingIn,
                            errorMessage = loginError,
                            onLoginClick = { url, pass ->
                                performLogin(url, pass)
                            }
                        )
                    }
                    is Screen.WebView -> {
                        WebViewScreen(
                            serverUrl = screen.url,
                            authToken = authToken,
                            onOpenSettings = {
                                currentScreen = Screen.Setup
                            },
                            onLogoutClick = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    apiClient.logout(screen.url)
                                    sessionManager.clearSession()
                                    withContext(Dispatchers.Main) {
                                        CookieManager.getInstance().removeAllCookies(null)
                                        CookieManager.getInstance().flush()
                                        authToken = null
                                        currentScreen = Screen.Setup
                                    }
                                }
                            },
                            onRequireReLogin = {
                                val pass = sessionManager.getSavedPassword()
                                if (!pass.isNullOrBlank()) {
                                    performLogin(screen.url, pass)
                                } else {
                                    currentScreen = Screen.Setup
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private sealed interface Screen {
        object Loading : Screen
        object Setup : Screen
        data class WebView(val url: String) : Screen
    }
}
