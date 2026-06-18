package com.reyzie.hymns.data

import android.content.Context
import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.charset.Charset

class GitHubSyncService(context: Context) {
    private val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    companion object {
        private const val GITHUB_TOKEN_KEY = "github_token"
        private const val GITHUB_REPO_KEY = "github_repo"
        private const val GITHUB_FILE_PATH_KEY = "github_file_path"
        private const val DEFAULT_REPO = "Reynold29/csi-hymns-vault"
        private const val DEFAULT_FILE_PATH = "carols_data.json"
    }

    fun setGitHubToken(token: String) = prefs.edit().putString(GITHUB_TOKEN_KEY, token).apply()
    fun getGitHubToken(): String? = prefs.getString(GITHUB_TOKEN_KEY, null)
    
    fun setGitHubRepo(repo: String) = prefs.edit().putString(GITHUB_REPO_KEY, repo).apply()
    fun getGitHubRepo(): String = prefs.getString(GITHUB_REPO_KEY, null) ?: DEFAULT_REPO
    
    fun setFilePath(path: String) = prefs.edit().putString(GITHUB_FILE_PATH_KEY, path).apply()
    fun getFilePath(): String = prefs.getString(GITHUB_FILE_PATH_KEY, null) ?: DEFAULT_FILE_PATH

    suspend fun pullFromGitHub(): List<ChristmasCarol>? = withContext(Dispatchers.IO) {
        try {
            val repo = getGitHubRepo()
            val filePath = getFilePath()
            
            // Try fetching raw JSON directly from GitHub raw URL (needs no auth token!)
            val mainUrl = "https://raw.githubusercontent.com/$repo/refs/heads/main/$filePath"
            val response = client.get(mainUrl)
            
            if (response.status == HttpStatusCode.OK) {
                val decodedString = response.bodyAsText()
                return@withContext Json.decodeFromString<List<ChristmasCarol>>(decodedString)
            } else {
                // Try fallback to master branch
                val masterUrl = "https://raw.githubusercontent.com/$repo/master/$filePath"
                val fallbackResponse = client.get(masterUrl)
                if (fallbackResponse.status == HttpStatusCode.OK) {
                    val decodedString = fallbackResponse.bodyAsText()
                    return@withContext Json.decodeFromString<List<ChristmasCarol>>(decodedString)
                }
                if (fallbackResponse.status == HttpStatusCode.NotFound) {
                    return@withContext emptyList()
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GitHubSyncService", "Error pulling from GitHub raw content", e)
            null
        }
    }

    suspend fun pushToGitHub(carols: List<ChristmasCarol>): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getGitHubToken() ?: return@withContext false
            val repo = getGitHubRepo()
            val filePath = getFilePath()
            
            val jsonData = Json.encodeToString<List<ChristmasCarol>>(carols)
            val base64Content = Base64.encodeToString(jsonData.toByteArray(), Base64.NO_WRAP)
            
            // Get existing file SHA if it exists
            val existingResponse = client.get("https://api.github.com/repos/$repo/contents/$filePath") {
                header("Authorization", "token $token")
                header("Accept", "application/vnd.github.v3+json")
            }
            
            val sha = if (existingResponse.status == HttpStatusCode.OK) {
                existingResponse.body<JsonObject>()["sha"]?.jsonPrimitive?.content
            } else null
            
            val body = buildJsonObject {
                put("message", "Update carols data - ${System.currentTimeMillis()}")
                put("content", base64Content)
                if (sha != null) {
                    put("sha", sha)
                }
            }
            
            val putResponse = client.put("https://api.github.com/repos/$repo/contents/$filePath") {
                header("Authorization", "token $token")
                header("Accept", "application/vnd.github.v3+json")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            
            putResponse.status == HttpStatusCode.OK || putResponse.status == HttpStatusCode.Created
        } catch (e: Exception) {
            Log.e("GitHubSyncService", "Error pushing to GitHub", e)
            false
        }
    }
}
