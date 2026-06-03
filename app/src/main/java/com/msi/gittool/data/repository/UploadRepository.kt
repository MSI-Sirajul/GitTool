package com.msi.gittool.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.msi.gittool.data.local.TokenManager
import com.msi.gittool.data.remote.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class ProjectFile(
    val relativePath: String,
    val fileUri: Uri,
    val size: Long
)

class UploadRepository(
    private val apiService: GitHubApiService,
    private val context: Context,
    private val tokenManager: TokenManager
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val repoListType = Types.newParameterizedType(List::class.java, GitHubRepo::class.java)
    private val listAdapter = moshi.adapter<List<GitHubRepo>>(repoListType)

    private fun addRepoToLocalList(username: String, repo: GitHubRepo) {
        try {
            val json = tokenManager.getLocalUserReposJson(username)
            val currentList = if (json.isNullOrEmpty()) {
                mutableListOf(
                    GitHubRepo(1, "gittool-companion", "Companion tool for uploading repositories to GitHub with dynamic Material 3 custom animations", false, "https://github.com/$username/gittool-companion", "Modern Jetpack Compose app", 42, 12, "Kotlin", "https://github.com/$username/gittool-companion.git"),
                    GitHubRepo(2, "esoteric-compiler-rust", "ESOLANG programming syntax parser and compiler constructed in safe systems Rust language", false, "https://github.com/$username/esoteric-compiler-rust", "Frictionless Rust parsing tool", 112, 11, "Rust", "https://github.com/$username/esoteric-compiler-rust.git"),
                    GitHubRepo(3, "private-project-vault", "Confidential repository housing personal credential logs and advanced system configurations", true, "https://github.com/$username/private-project-vault", "Private configurations catalog", 3, 0, "Python", "https://github.com/$username/private-project-vault.git")
                )
            } else {
                listAdapter.fromJson(json)?.toMutableList() ?: mutableListOf()
            }
            currentList.add(0, repo)
            tokenManager.saveLocalUserReposJson(username, listAdapter.toJson(currentList))
        } catch (e: Exception) {
            // safe fallback
        }
    }

    suspend fun createRepository(
        name: String,
        description: String?,
        private: Boolean
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        if (tokenManager.isMockLogin()) {
            val username = tokenManager.getUsername() ?: "local_user"
            val newRepo = GitHubRepo(
                id = System.currentTimeMillis(),
                name = name,
                full_name = "$username/$name",
                private = private,
                html_url = "https://github.com/$username/$name",
                description = description,
                stargazers_count = 0,
                forks_count = 0,
                language = "Kotlin",
                clone_url = "https://github.com/$username/$name.git"
            )
            addRepoToLocalList(username, newRepo)
            return@withContext Result.success(newRepo)
        }
        try {
            val response = apiService.createRepo(
                CreateRepoRequest(
                    name = name,
                    description = description,
                    private = private,
                    auto_init = true // Guarantee branch 'main' exists
                )
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanProjectFolder(rootTreeUri: Uri): List<ProjectFile> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<ProjectFile>()
        val rootDir = DocumentFile.fromTreeUri(context, rootTreeUri)
        if (rootDir != null && rootDir.exists() && rootDir.isDirectory) {
            scanDirRecursive(rootDir, "", resultList)
        }
        resultList
    }

    private fun scanDirRecursive(
        currentDir: DocumentFile,
        currentPath: String,
        resultList: MutableList<ProjectFile>
    ) {
        val files = currentDir.listFiles()
        for (file in files ?: emptyArray()) {
            val name = file.name ?: continue
            // Ignore build files, hidden files, and giant dependencies
            if (name.startsWith(".") || 
                name == "build" || 
                name == "node_modules" || 
                name == "bin" || 
                name == "obj" || 
                name == "out" || 
                name == ".gradle" || 
                name == ".idea" || 
                name == ".git") {
                continue
            }
            
            val relativePath = if (currentPath.isEmpty()) name else "$currentPath/$name"
            if (file.isDirectory) {
                scanDirRecursive(file, relativePath, resultList)
            } else {
                // Ignore files strictly above 100MB (GitHub's hard limit for standard blob API)
                if (file.length() <= 100 * 1024 * 1024) {
                    resultList.add(ProjectFile(relativePath, file.uri, file.length()))
                }
            }
        }
    }

    suspend fun uploadProject(
        owner: String,
        repo: String,
        files: List<ProjectFile>,
        onProgress: (stage: String, progress: Float, count: Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        if (tokenManager.isMockLogin()) {
            try {
                onProgress("Initializing local vault upload...", 0.05f, 0)
                var uploadedCount = 0
                for (file in files) {
                    uploadedCount++
                    val stageMsg = "Uploading files ($uploadedCount/${files.size})"
                    val uploadProgress = 0.05f + ((uploadedCount.toFloat() / files.size) * 0.80f)
                    onProgress(stageMsg, uploadProgress, uploadedCount)
                    kotlinx.coroutines.delay(100)
                }
                onProgress("Assembling secure archive structural tree...", 0.88f, uploadedCount)
                kotlinx.coroutines.delay(200)
                onProgress("Composing repository commit hashes...", 0.92f, uploadedCount)
                kotlinx.coroutines.delay(200)
                onProgress("Committing modifications...", 0.95f, uploadedCount)
                kotlinx.coroutines.delay(200)
                onProgress("Syncing remote configurations...", 0.98f, uploadedCount)
                kotlinx.coroutines.delay(200)
                onProgress("Successfully completed!", 1.0f, uploadedCount)
                return@withContext Result.success("MOCK_SUCCESS")
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        try {
            onProgress("Initializing upload...", 0.05f, 0)
            val treeEntries = mutableListOf<TreeEntry>()
            var uploadedCount = 0

            // Upload all blobs one by one
            for (file in files) {
                val stageMsg = "Uploading files (${uploadedCount + 1}/${files.size})"
                val base64 = readFileAsBase64(file.fileUri) ?: throw Exception("Failed to read ${file.relativePath}")
                
                // Create Blob on GitHub
                val blobResponse = apiService.createBlob(
                    owner = owner,
                    repo = repo,
                    request = CreateBlobRequest(content = base64)
                )

                // Add to tree entries list
                treeEntries.add(
                    TreeEntry(
                        path = file.relativePath,
                        sha = blobResponse.sha
                    )
                )

                uploadedCount++
                val uploadProgress = 0.05f + ((uploadedCount.toFloat() / files.size) * 0.80f)
                onProgress(stageMsg, uploadProgress, uploadedCount)
            }

            // Create Git Tree containing our newly created blobs
            onProgress("Creating project structure tree...", 0.88f, uploadedCount)
            val treeResponse = apiService.createTree(
                owner = owner,
                repo = repo,
                request = CreateTreeRequest(tree = treeEntries)
            )

            // Retrieve parent commit SHA to keep history linear (since auto_init created the first commit with README)
            onProgress("Assembling commit history...", 0.92f, uploadedCount)
            val parents = try {
                val refResponse = apiService.getReference(owner, repo, "main")
                listOf(refResponse.`object`.sha)
            } catch (e: Exception) {
                // Fallback: If 'main' ref fails, try empty list representing a fresh root commit
                emptyList()
            }

            // Create commit
            onProgress("Submitting commit changes...", 0.95f, uploadedCount)
            val commitResponse = apiService.createCommit(
                owner = owner,
                repo = repo,
                request = CreateCommitRequest(
                    message = "Initial upload of local project files via GitTool",
                    tree = treeResponse.sha,
                    parents = parents
                )
            )

            // Point 'main' branch HEAD references to this brand new commit
            onProgress("Publishing repository references...", 0.98f, uploadedCount)
            try {
                // Try updating existing ref first
                apiService.updateReference(
                    owner = owner,
                    repo = repo,
                    branch = "main",
                    request = UpdateRefRequest(sha = commitResponse.sha, force = true)
                )
            } catch (e: Exception) {
                // If it doesn't exist yet, construct the reference anew
                apiService.createReference(
                    owner = owner,
                    repo = repo,
                    body = CreateRefRequest(ref = "refs/heads/main", sha = commitResponse.sha)
                )
            }

            onProgress("Repository published successfully!", 1.0f, uploadedCount)
            Result.success("https://github.com/$owner/$repo")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readFileAsBase64(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }
}
