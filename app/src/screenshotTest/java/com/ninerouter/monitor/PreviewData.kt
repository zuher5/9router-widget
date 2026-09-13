package com.ninerouter.monitor

import com.ninerouter.monitor.data.model.*

object PreviewData {
    val sampleStats = UsageStatsResponse(
        totalRequests = 12543,
        totalPromptTokens = 8420300,
        totalCompletionTokens = 1942000,
        totalCachedTokens = 3100000,
        totalCost = 14.8250,
        byProvider = mapOf(
            "anthropic" to ProviderStat(requests = 6200, promptTokens = 4500000, completionTokens = 1200000, cost = 9.40),
            "openai" to ProviderStat(requests = 4100, promptTokens = 2600000, completionTokens = 500000, cost = 4.12),
            "google" to ProviderStat(requests = 2243, promptTokens = 1320300, completionTokens = 242000, cost = 1.305)
        ),
        byModel = mapOf(
            "claude-sonnet-4-5 (anthropic)" to ModelStat(
                requests = 4800,
                promptTokens = 3800000,
                completionTokens = 950000,
                cost = 8.10,
                rawModel = "claude-sonnet-4-5",
                provider = "anthropic"
            ),
            "gpt-4o (openai)" to ModelStat(
                requests = 3100,
                promptTokens = 2100000,
                completionTokens = 400000,
                cost = 3.50,
                rawModel = "gpt-4o",
                provider = "openai"
            ),
            "gemini-2.5-flash (google)" to ModelStat(
                requests = 2243,
                promptTokens = 1320300,
                completionTokens = 242000,
                cost = 1.305,
                rawModel = "gemini-2.5-flash",
                provider = "google"
            ),
            "claude-haiku (anthropic)" to ModelStat(
                requests = 1400,
                promptTokens = 700000,
                completionTokens = 250000,
                cost = 1.30,
                rawModel = "claude-haiku",
                provider = "anthropic"
            ),
            "o3-mini (openai)" to ModelStat(
                requests = 1000,
                promptTokens = 500000,
                completionTokens = 100000,
                cost = 0.62,
                rawModel = "o3-mini",
                provider = "openai"
            )
        ),
        recentRequests = listOf(
            RecentRequestItem(
                timestamp = "2026-09-13T06:00:00Z",
                model = "claude-sonnet-4-5",
                provider = "anthropic",
                promptTokens = 1240,
                completionTokens = 340,
                status = "ok"
            ),
            RecentRequestItem(
                timestamp = "2026-09-13T05:59:12Z",
                model = "gpt-4o",
                provider = "openai",
                promptTokens = 2800,
                completionTokens = 820,
                status = "ok"
            ),
            RecentRequestItem(
                timestamp = "2026-09-13T05:58:30Z",
                model = "gemini-2.5-flash",
                provider = "google",
                promptTokens = 850,
                completionTokens = 120,
                status = "failed"
            ),
            RecentRequestItem(
                timestamp = "2026-09-13T05:57:45Z",
                model = "claude-sonnet-4-5",
                provider = "anthropic",
                promptTokens = 4200,
                completionTokens = 1100,
                status = "ok"
            )
        )
    )
}
