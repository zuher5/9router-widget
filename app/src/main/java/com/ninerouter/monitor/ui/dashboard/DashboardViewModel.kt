package com.ninerouter.monitor.ui.dashboard

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.data.repository.UsageRepository
import com.ninerouter.monitor.widget.NineRouterSolarWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: UsageStatsResponse? = null,
    val selectedPeriod: String = "today",
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val appContext: Context,
    private val repository: UsageRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    init {
        // Muat cache saat startup
        val cached = repository.getCachedStats()
        if (cached != null) {
            _uiState.value = _uiState.value.copy(stats = cached, isOffline = true)
        }
        refresh()
        startStreaming()
    }

    fun onPeriodSelected(period: String) {
        if (_uiState.value.selectedPeriod == period) return
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.fetchUsageStats(_uiState.value.selectedPeriod)
            result.fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        stats = data,
                        isLoading = false,
                        isOffline = false
                    )
                    // Push update ke widget setiap refresh sukses
                    triggerWidgetUpdate()
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

    /**
     * Mengikuti pola web UsageStats.js:281
     * Subscribe ke SSE /api/usage/stream untuk merge activeRequests, recentRequests, errorProvider
     * secara realtime tanpa menimpa hitungan total stats periode yang aktif.
     */
    fun startStreaming() {
        streamJob?.cancel()
        val flow = repository.streamUsage() ?: return
        streamJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStreaming = true)
            try {
                flow.collect { update ->
                    val current = _uiState.value.stats ?: return@collect
                    val merged = current.copy(
                        activeRequests = update.activeRequests,
                        recentRequests = update.recentRequests,
                        errorProvider = update.errorProvider
                    )
                    _uiState.value = _uiState.value.copy(stats = merged)
                    repository.saveCachedStats(merged)
                    triggerWidgetUpdate()
                }
            } catch (_: Exception) {
                // Ignore SSE errors; REST refresh tetap berjalan
            } finally {
                _uiState.value = _uiState.value.copy(isStreaming = false)
            }
        }
    }

    private fun triggerWidgetUpdate() {
        viewModelScope.launch {
            try {
                NineRouterSolarWidget().updateAll(appContext)
            } catch (_: Exception) {
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        streamJob?.cancel()
        viewModelScope.launch {
            val serverUrl = sessionManager.getServerUrl()
            if (serverUrl != null) {
                try {
                    repository.apiClient.logout(serverUrl)
                } catch (_: Exception) {
                }
            }
            repository.clearCache()
            sessionManager.clearSession()
            triggerWidgetUpdate()
            onLoggedOut()
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
