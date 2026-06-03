package com.example.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.RepoRepository
import com.example.data.remote.GitHubRepo
import com.example.data.remote.GitHubUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class RepoFilter {
    PUBLIC, BOOKMARKS, PRIVATE
}

data class RepoListUiState(
    val user: GitHubUser? = null,
    val publicRepos: List<GitHubRepo> = emptyList(),
    val privateRepos: List<GitHubRepo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class RepoListViewModel(
    private val repoRepository: RepoRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoListUiState())
    val uiState: StateFlow<RepoListUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(RepoFilter.PUBLIC)
    val currentFilter: StateFlow<RepoFilter> = _currentFilter.asStateFlow()

    private val _showPrivateWarning = MutableStateFlow(false)
    val showPrivateWarning: StateFlow<Boolean> = _showPrivateWarning.asStateFlow()

    private var privateWarningAccepted = false

    val bookmarksFlow: StateFlow<List<GitHubRepo>> = repoRepository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRepos: StateFlow<List<GitHubRepo>> = combine(
        uiState,
        _currentFilter,
        bookmarksFlow
    ) { state, filter, bookmarks ->
        when (filter) {
            RepoFilter.PUBLIC -> state.publicRepos
            RepoFilter.PRIVATE -> state.privateRepos
            RepoFilter.BOOKMARKS -> bookmarks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFilter(filter: RepoFilter) {
        if (filter == RepoFilter.PRIVATE && !privateWarningAccepted) {
            _showPrivateWarning.value = true
        } else {
            _currentFilter.value = filter
        }
    }

    fun onPrivateWarningResult(accepted: Boolean) {
        _showPrivateWarning.value = false
        if (accepted) {
            privateWarningAccepted = true
            _currentFilter.value = RepoFilter.PRIVATE
        }
    }

    fun loadUserAndRepos(context: Context, forceRefresh: Boolean = false) {
        _uiState.update { it.copy(isLoading = !forceRefresh, isRefreshing = forceRefresh, errorMessage = null) }
        viewModelScope.launch {
            // Read User details
            val userResult = repoRepository.getCurrentUser()
            userResult.onSuccess { user ->
                _uiState.update { it.copy(user = user) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Profile load failed: ${error.localizedMessage}") }
            }

            // Retrieve Public Repos
            val publicResult = repoRepository.getUserRepos(isPrivateFeed = false, forceRefresh = forceRefresh, context = context)
            publicResult.onSuccess { repos ->
                _uiState.update { it.copy(publicRepos = repos) }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = "Failed to load public repositories: ${error.localizedMessage}") }
            }

            // Retrieve Private Repos
            val privateResult = repoRepository.getUserRepos(isPrivateFeed = true, forceRefresh = forceRefresh, context = context)
            privateResult.onSuccess { repos ->
                _uiState.update { it.copy(privateRepos = repos) }
            }

            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    fun toggleBookmark(repo: GitHubRepo) {
        viewModelScope.launch {
            repoRepository.toggleBookmark(repo)
        }
    }

    fun isBookmarkedFlow(id: Long): Flow<Boolean> = repoRepository.isBookmarkedFlow(id)

    fun updateProfile(
        name: String?,
        bio: String?,
        blog: String?,
        location: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repoRepository.updateProfile(name, bio, blog, location)
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { updatedUser ->
                _uiState.update { it.copy(user = updatedUser) }
                onResult(true, "Profile updated successfully.")
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Failed to update profile.")
            }
        }
    }

    fun forkRepo(owner: String, repoName: String, context: Context, onResult: (Boolean, String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val notificationId = 1884
            com.example.util.NotificationHelper.showProgressNotification(
                context,
                "Forking Repository",
                "Creating a fork of $owner/$repoName...",
                -1,
                100,
                notificationId
            )
            val result = repoRepository.createFork(owner, repoName)
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess { forkRepo ->
                loadUserAndRepos(context, forceRefresh = true)
                com.example.util.NotificationHelper.showNotification(
                    context,
                    "Fork Succeeded",
                    "Forked $owner/$repoName to your account.",
                    notificationId
                )
                onResult(true, "Successfully forked: ${forkRepo.full_name}")
            }.onFailure { error ->
                com.example.util.NotificationHelper.showNotification(
                    context,
                    "Fork Failed",
                    "Could not fork $owner/$repoName: ${error.localizedMessage}",
                    notificationId
                )
                onResult(false, error.localizedMessage ?: "Failed to fork repository.")
            }
        }
    }

    fun importExternalRepo(
        url: String, 
        context: Context, 
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanUrl = url.trim()
            if (cleanUrl.contains("github.com")) {
                val parsed = cleanUrl.substringAfter("github.com/").removeSuffix(".git")
                val parts = parsed.split("/")
                if (parts.size >= 2) {
                    val owner = parts[0]
                    val repo = parts[1]
                    val notificationId = 1885
                    com.example.util.NotificationHelper.showProgressNotification(
                        context,
                        "Importing Repository",
                        "Importing $owner/$repo to your repositories...",
                        -1,
                        100,
                        notificationId
                    )
                    forkRepo(owner, repo, context) { success, msg ->
                        onResult(success, msg)
                    }
                    return@launch
                }
            }
            onResult(false, "Unrecognized or external Git import URLs are current Web beta items. Please support github.com URLs.")
        }
    }

    fun deleteRepository(context: Context, owner: String, repoName: String, onResult: (Boolean, String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val notificationId = 1883
            com.example.util.NotificationHelper.showProgressNotification(
                context,
                "Deleting Repository",
                "Deleting $owner/$repoName...",
                -1,
                100,
                notificationId
            )
            val result = repoRepository.deleteRepo(owner, repoName, context)
            _uiState.update { it.copy(isLoading = false) }
            result.onSuccess {
                loadUserAndRepos(context, forceRefresh = true)
                com.example.util.NotificationHelper.showNotification(
                    context,
                    "Repository Deleted",
                    "Successfully deleted $owner/$repoName from your accounts.",
                    notificationId
                )
                onResult(true, "Successfully deleted $owner/$repoName")
            }.onFailure { error ->
                com.example.util.NotificationHelper.showNotification(
                    context,
                    "Delete Failed",
                    "Could not delete $owner/$repoName: ${error.localizedMessage}",
                    notificationId
                )
                onResult(false, error.localizedMessage ?: "Failed to delete repository.")
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun downloadRepositoryZip(context: Context, owner: String, repoName: String, branch: String) {
        viewModelScope.launch {
            repoRepository.downloadRepoZip(context, owner, repoName, branch)
        }
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
