package com.reyzie.hymns.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.reyzie.hymns.ui.motion.ExpressiveOverlayScreen
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.reyzie.hymns.data.*
import com.reyzie.hymns.data.displayMessage
import com.reyzie.hymns.data.imageUrl
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

enum class AdminTab {
    Main,
    Lyrics,
    Announcements
}

enum class LyricSongType {
    Hymn,
    Keerthane,
    OrderOfService,
    MangaloreHymn
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(AdminTab.Main) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Controls", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            onBackClick()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AdminMenuSelection(
                    onSelectTab = { currentTab = it }
                )
            }
        }

        ExpressiveOverlayScreen(
            visible = currentTab == AdminTab.Lyrics,
            onDismiss = { currentTab = AdminTab.Main }
        ) {
            LyricCorrectionPanel(onBackClick = { currentTab = AdminTab.Main })
        }

        ExpressiveOverlayScreen(
            visible = currentTab == AdminTab.Announcements,
            onDismiss = { currentTab = AdminTab.Main }
        ) {
            AnnouncementsManagerPanel(onBackClick = { currentTab = AdminTab.Main })
        }
    }
}

@Composable
private fun AdminMenuSelection(
    onSelectTab: (AdminTab) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clickable {
                    HapticFeedbackManager.smoothClick(context)
                    onSelectTab(AdminTab.Lyrics)
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Lyric Correction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Directly modify stanzas & bilingual texts for Hymns, Keerthanes & Order of Service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clickable {
                    HapticFeedbackManager.smoothClick(context)
                    onSelectTab(AdminTab.Announcements)
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Announcements Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trigger, modify, or archive dynamic in-app announcements & popup warnings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricCorrectionPanel(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    var searchQuery by remember { mutableStateOf("") }

    val settingsViewModel: SettingsViewModel = viewModel()
    val remoteConfig by settingsViewModel.remoteAppConfig.collectAsState()
    val gitHubSyncService = remember { GitHubSyncService(context) }

    val hymnsRepo = remember { HymnsRepository(context) }
    val orderServiceRepo = remember { OrderOfServiceRepository(context) }

    fun syncFileToGitHub(filePath: String, content: String, message: String) {
        val rawToken = remoteConfig.githubToken
        if (rawToken.isNullOrBlank()) {
            Toast.makeText(context, "GitHub Token is missing in app_config! Cannot sync to remote.", Toast.LENGTH_LONG).show()
            return
        }
        val token = rawToken.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim()
        Log.d("GitHubSync", "Token length=${token.length}, prefix=${token.take(10)}…, rawLen=${rawToken.length}")
        val userEmail = com.reyzie.hymns.data.SupabaseService.getInstance().currentUser?.email
        val commitMsg = if (!userEmail.isNullOrBlank()) "$message by $userEmail" else message
        scope.launch {
            val error = gitHubSyncService.pushFileToGitHub(
                token = token,
                repo = "Reynold29/csi-hymns-vault",
                filePath = filePath,
                content = content,
                commitMessage = commitMsg
            )
            if (error == null) {
                Toast.makeText(context, "Successfully updated GitHub remote repo!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to update GitHub remote: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    var allHymns by remember { mutableStateOf<List<Hymn>>(emptyList()) }
    var allKeerthanes by remember { mutableStateOf<List<Keerthane>>(emptyList()) }
    var allOrderPages by remember { mutableStateOf<List<OrderPage>>(emptyList()) }
    var allMangaloreHymns by remember { mutableStateOf<List<Hymn>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Editor target states
    var editingHymn by remember { mutableStateOf<Hymn?>(null) }
    var editingKeerthane by remember { mutableStateOf<Keerthane?>(null) }
    var editingOrderPage by remember { mutableStateOf<OrderPage?>(null) }
    var editingMangaloreHymn by remember { mutableStateOf<Hymn?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            allHymns = hymnsRepo.loadHymns()
            allKeerthanes = hymnsRepo.loadKeerthanes()
            allMangaloreHymns = hymnsRepo.loadHymns(AppSection.MT)
            
            val regular = orderServiceRepo.loadPages("regular").pages
            val festival = orderServiceRepo.loadPages("festival").pages
            allOrderPages = regular + festival
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (editingHymn != null) {
        LyricEditorForm(
            title = "Edit Hymn ${editingHymn!!.number}",
            initialTitle = editingHymn!!.title,
            initialSignature = editingHymn!!.signature,
            initialLyrics = editingHymn!!.lyrics,
            initialKannadaLyrics = editingHymn!!.kannadaLyrics ?: "",
            onDismiss = { editingHymn = null },
            onSave = { title, signature, lyrics, kannada ->
                scope.launch {
                    val updated = editingHymn!!.copy(
                        title = title,
                        signature = signature,
                        lyrics = lyrics,
                        kannadaLyrics = kannada.takeIf { it.isNotBlank() }
                    )
                    hymnsRepo.saveHymn(updated)
                    Toast.makeText(context, "Hymn updated locally!", Toast.LENGTH_SHORT).show()
                    editingHymn = null
                    loadData()

                    // Read updated json content and push to github
                    val store = ContentLocalStore(context)
                    val json = store.readHymnsJson()
                    if (json != null) {
                        syncFileToGitHub("hymns_data.json", json, "Update Hymn ${updated.number} lyrics via App")
                    }
                }
            }
        )
    } else if (editingKeerthane != null) {
        LyricEditorForm(
            title = "Edit Keerthane ${editingKeerthane!!.number}",
            initialTitle = editingKeerthane!!.title,
            initialSignature = editingKeerthane!!.signature,
            initialLyrics = editingKeerthane!!.lyrics,
            initialKannadaLyrics = editingKeerthane!!.kannadaLyrics ?: "",
            onDismiss = { editingKeerthane = null },
            onSave = { title, signature, lyrics, kannada ->
                scope.launch {
                    val updated = editingKeerthane!!.copy(
                        title = title,
                        signature = signature,
                        lyrics = lyrics,
                        kannadaLyrics = kannada.takeIf { it.isNotBlank() }
                    )
                    hymnsRepo.saveKeerthane(updated)
                    Toast.makeText(context, "Keerthane updated locally!", Toast.LENGTH_SHORT).show()
                    editingKeerthane = null
                    loadData()

                    // Read updated json content and push to github
                    val store = ContentLocalStore(context)
                    val json = store.readKeerthaneJson()
                    if (json != null) {
                        syncFileToGitHub("keerthane_data.json", json, "Update Keerthane ${updated.number} lyrics via App")
                    }
                }
            }
        )
    } else if (editingOrderPage != null) {
        LyricEditorForm(
            title = "Edit Page ${editingOrderPage!!.pageNo} (${editingOrderPage!!.type.uppercase()})",
            initialTitle = editingOrderPage!!.title ?: "",
            initialSignature = "",
            initialLyrics = editingOrderPage!!.content,
            initialKannadaLyrics = "",
            showSignatureAndKannada = false,
            onDismiss = { editingOrderPage = null },
            onSave = { title, _, content, _ ->
                scope.launch {
                    val updated = editingOrderPage!!.copy(
                        title = title.takeIf { it.isNotBlank() },
                        content = content
                    )
                    orderServiceRepo.savePage(updated)
                    Toast.makeText(context, "Order Page updated locally!", Toast.LENGTH_SHORT).show()
                    editingOrderPage = null
                    loadData()

                    // Read updated json content and push to github
                    val store = ContentLocalStore(context)
                    val json = store.readOrderOfServiceJson()
                    if (json != null) {
                        syncFileToGitHub("order-of-service_data.json", json, "Update Order Page ${updated.pageNo} content via App")
                    }
                }
            }
        )
    } else if (editingMangaloreHymn != null) {
        LyricEditorForm(
            title = "Edit MT Hymn ${editingMangaloreHymn!!.number}",
            initialTitle = editingMangaloreHymn!!.title,
            initialSignature = editingMangaloreHymn!!.signature,
            initialLyrics = editingMangaloreHymn!!.lyrics,
            initialKannadaLyrics = editingMangaloreHymn!!.kannadaLyrics ?: "",
            onDismiss = { editingMangaloreHymn = null },
            onSave = { title, signature, lyrics, kannada ->
                scope.launch {
                    val updated = editingMangaloreHymn!!.copy(
                        title = title,
                        signature = signature,
                        lyrics = lyrics,
                        kannadaLyrics = kannada.takeIf { it.isNotBlank() }
                    )
                    hymnsRepo.saveHymn(updated, AppSection.MT)
                    Toast.makeText(context, "MT Hymn updated locally!", Toast.LENGTH_SHORT).show()
                    editingMangaloreHymn = null
                    loadData()

                    // Read updated json content and push to github
                    val store = ContentLocalStore(context)
                    val json = store.readMangaloreHymnsJson()
                    if (json != null) {
                        syncFileToGitHub("mangalore_hymns_data.json", json, "Update MT Hymn ${updated.number} lyrics via App")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lyric Correction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StandardButtonGroup(
            buttonCount = 4,
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
            Button(
                index = 2,
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    scope.launch { pagerState.animateScrollToPage(2) }
                },
                icon = Icons.Default.Book,
                label = "Order",
                isSelected = pagerState.currentPage == 2,
                autoSizeLabel = true
            )
            Button(
                index = 3,
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    scope.launch { pagerState.animateScrollToPage(3) }
                },
                icon = Icons.Default.LibraryMusic,
                label = "MT Tunes",
                isSelected = pagerState.currentPage == 3,
                autoSizeLabel = true
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by title or number...") },
                modifier = Modifier.weight(1f),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            var isRefreshing by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    if (!isRefreshing) {
                        isRefreshing = true
                        Toast.makeText(context, "Fetching latest lyrics from GitHub...", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            val result = com.reyzie.hymns.data.ContentSyncManager(context).syncAll()
                            isRefreshing = false
                            if (result.anyUpdated) {
                                Toast.makeText(context, "Successfully updated from remote!", Toast.LENGTH_SHORT).show()
                                loadData()
                            } else if (result.errorMessage != null) {
                                Toast.makeText(context, "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Local lyrics are already up to date.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isRefreshing,
                modifier = Modifier.size(48.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh from remote")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                ExpressiveCircularProgress(size = 50.dp)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> {
                        val filtered = allHymns.filter {
                            it.number.toString().contains(searchQuery) ||
                                    it.title.contains(searchQuery, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered) { hymn ->
                                Surface(
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        editingHymn = hymn
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = "${hymn.number}. ${hymn.title}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = hymn.signature,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    HapticFeedbackManager.smoothClick(context)
                                                    editingHymn = hymn
                                                },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        val filtered = allKeerthanes.filter {
                            it.number.toString().contains(searchQuery) ||
                                    it.title.contains(searchQuery, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered) { keerthane ->
                                Surface(
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        editingKeerthane = keerthane
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = "${keerthane.number}. ${keerthane.title}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = keerthane.signature,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    HapticFeedbackManager.smoothClick(context)
                                                    editingKeerthane = keerthane
                                                },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        val filtered = allOrderPages.filter {
                            it.pageNo.toString().contains(searchQuery) ||
                                    (it.title?.contains(searchQuery, ignoreCase = true) == true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered) { page ->
                                Surface(
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        editingOrderPage = page
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = "Page ${page.pageNo}. ${page.title ?: "Untitled"}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = "Type: ${page.type.uppercase()}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    HapticFeedbackManager.smoothClick(context)
                                                    editingOrderPage = page
                                                },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        val filtered = allMangaloreHymns.filter {
                            it.number.toString().contains(searchQuery) ||
                                    it.title.contains(searchQuery, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered) { hymn ->
                                Surface(
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        editingMangaloreHymn = hymn
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = "${hymn.number}. ${hymn.title}",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = hymn.signature,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    HapticFeedbackManager.smoothClick(context)
                                                    editingMangaloreHymn = hymn
                                                },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors()
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricEditorForm(
    title: String,
    initialTitle: String,
    initialSignature: String,
    initialLyrics: String,
    initialKannadaLyrics: String,
    showSignatureAndKannada: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (title: String, signature: String, lyrics: String, kannadaLyrics: String) -> Unit
) {
    val context = LocalContext.current
    var songTitle by remember { mutableStateOf(initialTitle) }
    var signature by remember { mutableStateOf(initialSignature) }
    var lyrics by remember { mutableStateOf(initialLyrics) }
    var kannadaLyrics by remember { mutableStateOf(initialKannadaLyrics) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                onSave(songTitle, signature, lyrics, kannadaLyrics)
                            },
                            enabled = songTitle.isNotBlank() && lyrics.isNotBlank()
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = songTitle,
                    onValueChange = { songTitle = it },
                    label = { Text("Song Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                if (showSignatureAndKannada) {
                    OutlinedTextField(
                        value = signature,
                        onValueChange = { signature = it },
                        label = { Text("Signature") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }

                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Lyrics (English / Rom)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    maxLines = 100,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                if (showSignatureAndKannada) {
                    OutlinedTextField(
                        value = kannadaLyrics,
                        onValueChange = { kannadaLyrics = it },
                        label = { Text("Lyrics (Kannada)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        maxLines = 100,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementsManagerPanel(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = remember { BroadcastMessageService(context) }

    var announcements by remember { mutableStateOf<List<InAppMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFormDialog by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<InAppMessage?>(null) }

    fun refresh() {
        scope.launch {
            isLoading = true
            announcements = service.getAllBroadcasts()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    if (showFormDialog || editingAnnouncement != null) {
        val activeModel = editingAnnouncement
        AnnouncementEditorDialog(
            announcement = activeModel,
            onDismiss = {
                showFormDialog = false
                editingAnnouncement = null
            },
            onSave = { item ->
                scope.launch {
                    val error = if (editingAnnouncement == null) {
                        service.createBroadcast(item)
                    } else {
                        service.updateBroadcast(item)
                    }
                    if (error == null) {
                        Toast.makeText(context, "Announcement saved to Supabase!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save: $error", Toast.LENGTH_LONG).show()
                    }
                    showFormDialog = false
                    editingAnnouncement = null
                    refresh()
                }
            },
            onDelete = { id ->
                scope.launch {
                    val active = announcements.firstOrNull { it.id == id }
                    if (active != null && !active.imageUrl.isNullOrBlank()) {
                        service.deleteAnnouncementImage(active.imageUrl!!)
                    }
                    val error = service.deleteBroadcast(id)
                    if (error == null) {
                        Toast.makeText(context, "Announcement deleted!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to delete: $error", Toast.LENGTH_LONG).show()
                    }
                    editingAnnouncement = null
                    refresh()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcements Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    showFormDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Announcement")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveCircularProgress(size = 56.dp)
                }
            } else if (announcements.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No announcements created yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(announcements) { broadcast ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticFeedbackManager.smoothClick(context)
                                    editingAnnouncement = broadcast
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = broadcast.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = broadcast.isActive,
                                        onCheckedChange = { isChecked ->
                                            scope.launch {
                                                val error = service.toggleBroadcastActive(broadcast.id, isChecked)
                                                if (error != null) {
                                                    Toast.makeText(context, "Failed to toggle status: $error", Toast.LENGTH_LONG).show()
                                                }
                                                refresh()
                                            }
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = broadcast.displayMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!broadcast.actionText.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "${broadcast.actionText}: ${broadcast.actionUrl}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (broadcast.isActive) "Active & Displaying" else "Inactive",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (broadcast.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )

                                    var isRetriggering by remember { mutableStateOf(false) }
                                    TextButton(
                                        onClick = {
                                            HapticFeedbackManager.smoothClick(context)
                                            if (!isRetriggering) {
                                                isRetriggering = true
                                                scope.launch {
                                                    val newId = java.util.UUID.randomUUID().toString()
                                                    val error = service.retriggerBroadcast(broadcast.id, newId, broadcast)
                                                    isRetriggering = false
                                                    if (error == null) {
                                                        Toast.makeText(context, "Alert re-triggered successfully for all users!", Toast.LENGTH_SHORT).show()
                                                        refresh()
                                                    } else {
                                                        Toast.makeText(context, "Failed to re-trigger: $error", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isRetriggering && broadcast.isActive,
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Re-trigger Alert", style = MaterialTheme.typography.labelMedium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementEditorDialog(
    announcement: InAppMessage?,
    onDismiss: () -> Unit,
    onSave: (InAppMessage) -> Unit,
    onDelete: (id: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val service = remember { BroadcastMessageService(context) }
    
    var title by remember { mutableStateOf(announcement?.title ?: "") }
    var imageUrl by remember { mutableStateOf(announcement?.imageUrl ?: "") }
    var message by remember { mutableStateOf(announcement?.displayMessage ?: "") }
    var actionText by remember { mutableStateOf(announcement?.actionText ?: "") }
    var actionUrl by remember { mutableStateOf(announcement?.actionUrl ?: "") }
    var isActive by remember { mutableStateOf(announcement?.isActive ?: true) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        Toast.makeText(context, "Uploading image to storage...", Toast.LENGTH_SHORT).show()
                        val fileName = "${java.util.UUID.randomUUID()}.jpg"
                        val url = service.uploadAnnouncementImage(fileName, bytes)
                        if (imageUrl.isNotBlank()) {
                            service.deleteAnnouncementImage(imageUrl)
                        }
                        imageUrl = url
                        Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                TopAppBar(
                    title = { Text(if (announcement == null) "New Announcement" else "Edit Announcement", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (announcement != null) {
                            IconButton(onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                onDelete(announcement.id)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                val item = InAppMessage(
                                    id = announcement?.id ?: java.util.UUID.randomUUID().toString(),
                                    title = title,
                                    message = if (imageUrl.isNotBlank()) "$message ||image_url=$imageUrl" else message,
                                    actionText = actionText.takeIf { it.isNotBlank() },
                                    actionUrl = actionUrl.takeIf { it.isNotBlank() },
                                    isActive = isActive,
                                    createdAt = announcement?.createdAt ?: java.time.Instant.now().toString()
                                )
                                onSave(item)
                            },
                            enabled = title.isNotBlank() && message.isNotBlank()
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Announcement Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Image Selection Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Alert Image (Optional)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (imageUrl.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            coil.compose.AsyncImage(
                                model = imageUrl,
                                contentDescription = "Uploaded image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        service.deleteAnnouncementImage(imageUrl)
                                        imageUrl = ""
                                        Toast.makeText(context, "Image removed!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Image", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Image")
                        }
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Announcement Message") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 20,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                OutlinedTextField(
                    value = actionText,
                    onValueChange = { actionText = it },
                    label = { Text("Button Text (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                OutlinedTextField(
                    value = actionUrl,
                    onValueChange = { actionUrl = it },
                    label = { Text("Button URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Active Status", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = "Show this announcement popup to all users.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        }
    }
}
