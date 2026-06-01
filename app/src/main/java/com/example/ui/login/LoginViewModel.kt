package com.example.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val tokenInput: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false,
    val oauthClientIdInput: String = "",
    val oauthClientSecretInput: String = "",
    val oauthRedirectUriInput: String = ""
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        val savedId = authRepository.getOAuthClientId() ?: Constants.GITHUB_CLIENT_ID
        val savedSecret = authRepository.getOAuthClientSecret() ?: ""
        val savedRedirect = authRepository.getOAuthRedirectUri() ?: Constants.GITHUB_REDIRECT_URI

        _uiState.update {
            it.copy(
                oauthClientIdInput = savedId,
                oauthClientSecretInput = savedSecret,
                oauthRedirectUriInput = savedRedirect
            )
        }
    }

    fun updateOauthClientId(id: String) {
        _uiState.update { it.copy(oauthClientIdInput = id, errorMessage = null) }
        authRepository.saveOAuthClientId(id)
    }

    fun updateOauthClientSecret(secret: String) {
        _uiState.update { it.copy(oauthClientSecretInput = secret, errorMessage = null) }
        authRepository.saveOAuthClientSecret(secret)
    }

    fun updateOauthRedirectUri(uri: String) {
        _uiState.update { it.copy(oauthRedirectUriInput = uri, errorMessage = null) }
        authRepository.saveOAuthRedirectUri(uri)
    }

    fun updateTokenInput(token: String) {
        _uiState.update { it.copy(tokenInput = token, errorMessage = null) }
    }

    fun updateRememberMe(remember: Boolean) {
        _uiState.update { it.copy(rememberMe = remember) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loginWithToken() {
        val token = _uiState.value.tokenInput.trim()
        if (token.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a personal access token") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.loginWithToken(token, _uiState.value.rememberMe)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Authentication failed") }
            }
        }
    }

    fun handleOAuthRedirectCode(code: String) {
        val clientId = _uiState.value.oauthClientIdInput.trim()
        val clientSecret = _uiState.value.oauthClientSecretInput.trim().ifEmpty { null }
        val redirectUri = _uiState.value.oauthRedirectUriInput.trim().ifEmpty { Constants.GITHUB_REDIRECT_URI }
        val verifier = savedCodeVerifier

        if (clientId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "GitHub Client ID is missing!") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.exchangeOAuthCode(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                codeVerifier = verifier,
                redirectUri = redirectUri,
                rememberMe = _uiState.value.rememberMe
            )
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "OAuth Token exchange failed") }
            }
        }
    }

    fun startOAuthFlow(onTriggerUrl: (String) -> Unit) {
        clearError()
        val clientId = _uiState.value.oauthClientIdInput.trim()
        val redirectUri = _uiState.value.oauthRedirectUriInput.trim().ifEmpty { Constants.GITHUB_REDIRECT_URI }

        if (clientId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "GitHub client ID is required.") }
            return
        }
        val verifier = com.example.util.PkceUtil.generateCodeVerifier()
        savedCodeVerifier = verifier
        
        val challenge = com.example.util.PkceUtil.generateCodeChallenge(verifier)
        val url = "${Constants.GITHUB_OAUTH_AUTHORIZE_URL}?client_id=$clientId" +
                "&redirect_uri=$redirectUri" +
                "&scope=repo,user" +
                "&code_challenge=$challenge" +
                "&code_challenge_method=S256"
        onTriggerUrl(url)
    }

    companion object {
        var savedCodeVerifier: String? = null

        fun Factory(authRepository: AuthRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(authRepository) as T
            }
        }
    }
}
