package com.ninerouter.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ninerouter.monitor.ui.dashboard.DashboardScreen
import com.ninerouter.monitor.ui.dashboard.DashboardViewModel
import com.ninerouter.monitor.ui.login.LoginScreen
import com.ninerouter.monitor.ui.login.LoginViewModel
import com.ninerouter.monitor.ui.theme.NineRouterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NineRouterApp
        val sessionManager = app.sessionManager
        val apiClient = app.apiClient
        val repository = app.repository

        setContent {
            NineRouterTheme {
                var isLoggedIn by remember {
                    mutableStateOf(!sessionManager.getServerUrl().isNullOrBlank())
                }

                if (isLoggedIn) {
                    val dashboardVm: DashboardViewModel = viewModel {
                        DashboardViewModel(repository, sessionManager)
                    }
                    DashboardScreen(
                        viewModel = dashboardVm,
                        onLogoutClick = { isLoggedIn = false }
                    )
                } else {
                    val loginVm: LoginViewModel = viewModel {
                        LoginViewModel(apiClient, sessionManager)
                    }
                    LoginScreen(
                        viewModel = loginVm,
                        onLoginSuccess = { isLoggedIn = true }
                    )
                }
            }
        }
    }
}
