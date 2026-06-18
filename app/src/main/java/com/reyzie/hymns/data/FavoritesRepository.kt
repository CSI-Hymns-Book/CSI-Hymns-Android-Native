package com.reyzie.hymns.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("HymnsAppPrefs", Context.MODE_PRIVATE)
    private val supabase = SupabaseService.getInstance()
    
    private val _favoriteHymnIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteHymnIds: StateFlow<Set<Int>> = _favoriteHymnIds.asStateFlow()

    private val _favoriteKeerthaneIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteKeerthaneIds: StateFlow<Set<Int>> = _favoriteKeerthaneIds.asStateFlow()

    init {
        loadLocalFavorites()
    }

    private fun loadLocalFavorites() {
        val hymns = prefs.getStringSet("favoriteHymnIds", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        val keerthanes = prefs.getStringSet("favoriteKeerthaneIds", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        
        _favoriteHymnIds.value = hymns
        _favoriteKeerthaneIds.value = keerthanes
    }

    suspend fun syncWithSupabase() {
        if (supabase.currentUser == null) return

        val remoteFavorites = supabase.fetchFavorites()
        val hymns = remoteFavorites.filter { it["item_type"] == "hymn" }
            .mapNotNull { (it["item_number"] as? Number)?.toInt() }.toSet()
        val keerthanes = remoteFavorites.filter { it["item_type"] == "keerthane" }
            .mapNotNull { (it["item_number"] as? Number)?.toInt() }.toSet()

        _favoriteHymnIds.value = hymns
        _favoriteKeerthaneIds.value = keerthanes

        prefs.edit()
            .putStringSet("favoriteHymnIds", hymns.map { it.toString() }.toSet())
            .putStringSet("favoriteKeerthaneIds", keerthanes.map { it.toString() }.toSet())
            .apply()
    }

    fun clearLocalOnSignOut() {
        _favoriteHymnIds.value = emptySet()
        _favoriteKeerthaneIds.value = emptySet()
        prefs.edit()
            .remove("favoriteHymnIds")
            .remove("favoriteKeerthaneIds")
            .apply()
    }

    suspend fun toggleFavorite(id: Int, isHymn: Boolean) {
        val currentSet = if (isHymn) _favoriteHymnIds.value else _favoriteKeerthaneIds.value
        val isCurrentlyFavorite = currentSet.contains(id)
        
        val newSet = if (isCurrentlyFavorite) {
            currentSet - id
        } else {
            currentSet + id
        }
        
        val prefKey = if (isHymn) "favoriteHymnIds" else "favoriteKeerthaneIds"
        
        // Update local state immediately for UI responsiveness
        if (isHymn) {
            _favoriteHymnIds.value = newSet
        } else {
            _favoriteKeerthaneIds.value = newSet
        }
        
        prefs.edit()
            .putStringSet(prefKey, newSet.map { it.toString() }.toSet())
            .apply()
            
        // Sync with server if logged in
        if (supabase.currentUser != null) {
            try {
                if (isCurrentlyFavorite) {
                    supabase.removeFavorite(id, if (isHymn) "hymn" else "keerthane")
                } else {
                    supabase.addFavorite(id, if (isHymn) "hymn" else "keerthane")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
