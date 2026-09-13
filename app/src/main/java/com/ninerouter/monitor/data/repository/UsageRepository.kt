package com.ninerouter.monitor.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.model.ChartPoint
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.data.network.NineRouterApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "ninerouter_cache")

class UsageRepository(
    private val context: Context,
    private val apiClient: NineRouterApiClient,
    private val sessionManager: SessionManager
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val KEY_CACHED_STATS = stringPreferencesKey("cached_stats_json")
    private val KEY_CACHED_LATENCY = stringPreferencesKey("cached_avg_latency")

    val cachedStatsFlow: Flow<UsageStatsResponse?> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CACHED_STATS] ?: return@map null
        try {
            json.decodeFromString<UsageStatsResponse>(raw)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCachedStats(): UsageStatsResponse? {
        return cachedStatsFlow.firstOrNull()
    }

    suspend fun fetchUsageStats(period: String = "7d"): Result<UsageStatsResponse> {
        val serverUrl = sessionManager.getServerUrl()
            ?: return Result.failure(IllegalStateException("Server URL not configured"))

        var result = apiClient.getUsageStats(serverUrl, period)

        // Bila gagal (kemungkinan 401 JWT expired 24h), coba auto re-login bila password tersimpan
        if (result.isFailure && sessionManager.isRememberPassword()) {
            val savedPass = sessionManager.getSavedPassword()
            if (!savedPass.isNullOrBlank()) {
                val loginResult = apiClient.login(serverUrl, savedPass)
                if (loginResult.isSuccess && loginResult.getOrNull()?.success == true) {
                    // Retry request setelah re-login
                    result = apiClient.getUsageStats(serverUrl, period)
                }
            }
        }

        result.onSuccess { stats ->
            // Simpan cache ke DataStore untuk offline & widget
            try {
                val raw = json.encodeToString(UsageStatsResponse.serializer(), stats)
                context.dataStore.edit { it[KEY_CACHED_STATS] = raw }
            } catch (e: Exception) {
                // Jangan gagalkan fetch bila cache gagal
            }
        }

        return result
    }

    suspend fun fetchChart(period: String = "7d"): Result<List<ChartPoint>> {
        val serverUrl = sessionManager.getServerUrl()
            ?: return Result.failure(IllegalStateException("Server URL not configured"))
        return apiClient.getChartData(serverUrl, period)
    }

    suspend fun fetchAverageLatencySample(): Long? {
        val serverUrl = sessionManager.getServerUrl() ?: return null
        val res = apiClient.getRequestDetailsSample(serverUrl)
        val details = res.getOrNull()?.details ?: return null
        val latencies = details.mapNotNull { it.latency?.total }.filter { it > 0 }
        if (latencies.isEmpty()) return null
        return latencies.average().toLong()
    }
}
