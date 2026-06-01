package com.example.data.repository

import com.example.data.local.TokenManager
import com.example.data.remote.GitHubApiService
import com.example.data.remote.GitHubUser
import com.example.data.remote.OAuthTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: GitHubApiService,
    private val tokenManager: TokenManager
) {
    suspend fun loginWithToken(token: String, rememberMe: Boolean): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val oldToken = tokenManager.getAccessToken()
            tokenManager.saveAccessToken(token)
            
            val user = apiService.getCurrentUser()
            tokenManager.saveUsername(user.login)
            tokenManager.saveRememberMe(rememberMe)
            
            Result.success(user)
        } catch (e: Exception) {
            tokenManager.saveAccessToken(null)
            Result.failure(e)
        }
    }

    suspend fun exchangeOAuthCode(clientId: String, code: String, codeVerifier: String, redirectUri: String, rememberMe: Boolean): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.exchangeOAuthToken(
                clientId = clientId,
                code = code,
                codeVerifier = codeVerifier,
                redirectUri = redirectUri
            )
            val token = response.access_token
            if (!token.isNullOrEmpty()) {
                loginWithToken(token, rememberMe)
            } else {
                val errMsg = response.error_description ?: response.error ?: "Failed to retrieve access token"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkAutoLogin(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        val savedToken = tokenManager.getAccessToken()
        val isRemembered = tokenManager.isRememberMe()
        if (!savedToken.isNullOrEmpty() && isRemembered) {
            try {
                val user = apiService.getCurrentUser()
                tokenManager.saveUsername(user.login)
                Result.success(user)
            } catch (e: Exception) {
                tokenManager.clear()
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("No saved active session found"))
        }
    }

    fun logout() {
        tokenManager.clear()
    }
    
    fun getSavedAccessToken(): String? = tokenManager.getAccessToken()
}
