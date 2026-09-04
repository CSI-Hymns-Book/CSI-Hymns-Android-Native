package com.reyzie.hymns.data

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

@Serializable
private data class UserDeletedRow(
    val deleted: JsonElement? = null
)

@Serializable
data class PaymentGatewayRow(
    val id: String = "",
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
    val description: String? = null,
    @SerialName("edge_function_url") val edgeFunctionUrl: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean = false,
    @SerialName("icon_type") val iconType: String? = null,
    val config: JsonElement? = null
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

        private val prettyJsonCodec = Json { prettyPrint = true; prettyPrintIndent = "  " }

        private const val EXPORT_README = """CSI Hymns — your information

This zip contains the data we store about your account, including user id, name, email, favourites, custom lists, support tickets, and consent records.

No profile picture is stored.
"""
    }

    private var _client: SupabaseClient? = null
    val client: SupabaseClient
        get() = _client ?: throw IllegalStateException("Supabase not initialized")

    val isInitialized: Boolean
        get() = _client != null

    var anonKey: String = ""
        private set

    fun init(url: String, anonKey: String) {
        if (url.isBlank() || anonKey.isBlank()) return
        this.anonKey = anonKey
        
        _client = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = anonKey
        ) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                    isLenient = true
                }
            )
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
                buildJsonObject {
                    put("auth_uid", user.id)
                    put("full_name", fullName)
                }
            ) {
                onConflict = "auth_uid"
            }
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

    suspend fun isAccountDeleted(): Boolean = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext false
        try {
            val result = client.postgrest.rpc("is_my_account_deleted")
            return@withContext try {
                result.decodeAs<Boolean>()
            } catch (_: Exception) {
                parseDeletedFlag(result.data)
            }
        } catch (e: Exception) {
            Log.w("SupabaseService", "is_my_account_deleted RPC failed, falling back to users.deleted", e)
            try {
                val row = client.from("users")
                    .select(Columns.list("deleted")) {
                        filter { eq("auth_uid", user.id) }
                    }
                    .decodeSingleOrNull<UserDeletedRow>()
                jsonElementIsTrue(row?.deleted)
            } catch (fallback: Exception) {
                Log.e("SupabaseService", "Error checking deleted flag", fallback)
                false
            }
        }
    }

    suspend fun exportMyDataJson(): String = withContext(Dispatchers.IO) {
        val result = client.postgrest.rpc("export_my_data")
        try {
            result.decodeAs<String>()
        } catch (_: Exception) {
            result.data
        }
    }

    fun isDataExportRateLimited(error: Throwable): Boolean {
        val message = (error.message ?: error.toString()).lowercase()
        return message.contains("export_rate_limited")
            || message.contains("rate_limited")
            || message.contains("p0001")
    }

    suspend fun exportMyDataZipFile(cacheDir: File): File = withContext(Dispatchers.IO) {
        val json = prettyJson(exportMyDataJson())
        val zipFile = File(cacheDir, "csi-hymns-my-information.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.putNextEntry(ZipEntry("my-data.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(EXPORT_README.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        zipFile
    }

    suspend fun setPrivacyPolicyAcceptedInProfile(value: Int) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        val clamped = if (value == 0) 0 else 1
        try {
            client.from("users").upsert(
                buildJsonObject {
                    put("auth_uid", user.id)
                    put("privacy_policy_accepted", clamped)
                }
            )
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error setting privacy policy in profile", e)
        }
    }

    suspend fun syncConsentArtefact(
        requiredAccepted: Boolean,
        analytics: Boolean,
        push: Boolean,
        version: String?,
        recordedAtIso: String?,
        artefact: Map<String, Any>
    ) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        setPrivacyPolicyAcceptedInProfile(if (requiredAccepted) 1 else 0)
        val iso = recordedAtIso ?: java.time.Instant.now().toString()
        try {
            val artefactJson = buildJsonObject {
                put("policy_version", (artefact["policy_version"] as? String) ?: (version ?: ConsentManager.CURRENT_POLICY_VERSION))
                put("recorded_at", iso)
                put("language", (artefact["language"] as? String) ?: "en")
                put("privacy_accepted", requiredAccepted)
                put("terms_accepted", requiredAccepted)
                put("age_confirmed", requiredAccepted)
                put("analytics", analytics)
                put("push_notifications", push)
                put("notice", (artefact["notice"] as? String) ?: "")
            }
            client.from("users").update(
                buildJsonObject {
                    put("terms_accepted", if (requiredAccepted) 1 else 0)
                    put("analytics_consent", analytics)
                    put("push_consent", push)
                    if (version != null) put("consent_version", version)
                    put("consent_recorded_at", iso)
                    put("consent_artefact", artefactJson)
                }
            ) {
                filter { eq("auth_uid", user.id) }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "syncConsentArtefact failed (new columns may be missing)", e)
        }
    }

    suspend fun updateProfileFcmToken(token: String) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        if (token.isBlank()) return@withContext
        try {
            client.from("users").upsert(
                buildJsonObject {
                    put("auth_uid", user.id)
                    put("fcm_token", token)
                }
            )
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error updating FCM token in profile", e)
        }
    }

    // --- Favorites ---
    
    /** Returns null when the network/decode fails so callers can keep local data. */
    suspend fun fetchFavorites(): List<Map<String, Any>>? = withContext(Dispatchers.IO) {
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
            null
        }
    }

    suspend fun addFavorite(itemNumber: Int, itemType: String) = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext
        try {
            client.from("favorites").insert(
                buildJsonObject {
                    put("user_id", user.id)
                    put("item_number", itemNumber)
                    put("item_type", itemType)
                }
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
                buildJsonObject {
                    put("user_id", user.id)
                    put("name", name)
                }
            ) {
                select()
            }.decodeSingle<CustomCategoryRow>()
            return@withContext response.id
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
                buildJsonObject {
                    put("category_id", categoryId)
                    put("user_id", user.id)
                    put("song_id", songId)
                    put("song_type", songType)
                }
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
            val payload = buildJsonObject {
                put("ticket_key", ticketKey)
                put("ticket_url", ticketUrl)
                put("song_type", songType)
                put("song_number", songNumber)
                put("song_title", songTitle)
                if (description != null) {
                    put("description", description)
                }
                put("app_version", appVersion)
                if (userId != null) {
                    put("user_id", userId)
                } else if (deviceId != null) {
                    put("device_id", deviceId)
                }
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
            val update = buildJsonObject {
                put("jira_status", statusName)
                put("updated_at", java.time.Instant.now().toString())
                if (statusId != null) {
                    put("jira_status_id", statusId)
                }
            }
            client.from("jira_tickets").update(update) {
                filter { eq("ticket_key", ticketKey) }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error updating ticket status for $ticketKey", e)
        }
    }

    suspend fun getEnabledPaymentGateways(): List<PaymentGatewayRow> = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) return@withContext emptyList()
            client.from("payment_gateways")
                .select {
                    filter {
                        eq("is_enabled", true)
                    }
                }
                .decodeList<PaymentGatewayRow>()
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error fetching payment gateways", e)
            emptyList()
        }
    }

    suspend fun updatePaymentGatewayEnabled(gatewayName: String, isEnabled: Boolean): Unit = withContext(Dispatchers.IO) {
        try {
            if (!isInitialized) return@withContext
            val update = buildJsonObject {
                put("is_enabled", isEnabled)
            }
            client.from("payment_gateways").update(update) {
                filter { eq("name", gatewayName) }
            }
            Log.i("SupabaseService", "Successfully updated payment_gateways name=$gatewayName to is_enabled=$isEnabled")
        } catch (e: Exception) {
            Log.e("SupabaseService", "Error updating payment_gateways name=$gatewayName", e)
        }
    }

    private fun parseDeletedFlag(raw: String): Boolean {
        val trimmed = raw.trim().removeSurrounding("\"")
        return trimmed.equals("true", ignoreCase = true)
            || trimmed == "t"
            || trimmed == "1"
    }

    private fun jsonElementIsTrue(element: JsonElement?): Boolean {
        val primitive = element as? JsonPrimitive ?: return false
        if (primitive.booleanOrNull == true) return true
        if (primitive.intOrNull == 1) return true
        val text = primitive.contentOrNull?.trim().orEmpty()
        return parseDeletedFlag(text)
    }

    private fun prettyJson(raw: String): String {
        return try {
            prettyJsonCodec.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(raw))
        } catch (_: Exception) {
            raw
        }
    }
}
