package com.example.data.remote

object GitHubOAuthConfig {
    const val CLIENT_ID = "Ov23liPH9JTGqH82O72w"
    const val REDIRECT_URI = "gittool://callback"
    const val SCOPE = "repo user"
    const val AUTH_ENDPOINT = "https://github.com/login/oauth/authorize"
    const val TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token"
}
