package com.example.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RepoRepository
import com.example.data.remote.GitHubNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationUiState(
    val notifications: List<GitHubNotification> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class NotificationViewModel(
    private val repoRepository: RepoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications(forceRefresh: Boolean = false) {
        _uiState.update { it.copy(isLoading = !forceRefresh, isRefreshing = forceRefresh, errorMessage = null) }
        viewModelScope.launch {
            val result = repoRepository.getNotifications()
            result.onSuccess { list ->
                _uiState.update { it.copy(notifications = list, isLoading = false, isRefreshing = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = error.localizedMessage ?: "Failed to fetch notifications."
                ) }
            }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            val result = repoRepository.markNotificationAsRead(id)
            result.onSuccess {
                // Update local items state
                _uiState.update { state ->
                    state.copy(notifications = state.notifications.filter { it.id != id })
                }
            }
        }
    }

    fun markAllAsRead() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repoRepository.markAllNotificationsAsRead()
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess {
                _uiState.update { it.copy(notifications = emptyList()) }
            }
        }
    }

    companion object {
        fun Factory(repoRepository: RepoRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationViewModel(repoRepository) as T
            }
        }
    }
}
