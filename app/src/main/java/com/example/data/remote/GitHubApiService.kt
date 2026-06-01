package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface GitHubApiService {

    @GET("user")
    suspend fun getCurrentUser(): GitHubUser

    @GET("user/repos")
    suspend fun getUserRepos(
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int,
        @Query("sort") sort: String = "updated"
    ): List<GitHubRepo>

    @POST("user/repos")
    suspend fun createRepo(
        @Body request: CreateRepoRequest
    ): GitHubRepo

    @POST("https://github.com/login/oauth/access_token")
    @FormUrlEncoded
    @Headers("Accept: application/json")
    suspend fun exchangeOAuthToken(
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("redirect_uri") redirectUri: String
    ): OAuthTokenResponse

    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateBlobRequest
    ): CreateBlobResponse

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateTreeRequest
    ): CreateTreeResponse

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateCommitRequest
    ): CreateCommitResponse

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    suspend fun updateReference(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
        @Body request: UpdateRefRequest
    ): UpdateRefResponse

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getReference(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): RepoRefResponse

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createReference(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateRefRequest
    ): RepoRefResponse
}

@JsonClass(generateAdapter = true)
data class CreateRefRequest(
    val ref: String,
    val sha: String
)
