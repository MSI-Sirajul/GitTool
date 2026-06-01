package com.example.data.repository

import com.example.data.local.TokenManager
import com.example.data.remote.GitHubApiService
import com.example.data.remote.GitHubRepo
import com.example.data.remote.GitHubUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RepoRepository(
    private val apiService: GitHubApiService,
    private val tokenManager: TokenManager
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val repoListType = Types.newParameterizedType(List::class.java, GitHubRepo::class.java)
    private val listAdapter = moshi.adapter<List<GitHubRepo>>(repoListType)

    suspend fun getCurrentUser(): Result<GitHubUser> = withContext(Dispatchers.IO) {
        if (tokenManager.isMockLogin()) {
            val username = tokenManager.getUsername() ?: "local_user"
            return@withContext Result.success(
                GitHubUser(
                    login = username,
                    id = username.hashCode().toLong(),
                    avatar_url = "https://api.dicebear.com/7.x/bottts/svg?seed=$username",
                    name = username,
                    html_url = "https://github.com/$username"
                )
            )
        }
        try {
            val user = apiService.getCurrentUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRepos(page: Int, perPage: Int = 30): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        if (tokenManager.isMockLogin()) {
            val username = tokenManager.getUsername() ?: "local_user"
            val json = tokenManager.getLocalUserReposJson(username)
            val reposList = if (json.isNullOrEmpty()) {
                val initialRepos = listOf(
                    GitHubRepo(1, "gittool-companion", "Companion tool for uploading repositories to GitHub with dynamic Material 3 custom animations", false, "https://github.com/$username/gittool-companion", "Modern Jetpack Compose app", 42, 12, "Kotlin", "https://github.com/$username/gittool-companion.git"),
                    GitHubRepo(2, "esoteric-compiler-rust", "ESOLANG programming syntax parser and compiler constructed in safe systems Rust language", false, "https://github.com/$username/esoteric-compiler-rust", "Frictionless Rust parsing tool", 112, 11, "Rust", "https://github.com/$username/esoteric-compiler-rust.git"),
                    GitHubRepo(3, "private-project-vault", "Confidential repository housing personal credential logs and advanced system configurations", true, "https://github.com/$username/private-project-vault", "Private configurations catalog", 3, 0, "Python", "https://github.com/$username/private-project-vault.git")
                )
                saveLocalUserRepos(username, initialRepos)
                initialRepos
            } else {
                deserializeLocalUserRepos(json)
            }
            return@withContext Result.success(reposList)
        }
        try {
            val repos = apiService.getUserRepos(page = page, perPage = perPage)
            Result.success(repos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveLocalUserRepos(username: String, repos: List<GitHubRepo>) {
        try {
            val json = listAdapter.toJson(repos)
            tokenManager.saveLocalUserReposJson(username, json)
        } catch (e: Exception) {
            // Logger fallback
        }
    }

    fun deserializeLocalUserRepos(json: String): List<GitHubRepo> {
        return try {
            listAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
