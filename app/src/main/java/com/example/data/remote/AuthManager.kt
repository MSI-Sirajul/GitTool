package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.openid.appauth.*

class AuthManager(
    private val context: Context,
    private val tokenManager: TokenManager
) {
    private val authService = AuthorizationService(context)
    private var authState = AuthState()

    private val _oauthStateFlow = MutableStateFlow<OAuthState>(OAuthState.Idle)
    val oauthStateFlow: StateFlow<OAuthState> = _oauthStateFlow.asStateFlow()

    companion object {
        const val RC_AUTH = 1001
    }

    sealed interface OAuthState {
        object Idle : OAuthState
        object Loading : OAuthState
        data class Success(val accessToken: String) : OAuthState
        data class Error(val message: String) : OAuthState
    }

    fun startOAuth(activity: Activity) {
        _oauthStateFlow.value = OAuthState.Loading
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse(GitHubOAuthConfig.AUTH_ENDPOINT),
            Uri.parse(GitHubOAuthConfig.TOKEN_ENDPOINT)
        )
        
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            GitHubOAuthConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(GitHubOAuthConfig.REDIRECT_URI)
        ).setScope(GitHubOAuthConfig.SCOPE)
         .build()

        val authIntent = authService.getAuthorizationRequestIntent(authRequest)
        activity.startActivityForResult(authIntent, RC_AUTH)
    }

    fun handleAuthorizationResponse(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (requestCode == RC_AUTH) {
            if (data != null) {
                val response = AuthorizationResponse.fromIntent(data)
                val exception = AuthorizationException.fromIntent(data)
                
                if (response != null) {
                    authState.update(response, exception)
                    exchangeCode(response)
                } else {
                    _oauthStateFlow.value = OAuthState.Error(
                        exception?.message ?: "Authorization failed or cancelled"
                    )
                }
            } else {
                _oauthStateFlow.value = OAuthState.Error("No authorization response received")
            }
        }
    }

    private fun exchangeCode(response: AuthorizationResponse) {
        _oauthStateFlow.value = OAuthState.Loading
        authService.performTokenRequest(
            response.createTokenExchangeRequest(),
            NoClientAuthentication.INSTANCE
        ) { tokenResponse, exception ->
            authState.update(tokenResponse, exception)
            if (tokenResponse != null) {
                val token = tokenResponse.accessToken
                if (!token.isNullOrEmpty()) {
                    _oauthStateFlow.value = OAuthState.Success(token)
                } else {
                    _oauthStateFlow.value = OAuthState.Error("Received empty access token from GitHub")
                }
            } else {
                _oauthStateFlow.value = OAuthState.Error(
                    exception?.message ?: "Failed to exchange OAuth code"
                )
            }
        }
    }
}
