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
