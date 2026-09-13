package com.ninerouter.monitor.data.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class NineRouterCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val currentCookies = cookieStore.getOrPut(host) { mutableListOf() }
        cookies.forEach { newCookie ->
            currentCookies.removeAll { it.name == newCookie.name }
            currentCookies.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val list = cookieStore[host] ?: return emptyList()
        val now = System.currentTimeMillis()
        list.removeAll { it.expiresAt < now }
        return list.toList()
    }

    fun clear() {
        cookieStore.clear()
    }

    fun hasAuthToken(host: String): Boolean {
        return cookieStore[host]?.any { it.name == "auth_token" && it.expiresAt > System.currentTimeMillis() } == true
    }
}
