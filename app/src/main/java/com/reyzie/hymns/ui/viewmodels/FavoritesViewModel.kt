package com.reyzie.hymns.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.data.FavoritesRepository
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.HymnsRepository
import com.reyzie.hymns.data.Keerthane
import com.reyzie.hymns.data.SupabaseService
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FavoritesRepository.getInstance(application)
    private val hymnsRepository = HymnsRepository(application)

    val favoriteHymnIds = repository.favoriteHymnIds
    val favoriteKeerthaneIds = repository.favoriteKeerthaneIds

    private val _favoriteHymns = MutableStateFlow<List<Hymn>>(emptyList())
    val favoriteHymns: StateFlow<List<Hymn>> = _favoriteHymns.asStateFlow()

    private val _favoriteKeerthanes = MutableStateFlow<List<Keerthane>>(emptyList())
    val favoriteKeerthanes: StateFlow<List<Keerthane>> = _favoriteKeerthanes.asStateFlow()

    init {
        viewModelScope.launch {
            repository.syncWithSupabase()
        }

        viewModelScope.launch {
            SupabaseService.getInstance().authStream.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> repository.syncWithSupabase()
                    is SessionStatus.NotAuthenticated -> {
                        if (status.isSignOut) {
                            repository.clearLocalOnSignOut()
                        }
                    }
                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            favoriteHymnIds.collect { ids ->
                val allHymns = hymnsRepository.loadHymns()
                _favoriteHymns.value = allHymns
                    .filter { it.number in ids }
                    .sortedBy { it.number }
            }
        }

        viewModelScope.launch {
            favoriteKeerthaneIds.collect { ids ->
                val allKeerthanes = hymnsRepository.loadKeerthanes()
                _favoriteKeerthanes.value = allKeerthanes
                    .filter { it.number in ids }
                    .sortedBy { it.number }
            }
        }
    }

    fun toggleFavoriteHymn(id: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(id, isHymn = true)
        }
    }

    fun toggleFavoriteKeerthane(id: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(id, isHymn = false)
        }
    }
}
