package com.reyzie.hymns.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FavoritesRepository private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("HymnsAppPrefs", Context.MODE_PRIVATE)
    private val supabase = SupabaseService.getInstance()

    private val _favoriteHymnIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteHymnIds: StateFlow<Set<Int>> = _favoriteHymnIds.asStateFlow()

    private val _favoriteKeerthaneIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteKeerthaneIds: StateFlow<Set<Int>> = _favoriteKeerthaneIds.asStateFlow()
    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "FavoritesRepository"

        @Volatile
        private var instance: FavoritesRepository? = null

        fun getInstance(context: Context): FavoritesRepository {
            return instance ?: synchronized(this) {
                instance ?: FavoritesRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        loadLocalFavorites()
    }

    private fun loadLocalFavorites() {
        val hymns = prefs.getStringSet("favoriteHymnIds", emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        val keerthanes = prefs.getStringSet("favoriteKeerthaneIds", emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

        _favoriteHymnIds.value = hymns
        _favoriteKeerthaneIds.value = keerthanes
    }

    suspend fun syncWithSupabase() = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            if (supabase.currentUser == null) return@withLock

            // null means the fetch failed — keep local stars instead of treating
            // that as "this account has no favorites".
            val remoteFavorites = supabase.fetchFavorites() ?: return@withLock

            val remote = FavoritesSync.Sets(
                hymns = remoteFavorites.filter { it["item_type"] == "hymn" }
                    .mapNotNull { (it["item_number"] as? Number)?.toInt() }.toSet(),
                keerthanes = remoteFavorites.filter { it["item_type"] == "keerthane" }
                    .mapNotNull { (it["item_number"] as? Number)?.toInt() }.toSet()
            )
            val local = FavoritesSync.Sets(
                hymns = _favoriteHymnIds.value,
                keerthanes = _favoriteKeerthaneIds.value
            )
            val merged = FavoritesSync.union(local, remote)
            val toUpload = FavoritesSync.localOnly(local, remote)

            if (supabase.currentUser == null) return@withLock

            persistFavorites(merged)

            for (id in toUpload.hymns) {
                supabase.addFavorite(id, "hymn")
            }
            for (id in toUpload.keerthanes) {
                supabase.addFavorite(id, "keerthane")
            }
        }
    }

    fun clearLocalOnSignOut() {
        persistFavorites(FavoritesSync.Sets(emptySet(), emptySet()))
    }

    suspend fun toggleFavorite(id: Int, isHymn: Boolean) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val currentSet = if (isHymn) _favoriteHymnIds.value else _favoriteKeerthaneIds.value
            val isCurrentlyFavorite = currentSet.contains(id)
            val newSet = if (isCurrentlyFavorite) currentSet - id else currentSet + id

            persistFavorites(
                if (isHymn) {
                    FavoritesSync.Sets(hymns = newSet, keerthanes = _favoriteKeerthaneIds.value)
                } else {
                    FavoritesSync.Sets(hymns = _favoriteHymnIds.value, keerthanes = newSet)
                }
            )

            if (supabase.currentUser != null) {
                try {
                    if (isCurrentlyFavorite) {
                        supabase.removeFavorite(id, if (isHymn) "hymn" else "keerthane")
                    } else {
                        supabase.addFavorite(id, if (isHymn) "hymn" else "keerthane")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync favorite to Supabase", e)
                }
            }
        }
    }

    private fun persistFavorites(sets: FavoritesSync.Sets) {
        _favoriteHymnIds.value = sets.hymns
        _favoriteKeerthaneIds.value = sets.keerthanes
        prefs.edit()
            .putStringSet("favoriteHymnIds", sets.hymns.map { it.toString() }.toSet())
            .putStringSet("favoriteKeerthaneIds", sets.keerthanes.map { it.toString() }.toSet())
            .apply()
    }
}
