package com.example.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GitToolDao {

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks ORDER BY name ASC")
    fun getBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY name ASC")
    suspend fun getBookmarks(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = :id)")
    fun isBookmarkedFlow(id: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = :id)")
    suspend fun isBookmarked(id: Long): Boolean


    // --- Cached Repos ---
    @Query("SELECT * FROM cached_repos WHERE isPrivateList = :isPrivate ORDER BY name ASC")
    suspend fun getCachedRepos(isPrivate: Boolean): List<CachedRepoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRepos(repos: List<CachedRepoEntity>)

    @Query("DELETE FROM cached_repos WHERE isPrivateList = :isPrivate")
    suspend fun clearCachedRepos(isPrivate: Boolean)


    // --- Cached Files & Directories ---
    @Query("SELECT * FROM cached_files WHERE fullName = :fullName AND parentPath = :parentPath ORDER BY type DESC, name ASC")
    suspend fun getCachedDirectoryContents(fullName: String, parentPath: String): List<CachedFileEntity>

    @Query("SELECT * FROM cached_files WHERE pathId = :pathId")
    suspend fun getCachedFile(pathId: String): CachedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedFiles(files: List<CachedFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedFile(file: CachedFileEntity)

    @Query("DELETE FROM cached_files WHERE fullName = :fullName")
    suspend fun clearCachedFilesForRepo(fullName: String)
}
