package com.ninerouter.monitor

import android.os.Bundle
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
import com.ninerouter.monitor.data.repository.UsageRepository
import com.ninerouter.monitor.ui.dashboard.DashboardScreen
import com.ninerouter.monitor.ui.dashboard.DashboardViewModel
import com.ninerouter.monitor.ui.setup.SetupScreen
import androidx.glance.appwidget.updateAll
import com.ninerouter.monitor.ui.theme.NineRouterBrand
import com.ninerouter.monitor.ui.theme.NineRouterTheme
import com.ninerouter.monitor.widget.NineRouterSolarWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var apiClient: NineRouterApiClient
    private lateinit var repository: UsageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sessionManager = SessionManager(applicationContext)
        apiClient = NineRouterApiClient()
        repository = UsageRepository(applicationContext, apiClient, sessionManager)

        setContent {
            NineRouterTheme(darkTheme = true) {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Loading) }
                var isLoggingIn by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf<String?>(null) }
                var dashboardViewModel by remember { mutableStateOf<DashboardViewModel?>(null) }

                fun performLogin(url: String, pass: String) {
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
                                    dashboardViewModel = DashboardViewModel(
                                        appContext = applicationContext,
                                        repository = repository,
                                        sessionManager = sessionManager
                                    )
                                    currentScreen = Screen.Dashboard
                                    lifecycleScope.launch {
                                        try {
                                            NineRouterSolarWidget().updateAll(applicationContext)
                                        } catch (_: Exception) {}
                                    }
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
                                }
                            }.onFailure { err ->
                                loginError = err.localizedMessage ?: getString(R.string.error_connection_failed)
                                currentScreen = Screen.Setup
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

                when (currentScreen) {
                    is Screen.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NineRouterBrand)
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
                    is Screen.Dashboard -> {
                        val vm = dashboardViewModel ?: remember {
                            DashboardViewModel(
                                appContext = applicationContext,
                                repository = repository,
                                sessionManager = sessionManager
                            )
                        }
                        DashboardScreen(
                            viewModel = vm,
                            onLogoutClick = {
                                dashboardViewModel = null
                                currentScreen = Screen.Setup
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
        object Dashboard : Screen
    }
}
