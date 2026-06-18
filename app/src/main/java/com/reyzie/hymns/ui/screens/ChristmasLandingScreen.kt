package com.reyzie.hymns.ui.screens

import android.net.Uri
import com.reyzie.hymns.ui.motion.ExpressiveOverlayScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.reyzie.hymns.data.ChristmasCarol
import com.reyzie.hymns.data.MusicalScales
import com.reyzie.hymns.ui.viewmodels.CarolSortOrder
import com.reyzie.hymns.ui.viewmodels.ChristmasCarolsViewModel
import com.reyzie.hymns.utils.HapticFeedbackManager
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.TextFieldDefaults
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun ChristmasLandingScreen(
    onOpenHymns: () -> Unit,
    onOpenKeerthanes: () -> Unit,
    onOpenCarols: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val infinite = rememberInfiniteTransition(label = "snow")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B263B),
                        Color(0xFF1B263B),
                        Color(0xFF0D1B2A)
                    )
                )
            )
    ) {
        SnowfallLayer(progress = progress)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        onMenuClick()
                    }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFB22222).copy(alpha = 0.22f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            "🎄",
                            modifier = Modifier.padding(14.dp),
                            fontSize = 28.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Merry Christmas!",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Glory to God in the highest",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Text("⭐", fontSize = 24.sp)
                }
            }
            item {
                ChristmasCategoryCard(
                    title = "Hymns",
                    subtitle = "Traditional hymns from the CSI hymn book",
                    emoji = "🎵",
                    gradient = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
                    onTap = onOpenHymns
                )
                Spacer(modifier = Modifier.height(14.dp))
                ChristmasCategoryCard(
                    title = "Keerthane",
                    subtitle = "Kannada devotional songs and lyrics",
                    emoji = "🎶",
                    gradient = listOf(Color(0xFF1976D2), Color(0xFF0D47A1)),
                    onTap = onOpenKeerthanes
                )
                Spacer(modifier = Modifier.height(14.dp))
                ChristmasCategoryCard(
                    title = "Christmas Carols",
                    subtitle = "Celebrate the season with festive songs",
                    emoji = "🎄",
                    gradient = listOf(Color(0xFFC62828), Color(0xFF8E0000)),
                    highlighted = true,
                    onTap = onOpenCarols
                )
            }
        }
    }
}

@Composable
private fun SnowfallLayer(progress: Float) {
    val flakes = remember {
        List(110) { idx ->
            val x = (idx * 47 % 100) / 100f
            val y = (idx * 71 % 100) / 100f
            val size = ((idx * 13 % 4) + 1).toFloat()
            Triple(x, y, size)
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        flakes.forEach { (x, y, fSize) ->
            val yy = ((y + progress * (0.12f + fSize * 0.03f)) % 1f) * size.height
            val drift = sin((progress + y) * 6.283f) * 16f
            val xx = x * size.width + drift
            drawCircle(
                color = Color.White.copy(alpha = 0.35f + fSize * 0.1f),
                radius = fSize,
                center = Offset(xx, yy)
            )
        }
    }
}

@Composable
private fun ChristmasCategoryCard(
    title: String,
    subtitle: String,
    emoji: String,
    gradient: List<Color>,
    onTap: () -> Unit,
    highlighted: Boolean = false
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clickable {
                HapticFeedbackManager.smoothClick(context)
                onTap()
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlighted) 12.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 26.sp)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChristmasCarolsScreen(
    viewModel: ChristmasCarolsViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedChurch by viewModel.selectedChurch.collectAsState()
    var showDeleteChurch by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCarol by rememberSaveable { mutableStateOf<ChristmasCarol?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadLocal()
        viewModel.refreshRemote()
    }
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(error!!)
            viewModel.clearError()
        }
    }

    val churches = viewModel.groupedChurchesFiltered()
    val churchCarols = selectedChurch?.let { viewModel.churchCarolsFiltered(it) }.orEmpty()

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedChurch == null) "Christmas Carols" else selectedChurch!!,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        if (selectedChurch != null) viewModel.setSelectedChurch(null) else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        viewModel.refreshRemote()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    if (!viewModel.isAuthenticated) {
                        scope.launch { snackbarHostState.showSnackbar("Sign in to add carols.") }
                    } else {
                        showAddDialog = true
                    }
                },
                icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                text = { Text(if (viewModel.isAuthenticated) "Add Carol" else "Sign in to add") },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = query,
                    onValueChange = viewModel::onSearchChanged,
                    placeholder = {
                        Text(
                            if (selectedChurch == null) "Search churches or songs..." else "Search songs..."
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            if (selectedChurch == null) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(churches) { (churchName, carols) ->
                        ChurchCard(
                            churchName = churchName,
                            count = carols.size,
                            onTap = { viewModel.setSelectedChurch(churchName) },
                            canDelete = viewModel.canDeleteChurch(churchName),
                            onDelete = { showDeleteChurch = churchName }
                        )
                    }
                }
            } else {
                ChurchDetailHeader(viewModel = viewModel)
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    items(churchCarols, key = { it.id }) { carol ->
                        CarolTile(
                            carol = carol,
                            onOpen = { selectedCarol = carol },
                            canEdit = viewModel.canEditCarol(carol),
                            onDelete = { viewModel.deleteCarol(carol.id) }
                        )
                    }
                }
            }
            if (isLoading) {
                Text("Loading...", modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
            }
        }
    }

    ExpressiveOverlayScreen(
        item = selectedCarol,
        onDismiss = { selectedCarol = null }
    ) { carol ->
        CarolDetailScreen(
            carol = carol,
            onBackClick = { selectedCarol = null }
        )
    }
    }

    if (showDeleteChurch != null) {
        AlertDialog(
            onDismissRequest = { showDeleteChurch = null },
            title = { Text("Delete Church?") },
            text = { Text("Delete $showDeleteChurch and all carols?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChurch(showDeleteChurch!!)
                    showDeleteChurch = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteChurch = null }) { Text("Cancel") } }
        )
    }

    if (showAddDialog) {
        AddCarolDialog(
            selectedChurch = selectedChurch,
            onDismiss = { showAddDialog = false },
            onAddSong = { church, title, lyrics, number, scale ->
                viewModel.addSong(church, title, lyrics, number, scale)
                showAddDialog = false
            },
            onAddPdf = { church, title, pdfUri ->
                viewModel.addPdf(church, title, pdfUri)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ChurchCard(
    churchName: String,
    count: Int,
    onTap: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏛️")
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(churchName, fontWeight = FontWeight.Bold)
                Text("$count carols", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ChurchDetailHeader(viewModel: ChristmasCarolsViewModel) {
    val sortOrder by viewModel.sortOrder.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            AssistChip(
                onClick = { expanded = true },
                label = { Text(if (sortOrder == CarolSortOrder.NUMBER) "By Number" else "Newest") },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) }
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("By Song Number") },
                    onClick = {
                        viewModel.onSortChanged(CarolSortOrder.NUMBER)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Newest First") },
                    onClick = {
                        viewModel.onSortChanged(CarolSortOrder.NEWEST)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CarolTile(
    carol: ChristmasCarol,
    onOpen: () -> Unit,
    canEdit: Boolean,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onOpen() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!carol.songNumber.isNullOrBlank()) {
                    Text(carol.songNumber, modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text(carol.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                if (canEdit) {
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete") }
                }
            }
            if (carol.hasPdf) {
                Text(
                    "PDF carol — tap to open",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(carol.lyrics ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 4)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Scale: ${carol.scale}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete song?") },
            text = { Text("Remove ${carol.title}?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDelete = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCarolDialog(
    selectedChurch: String?,
    onDismiss: () -> Unit,
    onAddSong: (church: String, title: String, lyrics: String, number: String?, scale: String) -> Unit,
    onAddPdf: (church: String, title: String, pdfUri: Uri) -> Unit
) {
    var churchName by remember { mutableStateOf(selectedChurch ?: "") }
    var title by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var scale by remember { mutableStateOf("C Major") }
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var isPdfMode by remember { mutableStateOf(false) }
    var scaleExpanded by remember { mutableStateOf(false) }
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        pickedPdfUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Christmas Carol") },
        text = {
            Column {
                Row {
                    AssistChip(onClick = { isPdfMode = false }, label = { Text("Song") }, leadingIcon = { Icon(Icons.Default.MusicNote, null) })
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(onClick = { isPdfMode = true }, label = { Text("PDF") }, leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) })
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = churchName, onValueChange = { churchName = it }, label = { Text("Church/Group") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                if (isPdfMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (pickedPdfUri != null) "PDF selected" else "No PDF selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { pdfPicker.launch("application/pdf") }) {
                        Text(if (pickedPdfUri != null) "Change PDF" else "Pick PDF")
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Song Number (optional)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = scaleExpanded, onExpandedChange = { scaleExpanded = !scaleExpanded }) {
                        OutlinedTextField(
                            value = scale,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scaleExpanded) },
                            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            label = { Text("Scale") }
                        )
                        ExposedDropdownMenu(expanded = scaleExpanded, onDismissRequest = { scaleExpanded = false }) {
                            MusicalScales.allScales.take(12).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        scale = option
                                        scaleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = lyrics, onValueChange = { lyrics = it }, label = { Text("Lyrics") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (churchName.isBlank() || title.isBlank()) return@TextButton
                if (isPdfMode) {
                    val uri = pickedPdfUri ?: return@TextButton
                    onAddPdf(churchName, title, uri)
                } else {
                    if (lyrics.isBlank()) return@TextButton
                    onAddSong(churchName, title, lyrics, number.ifBlank { null }, scale)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
