package com.example.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
    private val sharedPrefs = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "gittool_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("gittool_standard_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "github_access_token"
        private const val KEY_USERNAME = "github_username"
        private const val KEY_REMEMBER_ME = "github_remember_me"
        private const val KEY_OAUTH_CLIENT_ID = "github_oauth_client_id"
        private const val KEY_OAUTH_CLIENT_SECRET = "github_oauth_client_secret"
        private const val KEY_OAUTH_REDIRECT_URI = "github_oauth_redirect_uri"
    }

    fun saveOAuthClientId(clientId: String?) {
        sharedPrefs.edit().putString(KEY_OAUTH_CLIENT_ID, clientId).apply()
    }

    fun getOAuthClientId(): String? {
        return sharedPrefs.getString(KEY_OAUTH_CLIENT_ID, null)
    }

    fun saveOAuthClientSecret(clientSecret: String?) {
        sharedPrefs.edit().putString(KEY_OAUTH_CLIENT_SECRET, clientSecret).apply()
    }

    fun getOAuthClientSecret(): String? {
        return sharedPrefs.getString(KEY_OAUTH_CLIENT_SECRET, null)
    }

    fun saveOAuthRedirectUri(redirectUri: String?) {
        sharedPrefs.edit().putString(KEY_OAUTH_REDIRECT_URI, redirectUri).apply()
    }

    fun getOAuthRedirectUri(): String? {
        return sharedPrefs.getString(KEY_OAUTH_REDIRECT_URI, null)
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
