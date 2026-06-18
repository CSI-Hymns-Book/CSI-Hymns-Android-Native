package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.reyzie.hymns.data.CustomCategoriesRepository
import com.reyzie.hymns.data.CustomCategorySong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Int,
    categoryName: String,
    onBackClick: () -> Unit,
    onAddSongsClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CustomCategoriesRepository(context) }
    val scope = rememberCoroutineScope()
    
    var songs by remember { mutableStateOf<List<CustomCategorySong>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    fun loadSongs() {
        scope.launch {
            loading = true
            songs = repository.getSongsInCategory(categoryId)
            loading = false
        }
    }

    LaunchedEffect(categoryId) {
        loadSongs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSongsClick,
                icon = { Icon(Icons.Default.LibraryAddCheck, contentDescription = "Add Songs") },
                text = { Text("Add Songs") }
            )
        }
    ) { padding ->
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No songs yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(songs) { row ->
                    val title = "${row.songType} #${row.songId}"
                    ListItem(
                        headlineContent = { Text(title) },
                        trailingContent = {
                            IconButton(onClick = {
                                scope.launch {
                                    repository.removeSongFromCategory(categoryId, row.songId, row.songType)
                                    loadSongs()
                                }
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove")
                            }
                        }
                    )
                }
            }
        }
    }
}
