package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.reyzie.hymns.R
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.Keerthane
import com.reyzie.hymns.data.HymnsRepository
import com.reyzie.hymns.data.CustomCategoriesRepository
import com.reyzie.hymns.data.CustomCategorySong
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Int,
    categoryName: String,
    refreshTrigger: Int = 0,
    onBackClick: () -> Unit,
    onAddSongsClick: () -> Unit,
    onHymnClick: (Hymn) -> Unit,
    onKeerthaneClick: (Keerthane) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CustomCategoriesRepository(context) }
    val hymnsRepository = remember { HymnsRepository(context) }
    val scope = rememberCoroutineScope()
    
    var songs by remember { mutableStateOf<List<CustomCategorySong>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    
    var allHymns by remember { mutableStateOf<Map<Int, Hymn>>(emptyMap()) }
    var allKeerthanes by remember { mutableStateOf<Map<Int, Keerthane>>(emptyMap()) }

    val pagerState = rememberPagerState(pageCount = { 2 })

    val hymnSongs = remember(songs) { songs.filter { it.songType.equals("hymn", ignoreCase = true) } }
    val keerthaneSongs = remember(songs) { songs.filter { it.songType.equals("keerthane", ignoreCase = true) } }

    fun loadSongs() {
        scope.launch {
            loading = true
            songs = repository.getSongsInCategory(categoryId)
            loading = false
        }
    }

    LaunchedEffect(categoryId, refreshTrigger) {
        scope.launch {
            allHymns = hymnsRepository.loadHymns().associateBy { it.number }
            allKeerthanes = hymnsRepository.loadKeerthanes().associateBy { it.number }
        }
        loadSongs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab switch button group
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

            if (loading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { page ->
                    val currentList = if (page == 0) hymnSongs else keerthaneSongs
                    if (currentList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (page == 0) "No hymns added yet" else "No keerthanes added yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(currentList, key = { "${it.songType}-${it.songId}" }) { row ->
                                val isHymn = row.songType.equals("hymn", ignoreCase = true)
                                val title = if (isHymn) {
                                    allHymns[row.songId]?.title ?: "Hymn #${row.songId}"
                                } else {
                                    allKeerthanes[row.songId]?.title ?: "Keerthane #${row.songId}"
                                }
                                
                                CategorySongListTile(
                                    title = title,
                                    number = row.songId,
                                    isHymn = isHymn,
                                    onDeleteClick = {
                                        scope.launch {
                                            repository.removeSongFromCategory(categoryId, row.songId, row.songType)
                                            loadSongs()
                                        }
                                    },
                                    onClick = {
                                        if (isHymn) {
                                            allHymns[row.songId]?.let { onHymnClick(it) }
                                        } else {
                                            allKeerthanes[row.songId]?.let { onKeerthaneClick(it) }
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

@Composable
fun CategorySongListTile(
    title: String,
    number: Int,
    isHymn: Boolean,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = {
                HapticFeedbackManager.smoothClick(context)
                onClick()
            }),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = if (isHymn) R.drawable.hymn else R.drawable.keerthane),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHymn) "Hymn $number" else "Keerthane $number",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
            
            IconButton(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    onDeleteClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}
