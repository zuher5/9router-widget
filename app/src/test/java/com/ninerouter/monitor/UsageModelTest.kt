package com.ninerouter.monitor

import com.ninerouter.monitor.data.model.ActiveRequestItem
import com.ninerouter.monitor.data.model.RecentRequestItem
import com.ninerouter.monitor.data.model.StreamUpdatePayload
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

    @Test
    fun testParseStreamUpdatePayload() {
        val payloadJson = """
            {
                "activeRequests": [
                    { "id": "req-1", "model": "gpt-4o", "provider": "openai", "startTime": 1000 }
                ],
                "recentRequests": [
                    { "model": "gpt-4o", "status": "ok", "promptTokens": 10, "completionTokens": 20 }
                ],
                "errorProvider": null
            }
        """.trimIndent()

        val payload = json.decodeFromString<StreamUpdatePayload>(payloadJson)
        assertEquals(1, payload.activeRequests.size)
        assertEquals("req-1", payload.activeRequests[0].id)
        assertEquals(1, payload.recentRequests.size)
        assertEquals("gpt-4o", payload.recentRequests[0].model)
    }
}
