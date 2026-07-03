package com.reyzie.hymns.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.data.ContentUpdateBus
import com.reyzie.hymns.data.HymnsRepository
import com.reyzie.hymns.data.Keerthane
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay

class KeerthaneViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HymnsRepository(application)

    private val _allKeerthanes = MutableStateFlow<List<Keerthane>>(emptyList())
    private val _filteredKeerthanes = MutableStateFlow<List<Keerthane>>(emptyList())
    val filteredKeerthanes: StateFlow<List<Keerthane>> = _filteredKeerthanes.asStateFlow()

    private val _groupedKeerthanes = MutableStateFlow<Map<String, List<Keerthane>>>(emptyMap())
    val groupedKeerthanes: StateFlow<Map<String, List<Keerthane>>> = _groupedKeerthanes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _syncState = MutableStateFlow<com.reyzie.hymns.ui.widgets.SyncState>(com.reyzie.hymns.ui.widgets.SyncState.Idle)
    val syncState: StateFlow<com.reyzie.hymns.ui.widgets.SyncState> = _syncState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NUMBER)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    init {
        viewModelScope.launch {
            ContentUpdateBus.keerthanesUpdated.collectLatest { reloadFromLocal() }
        }
        loadKeerthanes()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun loadKeerthanes() {
        viewModelScope.launch {
            _isLoading.value = true
            val keerthanes = repository.loadKeerthanes()
            _allKeerthanes.value = keerthanes
            applySortAndFilter()
            _isLoading.value = false
        }
    }

    private suspend fun reloadFromLocal() {
        val keerthanes = repository.loadKeerthanes()
        if (keerthanes.isNotEmpty()) {
            _allKeerthanes.value = keerthanes
            applySortAndFilter()
        }
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    fun refreshKeerthanes() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Loading
            val result = repository.fetchAndUpdateKeerthanes()
            if (result.errorMessage != null) {
                _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Error(result.errorMessage)
            } else {
                _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Success
                if (result.data.isNotEmpty()) {
                    _allKeerthanes.value = result.data
                }
                applySortAndFilter()
                delay(1500)
                _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Idle
            }
        }
    }

    fun dismissSyncDialog() {
        syncJob?.cancel()
        _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Idle
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        applyFilter()
    }

    fun onSortOrderChanged(order: SortOrder) {
        _sortOrder.value = order
        applySortAndFilter()
    }

    private fun applySortAndFilter() {
        val currentKeerthanes = _allKeerthanes.value.toMutableList()
        when (_sortOrder.value) {
            SortOrder.NUMBER -> currentKeerthanes.sortBy { it.number }
            SortOrder.TITLE -> currentKeerthanes.sortBy { it.title }
            SortOrder.METER -> {
                currentKeerthanes.sortBy { it.signature }
                groupKeerthanesBySignature(currentKeerthanes)
            }
        }
        _allKeerthanes.value = currentKeerthanes
        applyFilter()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val currentKeerthanes = _allKeerthanes.value

        if (query.isEmpty()) {
            if (_sortOrder.value == SortOrder.METER) {
                groupKeerthanesBySignature(currentKeerthanes)
            } else {
                _filteredKeerthanes.value = currentKeerthanes
            }
        } else {
            if (_sortOrder.value == SortOrder.METER) {
                val filteredGroups = mutableMapOf<String, List<Keerthane>>()
                _groupedKeerthanes.value.forEach { (key, keerthanes) ->
                    val keyMatches = key.lowercase() == query
                    val matchingKeerthanes = keerthanes.filter { k ->
                        keyMatches || k.title.lowercase().contains(query) ||
                        k.number.toString().contains(query)
                    }
                    if (matchingKeerthanes.isNotEmpty()) {
                        filteredGroups[key] = matchingKeerthanes
                    }
                }
                _groupedKeerthanes.value = filteredGroups
                _filteredKeerthanes.value = filteredGroups.values.flatten()
            } else {
                _filteredKeerthanes.value = currentKeerthanes.filter { k ->
                    k.title.lowercase().contains(query) ||
                    k.number.toString().contains(query) ||
                    k.signature.lowercase().contains(query)
                }
            }
        }
    }

    private fun isTuneReference(part: String): Boolean {
        if (part.startsWith("(") && part.endsWith(")")) return true
        return part.matches(Regex("^(Mang\\.T\\.B\\.|M\\.T\\.)\\d.*", RegexOption.IGNORE_CASE))
    }

    private fun groupKeerthanesBySignature(keerthanes: List<Keerthane>) {
        val grouped = mutableMapOf<String, MutableList<Keerthane>>()
        for (k in keerthanes) {
            val parts = k.signature.split(" / ").map { it.trim() }.filter { it.isNotEmpty() }
            val realMeters = parts.filter { !isTuneReference(it) }
            val groupKeys = if (realMeters.isNotEmpty()) realMeters else listOf(k.signature)
            
            for (key in groupKeys) {
                grouped.getOrPut(key) { mutableListOf() }.add(k)
            }
        }
        _groupedKeerthanes.value = grouped
        _filteredKeerthanes.value = grouped.values.flatten()
    }
}
