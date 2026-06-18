package com.reyzie.hymns.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CustomCategory(
    val id: Int,
    val name: String,
    val deleted: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

data class CustomCategorySong(
    val categoryId: Int,
    val songId: Int,
    val songType: String,
    val deleted: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

class CustomCategoriesRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hymns_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val LOCAL_CATS_KEY = "local_custom_categories"
    private val LOCAL_CAT_SONGS_KEY = "local_custom_category_songs"
    private val supabase = SupabaseService.getInstance()
    
    private var migratedThisSession = false

    suspend fun getCategories(): List<CustomCategory> = withContext(Dispatchers.IO) {
        val user = supabase.currentUser
        if (user != null) {
            migrateLocalIfNeeded()
            val remote = supabase.fetchCustomCategories()
            if (remote.isNotEmpty()) {
                return@withContext remote.map {
                    CustomCategory(
                        id = (it["id"] as Number).toInt(),
                        name = it["name"] as String,
                        createdAt = it["created_at"] as? String ?: "",
                        updatedAt = it["updated_at"] as? String ?: ""
                    )
                }
            }
        }
        
        // Fallback to local
        return@withContext getLocalCategories()
    }

    private fun getLocalCategories(): List<CustomCategory> {
        val raw = prefs.getString(LOCAL_CATS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<CustomCategory>>() {}.type
        return try {
            val list: List<CustomCategory> = gson.fromJson(raw, type)
            list.filter { it.deleted == 0 }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addCategory(name: String): Int = withContext(Dispatchers.IO) {
        val user = supabase.currentUser
        if (user == null) {
            val current = getLocalCategories().toMutableList()
            if (current.size >= 5) return@withContext -1 // Limit reached
            
            val nextId = if (current.isEmpty()) -1 else current.minOf { it.id } - 1
            
            val newCat = CustomCategory(
                id = nextId,
                name = name,
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            current.add(newCat)
            saveCategories(current)
            return@withContext nextId
        }
        
        migrateLocalIfNeeded()
        return@withContext supabase.createCustomCategory(name) ?: -1
    }

    private fun saveCategories(categories: List<CustomCategory>) {
        prefs.edit().putString(LOCAL_CATS_KEY, gson.toJson(categories)).apply()
    }

    // --- Songs logic ---
    
    suspend fun getSongsInCategory(categoryId: Int): List<CustomCategorySong> = withContext(Dispatchers.IO) {
        val user = supabase.currentUser
        if (user != null && categoryId >= 0) {
            val remote = supabase.fetchSongsInCategory(categoryId)
            if (remote.isNotEmpty()) {
                return@withContext remote.map {
                    CustomCategorySong(
                        categoryId = categoryId,
                        songId = (it["song_id"] as Number).toInt(),
                        songType = it["song_type"] as String,
                        createdAt = it["created_at"] as? String ?: "",
                        updatedAt = it["updated_at"] as? String ?: ""
                    )
                }
            }
        }
        
        // Fallback to local
        val raw = prefs.getString(LOCAL_CAT_SONGS_KEY, null) ?: return@withContext emptyList()
        val type = object : TypeToken<List<CustomCategorySong>>() {}.type
        return@withContext try {
            val list: List<CustomCategorySong> = gson.fromJson(raw, type)
            list.filter { it.categoryId == categoryId && it.deleted == 0 }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSongToCategory(categoryId: Int, songId: Int, songType: String) = withContext(Dispatchers.IO) {
        val user = supabase.currentUser
        if (user == null || categoryId < 0) {
            val raw = prefs.getString(LOCAL_CAT_SONGS_KEY, null)
            val type = object : TypeToken<List<CustomCategorySong>>() {}.type
            val current: MutableList<CustomCategorySong> = try {
                raw?.let { gson.fromJson(it, type) } ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            if (current.any { it.categoryId == categoryId && it.songId == songId && it.songType == songType && it.deleted == 0 }) {
                return@withContext
            }
            
            current.add(
                CustomCategorySong(
                    categoryId = categoryId,
                    songId = songId,
                    songType = songType,
                    createdAt = System.currentTimeMillis().toString(),
                    updatedAt = System.currentTimeMillis().toString()
                )
            )
            prefs.edit().putString(LOCAL_CAT_SONGS_KEY, gson.toJson(current)).apply()
            return@withContext
        }
        
        supabase.addSongToCategory(categoryId, songId, songType)
    }

    suspend fun removeSongFromCategory(categoryId: Int, songId: Int, songType: String) = withContext(Dispatchers.IO) {
        val user = supabase.currentUser
        if (user == null || categoryId < 0) {
            val raw = prefs.getString(LOCAL_CAT_SONGS_KEY, null) ?: return@withContext
            val type = object : TypeToken<List<CustomCategorySong>>() {}.type
            val current: MutableList<CustomCategorySong> = try {
                gson.fromJson(raw, type)
            } catch (e: Exception) {
                return@withContext
            }
            
            val updated = current.map {
                if (it.categoryId == categoryId && it.songId == songId && it.songType == songType) {
                    it.copy(deleted = 1, updatedAt = System.currentTimeMillis().toString())
                } else {
                    it
                }
            }
            
            prefs.edit().putString(LOCAL_CAT_SONGS_KEY, gson.toJson(updated)).apply()
            return@withContext
        }
        
        supabase.removeSongFromCategory(categoryId, songId, songType)
    }
    
    suspend fun removeCategory(categoryId: Int) = withContext(Dispatchers.IO) {
        val user = supabase.currentUser
        if (user == null || categoryId < 0) {
            val categories = getLocalCategories().map {
                if (it.id == categoryId) it.copy(deleted = 1) else it
            }
            saveCategories(categories)
            return@withContext
        }
        supabase.softDeleteCustomCategory(categoryId)
    }
    
    private suspend fun migrateLocalIfNeeded() {
        if (migratedThisSession) return
        
        val localCats = getLocalCategories()
        val localSongsRaw = prefs.getString(LOCAL_CAT_SONGS_KEY, null) ?: "[]"
        val type = object : TypeToken<List<CustomCategorySong>>() {}.type
        val localSongs: List<CustomCategorySong> = try {
            gson.fromJson(localSongsRaw, type)
        } catch (e: Exception) {
            emptyList()
        }
        
        if (localCats.isEmpty() && localSongs.isEmpty()) {
            migratedThisSession = true
            return
        }
        
        val idMap = mutableMapOf<Int, Int>()
        for (cat in localCats) {
            val newId = supabase.createCustomCategory(cat.name)
            if (newId != null) {
                idMap[cat.id] = newId
            }
        }
        
        for (song in localSongs.filter { it.deleted == 0 }) {
            val remoteCatId = idMap[song.categoryId]
            if (remoteCatId != null) {
                supabase.addSongToCategory(remoteCatId, song.songId, song.songType)
            }
        }
        
        saveCategories(emptyList())
        prefs.edit().putString(LOCAL_CAT_SONGS_KEY, "[]").apply()
        migratedThisSession = true
    }
}
