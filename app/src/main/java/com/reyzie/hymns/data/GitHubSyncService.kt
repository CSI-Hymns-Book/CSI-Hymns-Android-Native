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
import kotlinx.serialization.json.contentOrNull
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

            getGitHubToken()?.let { token ->
                pullViaContentsApi(token, repo, filePath)?.let { return@withContext it }
            }

            pullViaRawUrl(repo, filePath)?.let { return@withContext it }

            emptyList()
        } catch (e: Exception) {
            Log.e("GitHubSyncService", "Error pulling from GitHub", e)
            null
        }
    }

    private suspend fun pullViaContentsApi(
        token: String,
        repo: String,
        filePath: String,
    ): List<ChristmasCarol>? {
        val cleanToken = token.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim()
        for (branch in listOf("main", "master")) {
            val response = client.get("https://api.github.com/repos/$repo/contents/$filePath") {
                header("Authorization", "Bearer $cleanToken")
                header("Accept", "application/vnd.github.v3+json")
                parameter("ref", branch)
            }
            if (response.status != HttpStatusCode.OK) continue

            val body = response.body<JsonObject>()
            val encoding = body["encoding"]?.jsonPrimitive?.contentOrNull
            val content = body["content"]?.jsonPrimitive?.contentOrNull ?: continue
            if (encoding != "base64") continue

            val decoded = String(
                Base64.decode(content.replace("\n", ""), Base64.DEFAULT),
                Charset.forName("UTF-8"),
            )
            val parsed = ChristmasCarolParser.parseJsonText(decoded)
            Log.i("GitHubSyncService", "Loaded ${parsed.size} carols via GitHub API ($branch)")
            return parsed
        }
        return null
    }

    private suspend fun pullViaRawUrl(repo: String, filePath: String): List<ChristmasCarol>? {
        for (branch in listOf("main", "master")) {
            val url = "https://raw.githubusercontent.com/$repo/$branch/$filePath"
            val response = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                val parsed = ChristmasCarolParser.parseJsonText(response.bodyAsText())
                Log.i("GitHubSyncService", "Loaded ${parsed.size} carols via raw URL ($branch)")
                return parsed
            }
        }
        return null
    }

    suspend fun pushToGitHub(carols: List<ChristmasCarol>): Boolean = withContext(Dispatchers.IO) {
        try {
            val rawToken = getGitHubToken() ?: return@withContext false
            val token = rawToken.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim()
            val repo = getGitHubRepo()
            val filePath = getFilePath()
            
            val jsonData = Json.encodeToString<List<ChristmasCarol>>(carols)
            val base64Content = Base64.encodeToString(jsonData.toByteArray(), Base64.NO_WRAP)
            
            // Get existing file SHA if it exists
            val existingResponse = client.get("https://api.github.com/repos/$repo/contents/$filePath") {
                header("Authorization", "Bearer $token")
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
                header("Authorization", "Bearer $token")
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

    suspend fun pushFileToGitHub(
        token: String,
        repo: String,
        filePath: String,
        content: String,
        commitMessage: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim()
            val base64Content = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            
            // Get existing file SHA if it exists - wrap in try-catch in case non-2xx throws
            val existingResponse = try {
                client.get("https://api.github.com/repos/$repo/contents/$filePath") {
                    header("Authorization", "Bearer $cleanToken")
                    header("Accept", "application/vnd.github.v3+json")
                    header("User-Agent", "CSI-Hymns-App")
                }
            } catch (e: Exception) {
                null
            }
            
            val sha = if (existingResponse != null && existingResponse.status == HttpStatusCode.OK) {
                existingResponse.body<JsonObject>()["sha"]?.jsonPrimitive?.content
            } else null
            
            val body = buildJsonObject {
                put("message", commitMessage)
                put("content", base64Content)
                if (sha != null) {
                    put("sha", sha)
                }
            }
            
            val putResponse = client.put("https://api.github.com/repos/$repo/contents/$filePath") {
                header("Authorization", "Bearer $cleanToken")
                header("Accept", "application/vnd.github.v3+json")
                header("User-Agent", "CSI-Hymns-App")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            
            if (putResponse.status == HttpStatusCode.OK || putResponse.status == HttpStatusCode.Created) {
                Log.d("GitHubSyncService", "Successfully pushed $filePath to GitHub contents")
                null
            } else {
                val errorResponse = putResponse.bodyAsText()
                val errorMsg = "HTTP ${putResponse.status.value}: $errorResponse"
                Log.e("GitHubSyncService", "Failed to push $filePath: $errorMsg")
                errorMsg
            }
        } catch (e: Exception) {
            Log.e("GitHubSyncService", "Error pushing file $filePath to GitHub", e)
            e.localizedMessage ?: e.toString()
        }
    }
}
