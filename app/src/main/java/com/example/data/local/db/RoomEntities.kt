package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val fullName: String,
    val isPrivate: Boolean,
    val htmlUrl: String,
    val description: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String?,
    val cloneUrl: String,
    val defaultBranch: String
)

@Entity(tableName = "cached_repos")
data class CachedRepoEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val fullName: String,
    val isPrivate: Boolean,
    val htmlUrl: String,
    val description: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String?,
    val cloneUrl: String,
    val defaultBranch: String,
    val isPrivateList: Boolean // differentiate cached public vs private main feeds
)

@Entity(tableName = "cached_files")
data class CachedFileEntity(
    @PrimaryKey val pathId: String, // Format: "fullName:path"
    val fullName: String, // owner/repo
    val path: String, // path e.g. "src/main/java"
    val parentPath: String, // path of the parent directory e.g. "src/main" or "" for root
    val name: String, // file or folder name
    val type: String, // "file" or "dir"
    val contentBase64: String?, // files content, null for dir
    val downloadUrl: String? = null // raw github user content url
)
