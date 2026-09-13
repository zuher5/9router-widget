package com.ninerouter.monitor.data.network

import com.ninerouter.monitor.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class NineRouterApiClient(
    val cookieJar: NineRouterCookieJar = NineRouterCookieJar()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getStatus(baseUrl: String): Result<AuthStatusResponse> {
        val url = cleanUrl(baseUrl) + "/api/auth/status"
        val request = Request.Builder().url(url).get().build()
        return executeJson(request)
    }

    suspend fun login(baseUrl: String, password: String): Result<LoginResponse> {
        val url = cleanUrl(baseUrl) + "/api/auth/login"
        val bodyStr = json.encodeToString(LoginRequest.serializer(), LoginRequest(password))
        val body = bodyStr.toRequestBody(jsonMediaType)
        val request = Request.Builder().url(url).post(body).build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        val raw = resp.body?.string().orEmpty()
                        try {
                            if (resp.isSuccessful) {
                                val parsed = json.decodeFromString(LoginResponse.serializer(), raw)
                                continuation.resume(Result.success(parsed))
                            } else {
                                val errResp = try {
                                    json.decodeFromString(LoginResponse.serializer(), raw)
                                } catch (e: Exception) {
                                    LoginResponse(success = false, error = "HTTP ${resp.code}: $raw")
                                }
                                continuation.resume(Result.success(errResp))
                            }
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }
                }
            })
        }
    }

    suspend fun getUsageStats(baseUrl: String, period: String = "today"): Result<UsageStatsResponse> {
        val url = cleanUrl(baseUrl) + "/api/usage/stats?period=$period"
        val request = Request.Builder().url(url).get().build()
        return executeJson(request)
    }

    suspend fun getProviders(baseUrl: String): Result<ProviderConnectionsResponse> {
        val url = cleanUrl(baseUrl) + "/api/providers"
        val request = Request.Builder().url(url).get().build()
        return executeJson(request)
    }

    suspend fun getProviderNodes(baseUrl: String): Result<ProviderNodesResponse> {
        val url = cleanUrl(baseUrl) + "/api/provider-nodes"
        val request = Request.Builder().url(url).get().build()
        return executeJson(request)
    }

    /**
     * SSE stream untuk /api/usage/stream.
     * Mengembalikan Flow berupa String JSON event dari server (atau null jika keepalive ping).
     */
    fun getUsageStream(baseUrl: String): Flow<StreamUpdatePayload> = callbackFlow {
        val url = cleanUrl(baseUrl) + "/api/usage/stream"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .get()
            .build()

        val streamingClient = client.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val call = streamingClient.newCall(request)

        val thread = Thread {
            try {
                val response = call.execute()
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(IOException("HTTP ${resp.code}"))
                        return@Thread
                    }
                    val source = resp.body?.byteStream() ?: run {
                        close(IOException("Empty SSE body"))
                        return@Thread
                    }
                    val reader = BufferedReader(InputStreamReader(source))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: break
                        if (l.startsWith("data:")) {
                            val payload = l.removePrefix("data:").trim()
                            if (payload.isNotEmpty()) {
                                try {
                                    val parsed = json.decodeFromString<StreamUpdatePayload>(payload)
                                    trySend(parsed)
                                } catch (_: Exception) {
                                    // Abaikan format payload yang berbeda
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                close(e)
            }
        }
        thread.isDaemon = true
        thread.start()

        awaitClose {
            call.cancel()
            thread.interrupt()
        }
    }

    suspend fun logout(baseUrl: String): Result<Boolean> {
        val url = cleanUrl(baseUrl) + "/api/auth/logout"
        val request = Request.Builder().url(url).post("{}".toRequestBody(jsonMediaType)).build()
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(Result.failure(e))
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        cookieJar.clear()
                        continuation.resume(Result.success(resp.isSuccessful))
                    }
                }
            })
        }
    }

    private suspend inline fun <reified T> executeJson(request: Request): Result<T> {
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        if (!resp.isSuccessful) {
                            continuation.resume(Result.failure(IOException("HTTP ${resp.code}: ${resp.message}")))
                            return
                        }
                        val body = resp.body?.string().orEmpty()
                        try {
                            val parsed = json.decodeFromString<T>(body)
                            continuation.resume(Result.success(parsed))
                        } catch (e: Exception) {
                            continuation.resume(Result.failure(e))
                        }
                    }
                }
            })
        }
    }

    private fun cleanUrl(raw: String): String {
        return raw.trim().removeSuffix("/")
    }
}
