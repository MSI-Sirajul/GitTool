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
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
            return@withContext Result.failure(Exception("Username and password cannot be empty"))
        }

        try {
            // 1. Construct Basic Auth header
            val basicHeader = "Basic " + android.util.Base64.encodeToString(
                "$trimmedUser:$trimmedPass".toByteArray(),
                android.util.Base64.NO_WRAP
            )

            // 2. Query user details using Basic Auth to verify credentials
            val user = apiService.getUserWithBasicAuth(basicHeader)

            // 3. Attempt token creation via POST /authorizations
            var finalToken: String? = null
            try {
                val authResponse = apiService.createAuthorization(
                    basicHeader,
                    com.example.data.remote.CreateAuthBody(
                        scopes = listOf("repo", "user"),
                        note = "GitTool Login (${System.currentTimeMillis() / 1000})"
                    )
                )
                finalToken = authResponse.token
            } catch (authException: Exception) {
                // If token creation fails but credentials were valid, check if they already provided a PAT in the password field
                if (trimmedPass.startsWith("ghp_") || trimmedPass.startsWith("github_pat_") || trimmedPass.length >= 35) {
                    finalToken = trimmedPass
                } else {
                    return@withContext Result.failure(Exception(
                        "Password login is discontinued by GitHub. Two-factor authentication or API policies on this account prevent automated token generation. Please use a Personal Access Token (PAT) with 'repo' and 'user' scopes, and paste it into the Password field instead."
                    ))
                }
            }

            if (!finalToken.isNullOrEmpty()) {
                tokenManager.saveIsMockLogin(false)
                tokenManager.saveAccessToken(finalToken)
                tokenManager.saveUsername(user.login)
                tokenManager.saveRememberMe(rememberMe)
                Result.success(user)
            } else {
                Result.failure(Exception("Could not retrieve a valid access token. Please log in using your Personal Access Token directly."))
            }

        } catch (e: Exception) {
            if (e is retrofit2.HttpException) {
                val otpHeader = e.response()?.headers()?.get("X-GitHub-OTP")
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                
                if (otpHeader != null || errorBody.contains("two-factor") || errorBody.contains("OTP", ignoreCase = true)) {
                    Result.failure(Exception("Two-factor authentication enabled. Please use a personal access token instead."))
                } else if (e.code() == 401) {
                    Result.failure(Exception("Incorrect username or password."))
                } else {
                    Result.failure(Exception("GitHub returned error: ${e.message()} (Code: ${e.code()})"))
                }
            } else {
                Result.failure(e)
            }
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
