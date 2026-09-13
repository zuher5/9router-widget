package com.ninerouter.monitor.data.network

import com.ninerouter.monitor.data.model.*
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
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
                    val raw = response.body?.string().orEmpty()
                    try {
                        if (response.isSuccessful) {
                            val parsed = json.decodeFromString(LoginResponse.serializer(), raw)
                            continuation.resume(Result.success(parsed))
                        } else {
                            val errResp = try {
                                json.decodeFromString(LoginResponse.serializer(), raw)
                            } catch (e: Exception) {
                                LoginResponse(success = false, error = "HTTP ${response.code}: $raw")
                            }
                            continuation.resume(Result.success(errResp))
                        }
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }
            })
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
                    cookieJar.clear()
                    continuation.resume(Result.success(response.isSuccessful))
                }
            })
        }
    }

    suspend fun getUsageStats(baseUrl: String, period: String = "7d"): Result<UsageStatsResponse> {
        val url = cleanUrl(baseUrl) + "/api/usage/stats?period=$period"
        val request = Request.Builder().url(url).get().build()
        return executeJson(request)
    }

    suspend fun getChartData(baseUrl: String, period: String = "7d"): Result<List<ChartPoint>> {
        val url = cleanUrl(baseUrl) + "/api/usage/chart?period=$period"
        val request = Request.Builder().url(url).get().build()
        return executeJson(request)
    }

    suspend fun getRequestDetailsSample(baseUrl: String): Result<RequestDetailsResponse> {
        val url = cleanUrl(baseUrl) + "/api/usage/request-details?page=1&pageSize=20"
        val request = Request.Builder().url(url).get().build()
        return executeJson(request)
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
                    if (!response.isSuccessful) {
                        continuation.resume(Result.failure(IOException("HTTP ${response.code}: ${response.message}")))
                        return
                    }
                    val body = response.body?.string().orEmpty()
                    try {
                        val parsed = json.decodeFromString<T>(body)
                        continuation.resume(Result.success(parsed))
                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }
            })
        }
    }

    private fun cleanUrl(raw: String): String {
        return raw.trim().removeSuffix("/")
    }
}
