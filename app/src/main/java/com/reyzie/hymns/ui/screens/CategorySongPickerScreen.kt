package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.Keerthane
import com.reyzie.hymns.data.AppSection
import com.reyzie.hymns.data.HymnsRepository
import com.reyzie.hymns.data.CustomCategoriesRepository
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategorySongPickerScreen(
    categoryId: Int,
    categoryName: String,
    onBackClick: () -> Unit,
    activeSection: AppSection = AppSection.CSI
) {
    val context = LocalContext.current
    val hymnsRepository = remember { HymnsRepository(context) }
    val categoriesRepository = remember { CustomCategoriesRepository(context) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    var allHymns by remember { mutableStateOf<List<Hymn>>(emptyList()) }
    var allKeerthanes by remember { mutableStateOf<List<Keerthane>>(emptyList()) }
    var isLoadingSongs by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Set of Pair(songId, songType)
    var selectedSongs by remember { mutableStateOf<Set<Pair<Int, String>>>(emptySet()) }

    LaunchedEffect(activeSection) {
        scope.launch {
            allHymns = hymnsRepository.loadHymns(activeSection)
            allKeerthanes = hymnsRepository.loadKeerthanes()
            isLoadingSongs = false
        }
    }

    val filteredHymns = remember(allHymns, searchQuery) {
        if (searchQuery.isBlank()) allHymns
        else {
            val query = searchQuery.trim().lowercase()
            allHymns.filter {
                it.number.toString().contains(query) ||
                it.title.lowercase().contains(query) ||
                it.lyrics.lowercase().contains(query) ||
                (it.kannadaLyrics?.lowercase()?.contains(query) ?: false)
            }
        }
    }

    val filteredKeerthanes = remember(allKeerthanes, searchQuery) {
        if (searchQuery.isBlank()) allKeerthanes
        else {
            val query = searchQuery.trim().lowercase()
            allKeerthanes.filter {
                it.number.toString().contains(query) ||
                it.title.lowercase().contains(query) ||
                it.lyrics.lowercase().contains(query) ||
                (it.kannadaLyrics?.lowercase()?.contains(query) ?: false)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to $categoryName") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                selectedSongs.forEach { (songId, songType) ->
                                    categoriesRepository.addSongToCategory(categoryId, songId, songType)
                                }
                                onBackClick()
                            }
                        },
                        enabled = selectedSongs.isNotEmpty()
                    ) {
                        Text("Add (${selectedSongs.size})")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search songs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // Tabs Selector
            if (activeSection != AppSection.MT) {
                StandardButtonGroup(
                    buttonCount = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        index = 0,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        icon = Icons.Default.FormatListNumbered,
                        label = "Hymns",
                        isSelected = pagerState.currentPage == 0
                    )
                    Button(
                        index = 1,
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        icon = Icons.Default.MusicNote,
                        label = "Keerthanes",
                        isSelected = pagerState.currentPage == 1
                    )
                }
            }

            if (isLoadingSongs) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (activeSection == AppSection.MT) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredHymns, key = { "hymn-${it.number}" }) { hymn ->
                            val itemPair = Pair(hymn.number, "hymn")
                            val isSelected = selectedSongs.contains(itemPair)
                            ListItem(
                                headlineContent = { Text(hymn.title, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("Hymn #${hymn.number}") },
                                trailingContent = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.clickable {
                                    selectedSongs = if (isSelected) {
                                        selectedSongs - itemPair
                                    } else {
                                        selectedSongs + itemPair
                                    }
                                }
                            )
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(filteredHymns, key = { "hymn-${it.number}" }) { hymn ->
                                        val itemPair = Pair(hymn.number, "hymn")
                                        val isSelected = selectedSongs.contains(itemPair)
                                        ListItem(
                                            headlineContent = { Text(hymn.title, fontWeight = FontWeight.Bold) },
                                            supportingContent = { Text("Hymn #${hymn.number}") },
                                            trailingContent = {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                selectedSongs = if (isSelected) {
                                                    selectedSongs - itemPair
                                                } else {
                                                    selectedSongs + itemPair
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(filteredKeerthanes, key = { "keerthane-${it.number}" }) { keerthane ->
                                        val itemPair = Pair(keerthane.number, "keerthane")
                                        val isSelected = selectedSongs.contains(itemPair)
                                        ListItem(
                                            headlineContent = { Text(keerthane.title, fontWeight = FontWeight.Bold) },
                                            supportingContent = { Text("Keerthane #${keerthane.number}") },
                                            trailingContent = {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                selectedSongs = if (isSelected) {
                                                    selectedSongs - itemPair
                                                } else {
                                                    selectedSongs + itemPair
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
