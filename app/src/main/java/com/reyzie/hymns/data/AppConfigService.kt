package com.reyzie.hymns.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class AppConfigEntry(
    val id: Int? = null,
    val key: String,
    val value: String? = null
)

class AppConfigService(
    private val supabaseService: SupabaseService = SupabaseService.getInstance()
) {
    companion object {
        private const val TABLE = "app_config"
    }

    suspend fun fetch(keys: Collection<String>): Map<String, String?> = withContext(Dispatchers.IO) {
        if (keys.isEmpty()) return@withContext emptyMap()
        try {
            val rows = supabaseService.client.from(TABLE)
                .select {
                    filter {
                        isIn("key", keys.toList())
                    }
                }
                .decodeList<AppConfigEntry>()
            rows.associate { it.key to it.value }
        } catch (e: Exception) {
            Log.e("AppConfigService", "Failed to fetch app_config", e)
            emptyMap()
        }
    }

    suspend fun fetchBoolean(key: String): Boolean? {
        return parseBoolean(fetch(listOf(key))[key])
    }

    fun parseBoolean(value: String?): Boolean? {
        val normalized = value?.trim()?.lowercase() ?: return null
        return when (normalized) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> null
        }
    }
}
