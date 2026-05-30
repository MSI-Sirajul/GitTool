package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.example.data.remote.*
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
    private val context: Context
) {

    suspend fun createRepository(
        name: String,
        description: String?,
        private: Boolean
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
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
