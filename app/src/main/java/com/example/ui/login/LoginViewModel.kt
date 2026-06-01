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
    val usernameInput: String = "",
    val passwordInput: String = ""
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateTokenInput(token: String) {
        _uiState.update { it.copy(tokenInput = token, errorMessage = null) }
    }

    fun updateUsernameInput(username: String) {
        _uiState.update { it.copy(usernameInput = username, errorMessage = null) }
    }

    fun updatePasswordInput(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
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

    fun loginWithCredentials() {
        val username = _uiState.value.usernameInput.trim()
        val password = _uiState.value.passwordInput.trim()

        if (username.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Username and password cannot be empty") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.loginWithCredentials(username, password, _uiState.value.rememberMe)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Login failed") }
            }
        }
    }

    fun handleOAuthRedirectCode(code: String) {
        val clientId = Constants.GITHUB_CLIENT_ID
        val clientSecret = null
        val redirectUri = Constants.GITHUB_REDIRECT_URI
        val verifier = savedCodeVerifier

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
        val clientId = Constants.GITHUB_CLIENT_ID
        val redirectUri = Constants.GITHUB_REDIRECT_URI

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
