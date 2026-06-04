package com.msi.gittool.ui.filebrowser

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.msi.gittool.data.repository.RepoRepository
import com.msi.gittool.data.remote.GitHubFileContentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

data class FileViewerUiState(
    val owner: String = "",
    val repo: String = "",
    val path: String = "",
    val contentItem: GitHubFileContentResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val decodedContent: String by lazy {
        val raw = contentItem?.content ?: return@lazy ""
        val clean = raw.replace("\\s".toRegex(), "")
        try {
            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            try {
                val bytes = android.util.Base64.decode(clean, android.util.Base64.URL_SAFE)
                String(bytes, StandardCharsets.UTF_8)
            } catch (e2: Exception) {
                raw
            }
        }
    }

    val isImage: Boolean
        get() {
            val p = path.lowercase()
            return p.endsWith(".png") || p.endsWith(".jpg") || p.endsWith(".jpeg") ||
                    p.endsWith(".gif") || p.endsWith(".webp") || p.endsWith(".bmp")
        }
}

class FileViewerViewModel(
    private val repoRepository: RepoRepository,
    private val owner: String,
    private val repo: String,
    private val path: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileViewerUiState(owner = owner, repo = repo, path = path))
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    fun loadFile(context: Context, forceRefresh: Boolean = false) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repoRepository.getRepoFileContent(owner, repo, path, forceRefresh, context)
            result.onSuccess { contentResponse ->
                _uiState.update { it.copy(contentItem = contentResponse, isLoading = false) }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    errorMessage = error.localizedMessage ?: "Failed to read file contents."
                ) }
            }
        }
    }

    fun downloadFile(context: Context) {
        val state = _uiState.value
        val url = state.contentItem?.download_url ?: return
        val filename = path.substringAfterLast("/")
        viewModelScope.launch {
            repoRepository.downloadSingleFile(context, filename, url)
        }
    }

    fun shareFileContent(context: Context) {
        val state = _uiState.value
        val shareText = "Check out this file on GitHub:\n" +
                (state.contentItem?.html_url ?: "https://github.com/${owner}/${repo}/blob/main/${path}")
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, path)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share File Link"))
    }

    companion object {
        fun Factory(repoRepository: RepoRepository, owner: String, repo: String, path: String): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FileViewerViewModel(repoRepository, owner, repo, path) as T
            }
        }
    }
}
