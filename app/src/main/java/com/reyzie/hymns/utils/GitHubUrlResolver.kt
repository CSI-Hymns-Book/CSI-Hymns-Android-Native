package com.reyzie.hymns.utils

import android.content.Context
import android.util.Log
import com.reyzie.hymns.data.AppConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GitHubUrlResolver {
    private const val TAG = "GitHubUrlResolver"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Cache the commit SHA in-memory for a short time to avoid duplicate network calls
    // during a multi-file sync operation.
    private var cachedSha: String? = null
    private var cacheTime: Long = 0L
    private const val CACHE_DURATION_MS = 60 * 1000L // 1 minute

    suspend fun getLatestCommitSha(context: Context): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedSha != null && (now - cacheTime < CACHE_DURATION_MS)) {
            return@withContext cachedSha
        }

        try {
            val appConfigRepository = AppConfigRepository(context = context)
            val config = appConfigRepository.getCachedRemoteConfig()
            val rawToken = config.githubMidiToken
            val cleanToken = rawToken?.replace("[", "")?.replace("]", "")?.replace("\"", "")?.replace("'", "")?.trim()

            val url = "https://api.github.com/repos/Reynold29/csi-hymns-vault/commits/main"
            val requestBuilder = Request.Builder().url(url)
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .addHeader("Expires", "0")
                .addHeader("User-Agent", "CSI-Hymns-App")
            
            if (!cleanToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "token $cleanToken")
            }

            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val sha = json.getString("sha")
                        if (sha.isNotBlank()) {
                            cachedSha = sha
                            cacheTime = System.currentTimeMillis()
                            return@withContext sha
                        }
                    }
                } else {
                    Log.w(TAG, "Failed to fetch commit SHA: HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest commit SHA", e)
        }
        return@withContext null
    }

    fun resolveRawUrl(url: String, sha: String?): String {
        if (sha.isNullOrBlank()) return url
        return if (url.contains("/refs/heads/main/")) {
            url.replace("/refs/heads/main/", "/$sha/")
        } else {
            url.replace("/main/", "/$sha/")
        }
    }
}
