package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.HymnsRepository
import com.reyzie.hymns.data.Keerthane
import com.reyzie.hymns.data.OccasionCategories
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccasionCategoryScreen(
    categoryName: String,
    onBackClick: () -> Unit,
    onHymnClick: (Hymn) -> Unit,
    onKeerthaneClick: (Keerthane) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { HymnsRepository(context) }
    val occasion = remember(categoryName) { OccasionCategories.find(categoryName) }

    var loading by remember { mutableStateOf(true) }
    var hymns by remember { mutableStateOf<List<Hymn>>(emptyList()) }
    var keerthanes by remember { mutableStateOf<List<Keerthane>>(emptyList()) }

    LaunchedEffect(categoryName) {
        loading = true
        withContext(Dispatchers.IO) {
            val cat = OccasionCategories.find(categoryName)
            if (cat != null) {
                val allHymns = repository.loadHymns()
                val allKeerthanes = repository.loadKeerthanes()
                hymns = cat.hymnNumbers.mapNotNull { n -> allHymns.find { it.number == n } }
                keerthanes = cat.keerthaneNumbers.mapNotNull { n -> allKeerthanes.find { it.number == n } }
            }
        }
        loading = false
    }

    val showHymns = occasion?.hasHymns == true
    val showKeerthanes = occasion?.hasKeerthanes == true
    val tabCount = listOf(showHymns, showKeerthanes).count { it }
    var selectedTab by remember(categoryName) { mutableIntStateOf(0) }

    val subtitle = when {
        showHymns && showKeerthanes -> "Hymns & Keerthanes"
        showHymns -> "Hymns"
        showKeerthanes -> "Keerthanes"
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(categoryName, fontWeight = FontWeight.Bold)
                        if (subtitle.isNotEmpty()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            occasion == null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Category not found")
                }
            }
            loading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpressiveCircularProgress()
                }
            }
            occasion.isEmpty -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No songs listed yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    if (tabCount > 1) {
                        TabRow(selectedTabIndex = selectedTab) {
                            if (showHymns) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Hymns") },
                                )
                            }
                            if (showKeerthanes) {
                                val index = if (showHymns) 1 else 0
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text("Keerthanes") },
                                )
                            }
                        }
                    }

                    val showingHymns = showHymns && (tabCount == 1 || selectedTab == 0)
                    if (showingHymns) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp),
                        ) {
                            items(hymns, key = { it.number }) { hymn ->
                                HymnListTile(hymn = hymn, onClick = { onHymnClick(hymn) })
                            }
                        }
                    } else if (showKeerthanes) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 100.dp),
                        ) {
                            items(keerthanes, key = { it.number }) { keerthane ->
                                KeerthaneListTile(
                                    keerthane = keerthane,
                                    onClick = { onKeerthaneClick(keerthane) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
