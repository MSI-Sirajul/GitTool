package com.example.util

import com.example.BuildConfig

object Constants {
    
    // Falls back gracefully if build config is unconfigured
    val GITHUB_CLIENT_ID: String = if (BuildConfig.GITHUB_CLIENT_ID == "YOUR_GITHUB_CLIENT_ID") {
        ""
    } else {
        BuildConfig.GITHUB_CLIENT_ID
    }

    val GITHUB_CLIENT_SECRET: String = if (BuildConfig.GITHUB_CLIENT_SECRET == "YOUR_GITHUB_CLIENT_SECRET") {
        ""
    } else {
        BuildConfig.GITHUB_CLIENT_SECRET
    }

    const val OAUTH_REDIRECT_URI = "gittool://callback"
    const val OAUTH_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
}
