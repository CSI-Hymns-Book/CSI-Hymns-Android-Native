package com.reyzie.hymns.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class AppConfigEntry(
    val id: Int? = null,
    val key: String,
    /** Stored as jsonb in Postgres (bool, number, or string). */
    val value: JsonElement? = null
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
            rows.associate { it.key to configValueAsString(it.value) }
        } catch (e: Exception) {
            Log.e("AppConfigService", "Failed to fetch app_config", e)
            emptyMap()
        }
    }

    /** Normalize jsonb app_config values to strings for existing parsers. */
    internal fun configValueAsString(element: JsonElement?): String? {
        if (element == null) return null
        val primitive = runCatching { element.jsonPrimitive }.getOrNull() ?: return element.toString()
        if (primitive.isString) return primitive.content
        primitive.booleanOrNull?.let { return if (it) "true" else "false" }
        primitive.intOrNull?.let { return it.toString() }
        return primitive.contentOrNull
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

    suspend fun update(key: String, jsonValue: JsonElement): Unit = withContext(Dispatchers.IO) {
        try {
            val response = supabaseService.client.from(TABLE).update(
                buildJsonObject {
                    put("value", jsonValue)
                }
            ) {
                filter {
                    eq("key", key)
                }
                select()
            }
            val rows = response.decodeList<AppConfigEntry>()
            if (rows.isEmpty()) {
                throw Exception("RLS policy restriction or invalid key. 0 rows updated.")
            }
        } catch (e: Exception) {
            Log.e("AppConfigService", "Failed to update app_config key $key", e)
            throw e
        }
    }
}
