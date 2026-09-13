package com.ninerouter.monitor.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.network.NineRouterApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "http://192.168.1.100:20128",
    val password: String = "",
    val rememberPassword: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val apiClient: NineRouterApiClient,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            serverUrl = sessionManager.getServerUrl() ?: "http://192.168.1.100:20128",
            password = sessionManager.getSavedPassword() ?: "",
            rememberPassword = sessionManager.isRememberPassword()
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUrlChanged(newUrl: String) {
        _uiState.value = _uiState.value.copy(serverUrl = newUrl, errorMessage = null)
    }

    fun onPasswordChanged(newPass: String) {
        _uiState.value = _uiState.value.copy(password = newPass, errorMessage = null)
    }

    fun onRememberChanged(remember: Boolean) {
        _uiState.value = _uiState.value.copy(rememberPassword = remember)
    }

    fun connect() {
        val state = _uiState.value
        val url = state.serverUrl.trim()
        val pass = state.password

        if (url.isBlank()) {
            _uiState.value = state.copy(errorMessage = "URL server tidak boleh kosong")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val statusRes = apiClient.getStatus(url)
            val isNoLoginRequired = statusRes.getOrNull()?.requireLogin == false

            if (isNoLoginRequired) {
                sessionManager.saveServerConfig(url, "", false)
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                return@launch
            }

            val loginRes = apiClient.login(url, pass)
            loginRes.fold(
                onSuccess = { resp ->
                    if (resp.success) {
                        sessionManager.saveServerConfig(url, pass, state.rememberPassword)
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    } else if (resp.mustChangePassword) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resp.error ?: "Password default harus diubah dari host lokal server."
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resp.error ?: "Gagal login ke 9Router"
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Tidak dapat terhubung ke server"
                    )
                }
            )
        }
    }
}
