package com.example.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.RepoRepository
import com.example.data.remote.GitHubRepo
import com.example.data.remote.GitHubUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RepoListUiState(
    val user: GitHubUser? = null,
    val repos: List<GitHubRepo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = true
)

class RepoListViewModel(
    private val repoRepository: RepoRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoListUiState())
    val uiState: StateFlow<RepoListUiState> = _uiState.asStateFlow()

    init {
        loadUserAndRepos()
    }

    fun loadUserAndRepos() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val userResult = repoRepository.getCurrentUser()
            userResult.onSuccess { user ->
                _uiState.update { it.copy(user = user) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Profile check failed: ${error.message}") }
            }

            loadRepos(page = 1, append = false)
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        viewModelScope.launch {
            val userResult = repoRepository.getCurrentUser()
            userResult.onSuccess { user ->
                _uiState.update { it.copy(user = user) }
            }
            loadRepos(page = 1, append = false)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isRefreshing || !currentState.hasMore) return
        val nextPage = currentState.page + 1
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            loadRepos(page = nextPage, append = true)
        }
    }

    private suspend fun loadRepos(page: Int, append: Boolean) {
        val result = repoRepository.getUserRepos(page = page)
        result.onSuccess { fetchedRepos ->
            _uiState.update { state ->
                val newRepos = if (append) state.repos + fetchedRepos else fetchedRepos
                state.copy(
                    repos = newRepos,
                    isLoading = false,
                    page = page,
                    hasMore = fetchedRepos.size >= 30
                )
            }
        }.onFailure { error ->
            _uiState.update { it.copy(
                isLoading = false,
                errorMessage = error.message ?: "Failed to load repositories"
            ) }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    companion object {
        fun Factory(repoRepository: RepoRepository, authRepository: AuthRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RepoListViewModel(repoRepository, authRepository) as T
            }
        }
    }
}
