package com.reyzie.hymns.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

data class RecentSongEntry(
    val itemType: String,
    val itemId: String,
    val title: String,
    val viewedAtMs: Long
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("itemType", itemType)
        json.put("itemId", itemId)
        json.put("title", title)
        json.put("viewedAtMs", viewedAtMs)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): RecentSongEntry {
            return RecentSongEntry(
                itemType = json.optString("itemType", ""),
                itemId = json.optString("itemId", ""),
                title = json.optString("title", ""),
                viewedAtMs = json.optLong("viewedAtMs", 0L)
            )
        }
    }
}

class RecentSongsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("HymnsAppPrefs", Context.MODE_PRIVATE)
    private val key = "recent_song_entries_v1"
    private val maxEntries = 30
    
    private val _recentSongs = MutableStateFlow<List<RecentSongEntry>>(emptyList())
    val recentSongs: StateFlow<List<RecentSongEntry>> = _recentSongs.asStateFlow()

    init {
        loadRecentSongs()
    }

    private fun loadRecentSongs() {
        val encoded = prefs.getString(key, null)
        if (encoded.isNullOrEmpty()) {
            _recentSongs.value = emptyList()
            return
        }
        
        try {
            val jsonArray = JSONArray(encoded)
            val entries = mutableListOf<RecentSongEntry>()
            for (i in 0 until jsonArray.length()) {
                entries.add(RecentSongEntry.fromJson(jsonArray.getJSONObject(i)))
            }
            _recentSongs.value = entries.sortedByDescending { it.viewedAtMs }
        } catch (e: Exception) {
            e.printStackTrace()
            _recentSongs.value = emptyList()
        }
    }

    fun trackViewed(itemType: String, itemId: String, title: String) {
        val entries = _recentSongs.value.toMutableList()
        entries.removeAll { it.itemType == itemType && it.itemId == itemId }
        
        entries.add(0, RecentSongEntry(
            itemType = itemType,
            itemId = itemId,
            title = title,
            viewedAtMs = System.currentTimeMillis()
        ))
        
        val trimmed = entries.take(maxEntries)
        _recentSongs.value = trimmed
        
        try {
            val jsonArray = JSONArray()
            trimmed.forEach { jsonArray.put(it.toJson()) }
            prefs.edit().putString(key, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
