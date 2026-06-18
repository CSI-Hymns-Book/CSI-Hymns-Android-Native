package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.CustomCategoriesRepository
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySongPickerScreen(
    categoryId: Int,
    categoryName: String,
    onBackClick: () -> Unit,
    viewModel: HymnsViewModel = viewModel() // Reusing HymnsViewModel for simplicity
) {
    val hymns by viewModel.filteredHymns.collectAsState()
    val context = LocalContext.current
    val repository = remember { CustomCategoriesRepository(context) }
    val scope = rememberCoroutineScope()
    
    // We can just show hymns for now
    var selectedSongs by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
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
                                selectedSongs.forEach { songId ->
                                    repository.addSongToCategory(categoryId, songId, "hymn")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(hymns) { hymn ->
                val isSelected = selectedSongs.contains(hymn.number)
                ListItem(
                    headlineContent = { Text(hymn.title) },
                    supportingContent = { Text("Hymn #${hymn.number}") },
                    trailingContent = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable {
                        selectedSongs = if (isSelected) {
                            selectedSongs - hymn.number
                        } else {
                            selectedSongs + hymn.number
                        }
                    }
                )
            }
        }
    }
}
