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
    val isLoginSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

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
        val clientId = Constants.GITHUB_CLIENT_ID
        val clientSecret = Constants.GITHUB_CLIENT_SECRET

        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "OAuth Client keys are missing! Please configure them in AI Studio Secrets panel, or use Personal Access Token login instead.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.exchangeOAuthCode(
                clientId = clientId,
                clientSecret = clientSecret,
                code = code,
                rememberMe = _uiState.value.rememberMe
            )
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "OAuth Token exchange failed") }
            }
        }
    }

    companion object {
        fun Factory(authRepository: AuthRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(authRepository) as T
            }
        }
    }
}
