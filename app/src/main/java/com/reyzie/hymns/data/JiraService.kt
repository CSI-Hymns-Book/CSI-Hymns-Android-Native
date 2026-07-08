package com.reyzie.hymns.data

import android.util.Log
import com.reyzie.hymns.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Base64
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.*
import java.util.concurrent.TimeoutException

data class JiraTicketResult(
    val success: Boolean,
    val ticketKey: String? = null,
    val ticketUrl: String? = null,
    val errorMessage: String? = null
)

class JiraService {

    private val client = HttpClient(Android) {
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
    }

    private val isConfigured: Boolean
        get() {
            return BuildConfig.JIRA_URL.isNotEmpty() &&
                   BuildConfig.JIRA_EMAIL.isNotEmpty() &&
                   BuildConfig.JIRA_API_TOKEN.isNotEmpty() &&
                   BuildConfig.JIRA_PROJECT_KEY.isNotEmpty()
        }

    suspend fun createTicket(
        songType: String,
        songNumber: Int,
        songTitle: String,
        description: String?,
        appVersion: String,
        guestDeviceId: String? = null
    ): JiraTicketResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext JiraTicketResult(false, errorMessage = "Jira is not configured in app build")
        }

        try {
            val url = BuildConfig.JIRA_URL
            val email = BuildConfig.JIRA_EMAIL
            val apiToken = BuildConfig.JIRA_API_TOKEN
            val projectKey = BuildConfig.JIRA_PROJECT_KEY
            val issueTypeConfig = "Task" // Fallback to Task by default for simplicity

            val credentials = Base64.encodeToString("$email:$apiToken".toByteArray(), Base64.NO_WRAP)

            val apiUrl = if (url.endsWith("/")) "${url}rest/api/3/issue" else "$url/rest/api/3/issue"

            val summary = "$songType $songNumber Lyrics Issue"
            val ticketDescription = """
                *Song Information:*
                * Type: $songType
                * Number: $songNumber
                * Title: $songTitle
                
                *App Information:*
                * Version: $appVersion
                
                *Issue Description:*
                ${description?.takeIf { it.isNotBlank() } ?: "No description provided"}
                
                *Reported via:* CSI Hymns App Android
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("project", JSONObject().apply { put("key", projectKey) })
                    put("summary", summary)
                    put("issuetype", JSONObject().apply { put("name", issueTypeConfig) })
                    put("description", JSONObject().apply {
                        put("type", "doc")
                        put("version", 1)
                        put("content", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "paragraph")
                                put("content", org.json.JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put("text", ticketDescription)
                                    })
                                })
                            })
                        })
                    })
                    put("labels", org.json.JSONArray().apply {
                        put("lyrics-issue")
                        put("app-reported")
                        put("android-app")
                    })
                })
            }

            val response: HttpResponse = client.post(apiUrl) {
                header("Authorization", "Basic $credentials")
                header("Content-Type", "application/json")
                header("Accept", "application/json")
                setBody(requestBody.toString())
            }

            if (response.status.value == 201) {
                val responseData = JSONObject(response.bodyAsText())
                val ticketKey = responseData.getString("key")
                val ticketUrl = if (url.endsWith("/")) "${url}browse/$ticketKey" else "$url/browse/$ticketKey"

                // Save to Supabase
                saveTicketToSupabase(
                    ticketKey = ticketKey,
                    ticketUrl = ticketUrl,
                    songType = songType,
                    songNumber = songNumber,
                    songTitle = songTitle,
                    description = description,
                    appVersion = appVersion,
                    guestDeviceId = guestDeviceId
                )

                return@withContext JiraTicketResult(true, ticketKey, ticketUrl)
            } else {
                return@withContext JiraTicketResult(false, errorMessage = "Failed to create ticket: ${response.status}")
            }
        } catch (e: Exception) {
            Log.e("JiraService", "Error creating ticket", e)
            return@withContext JiraTicketResult(false, errorMessage = "Network error or exception: ${e.message}")
        }
    }

    suspend fun syncTicketStatus(ticketKey: String) = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext
        try {
            val url = BuildConfig.JIRA_URL
            val email = BuildConfig.JIRA_EMAIL
            val apiToken = BuildConfig.JIRA_API_TOKEN
            val credentials = Base64.encodeToString("$email:$apiToken".toByteArray(), Base64.NO_WRAP)
            val apiUrl = if (url.endsWith("/")) "${url}rest/api/3/issue/$ticketKey"
            else "$url/rest/api/3/issue/$ticketKey"

            val response: HttpResponse = client.get(apiUrl) {
                header("Authorization", "Basic $credentials")
                header("Accept", "application/json")
            }
            if (response.status.value != 200) return@withContext

            val data = JSONObject(response.bodyAsText())
            val status = data.optJSONObject("fields")?.optJSONObject("status")
            val statusName = status?.optString("name") ?: return@withContext
            val statusId = status?.optString("id")?.takeIf { it.isNotEmpty() }

            SupabaseService.getInstance().updateJiraTicketStatus(
                ticketKey = ticketKey,
                statusName = statusName,
                statusId = statusId
            )
        } catch (e: Exception) {
            Log.e("JiraService", "Error syncing ticket $ticketKey", e)
        }
    }

    private suspend fun saveTicketToSupabase(
        ticketKey: String,
        ticketUrl: String,
        songType: String,
        songNumber: Int,
        songTitle: String,
        description: String?,
        appVersion: String,
        guestDeviceId: String? = null
    ) {
        try {
            val supabase = SupabaseService.getInstance()
            val userId = supabase.currentUser?.id
            supabase.createJiraTicket(
                ticketKey = ticketKey,
                ticketUrl = ticketUrl,
                songType = songType,
                songNumber = songNumber,
                songTitle = songTitle,
                description = description,
                appVersion = appVersion,
                userId = userId,
                deviceId = if (userId == null) guestDeviceId else null
            )
        } catch (e: Exception) {
            Log.e("JiraService", "Failed to save ticket to Supabase", e)
        }
    }

    suspend fun addComment(ticketKey: String, commentText: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        try {
            val url = BuildConfig.JIRA_URL
            val email = BuildConfig.JIRA_EMAIL
            val apiToken = BuildConfig.JIRA_API_TOKEN
            val credentials = Base64.encodeToString("$email:$apiToken".toByteArray(), Base64.NO_WRAP)
            
            val apiUrl = if (url.endsWith("/")) "${url}rest/api/3/issue/$ticketKey/comment"
            else "$url/rest/api/3/issue/$ticketKey/comment"

            val commentTextWithTag = "$commentText\n\n[via CSI Android App]"

            val requestBody = JSONObject().apply {
                put("body", JSONObject().apply {
                    put("type", "doc")
                    put("version", 1)
                    put("content", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "paragraph")
                            put("content", org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", commentTextWithTag)
                                })
                            })
                        })
                    })
                })
            }

            val response: HttpResponse = client.post(apiUrl) {
                header("Authorization", "Basic $credentials")
                header("Content-Type", "application/json")
                header("Accept", "application/json")
                setBody(requestBody.toString())
            }

            response.status.value == 201
        } catch (e: Exception) {
            Log.e("JiraService", "Error adding comment to $ticketKey", e)
            false
        }
    }

    suspend fun syncTicketComments(ticketId: String, ticketKey: String) = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext
        try {
            val url = BuildConfig.JIRA_URL
            val email = BuildConfig.JIRA_EMAIL
            val apiToken = BuildConfig.JIRA_API_TOKEN
            val credentials = Base64.encodeToString("$email:$apiToken".toByteArray(), Base64.NO_WRAP)
            val apiUrl = if (url.endsWith("/")) "${url}rest/api/3/issue/$ticketKey/comment"
            else "$url/rest/api/3/issue/$ticketKey/comment"

            val response: HttpResponse = client.get(apiUrl) {
                header("Authorization", "Basic $credentials")
                header("Accept", "application/json")
            }
            if (response.status.value != 200) return@withContext

            val data = JSONObject(response.bodyAsText())
            val commentsArray = data.optJSONArray("comments") ?: return@withContext
            
            val supabase = SupabaseService.getInstance()
            
            for (i in 0 until commentsArray.length()) {
                val commentObj = commentsArray.optJSONObject(i) ?: continue
                val bodyObj = commentObj.optJSONObject("body") ?: continue
                val text = parseJiraDocText(bodyObj)
                if (text.isEmpty()) continue

                if (text.contains("[via CSI Android App]")) {
                    continue
                }

                val existing = supabase.client.from("ticket_messages")
                    .select {
                        filter {
                            eq("ticket_key", ticketKey)
                            eq("sender", "admin")
                            eq("message", text)
                        }
                    }.decodeList<TicketMessage>()
                
                if (existing.isEmpty()) {
                    val msg = TicketMessage(
                        ticketId = ticketId,
                        ticketKey = ticketKey,
                        sender = "admin",
                        message = text
                    )
                    supabase.client.from("ticket_messages").insert(msg)
                }
            }
        } catch (e: Exception) {
            Log.e("JiraService", "Error syncing comments for $ticketKey", e)
        }
    }

    private fun parseJiraDocText(bodyObj: JSONObject): String {
        val sb = java.lang.StringBuilder()
        val contentArray = bodyObj.optJSONArray("content")
        if (contentArray != null) {
            for (i in 0 until contentArray.length()) {
                val block = contentArray.optJSONObject(i) ?: continue
                val innerContent = block.optJSONArray("content")
                if (innerContent != null) {
                    for (j in 0 until innerContent.length()) {
                        val leaf = innerContent.optJSONObject(j) ?: continue
                        val text = leaf.optString("text")
                        if (text.isNotEmpty()) {
                            sb.append(text)
                        }
                    }
                    sb.append("\n")
                }
            }
        }
        return sb.toString().trim()
    }
}
