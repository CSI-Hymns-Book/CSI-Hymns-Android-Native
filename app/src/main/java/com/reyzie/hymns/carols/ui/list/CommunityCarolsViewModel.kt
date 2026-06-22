package com.reyzie.hymns.carols.ui.list

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.data.repository.CarolsRepository
import com.reyzie.hymns.carols.domain.CarolsPermissions
import com.reyzie.hymns.data.SupabaseService
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "CommunityCarolsVM"

enum class CarolContentTab { SONGS, PDFS }

enum class CarolSortOrder { NUMBER, NEWEST }

class CommunityCarolsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CarolsRepository.getInstance(application)
    private val supabase = SupabaseService.getInstance()

    val churches = repository.churches
    val songs = repository.songs
    val pdfs = repository.pdfs

    private val _isInitialLoading = MutableStateFlow(!repository.hasLocalData())
    private val _isSyncing = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _syncHint = MutableStateFlow<String?>(null)
    private val _selectedChurchId = MutableStateFlow<String?>(null)
    private val _selectedChurch = MutableStateFlow<CarolChurch?>(null)
    private val _contentTab = MutableStateFlow(CarolContentTab.SONGS)
    private val _sortOrder = MutableStateFlow(CarolSortOrder.NUMBER)

    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val syncHint: StateFlow<String?> = _syncHint.asStateFlow()
    val selectedChurchId: StateFlow<String?> = _selectedChurchId.asStateFlow()
    val selectedChurch: StateFlow<CarolChurch?> = _selectedChurch.asStateFlow()
    val contentTab: StateFlow<CarolContentTab> = _contentTab.asStateFlow()
    val sortOrder: StateFlow<CarolSortOrder> = _sortOrder.asStateFlow()

    val isAuthenticated: Boolean get() = supabase.currentUser != null
    val userId: String? get() = supabase.currentUser?.id
    val userEmail: String? get() = supabase.currentUser?.email

    val isAdmin: Boolean get() = CarolsPermissions.isAdmin(userEmail)

    init {
        repository.loadLocal()
        if (!repository.hasLocalData()) {
            refresh(force = true)
        }
        viewModelScope.launch {
            churches.collect { list ->
                val id = _selectedChurchId.value ?: return@collect
                _selectedChurch.value = list.find { it.id == id } ?: _selectedChurch.value
            }
        }
        viewModelScope.launch {
            supabase.authStream.collect { status ->
                if (status is SessionStatus.Authenticated) refresh(force = true)
            }
        }
    }

    fun ensureLocalLoaded() {
        repository.loadLocal()
        if (repository.hasLocalData()) {
            _isInitialLoading.value = false
        }
    }

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedChurch(churchId: String?) {
        _selectedChurchId.value = churchId
        _selectedChurch.value = churchId?.let { id -> repository.churchById(id) }
        _contentTab.value = CarolContentTab.SONGS
    }

    fun refreshIfNeeded() {
        if (!repository.hasLocalData()) {
            refresh(force = true)
        } else {
            refresh(force = false)
        }
    }

    fun setContentTab(tab: CarolContentTab) {
        _contentTab.value = tab
    }

    fun setSortOrder(order: CarolSortOrder) {
        _sortOrder.value = order
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            val showBlockingLoader = !repository.hasLocalData()
            if (showBlockingLoader) _isInitialLoading.value = true else _isSyncing.value = true
            _errorMessage.value = null
            try {
                val snapshot = repository.refresh(force = force)
                _syncHint.value = snapshot.userHint()
            } catch (e: Exception) {
                Log.e(TAG, "refresh failed", e)
                _errorMessage.value = e.message ?: "Could not sync carols."
            } finally {
                _isInitialLoading.value = false
                _isSyncing.value = false
            }
        }
    }

    fun createChurch(name: String, description: String?) {
        viewModelScope.launch {
            try {
                repository.createChurch(name, description)
            } catch (e: Exception) {
                Log.e(TAG, "createChurch failed", e)
                _errorMessage.value = e.message ?: "Could not create church."
            }
        }
    }

    fun addSong(
        churchId: String,
        title: String,
        songNumber: String?,
        lyricsKannada: String,
        lyricsEnglish: String?,
        scale: String,
    ) {
        viewModelScope.launch {
            try {
                repository.addSong(churchId, title, songNumber, lyricsKannada, lyricsEnglish, scale)
            } catch (e: Exception) {
                Log.e(TAG, "addSong failed", e)
                _errorMessage.value = e.message ?: "Could not add song."
            }
        }
    }

    fun addPdf(churchId: String, title: String, songNumber: String?, uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Could not read the selected PDF.")
                repository.addPdf(churchId, title, songNumber, bytes)
            } catch (e: Exception) {
                Log.e(TAG, "addPdf failed", e)
                _errorMessage.value = e.message ?: "Could not add PDF."
            }
        }
    }

    fun deleteChurch(churchId: String) {
        viewModelScope.launch {
            try {
                repository.deleteChurch(churchId)
                if (_selectedChurchId.value == churchId) _selectedChurchId.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not delete church."
            }
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSong(songId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not delete song."
            }
        }
    }

    fun deletePdf(pdfId: String) {
        viewModelScope.launch {
            try {
                repository.deletePdf(pdfId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Could not delete PDF."
            }
        }
    }

    fun canDeleteChurch(church: CarolChurch): Boolean =
        CarolsPermissions.canDeleteChurch(church, userId, userEmail)

    fun canDeleteSong(song: CarolSong): Boolean =
        CarolsPermissions.canDeleteSong(song, userId, userEmail)

    fun canDeletePdf(pdf: CarolPdf): Boolean =
        CarolsPermissions.canDeletePdf(pdf, userId, userEmail)

    fun filteredChurches(
        allChurches: List<CarolChurch>,
        allSongs: List<CarolSong>,
        allPdfs: List<CarolPdf>,
    ): List<CarolChurch> {
        val q = _searchQuery.value.trim().lowercase()
        if (q.isEmpty()) return allChurches.sortedBy { it.name.lowercase() }
        return allChurches.filter { church ->
            church.name.lowercase().contains(q) ||
                church.description?.lowercase()?.contains(q) == true ||
                allSongs.any { it.churchId == church.id && (it.title.lowercase().contains(q) || it.songNumber?.contains(q) == true) } ||
                allPdfs.any { it.churchId == church.id && (it.title.lowercase().contains(q) || it.songNumber?.contains(q) == true) }
        }.sortedBy { it.name.lowercase() }
    }

    fun filteredSongs(churchId: String): List<CarolSong> {
        val q = _searchQuery.value.trim().lowercase()
        var list = repository.songsForChurch(churchId)
        if (q.isNotEmpty()) {
            list = list.filter {
                it.title.lowercase().contains(q) || it.songNumber?.lowercase()?.contains(q) == true
            }
        }
        return when (_sortOrder.value) {
            CarolSortOrder.NEWEST -> list.sortedByDescending { it.updatedAt ?: it.createdAt }
            CarolSortOrder.NUMBER -> list.sortedWith(
                compareBy<CarolSong> { it.songNumber == null }
                    .thenBy { it.songNumber?.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenByDescending { it.updatedAt ?: it.createdAt },
            )
        }
    }

    fun filteredPdfs(churchId: String): List<CarolPdf> {
        val q = _searchQuery.value.trim().lowercase()
        var list = repository.pdfsForChurch(churchId)
        if (q.isNotEmpty()) {
            list = list.filter {
                it.title.lowercase().contains(q) || it.songNumber?.lowercase()?.contains(q) == true
            }
        }
        return when (_sortOrder.value) {
            CarolSortOrder.NEWEST -> list.sortedByDescending { it.updatedAt ?: it.createdAt }
            CarolSortOrder.NUMBER -> list.sortedWith(
                compareBy<CarolPdf> { it.songNumber == null }
                    .thenBy { it.songNumber?.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenByDescending { it.updatedAt ?: it.createdAt },
            )
        }
    }

    fun churchStats(churchId: String, allSongs: List<CarolSong>, allPdfs: List<CarolPdf>): Pair<Int, Int> {
        val songCount = allSongs.count { it.churchId == churchId }
        val pdfCount = allPdfs.count { it.churchId == churchId }
        return songCount to pdfCount
    }
}
