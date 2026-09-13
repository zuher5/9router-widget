package com.ninerouter.monitor.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.model.StreamUpdatePayload
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.data.network.NineRouterApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class UsageRepository(
    private val context: Context,
    val apiClient: NineRouterApiClient,
    val sessionManager: SessionManager
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val prefs: SharedPreferences = context.getSharedPreferences("ninerouter_stats_cache", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_STATS_JSON = "cached_stats_json"
    }

    fun getCachedStats(): UsageStatsResponse? {
        val raw = prefs.getString(KEY_STATS_JSON, null) ?: return null
        return try {
            json.decodeFromString<UsageStatsResponse>(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun saveCachedStats(stats: UsageStatsResponse) {
        try {
            val raw = json.encodeToString(UsageStatsResponse.serializer(), stats)
            prefs.edit().putString(KEY_STATS_JSON, raw).apply()
        } catch (_: Exception) {
        }
    }

    fun clearCache() {
        prefs.edit().remove(KEY_STATS_JSON).apply()
    }

    suspend fun fetchUsageStats(period: String = "today"): Result<UsageStatsResponse> {
        val serverUrl = sessionManager.getServerUrl()
            ?: return Result.failure(IllegalStateException("Server URL not configured"))

        var result = apiClient.getUsageStats(serverUrl, period)

        // Jika 401 (token expired 24h), coba login ulang pakai password tersimpan
        if (result.isFailure) {
            val savedPass = sessionManager.getSavedPassword()
            if (!savedPass.isNullOrBlank()) {
                val loginResult = apiClient.login(serverUrl, savedPass)
                if (loginResult.isSuccess && loginResult.getOrNull()?.success == true) {
                    result = apiClient.getUsageStats(serverUrl, period)
                }
            }
        }

        result.onSuccess { saveCachedStats(it) }
        return result
    }

    fun streamUsage(): Flow<StreamUpdatePayload>? {
        val serverUrl = sessionManager.getServerUrl() ?: return null
        return apiClient.getUsageStream(serverUrl)
    }
}
