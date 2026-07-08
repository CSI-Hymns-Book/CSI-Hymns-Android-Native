package com.reyzie.hymns.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reyzie.hymns.data.ContentUpdateBus
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.HymnsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.Context
import com.reyzie.hymns.data.AppSection

enum class SortOrder {
    NUMBER, TITLE, METER
}

class HymnsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HymnsRepository(application)
    private val settingsPrefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private var currentSection = AppSection.CSI

    fun setSection(section: AppSection) {
        if (currentSection != section) {
            currentSection = section
            loadHymns()
        }
    }

    private val _allHymns = MutableStateFlow<List<Hymn>>(emptyList())
    private val _filteredHymns = MutableStateFlow<List<Hymn>>(emptyList())
    val filteredHymns: StateFlow<List<Hymn>> = _filteredHymns.asStateFlow()

    private val _groupedHymns = MutableStateFlow<Map<String, List<Hymn>>>(emptyMap())
    val groupedHymns: StateFlow<Map<String, List<Hymn>>> = _groupedHymns.asStateFlow()

    private val _meterKeys = MutableStateFlow<List<String>>(emptyList())
    val meterKeys: StateFlow<List<String>> = _meterKeys.asStateFlow()

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

    private val _isChristmasMode = MutableStateFlow(settingsPrefs.getBoolean("christmas_mode", false))

    init {
        viewModelScope.launch {
            while (true) {
                val currentMode = settingsPrefs.getBoolean("christmas_mode", false)
                if (currentMode != _isChristmasMode.value) {
                    _isChristmasMode.value = currentMode
                    loadHymns()
                }
                delay(2000)
            }
        }
        viewModelScope.launch {
            ContentUpdateBus.hymnsUpdated.collectLatest { reloadFromLocal() }
        }
        loadHymns()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun loadHymns() {
        viewModelScope.launch {
            _isLoading.value = true
            val hymns = repository.loadHymns(currentSection)
            _allHymns.value = hymns
            applySortAndFilter()
            _isLoading.value = false
        }
    }

    private suspend fun reloadFromLocal() {
        val hymns = repository.loadHymns(currentSection)
        if (hymns.isNotEmpty()) {
            _allHymns.value = hymns
            applySortAndFilter()
        }
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    fun refreshHymns() {
        if (currentSection == AppSection.MT) {
            loadHymns()
            return
        }
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Loading
            val result = repository.fetchAndUpdateHymns()
            if (result.errorMessage != null) {
                _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Error(result.errorMessage)
            } else {
                _syncState.value = com.reyzie.hymns.ui.widgets.SyncState.Success
                if (result.data.isNotEmpty()) {
                    _allHymns.value = result.data
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
        val currentHymns = _allHymns.value.toMutableList()
        when (_sortOrder.value) {
            SortOrder.NUMBER -> currentHymns.sortBy { it.number }
            SortOrder.TITLE -> currentHymns.sortBy { it.title }
            SortOrder.METER -> {
                currentHymns.sortBy { it.signature }
                groupHymnsBySignature(currentHymns)
            }
        }
        _allHymns.value = currentHymns
        applyFilter()
    }

    private fun applyFilter() {
        val query = _searchQuery.value.trim().lowercase()
        val currentHymns = _allHymns.value

        if (query.isEmpty()) {
            if (_sortOrder.value == SortOrder.METER) {
                groupHymnsBySignature(currentHymns)
            } else {
                _filteredHymns.value = currentHymns
            }
        } else {
            if (_sortOrder.value == SortOrder.METER) {
                val filteredGroups = mutableMapOf<String, List<Hymn>>()
                _groupedHymns.value.forEach { (key, hymns) ->
                    val keyMatches = key.lowercase() == query
                    val matchingHymns = hymns.filter { hymn ->
                        keyMatches || hymn.title.lowercase().contains(query) ||
                        hymn.number.toString().contains(query)
                    }
                    if (matchingHymns.isNotEmpty()) {
                        filteredGroups[key] = matchingHymns
                    }
                }
                _groupedHymns.value = filteredGroups
                _meterKeys.value = filteredGroups.keys.sorted()
                _filteredHymns.value = filteredGroups.values.flatten()
            } else {
                _filteredHymns.value = currentHymns.filter { hymn ->
                    hymn.title.lowercase().contains(query) ||
                    hymn.number.toString().contains(query) ||
                    hymn.signature.lowercase().contains(query)
                }
            }
        }
    }

    private fun isTuneReference(part: String): Boolean {
        if (part.startsWith("(") && part.endsWith(")")) return true
        return part.matches(Regex("^(Mang\\.T\\.B\\.|M\\.T\\.)\\d.*", RegexOption.IGNORE_CASE))
    }

    private fun groupHymnsBySignature(hymns: List<Hymn>) {
        val grouped = mutableMapOf<String, MutableList<Hymn>>()
        for (hymn in hymns) {
            val parts = hymn.signature.split(" / ").map { it.trim() }.filter { it.isNotEmpty() }
            val realMeters = parts.filter { !isTuneReference(it) }
            val groupKeys = if (realMeters.isNotEmpty()) realMeters else listOf(hymn.signature)
            
            for (key in groupKeys) {
                grouped.getOrPut(key) { mutableListOf() }.add(hymn)
            }
        }
        _groupedHymns.value = grouped
        _meterKeys.value = grouped.keys.sorted()
        _filteredHymns.value = grouped.values.flatten()
    }

    fun meterIndexForKey(key: String): Int = _groupedHymns.value.keys.toList().let { keys ->
        if (key in keys) keys.indexOf(key) else _meterKeys.value.indexOf(key)
    }
}
