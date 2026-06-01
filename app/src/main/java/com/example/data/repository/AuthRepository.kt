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
            tokenManager.saveIsMockLogin(false)
            
            val user = apiService.getCurrentUser()
            tokenManager.saveUsername(user.login)
            tokenManager.saveRememberMe(rememberMe)
            
            Result.success(user)
        } catch (e: Exception) {
            tokenManager.saveAccessToken(null)
            Result.failure(e)
        }
    }

    suspend fun exchangeOAuthCode(clientId: String, clientSecret: String?, code: String, codeVerifier: String?, redirectUri: String, rememberMe: Boolean): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.exchangeOAuthToken(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                codeVerifier = codeVerifier,
                redirectUri = redirectUri
            )
            val token = response.access_token
            if (!token.isNullOrEmpty()) {
                tokenManager.saveIsMockLogin(false)
                loginWithToken(token, rememberMe)
            } else {
                val errMsg = response.error_description ?: response.error ?: "Failed to retrieve access token"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithCredentials(username: String, password: String, rememberMe: Boolean): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val trimmedUser = username.trim()
            val trimmedPass = password.trim()
            if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
                return@withContext Result.failure(Exception("Username and password cannot be empty"))
            }
            val success = tokenManager.registerLocalUser(trimmedUser, trimmedPass)
            if (success) {
                tokenManager.saveIsMockLogin(true)
                tokenManager.saveUsername(trimmedUser)
                tokenManager.saveAccessToken("mock_pass_$trimmedPass")
                tokenManager.saveRememberMe(rememberMe)

                val user = GitHubUser(
                    login = trimmedUser,
                    id = trimmedUser.hashCode().toLong(),
                    avatar_url = "https://api.dicebear.com/7.x/bottts/svg?seed=$trimmedUser",
                    name = trimmedUser,
                    html_url = "https://github.com/$trimmedUser"
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Incorrect password for this username. Please try a different one!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkAutoLogin(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        val savedToken = tokenManager.getAccessToken()
        val isRemembered = tokenManager.isRememberMe()
        if (!savedToken.isNullOrEmpty() && isRemembered) {
            if (tokenManager.isMockLogin()) {
                val username = tokenManager.getUsername() ?: "local_user"
                val user = GitHubUser(
                    login = username,
                    id = username.hashCode().toLong(),
                    avatar_url = "https://api.dicebear.com/7.x/bottts/svg?seed=$username",
                    name = username,
                    html_url = "https://github.com/$username"
                )
                Result.success(user)
            } else {
                try {
                    val user = apiService.getCurrentUser()
                    tokenManager.saveUsername(user.login)
                    Result.success(user)
                } catch (e: Exception) {
                    tokenManager.clear()
                    Result.failure(e)
                }
            }
        } else {
            Result.failure(Exception("No saved active session found"))
        }
    }

    fun logout() {
        tokenManager.clear()
    }
    
    fun getSavedAccessToken(): String? = tokenManager.getAccessToken()

    fun getOAuthClientId(): String? = tokenManager.getOAuthClientId()
    fun saveOAuthClientId(clientId: String?) = tokenManager.saveOAuthClientId(clientId)

    fun getOAuthClientSecret(): String? = tokenManager.getOAuthClientSecret()
    fun saveOAuthClientSecret(clientSecret: String?) = tokenManager.saveOAuthClientSecret(clientSecret)

    fun getOAuthRedirectUri(): String? = tokenManager.getOAuthRedirectUri()
    fun saveOAuthRedirectUri(redirectUri: String?) = tokenManager.saveOAuthRedirectUri(redirectUri)
}
