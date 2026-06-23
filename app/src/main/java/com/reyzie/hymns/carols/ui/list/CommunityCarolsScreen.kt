package com.reyzie.hymns.carols.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.ui.create.AddCarolPdfDialog
import com.reyzie.hymns.carols.ui.create.AddCarolSongDialog
import com.reyzie.hymns.carols.ui.create.ChurchAddMenuDialog
import com.reyzie.hymns.carols.ui.create.CreateChurchDialog
import com.reyzie.hymns.carols.ui.detail.CarolPdfDetailScreen
import com.reyzie.hymns.carols.ui.detail.CarolSongDetailScreen
import com.reyzie.hymns.ui.motion.ExpressiveOverlayScreen
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.ui.widgets.rememberChristmasScreenColors
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityCarolsScreen(
    onBackClick: () -> Unit,
    viewModel: CommunityCarolsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val churches by viewModel.churches.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val pdfs by viewModel.pdfs.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val syncHint by viewModel.syncHint.collectAsState()
    val selectedChurchId by viewModel.selectedChurchId.collectAsState()
    val selectedChurch by viewModel.selectedChurch.collectAsState()
    val activeChurch = selectedChurch
    val contentTab by viewModel.contentTab.collectAsState()

    var showCreateChurch by rememberSaveable { mutableStateOf(false) }
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    var showAddSong by rememberSaveable { mutableStateOf(false) }
    var showAddPdf by rememberSaveable { mutableStateOf(false) }
    var deleteChurchTarget by remember { mutableStateOf<CarolChurch?>(null) }
    var selectedSong by rememberSaveable { mutableStateOf<CarolSong?>(null) }
    var selectedPdf by rememberSaveable { mutableStateOf<CarolPdf?>(null) }

    LaunchedEffect(Unit) {
        viewModel.ensureLocalLoaded()
        viewModel.refreshIfNeeded()
    }
    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val filteredChurches = remember(churches, songs, pdfs, query) {
        viewModel.filteredChurches(churches, songs, pdfs)
    }
    val churchSongs = remember(selectedChurchId, songs, query) {
        selectedChurchId?.let { viewModel.filteredSongs(it) }.orEmpty()
    }
    val churchPdfs = remember(selectedChurchId, pdfs, query) {
        selectedChurchId?.let { viewModel.filteredPdfs(it) }.orEmpty()
    }

    val christmasColors = rememberChristmasScreenColors()
    val contentColor = christmasColors.onBackground
    val contentColorMuted = christmasColors.onBackgroundMuted

    ChristmasScreenBackground {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                when {
                                    activeChurch != null -> activeChurch.name
                                    else -> "Community Carols 🎄"
                                },
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = contentColor,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                if (activeChurch != null) viewModel.setSelectedChurch(null)
                                else onBackClick()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = contentColor,
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                viewModel.refresh()
                            }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = contentColor,
                                )
                            }
                            if (activeChurch != null && viewModel.isAuthenticated) {
                                IconButton(onClick = { showAddMenu = true }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = contentColor,
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                    )
                },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = {
                if (activeChurch == null) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            if (!viewModel.isAuthenticated) {
                                scope.launch { snackbar.showSnackbar("Sign in to create a church.") }
                            } else {
                                showCreateChurch = true
                            }
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(if (viewModel.isAuthenticated) "New Church" else "Sign in") },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.navigationBarsPadding(),
                    )
                }
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                SearchField(
                    query = query,
                    onQueryChange = viewModel::onSearchChanged,
                    placeholder = if (activeChurch == null) "Search churches or songs..." else "Search in this church...",
                )

                if (isInitialLoading && churches.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ExpressiveCircularProgress()
                    }
                } else if (activeChurch == null) {
                    if (filteredChurches.isEmpty()) {
                        EmptyChurchesState(
                            syncHint = syncHint,
                            isAuthenticated = viewModel.isAuthenticated,
                            onRefresh = { viewModel.refresh() },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                        ) {
                            items(filteredChurches, key = { it.id }) { church ->
                                val (songCount, pdfCount) = viewModel.churchStats(church.id, songs, pdfs)
                                ChurchListCard(
                                    church = church,
                                    songCount = songCount,
                                    pdfCount = pdfCount,
                                    canDelete = viewModel.canDeleteChurch(church),
                                    onTap = { viewModel.setSelectedChurch(church.id) },
                                    onDelete = { deleteChurchTarget = church },
                                )
                            }
                        }
                    }
                } else {
                    activeChurch.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColorMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    SortChipRow(viewModel = viewModel)
                    StandardButtonGroup(
                        buttonCount = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        highContrast = true,
                    ) {
                        Button(
                            index = 0,
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                viewModel.setContentTab(CarolContentTab.SONGS)
                            },
                            icon = Icons.Default.MusicNote,
                            label = "Songs (${churchSongs.size})",
                            isSelected = contentTab == CarolContentTab.SONGS,
                        )
                        Button(
                            index = 1,
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                viewModel.setContentTab(CarolContentTab.PDFS)
                            },
                            icon = Icons.Default.PictureAsPdf,
                            label = "PDFs (${churchPdfs.size})",
                            isSelected = contentTab == CarolContentTab.PDFS,
                        )
                    }
                    when (contentTab) {
                        CarolContentTab.SONGS -> {
                            if (churchSongs.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No songs yet", color = contentColorMuted)
                                }
                            } else {
                                LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp)) {
                                    items(churchSongs, key = { it.id }) { song ->
                                        SongRow(
                                            song = song,
                                            canDelete = viewModel.canDeleteSong(song),
                                            onOpen = { selectedSong = song },
                                            onDelete = { viewModel.deleteSong(song.id) },
                                        )
                                    }
                                }
                            }
                        }
                        CarolContentTab.PDFS -> {
                            if (churchPdfs.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No PDFs yet", color = contentColorMuted)
                                }
                            } else {
                                LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp)) {
                                    items(churchPdfs, key = { it.id }) { pdf ->
                                        PdfRow(
                                            pdf = pdf,
                                            canDelete = viewModel.canDeletePdf(pdf),
                                            onOpen = { selectedPdf = pdf },
                                            onDelete = { viewModel.deletePdf(pdf.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ExpressiveOverlayScreen(item = selectedSong, onDismiss = { selectedSong = null }) { song ->
            CarolSongDetailScreen(
                song = song,
                churchName = activeChurch?.name.orEmpty(),
                onBackClick = { selectedSong = null },
            )
        }

        ExpressiveOverlayScreen(item = selectedPdf, onDismiss = { selectedPdf = null }) { pdf ->
            CarolPdfDetailScreen(
                title = pdf.title,
                pdfUrl = pdf.pdfUrl,
                onBackClick = { selectedPdf = null },
            )
        }
        }
    }

    if (showCreateChurch) {
        CreateChurchDialog(
            onDismiss = { showCreateChurch = false },
            onCreate = { name, desc ->
                viewModel.createChurch(name, desc)
                showCreateChurch = false
            },
        )
    }
    if (showAddMenu) {
        ChurchAddMenuDialog(
            onDismiss = { showAddMenu = false },
            onAddSong = { showAddMenu = false; showAddSong = true },
            onAddPdf = { showAddMenu = false; showAddPdf = true },
        )
    }
    if (showAddSong && selectedChurchId != null) {
        AddCarolSongDialog(
            onDismiss = { showAddSong = false },
            onAdd = { title, number, kn, en, scale ->
                viewModel.addSong(selectedChurchId!!, title, number, kn, en, scale)
                showAddSong = false
            },
        )
    }
    if (showAddPdf && selectedChurchId != null) {
        AddCarolPdfDialog(
            onDismiss = { showAddPdf = false },
            onAdd = { title, number, uri ->
                viewModel.addPdf(selectedChurchId!!, title, number, uri)
                showAddPdf = false
            },
        )
    }
    deleteChurchTarget?.let { church ->
        AlertDialog(
            onDismissRequest = { deleteChurchTarget = null },
            title = { Text("Delete church?") },
            text = { Text("Delete ${church.name} and all its songs and PDFs?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChurch(church.id)
                    deleteChurchTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteChurchTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
    }
}

@Composable
private fun EmptyChurchesState(syncHint: String?, isAuthenticated: Boolean, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🏛️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("No churches yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            syncHint ?: if (isAuthenticated) "Tap + to create a church or refresh to sync." else "Sign in to contribute, or refresh to browse.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sync")
        }
    }
}

@Composable
private fun ChurchListCard(
    church: CarolChurch,
    songCount: Int,
    pdfCount: Int,
    canDelete: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🏛️", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(church.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append("$songCount song${if (songCount == 1) "" else "s"}")
                        if (pdfCount > 0) append(" · $pdfCount PDF${if (pdfCount == 1) "" else "s"}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete church")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChipRow(viewModel: CommunityCarolsViewModel) {
    val sortOrder by viewModel.sortOrder.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            AssistChip(
                onClick = { expanded = true },
                label = { Text(if (sortOrder == CarolSortOrder.NUMBER) "By Number" else "Newest") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("By Song Number") }, onClick = {
                    viewModel.setSortOrder(CarolSortOrder.NUMBER); expanded = false
                })
                DropdownMenuItem(text = { Text("Newest First") }, onClick = {
                    viewModel.setSortOrder(CarolSortOrder.NEWEST); expanded = false
                })
            }
        }
    }
}

@Composable
private fun SongRow(song: CarolSong, canDelete: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    song.songNumber?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(song.title, fontWeight = FontWeight.SemiBold)
                }
                Text("Scale: ${song.scale}", style = MaterialTheme.typography.labelSmall)
            }
            if (canDelete) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete song?") },
            text = { Text("Remove ${song.title}?") },
            confirmButton = { TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PdfRow(pdf: CarolPdf, canDelete: Boolean, onOpen: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    pdf.songNumber?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(pdf.title, fontWeight = FontWeight.SemiBold)
                }
                Text("Tap to open PDF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (canDelete) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete PDF?") },
            text = { Text("Remove ${pdf.title}?") },
            confirmButton = { TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
