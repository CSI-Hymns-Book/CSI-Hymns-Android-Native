package com.reyzie.hymns.carols.ui.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.carols.data.local.CarolReaderStore
import com.reyzie.hymns.interop.CarolHandoff
import com.reyzie.hymns.interop.ReturnToCallerChip
import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.ui.create.AddCarolPdfDialog
import com.reyzie.hymns.carols.ui.create.CarolSongFormDialog
import com.reyzie.hymns.carols.ui.create.ChurchAddMenuDialog
import com.reyzie.hymns.carols.ui.create.CreateChurchDialog
import com.reyzie.hymns.carols.ui.detail.CarolPdfDetailScreen
import com.reyzie.hymns.carols.ui.detail.CarolSongDetailScreen
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.LocalSnowCapsVisible
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.ui.motion.ExpressiveOverlayScreen
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

private val ListPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp)

/**
 * Community Carols: churches share carol lyrics and sheet music. The first view lists the
 * churches (with the reader's favourite and recently opened carols on top); tapping one
 * shows its songs and PDFs. Pull down anywhere to sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityCarolsScreen(
    onBackClick: () -> Unit,
    viewModel: CommunityCarolsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val store = remember { CarolReaderStore.getInstance(context) }

    val churches by viewModel.churches.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val pdfs by viewModel.pdfs.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val syncHint by viewModel.syncHint.collectAsState()
    val selectedChurchId by viewModel.selectedChurchId.collectAsState()
    val activeChurch by viewModel.selectedChurch.collectAsState()
    val contentTab by viewModel.contentTab.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val favouriteIds by store.favourites.collectAsState()
    val recentIds by store.recents.collectAsState()
    val defaultChurchId by store.defaultChurchId.collectAsState()
    val handoffPending by CarolHandoff.selectionPending.collectAsState()
    val handoffRequest by CarolHandoff.openRequest.collectAsState()

    var showCreateChurch by rememberSaveable { mutableStateOf(false) }
    var showAddMenu by rememberSaveable { mutableStateOf(false) }
    var showAddSong by rememberSaveable { mutableStateOf(false) }
    var showAddPdf by rememberSaveable { mutableStateOf(false) }
    var editSongTarget by remember { mutableStateOf<CarolSong?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var openSongId by rememberSaveable { mutableStateOf<String?>(null) }
    var openPdfId by rememberSaveable { mutableStateOf<String?>(null) }

    // The view model outlives this screen, so start each visit on the church list.
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!started) {
            viewModel.setSelectedChurch(null)
            started = true
        }
        viewModel.ensureLocalLoaded()
        viewModel.refreshIfNeeded()
    }
    // Back inside a church returns to the church list instead of leaving Carols.
    BackHandler(enabled = activeChurch != null && openSongId == null && openPdfId == null) {
        viewModel.setSelectedChurch(null)
    }
    // A church's search box starts closed each time you open a church.
    LaunchedEffect(activeChurch?.id) { searchOpen = false }
    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // A partner app asked for Carols: go to the default church if one is set, else the church list.
    // A new request also closes any song that was open.
    var handledRequest by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(handoffRequest) {
        if (handoffRequest > handledRequest) {
            handledRequest = handoffRequest
            openSongId = null
            openPdfId = null
        }
    }
    LaunchedEffect(handoffPending, churches, isInitialLoading, defaultChurchId) {
        if (!handoffPending) return@LaunchedEffect
        if (churches.isEmpty() && isInitialLoading) return@LaunchedEffect // wait for the list to load
        viewModel.setSelectedChurch(null)
        defaultChurchId?.takeIf { id -> churches.any { it.id == id } }?.let(viewModel::setSelectedChurch)
        CarolHandoff.selectionHandled()
    }

    // The default church comes first in the list.
    val filteredChurches = remember(churches, songs, pdfs, query, defaultChurchId) {
        viewModel.filteredChurches(churches, songs, pdfs).sortedByDescending { it.id == defaultChurchId }
    }
    val churchSongs = remember(selectedChurchId, songs, query, sortOrder) {
        selectedChurchId?.let { viewModel.filteredSongs(it) }.orEmpty()
    }
    val churchPdfs = remember(selectedChurchId, pdfs, query, sortOrder) {
        selectedChurchId?.let { viewModel.filteredPdfs(it) }.orEmpty()
    }
    val favouriteSongs = remember(songs, favouriteIds) { songs.filter { it.id in favouriteIds } }
    val recentSongs = remember(songs, recentIds) { recentIds.mapNotNull { id -> songs.find { it.id == id } } }
    val openSong = songs.find { it.id == openSongId }
    val openPdf = pdfs.find { it.id == openPdfId }
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val listPane: @Composable (Modifier) -> Unit = { modifier ->
        CarolsListPane(
            modifier = modifier,
            isLoading = isInitialLoading && churches.isEmpty(),
            isSyncing = isSyncing,
            onRefresh = { viewModel.refresh(force = true) },
            query = query,
            activeChurch = activeChurch,
            contentTab = contentTab,
            churches = filteredChurches,
            churchSongs = churchSongs,
            churchPdfs = churchPdfs,
            favouriteSongs = if (query.isBlank()) favouriteSongs else emptyList(),
            recentSongs = if (query.isBlank()) recentSongs else emptyList(),
            favouriteIds = favouriteIds,
            defaultChurchId = defaultChurchId,
            onToggleDefault = { store.toggleDefaultChurch(it.id) },
            emptyState = {
                EmptyChurchesState(syncHint, viewModel.isAuthenticated, onRefresh = { viewModel.refresh(force = true) })
            },
            churchStats = { viewModel.churchStats(it, songs, pdfs) },
            viewModel = viewModel,
            onOpenChurch = { viewModel.setSelectedChurch(it.id) },
            onDeleteChurch = { viewModel.deleteChurch(it.id) },
            onEditSong = { editSongTarget = it },
            onOpenSong = { openSongId = it.id },
            onOpenPdf = { openPdfId = it.id },
        )
    }

    ChristmasScreenBackground {
        CompositionLocalProvider(LocalSnowCapsVisible provides (openSong == null && openPdf == null)) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CarolsTopBar(
                        title = activeChurch?.name ?: "Community Carols",
                        canAdd = activeChurch != null && viewModel.isAuthenticated,
                        canSearch = activeChurch != null,
                        onSearch = {
                            HapticFeedbackManager.smoothClick(context)
                            searchOpen = !searchOpen
                            if (!searchOpen) viewModel.onSearchChanged("")
                        },
                        onBack = {
                            HapticFeedbackManager.smoothClick(context)
                            if (activeChurch != null) viewModel.setSelectedChurch(null) else onBackClick()
                        },
                        onAdd = { showAddMenu = true },
                    )
                },
                snackbarHost = { SnackbarHost(snackbar) },
                floatingActionButton = {
                    if (activeChurch == null) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                if (!viewModel.isAuthenticated) {
                                    scope.launch { snackbar.showSnackbar("Sign in to add a church.") }
                                } else {
                                    showCreateChurch = true
                                }
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text(if (viewModel.isAuthenticated) "New church" else "Sign in") },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.navigationBarsPadding(),
                        )
                    }
                },
            ) { padding ->
                val header: @Composable () -> Unit = {
                    CarolsHeader(
                        query = query,
                        onQueryChange = viewModel::onSearchChanged,
                        searchOpen = searchOpen,
                        onCloseSearch = {
                            searchOpen = false
                            viewModel.onSearchChanged("")
                        },
                        activeChurch = activeChurch,
                        contentTab = contentTab,
                        songCount = churchSongs.size,
                        pdfCount = churchPdfs.size,
                        viewModel = viewModel,
                    )
                }
                if (isLandscape) {
                    Row(Modifier.fillMaxSize().padding(padding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp)) { header() }
                        listPane(Modifier.weight(1.2f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        header()
                        listPane(Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
        }

        ExpressiveOverlayScreen(item = openSong, onDismiss = { openSongId = null }) { song ->
            val readerSongs = remember(song.churchId, churchSongs) {
                churchSongs.takeIf { list -> list.any { it.id == song.id } } ?: viewModel.filteredSongs(song.churchId).ifEmpty { listOf(song) }
            }
            CarolSongDetailScreen(
                songs = readerSongs,
                startSongId = song.id,
                churchName = churches.find { it.id == song.churchId }?.name.orEmpty(),
                onBackClick = { openSongId = null },
                canEdit = viewModel::canEditSong,
                onEdit = { editSongTarget = it },
            )
        }

        ExpressiveOverlayScreen(item = openPdf, onDismiss = { openPdfId = null }) { pdf ->
            CarolPdfDetailScreen(title = pdf.title, pdfUrl = pdf.pdfUrl, onBackClick = { openPdfId = null })
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
    val churchId = selectedChurchId
    if (showAddSong && churchId != null) {
        CarolSongFormDialog(
            onDismiss = { showAddSong = false },
            onSave = { title, number, kn, en, scale ->
                viewModel.addSong(churchId, title, number, kn, en, scale)
                showAddSong = false
            },
        )
    }
    if (showAddPdf && churchId != null) {
        AddCarolPdfDialog(
            onDismiss = { showAddPdf = false },
            onAdd = { title, number, uri ->
                viewModel.addPdf(churchId, title, number, uri)
                showAddPdf = false
            },
        )
    }
    editSongTarget?.let { song ->
        CarolSongFormDialog(
            initial = song,
            onDismiss = { editSongTarget = null },
            onSave = { title, number, kn, en, scale ->
                viewModel.updateSong(song.id, title, number, kn, en, scale)
                editSongTarget = null
            },
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarolsTopBar(title: String, canAdd: Boolean, canSearch: Boolean, onSearch: () -> Unit, onBack: () -> Unit, onAdd: () -> Unit) {
    val palette = rememberChristmasPalette()
    TopAppBar(
        title = {
            Text(
                title,
                fontFamily = ChristmasDisplayFont,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = palette.ink,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
            }
        },
        actions = {
            if (canSearch) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Search this church", tint = palette.ink)
                }
            }
            if (canAdd) {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add a song or PDF", tint = palette.ink)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
    )
}

/** Search, plus (inside a church) its description, sort menu and the Songs/PDFs switch. */
@Composable
private fun CarolsHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    searchOpen: Boolean,
    onCloseSearch: () -> Unit,
    activeChurch: CarolChurch?,
    contentTab: CarolContentTab,
    songCount: Int,
    pdfCount: Int,
    viewModel: CommunityCarolsViewModel,
) {
    val context = LocalContext.current
    val palette = rememberChristmasPalette()
    ReturnToCallerChip(Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp))
    if (activeChurch == null) {
        CarolsSearchField(query, onQueryChange, placeholder = "Search churches or carols, in English or ಕನ್ನಡ…")
        return
    }
    if (searchOpen) {
        CarolsSearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "Title, number, scale or lyrics, in English or ಕನ್ನಡ…",
            autoFocus = true,
            onClose = onCloseSearch,
        )
    }
    activeChurch.description?.takeIf { it.isNotBlank() }?.let { description ->
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.inkMuted,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    SortChipRow(viewModel = viewModel)
    StandardButtonGroup(
        buttonCount = 2,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        highContrast = true,
    ) {
        Button(
            index = 0,
            onClick = {
                HapticFeedbackManager.smoothClick(context)
                viewModel.setContentTab(CarolContentTab.SONGS)
            },
            icon = Icons.Default.MusicNote,
            label = "Songs ($songCount)",
            isSelected = contentTab == CarolContentTab.SONGS,
        )
        Button(
            index = 1,
            onClick = {
                HapticFeedbackManager.smoothClick(context)
                viewModel.setContentTab(CarolContentTab.PDFS)
            },
            icon = Icons.Default.PictureAsPdf,
            label = "PDFs ($pdfCount)",
            isSelected = contentTab == CarolContentTab.PDFS,
        )
    }
}

/** The scrolling part: shimmer while loading, then churches, or a church's songs or PDFs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarolsListPane(
    modifier: Modifier,
    isLoading: Boolean,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    query: String,
    activeChurch: CarolChurch?,
    contentTab: CarolContentTab,
    churches: List<CarolChurch>,
    churchSongs: List<CarolSong>,
    churchPdfs: List<CarolPdf>,
    favouriteSongs: List<CarolSong>,
    recentSongs: List<CarolSong>,
    favouriteIds: Set<String>,
    defaultChurchId: String?,
    onToggleDefault: (CarolChurch) -> Unit,
    emptyState: @Composable () -> Unit,
    churchStats: (String) -> Pair<Int, Int>,
    viewModel: CommunityCarolsViewModel,
    onOpenChurch: (CarolChurch) -> Unit,
    onDeleteChurch: (CarolChurch) -> Unit,
    onEditSong: (CarolSong) -> Unit,
    onOpenSong: (CarolSong) -> Unit,
    onOpenPdf: (CarolPdf) -> Unit,
) {
    PullToRefreshBox(isRefreshing = isSyncing, onRefresh = onRefresh, modifier = modifier) {
        when {
            isLoading -> CarolsShimmer()
            activeChurch == null && churches.isEmpty() -> emptyState()
            activeChurch == null -> LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding) {
                item(key = "your-carols") { YourCarolsStrip(favouriteSongs, recentSongs, onOpenSong) }
                items(churches, key = { it.id }) { church ->
                    val (songCount, pdfCount) = churchStats(church.id)
                    ChurchListCard(
                        church = church,
                        songCount = songCount,
                        pdfCount = pdfCount,
                        canDelete = viewModel.canDeleteChurch(church),
                        isDefault = church.id == defaultChurchId,
                        onTap = { onOpenChurch(church) },
                        onToggleDefault = { onToggleDefault(church) },
                        onDelete = { onDeleteChurch(church) },
                    )
                }
            }
            contentTab == CarolContentTab.SONGS && churchSongs.isEmpty() ->
                EmptyNote(if (query.isBlank()) "No songs yet" else "No songs match \"$query\"")
            contentTab == CarolContentTab.SONGS -> LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding) {
                items(churchSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isFavourite = song.id in favouriteIds,
                        canEdit = viewModel.canEditSong(song),
                        canDelete = viewModel.canDeleteSong(song),
                        onOpen = { onOpenSong(song) },
                        onEdit = { onEditSong(song) },
                        onDelete = { viewModel.deleteSong(song.id) },
                    )
                }
            }
            churchPdfs.isEmpty() -> EmptyNote("No PDFs yet")
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = ListPadding) {
                items(churchPdfs, key = { it.id }) { pdf ->
                    PdfRow(
                        pdf = pdf,
                        canDelete = viewModel.canDeletePdf(pdf),
                        onOpen = { onOpenPdf(pdf) },
                        onDelete = { viewModel.deletePdf(pdf.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    val palette = rememberChristmasPalette()
    // A scrollable list so pull-to-refresh still works on an empty church.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 80.dp)) {
        item {
            Text(text, color = palette.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
