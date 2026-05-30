package com.example.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val login: String,
    val id: Long,
    val avatar_url: String?,
    val name: String?,
    val html_url: String?
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long,
    val name: String,
    val full_name: String,
    val private: Boolean,
    val html_url: String,
    val description: String?,
    val stargazers_count: Int,
    val forks_count: Int,
    val language: String?,
    val clone_url: String,
    val default_branch: String? = "main"
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String?,
    val private: Boolean,
    val auto_init: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateBlobRequest(
    val content: String,
    val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class CreateBlobResponse(
    val sha: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class TreeEntry(
    val path: String,
    val mode: String = "100644", // normal file
    val type: String = "blob",
    val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateTreeRequest(
    val base_tree: String? = null,
    val tree: List<TreeEntry>
)

@JsonClass(generateAdapter = true)
data class CreateTreeResponse(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class CreateCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String>
)

@JsonClass(generateAdapter = true)
data class CreateCommitResponse(
    val sha: String
)

@JsonClass(generateAdapter = true)
data class UpdateRefRequest(
    val sha: String,
    val force: Boolean = true
)

@JsonClass(generateAdapter = true)
data class UpdateRefResponse(
    val ref: String,
    val url: String,
    val `object`: RefObject
)

@JsonClass(generateAdapter = true)
data class RefObject(
    val sha: String,
    val type: String,
    val url: String
)

@JsonClass(generateAdapter = true)
data class OAuthTokenRequest(
    val client_id: String,
    val client_secret: String,
    val code: String,
    val redirect_uri: String? = null
)

@JsonClass(generateAdapter = true)
data class OAuthTokenResponse(
    val access_token: String?,
    val token_type: String?,
    val scope: String?,
    val error: String?,
    val error_description: String?
)

@JsonClass(generateAdapter = true)
data class RepoRefResponse(
    val ref: String,
    val `object`: RefObject
)
