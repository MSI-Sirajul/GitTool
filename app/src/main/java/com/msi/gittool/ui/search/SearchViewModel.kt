package com.msi.gittool.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.msi.gittool.data.repository.RepoRepository
import com.msi.gittool.data.remote.GitHubRepo
import com.msi.gittool.data.remote.GitHubUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(
        val repos: List<GitHubRepo>, 
        val users: List<GitHubUser>,
        val ownRepos: List<GitHubRepo> = emptyList()
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val repoRepository: RepoRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _popularRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val popularRepos: StateFlow<List<GitHubRepo>> = _popularRepos.asStateFlow()

    private val _isPopularLoading = MutableStateFlow(false)
    val isPopularLoading: StateFlow<Boolean> = _isPopularLoading.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<SearchUiState> = _searchQuery
        .debounce(400)
        .flatMapLatest { query ->
            if (query.trim().length >= 2) {
                flow<SearchUiState> {
                    emit(SearchUiState.Loading)
                    val reposResult = repoRepository.searchRepositories(query)
                    val usersResult = repoRepository.searchUsers(query)
                    
                    val ownRepos = try {
                        repoRepository.getLocalCachedRepos().filter {
                            it.name.contains(query, ignoreCase = true) ||
                            it.full_name.contains(query, ignoreCase = true)
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }

                    if (reposResult.isSuccess || usersResult.isSuccess) {
                        emit(
                            SearchUiState.Success(
                                repos = reposResult.getOrNull() ?: emptyList(),
                                users = usersResult.getOrNull() ?: emptyList(),
                                ownRepos = ownRepos
                            )
                        )
                    } else {
                        emit(SearchUiState.Error("An error occurred during search. Please check network connection."))
                    }
                }
            } else {
                flowOf<SearchUiState>(SearchUiState.Idle)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Idle)

    init {
        loadPopularRepos()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun loadPopularRepos() {
        _isPopularLoading.value = true
        viewModelScope.launch {
            val result = repoRepository.getPopularRepositories()
            _isPopularLoading.value = false
            result.onSuccess { list ->
                _popularRepos.value = list
            }
        }
    }

    fun toggleBookmark(repo: GitHubRepo) {
        viewModelScope.launch {
            repoRepository.toggleBookmark(repo)
        }
    }

    companion object {
        fun Factory(repoRepository: RepoRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(repoRepository) as T
            }
        }
    }
}
