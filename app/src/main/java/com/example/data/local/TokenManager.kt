package com.example.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "gittool_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCESS_TOKEN = "github_access_token"
        private const val KEY_USERNAME = "github_username"
        private const val KEY_REMEMBER_ME = "github_remember_me"
    }

    fun saveAccessToken(token: String?) {
        sharedPrefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return sharedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveUsername(username: String?) {
        sharedPrefs.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? {
        return sharedPrefs.getString(KEY_USERNAME, null)
    }

    fun saveRememberMe(remember: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_REMEMBER_ME, remember).apply()
    }

    fun isRememberMe(): Boolean {
        return sharedPrefs.getBoolean(KEY_REMEMBER_ME, false)
    }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}
