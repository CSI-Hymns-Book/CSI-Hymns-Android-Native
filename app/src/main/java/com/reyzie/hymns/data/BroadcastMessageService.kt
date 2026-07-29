package com.reyzie.hymns.data

import android.content.Context
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BroadcastMessageService(private val context: Context) {
    private val supabase = SupabaseService.getInstance()
    private val prefs = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val DISMISSED_BROADCASTS_KEY = "dismissed_broadcasts_ids_v1"
    }

    /**
     * Fetches active announcements from the database and returns the first one
     * that hasn't been dismissed by the user yet.
     */
    suspend fun getActiveUndismissedBroadcast(): InAppMessage? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            
            val activeAnnouncements = supabase.client.from("in_app_messages")
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<InAppMessage>()
                .filter { it.targetVersion.isNullOrBlank() || it.targetVersion == currentVersion }
                .sortedByDescending { it.createdAt }

            val dismissedIds = getDismissedIds()
            activeAnnouncements.firstOrNull { it.id !in dismissedIds }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Marks a broadcast ID as dismissed so it won't be shown to this device again.
     */
    fun dismissBroadcast(id: String) {
        val current = getDismissedIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(DISMISSED_BROADCASTS_KEY, current).apply()
    }

    suspend fun getAllBroadcasts(): List<InAppMessage> = withContext(Dispatchers.IO) {
        try {
            supabase.client.from("in_app_messages")
                .select()
                .decodeList<InAppMessage>()
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun createBroadcast(message: InAppMessage): String? = withContext(Dispatchers.IO) {
        try {
            supabase.client.from("in_app_messages").insert(message)
            null
        } catch (e: Exception) {
            e.printStackTrace()
            e.localizedMessage ?: e.toString()
        }
    }

    suspend fun updateBroadcast(message: InAppMessage): String? = withContext(Dispatchers.IO) {
        try {
            supabase.client.from("in_app_messages").update(message) {
                filter { eq("id", message.id) }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            e.localizedMessage ?: e.toString()
        }
    }

    suspend fun toggleBroadcastActive(id: String, isActive: Boolean): String? = withContext(Dispatchers.IO) {
        try {
            supabase.client.from("in_app_messages").update(
                buildJsonObject {
                    put("is_active", isActive)
                }
            ) {
                filter { eq("id", id) }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            e.localizedMessage ?: e.toString()
        }
    }

    suspend fun deleteBroadcast(id: String): String? = withContext(Dispatchers.IO) {
        try {
            supabase.client.from("in_app_messages").delete {
                filter { eq("id", id) }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            e.localizedMessage ?: e.toString()
        }
    }

    suspend fun uploadAnnouncementImage(fileName: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val path = "announcements/$fileName"
        val bucket = supabase.client.storage.from("carol-pdfs")
        bucket.upload(path, bytes) {
            upsert = true
        }
        bucket.publicUrl(path)
    }

    suspend fun deleteAnnouncementImage(imageUrl: String) = withContext(Dispatchers.IO) {
        try {
            val marker = "/public/carol-pdfs/"
            val index = imageUrl.indexOf(marker)
            if (index != -1) {
                val path = imageUrl.substring(index + marker.length)
                supabase.client.storage.from("carol-pdfs").delete(listOf(path))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun retriggerBroadcast(oldId: String, newId: String, currentMessage: InAppMessage): String? = withContext(Dispatchers.IO) {
        try {
            val updated = currentMessage.copy(
                id = newId,
                createdAt = java.time.Instant.now().toString()
            )
            supabase.client.from("in_app_messages").delete {
                filter { eq("id", oldId) }
            }
            supabase.client.from("in_app_messages").insert(updated)
            null
        } catch (e: Exception) {
            e.printStackTrace()
            e.localizedMessage ?: e.toString()
        }
    }

    private fun getDismissedIds(): Set<String> {
        return prefs.getStringSet(DISMISSED_BROADCASTS_KEY, emptySet()) ?: emptySet()
    }
}
