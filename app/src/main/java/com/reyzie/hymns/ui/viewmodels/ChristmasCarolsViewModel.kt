package com.reyzie.hymns.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.data.ChristmasCarol
import com.reyzie.hymns.data.ChristmasCarolsRepository
import com.reyzie.hymns.data.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CarolSortOrder {
    NUMBER, NEWEST
}

class ChristmasCarolsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChristmasCarolsRepository(application)
    private val supabaseService = SupabaseService.getInstance()

    private val _allCarols = MutableStateFlow<List<ChristmasCarol>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(CarolSortOrder.NUMBER)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _selectedChurch = MutableStateFlow<String?>(null)

    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val sortOrder: StateFlow<CarolSortOrder> = _sortOrder.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val selectedChurch: StateFlow<String?> = _selectedChurch.asStateFlow()

    val isAuthenticated: Boolean
        get() = supabaseService.currentUser != null

    private val adminEmails = setOf(
        "reynoldclare29022902@gmail.com",
        "reynoldclare02@gmail.com",
        "reyziecrafts@gmail.com",
        "reynold.clare29022902@gmail.com"
    )

    val isAdmin: Boolean
        get() = supabaseService.currentUser?.email?.lowercase() in adminEmails

    fun setSelectedChurch(churchName: String?) {
        _selectedChurch.value = churchName
    }

    fun loadLocal() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _allCarols.value = repository.getAllLocal()
            _isLoading.value = false
        }
    }

    fun refreshRemote() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _allCarols.value = repository.refreshFromRemote()
            } catch (_: Exception) {
                _errorMessage.value = "Could not refresh Christmas carols."
                _allCarols.value = repository.getAllLocal()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortChanged(order: CarolSortOrder) {
        _sortOrder.value = order
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addSong(churchName: String, title: String, lyrics: String, songNumber: String?, scale: String) {
        viewModelScope.launch {
            try {
                repository.addCarol(
                    churchName = churchName,
                    title = title,
                    lyrics = lyrics,
                    songNumber = songNumber,
                    scale = scale
                )
                _allCarols.value = repository.getAllLocal()
            } catch (_: Exception) {
                _errorMessage.value = "Could not add song."
            }
        }
    }

    fun addPdf(churchName: String, title: String, pdfUri: Uri) {
        viewModelScope.launch {
            try {
                repository.addPdfCarol(
                    churchName = churchName,
                    title = title,
                    pdfUri = pdfUri
                )
                _allCarols.value = repository.getAllLocal()
            } catch (_: Exception) {
                _errorMessage.value = "Could not add PDF."
            }
        }
    }

    fun deleteChurch(churchName: String) {
        viewModelScope.launch {
            try {
                repository.deleteChurch(churchName)
                _allCarols.value = repository.getAllLocal()
                if (_selectedChurch.value == churchName) {
                    _selectedChurch.value = null
                }
            } catch (_: Exception) {
                _errorMessage.value = "Could not delete church."
            }
        }
    }

    fun deleteCarol(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteCarol(id)
                _allCarols.value = repository.getAllLocal()
            } catch (_: Exception) {
                _errorMessage.value = "Could not delete song."
            }
        }
    }

    fun canDeleteChurch(churchName: String): Boolean {
        val userId = supabaseService.currentUser?.id ?: return false
        if (isAdmin) return true
        val firstCarol = _allCarols.value.firstOrNull { it.churchName == churchName } ?: return false
        return firstCarol.createdByUserId == userId
    }

    fun canEditCarol(carol: ChristmasCarol): Boolean {
        val userId = supabaseService.currentUser?.id ?: return false
        return isAdmin || carol.createdByUserId == userId
    }

    fun updateCarol(carol: ChristmasCarol) {
        viewModelScope.launch {
            try {
                repository.updateCarol(carol)
                _allCarols.value = repository.getAllLocal()
            } catch (_: Exception) {
                _errorMessage.value = "Could not update song."
            }
        }
    }

    fun groupedChurchesFiltered(): List<Pair<String, List<ChristmasCarol>>> {
        val grouped = _allCarols.value.groupBy { it.churchName }
        val query = _searchQuery.value.trim().lowercase()
        return grouped
            .filter { (church, carols) ->
                if (query.isEmpty()) return@filter true
                church.lowercase().contains(query) ||
                    carols.any {
                        it.title.lowercase().contains(query) ||
                            (it.songNumber?.lowercase()?.contains(query) == true)
                    }
            }
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    fun churchCarolsFiltered(churchName: String): List<ChristmasCarol> {
        val query = _searchQuery.value.trim().lowercase()
        val all = _allCarols.value.filter { it.churchName == churchName }
        val filtered = if (query.isEmpty()) {
            all
        } else {
            all.filter {
                it.title.lowercase().contains(query) ||
                    (it.songNumber?.lowercase()?.contains(query) == true)
            }
        }
        return when (_sortOrder.value) {
            CarolSortOrder.NEWEST -> filtered.sortedByDescending { it.updatedAt ?: it.createdAt }
            CarolSortOrder.NUMBER -> filtered.sortedWith(
                compareBy<ChristmasCarol> { it.songNumber == null }
                    .thenBy { it.songNumber?.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenByDescending { it.updatedAt ?: it.createdAt }
            )
        }
    }
}
