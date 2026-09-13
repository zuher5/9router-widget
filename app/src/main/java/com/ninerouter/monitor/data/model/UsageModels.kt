package com.ninerouter.monitor.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean = false,
    val mustChangePassword: Boolean = false,
    val error: String? = null,
    val retryAfter: Int? = null,
    val remainingBeforeLock: Int? = null
)

@Serializable
data class AuthStatusResponse(
    val requireLogin: Boolean = true,
    val hasPassword: Boolean = false,
    val authenticated: Boolean = false,
    val authMode: String? = null,
    val error: String? = null
)

@Serializable
data class UsageStatsResponse(
    val totalRequests: Long = 0,
    val totalPromptTokens: Long = 0,
    val totalCompletionTokens: Long = 0,
    val totalCachedTokens: Long = 0,
    val totalCost: Double = 0.0,
    val byProvider: Map<String, ProviderStat> = emptyMap(),
    val byModel: Map<String, ModelStat> = emptyMap(),
    val recentRequests: List<RecentRequestItem> = emptyList(),
    val activeRequests: List<ActiveRequestItem> = emptyList(),
    val errorProvider: String? = null
) {
    val totalTokens: Long
        get() = totalPromptTokens + totalCompletionTokens

    val derivedSuccessRate: Double
        get() {
            if (recentRequests.isEmpty()) return 100.0
            val successCount = recentRequests.count {
                it.status.equals("ok", ignoreCase = true) || it.status.equals("success", ignoreCase = true)
            }
            return (successCount.toDouble() / recentRequests.size) * 100.0
        }
}

@Serializable
data class ActiveRequestItem(
    val id: String = "",
    val model: String = "",
    val provider: String = "",
    val startTime: Long = 0
)

@Serializable
data class ProviderStat(
    val requests: Long = 0,
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val cachedTokens: Long = 0,
    val cost: Double = 0.0
)

@Serializable
data class ModelStat(
    val requests: Long = 0,
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val cachedTokens: Long = 0,
    val cost: Double = 0.0,
    val rawModel: String = "",
    val provider: String = "",
    val lastUsed: String? = null
) {
    val totalTokens: Long
        get() = promptTokens + completionTokens
}

@Serializable
data class RecentRequestItem(
    val timestamp: String = "",
    val model: String = "",
    val provider: String = "",
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val cachedTokens: Long = 0,
    val status: String = "ok",
    val latencyMs: Long? = null
)

@Serializable
data class StreamUpdatePayload(
    val activeRequests: List<ActiveRequestItem> = emptyList(),
    val recentRequests: List<RecentRequestItem> = emptyList(),
    val errorProvider: String? = null
)
