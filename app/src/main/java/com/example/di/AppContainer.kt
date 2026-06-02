package com.example.di

import android.content.Context
import com.example.data.local.db.GitToolDatabase
import com.example.data.local.ThemePreferences
import com.example.data.local.TokenManager
import com.example.data.remote.AuthManager
import com.example.data.remote.GitHubApiService
import com.example.data.remote.RetrofitClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.RepoRepository
import com.example.data.repository.UploadRepository

interface AppContainer {
    val tokenManager: TokenManager
    val themePreferences: ThemePreferences
    val apiService: GitHubApiService
    val authRepository: AuthRepository
    val repoRepository: RepoRepository
    val uploadRepository: UploadRepository
    val authManager: AuthManager
    val database: GitToolDatabase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: GitToolDatabase by lazy {
        GitToolDatabase.getDatabase(context)
    }
    
    override val tokenManager: TokenManager by lazy {
        TokenManager(context)
    }

    override val themePreferences: ThemePreferences by lazy {
        ThemePreferences(context)
    }

    override val apiService: GitHubApiService by lazy {
        RetrofitClient.create(tokenManager)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepository(apiService, tokenManager)
    }

    override val repoRepository: RepoRepository by lazy {
        RepoRepository(apiService, tokenManager, database.gitToolDao())
    }

    override val uploadRepository: UploadRepository by lazy {
        UploadRepository(apiService, context, tokenManager)
    }

    override val authManager: AuthManager by lazy {
        AuthManager(context, tokenManager)
    }
}
