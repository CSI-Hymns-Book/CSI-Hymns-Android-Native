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
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

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
                        val pagerState = rememberPagerState(pageCount = { 2 })

                        StandardButtonGroup(
                            buttonCount = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
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

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            if (page == 0) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    items(hymns, key = { it.number }) { hymn ->
                                        HymnListTile(hymn = hymn, onClick = { onHymnClick(hymn) })
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    items(keerthanes, key = { it.number }) { keerthane ->
                                        KeerthaneListTile(
                                            keerthane = keerthane,
                                            onClick = { onKeerthaneClick(keerthane) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Direct rendering if only 1 tab type exists for this occasion category
                        val showingHymns = showHymns
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            if (showingHymns) {
                                items(hymns, key = { it.number }) { hymn ->
                                    HymnListTile(hymn = hymn, onClick = { onHymnClick(hymn) })
                                }
                            } else {
                                items(keerthanes, key = { it.number }) { keerthane ->
                                    KeerthaneListTile(
                                        keerthane = keerthane,
                                        onClick = { onKeerthaneClick(keerthane) }
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
