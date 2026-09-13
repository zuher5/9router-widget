package com.ninerouter.monitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.data.repository.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: UsageStatsResponse? = null,
    val selectedPeriod: String = "7d",
    val avgLatency: Long? = null,
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val repository: UsageRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadCachedData()
        refresh()
    }

    private fun loadCachedData() {
        viewModelScope.launch {
            repository.cachedStatsFlow.collect { cached ->
                if (cached != null && _uiState.value.stats == null) {
                    _uiState.value = _uiState.value.copy(stats = cached, isOffline = true)
                }
            }
        }
    }

    fun onPeriodSelected(period: String) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val statsResult = repository.fetchUsageStats(_uiState.value.selectedPeriod)
            val latency = repository.fetchAverageLatencySample()

            statsResult.fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        stats = data,
                        avgLatency = latency,
                        isLoading = false,
                        isOffline = false
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isOffline = true,
                        errorMessage = err.localizedMessage ?: "Gagal memperbarui data"
                    )
                }
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        sessionManager.clearSession()
        onLoggedOut()
    }
}
