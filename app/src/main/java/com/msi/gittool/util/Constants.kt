package com.msi.gittool.util

import com.msi.gittool.BuildConfig

object Constants {
    
    // Falls back gracefully if build config is unconfigured
    val GITHUB_CLIENT_ID: String = if (BuildConfig.GITHUB_CLIENT_ID.isNullOrEmpty() || BuildConfig.GITHUB_CLIENT_ID == "YOUR_GITHUB_CLIENT_ID") {
        "Ov23liPH9JTGqH82O72w"
    } else {
        BuildConfig.GITHUB_CLIENT_ID
    }

    val GITHUB_CLIENT_SECRET: String = if (BuildConfig.GITHUB_CLIENT_SECRET.isNullOrEmpty() || BuildConfig.GITHUB_CLIENT_SECRET == "YOUR_GITHUB_CLIENT_SECRET") {
        ""
    } else {
        BuildConfig.GITHUB_CLIENT_SECRET
    }

    const val GITHUB_REDIRECT_URI = "gittool://callback"
    const val GITHUB_OAUTH_AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
    const val GITHUB_OAUTH_TOKEN_URL = "https://github.com/login/oauth/access_token"
    
    const val APP_REPO_OWNER = "MSI-Sirajul"
    const val APP_REPO_NAME = "GitTool"
}
