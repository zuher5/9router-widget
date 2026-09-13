package com.ninerouter.monitor

import com.ninerouter.monitor.data.network.NineRouterApiClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ApiClientMockTest {

    private lateinit var server: MockWebServer
    private lateinit var client: NineRouterApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = NineRouterApiClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testLoginSuccessCapturesCookie() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Set-Cookie", "auth_token=jwt_token_sample; Path=/; HttpOnly")
                .setBody("""{"success": true, "mustChangePassword": false}""")
        )

        val baseUrl = server.url("/").toString()
        val result = client.login(baseUrl, "123456")

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull()?.success)
        assertTrue(client.cookieJar.hasAuthToken(server.hostName))
    }

    @Test
    fun testLoginMustChangePassword() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setBody("""{"success": false, "error": "Default password must be changed", "mustChangePassword": true}""")
        )

        val baseUrl = server.url("/").toString()
        val result = client.login(baseUrl, "123456")

        assertTrue(result.isSuccess)
        val resp = result.getOrNull()
        assertNotNull(resp)
        assertEquals(false, resp?.success)
        assertEquals(true, resp?.mustChangePassword)
    }

    @Test
    fun testLoginInvalidPassword() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Invalid password", "remainingBeforeLock": 3}""")
        )

        val baseUrl = server.url("/").toString()
        val result = client.login(baseUrl, "wrong")

        assertTrue(result.isSuccess)
        val resp = result.getOrNull()
        assertEquals(3, resp?.remainingBeforeLock)
    }

    @Test
    fun testCookieJarGetCookiesForHost() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Set-Cookie", "auth_token=jwt_sample_123; Path=/; HttpOnly")
                .setBody("""{"success": true}""")
        )

        val baseUrl = server.url("/").toString()
        client.login(baseUrl, "123456")

        val cookies = client.cookieJar.getCookiesForHost(server.hostName)
        assertEquals(1, cookies.size)
        assertEquals("auth_token", cookies[0].name)
        assertEquals("jwt_sample_123", cookies[0].value)
    }

    @Test
    fun testGetUsageStats() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "totalRequests": 42,
                        "totalPromptTokens": 1000,
                        "totalCompletionTokens": 500,
                        "totalCost": 0.05,
                        "byProvider": {},
                        "byModel": {}
                    }
                """.trimIndent())
        )

        val baseUrl = server.url("/").toString()
        val result = client.getUsageStats(baseUrl, "today")

        assertTrue(result.isSuccess)
        val stats = result.getOrNull()
        assertNotNull(stats)
        assertEquals(42L, stats?.totalRequests)
        assertEquals(1500L, stats?.totalTokens)
    }
}
