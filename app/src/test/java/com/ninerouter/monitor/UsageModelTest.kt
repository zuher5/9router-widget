package com.ninerouter.monitor

import com.ninerouter.monitor.data.model.RecentRequestItem
import com.ninerouter.monitor.data.model.UsageStatsResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageModelTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun testParseUsageStatsResponse() {
        val sampleJson = """
            {
                "totalRequests": 100,
                "totalPromptTokens": 450000,
                "totalCompletionTokens": 50000,
                "totalCost": 1.25,
                "byProvider": {
                    "anthropic": { "requests": 60, "promptTokens": 300000, "completionTokens": 30000 }
                },
                "byModel": {
                    "claude-sonnet-4-5 (anthropic)": {
                        "requests": 60,
                        "promptTokens": 300000,
                        "completionTokens": 30000,
                        "rawModel": "claude-sonnet-4-5",
                        "provider": "Anthropic"
                    }
                },
                "recentRequests": [
                    { "model": "claude-sonnet-4-5", "status": "ok" },
                    { "model": "claude-sonnet-4-5", "status": "ok" },
                    { "model": "gpt-4o", "status": "failed" },
                    { "model": "claude-sonnet-4-5", "status": "ok" }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<UsageStatsResponse>(sampleJson)

        assertEquals(100L, parsed.totalRequests)
        assertEquals(500000L, parsed.totalTokens)
        assertEquals(1.25, parsed.totalCost, 0.001)
        assertEquals(1, parsed.byModel.size)
        // 3 ok out of 4 -> 75%
        assertEquals(75.0, parsed.derivedSuccessRate, 0.1)
    }

    @Test
    fun testEmptyRecentRequestsDefault100() {
        val emptyStats = UsageStatsResponse(totalRequests = 0, recentRequests = emptyList())
        assertEquals(100.0, emptyStats.derivedSuccessRate, 0.1)
    }
}
