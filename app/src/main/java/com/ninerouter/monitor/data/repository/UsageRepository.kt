package com.ninerouter.monitor.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.ninerouter.monitor.data.auth.SessionManager
import com.ninerouter.monitor.data.model.StreamUpdatePayload
import com.ninerouter.monitor.data.model.TopologyProvider
import com.ninerouter.monitor.data.model.UsageStatsResponse
import com.ninerouter.monitor.data.network.NineRouterApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
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
        private const val KEY_PROVIDERS_JSON = "cached_providers_json"
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

    fun getCachedProviders(): List<TopologyProvider> {
        val raw = prefs.getString(KEY_PROVIDERS_JSON, null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(TopologyProvider.serializer()), raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCachedProviders(providers: List<TopologyProvider>) {
        try {
            val raw = json.encodeToString(ListSerializer(TopologyProvider.serializer()), providers)
            prefs.edit().putString(KEY_PROVIDERS_JSON, raw).apply()
        } catch (_: Exception) {
        }
    }

    fun clearCache() {
        prefs.edit().remove(KEY_STATS_JSON).remove(KEY_PROVIDERS_JSON).apply()
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

    /**
     * Mengikuti pola web UsageStats.js:225:
     * Ambil /api/providers dan /api/provider-nodes secara paralel,
     * filter koneksi aktif, mapping nodeName untuk custom provider,
     * deduplikasi per provider, dan sertakan provider noAuth (opencode).
     */
    suspend fun fetchProviders(): Result<List<TopologyProvider>> = coroutineScope {
        val serverUrl = sessionManager.getServerUrl()
            ?: return@coroutineScope Result.failure(IllegalStateException("Server URL not configured"))

        val connectionsDeferred = async { apiClient.getProviders(serverUrl) }
        val nodesDeferred = async { apiClient.getProviderNodes(serverUrl) }

        val connResult = connectionsDeferred.await()
        val nodesResult = nodesDeferred.await()

        val connections = connResult.getOrNull()?.connections ?: emptyList()
        val nodeNameMap = nodesResult.getOrNull()?.nodes?.associate { it.id to it.name } ?: emptyMap()

        val seen = mutableSetOf<String>()
        val resultList = mutableListOf<TopologyProvider>()

        // 1. Tambah koneksi yang aktif
        connections.filter { it.isActive }.forEach { conn ->
            val pid = conn.provider.lowercase().trim()
            if (pid.isNotEmpty() && seen.add(pid)) {
                resultList.add(
                    TopologyProvider(
                        id = conn.id.ifBlank { conn.provider },
                        provider = conn.provider,
                        name = conn.name,
                        nodeName = nodeNameMap[conn.provider]
                    )
                )
            }
        }

        // 2. OpenCode Free selalu disertakan jika belum ada di koneksi (meniru web noAuth)
        if (seen.add("opencode")) {
            resultList.add(
                TopologyProvider(
                    id = "opencode",
                    provider = "opencode",
                    name = "OpenCode Free",
                    nodeName = null
                )
            )
        }

        if (resultList.isNotEmpty()) {
            saveCachedProviders(resultList)
            Result.success(resultList)
        } else if (connResult.isFailure) {
            Result.failure(connResult.exceptionOrNull() ?: Exception("Gagal memuat providers"))
        } else {
            Result.success(emptyList())
        }
    }

    fun streamUsage(): Flow<StreamUpdatePayload>? {
        val serverUrl = sessionManager.getServerUrl() ?: return null
        return apiClient.getUsageStream(serverUrl)
    }
}
