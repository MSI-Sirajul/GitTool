package com.example.util

import com.example.BuildConfig

object Constants {
    
    // Falls back gracefully if build config is unconfigured
    val GITHUB_CLIENT_ID: String = if (BuildConfig.GITHUB_CLIENT_ID.isNullOrEmpty() || BuildConfig.GITHUB_CLIENT_ID == "YOUR_GITHUB_CLIENT_ID") {
        "Ov23liPH9JTGqH82O72w"
    } else {
        BuildConfig.GITHUB_CLIENT_ID
    }

    const val GITHUB_REDIRECT_URI = "gittool://callback"
    const val GITHUB_OAUTH_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
    const val GITHUB_OAUTH_TOKEN_URL = "https://github.com/login/oauth/access_token"
}
