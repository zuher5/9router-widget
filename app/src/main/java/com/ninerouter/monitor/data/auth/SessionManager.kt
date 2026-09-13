package com.ninerouter.monitor.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ninerouter_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveServerConfig(url: String, password: String, rememberPassword: Boolean) {
        prefs.edit().apply {
            putString(KEY_SERVER_URL, url.trim().removeSuffix("/"))
            if (rememberPassword) {
                putString(KEY_PASSWORD, password)
                putBoolean(KEY_REMEMBER, true)
            } else {
                remove(KEY_PASSWORD)
                putBoolean(KEY_REMEMBER, false)
            }
            apply()
        }
    }

    fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    fun getSavedPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    fun isRememberPassword(): Boolean = prefs.getBoolean(KEY_REMEMBER, false)

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_PASSWORD)
            remove(KEY_REMEMBER)
            apply()
        }
    }

    companion object {
        private const val KEY_SERVER_URL = "key_server_url"
        private const val KEY_PASSWORD = "key_password"
        private const val KEY_REMEMBER = "key_remember"
    }
}
