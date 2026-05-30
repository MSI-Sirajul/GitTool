package com.example.util

import com.example.BuildConfig

object Constants {
    
    // Falls back gracefully if build config is unconfigured
    val GITHUB_CLIENT_ID: String = if (BuildConfig.GITHUB_CLIENT_ID.isNullOrEmpty() || BuildConfig.GITHUB_CLIENT_ID == "YOUR_GITHUB_CLIENT_ID") {
        "Ov23liPH9JTGqH82O72w"
    } else {
        BuildConfig.GITHUB_CLIENT_ID
    }

    val GITHUB_CLIENT_SECRET: String = if (BuildConfig.GITHUB_CLIENT_SECRET.isNullOrEmpty() || BuildConfig.GITHUB_CLIENT_SECRET == "YOUR_GITHUB_CLIENT_SECRET") {
        "f9d41219c557078144548b68553fa3ed45232d06"
    } else {
        BuildConfig.GITHUB_CLIENT_SECRET
    }

    const val OAUTH_REDIRECT_URI = "gittool://callback"
    const val OAUTH_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
}
