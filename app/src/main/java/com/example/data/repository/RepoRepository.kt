package com.example.data.repository

import com.example.data.remote.GitHubApiService
import com.example.data.remote.GitHubRepo
import com.example.data.remote.GitHubUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepoRepository(
    private val apiService: GitHubApiService
) {
    suspend fun getCurrentUser(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val user = apiService.getCurrentUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRepos(page: Int, perPage: Int = 30): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val repos = apiService.getUserRepos(page = page, perPage = perPage)
            Result.success(repos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
