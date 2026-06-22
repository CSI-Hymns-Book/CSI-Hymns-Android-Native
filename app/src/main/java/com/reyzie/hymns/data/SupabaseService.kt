package com.reyzie.hymns.data

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class FavoriteRow(
    @SerialName("item_number") val itemNumber: Int,
    @SerialName("item_type") val itemType: String
)

@Serializable
private data class CustomCategoryRow(
    val id: Int,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
private data class CustomCategorySongRow(
    @SerialName("song_id") val songId: Int,
    @SerialName("song_type") val songType: String,
    @SerialName("created_at") val createdAt: String? = null
)

class SupabaseService private constructor() {
    companion object {
        @Volatile
        private var instance: SupabaseService? = null

        fun getInstance(): SupabaseService {
            return instance ?: synchronized(this) {
                instance ?: SupabaseService().also { instance = it }
            }
        }
    }

    private var _client: SupabaseClient? = null
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("Supabase not initialized")

    val isInitialized: Boolean
        get() = _client != null

    fun init(url: String, anonKey: String) {
        if (url.isBlank() || anonKey.isBlank()) return
        
        _client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            install(Auth) {
                scheme = "com.reyzie.hymns"
                host = "callback"
            }
            install(Postgrest)
            install(Storage)
            install(ComposeAuth)
            httpEngine = Android.create()
        }
    }

    // --- Auth ---

    val authStream: Flow<SessionStatus>
        get() = client.auth.sessionStatus

    val currentUser: UserInfo?
        get() = client.auth.currentUserOrNull()

    suspend fun signInWithGoogle() = withContext(Dispatchers.IO) {
        client.auth.signInWith(Google, redirectUrl = "com.reyzie.hymns://callback")
    }

    suspend fun signInWithEmail(email: String, password: String) = withContext(Dispatchers.IO) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUpWithEmail(email: String, password: String) = withContext(Dispatchers.IO) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun resetPasswordForEmail(email: String) = withContext(Dispatchers.IO) {
        client.auth.resetPasswordForEmail(email)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        client.auth.signOut()
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        try {
            client.postgrest.rpc("delete_user_account")
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error deleting account", e)
        } finally {
            signOut()
        }
    }

    // --- Profile ---

    suspend fun upsertProfile(fullName: String) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.from("users").upsert(
                mapOf(
                    "auth_uid" to user.id,
                    "full_name" to fullName
                )
            )
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error upserting profile", e)
        }
    }

    suspend fun getProfileName(): String? = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext null
        try {
            val response = client.from("users")
                .select(Columns.list("full_name")) {
                    filter {
                        eq("auth_uid", user.id)
                    }
                }
                .decodeSingleOrNull<Map<String, String>>()
            return@withContext response?.get("full_name")
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching profile name", e)
            null
        }
    }

    suspend fun setPrivacyPolicyAcceptedInProfile(value: Int) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        val clamped = if (value == 0) 0 else 1
        try {
            client.from("users").upsert(
                mapOf(
                    "auth_uid" to user.id,
                    "privacy_policy_accepted" to clamped
                )
            )
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error setting privacy policy in profile", e)
        }
    }

    // --- Favorites ---
    
    suspend fun fetchFavorites(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext emptyList()
        try {
            val rows = client.from("favorites")
                .select(Columns.list("item_number", "item_type")) {
                    filter { eq("user_id", user.id) }
                }
                .decodeList<FavoriteRow>()
            return@withContext rows.map {
                mapOf("item_number" to it.itemNumber, "item_type" to it.itemType)
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching favorites", e)
            emptyList()
        }
    }

    suspend fun addFavorite(itemNumber: Int, itemType: String) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.from("favorites").insert(
                mapOf(
                    "user_id" to user.id,
                    "item_number" to itemNumber,
                    "item_type" to itemType
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeFavorite(itemNumber: Int, itemType: String) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.from("favorites").delete {
                filter {
                    eq("user_id", user.id)
                    eq("item_number", itemNumber)
                    eq("item_type", itemType)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // --- Custom Categories ---
    
    suspend fun fetchCustomCategories(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext emptyList()
        try {
            val rows = client.from("custom_categories")
                .select(Columns.list("id", "name", "created_at", "updated_at")) {
                    filter {
                        eq("user_id", user.id)
                        eq("deleted", 0)
                    }
                }
                .decodeList<CustomCategoryRow>()
            return@withContext rows.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "created_at" to (it.createdAt ?: ""),
                    "updated_at" to (it.updatedAt ?: "")
                )
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching custom categories", e)
            emptyList()
        }
    }

    suspend fun createCustomCategory(name: String): Int? = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext null
        try {
            val response = client.from("custom_categories").insert(
                mapOf(
                    "user_id" to user.id,
                    "name" to name
                )
            ) {
                select()
            }.decodeSingle<Map<String, Any>>()
            return@withContext (response["id"] as? Number)?.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun softDeleteCustomCategory(categoryId: Int) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.from("custom_categories").update(
                mapOf("deleted" to 1)
            ) {
                filter {
                    eq("id", categoryId)
                    eq("user_id", user.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun fetchSongsInCategory(categoryId: Int): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext emptyList()
        try {
            val rows = client.from("custom_category_songs")
                .select(Columns.list("song_id", "song_type", "created_at")) {
                    filter {
                        eq("user_id", user.id)
                        eq("category_id", categoryId)
                        eq("deleted", 0)
                    }
                }
                .decodeList<CustomCategorySongRow>()
            return@withContext rows.map {
                mapOf(
                    "song_id" to it.songId,
                    "song_type" to it.songType,
                    "created_at" to (it.createdAt ?: "")
                )
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching category songs", e)
            emptyList()
        }
    }
    
    suspend fun addSongToCategory(categoryId: Int, songId: Int, songType: String) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.from("custom_category_songs").insert(
                mapOf(
                    "category_id" to categoryId,
                    "user_id" to user.id,
                    "song_id" to songId,
                    "song_type" to songType
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun removeSongFromCategory(categoryId: Int, songId: Int, songType: String) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.from("custom_category_songs").delete {
                filter {
                    eq("category_id", categoryId)
                    eq("user_id", user.id)
                    eq("song_id", songId)
                    eq("song_type", songType)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createJiraTicket(
        ticketKey: String,
        ticketUrl: String,
        songType: String,
        songNumber: Int,
        songTitle: String,
        description: String?,
        appVersion: String,
        userId: String?,
        deviceId: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val payload = mutableMapOf<String, Any?>(
                "ticket_key" to ticketKey,
                "ticket_url" to ticketUrl,
                "song_type" to songType,
                "song_number" to songNumber,
                "song_title" to songTitle,
                "description" to description,
                "app_version" to appVersion
            )
            if (userId != null) {
                payload["user_id"] = userId
            } else if (deviceId != null) {
                payload["device_id"] = deviceId
            }
            client.from("jira_tickets").insert(payload)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateJiraTicketStatus(
        ticketKey: String,
        statusName: String,
        statusId: String?
    ) = withContext(Dispatchers.IO) {
        try {
            val update = mutableMapOf<String, Any?>(
                "jira_status" to statusName,
                "updated_at" to java.time.Instant.now().toString()
            )
            if (statusId != null) update["jira_status_id"] = statusId
            client.from("jira_tickets").update(update) {
                filter { eq("ticket_key", ticketKey) }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error updating ticket status for $ticketKey", e)
        }
    }
}
