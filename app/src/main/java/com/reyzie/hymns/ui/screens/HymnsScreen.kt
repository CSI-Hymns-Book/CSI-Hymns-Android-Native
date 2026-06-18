package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.reyzie.hymns.R
import com.reyzie.hymns.data.Hymn
import androidx.compose.ui.graphics.Color
import com.reyzie.hymns.ui.viewmodels.SettingsViewModel
import com.reyzie.hymns.utils.HapticFeedbackManager
import com.reyzie.hymns.utils.expressiveClick
import com.reyzie.hymns.utils.jiggle
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.ui.widgets.ExpressiveScreenTopBar
import com.reyzie.hymns.ui.widgets.GroupButtonVariant
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HymnsScreen(
    viewModel: HymnsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onHymnClick: (Hymn) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val isChristmasMode by settingsViewModel.isChristmasMode.collectAsState()
    val filteredHymns by viewModel.filteredHymns.collectAsState()
    val groupedHymns by viewModel.groupedHymns.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ExpressiveScreenTopBar(
                title = if (isChristmasMode) "Christmas Carols" else "CSI Kannada Hymns",
                onMenuClick = onSettingsClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern Search Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search number, title, or meter...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    HapticFeedbackManager.smoothClick(context)
                                    viewModel.clearSearch() 
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }

        StandardButtonGroup(
            buttonCount = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                index = 0,
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    viewModel.onSortOrderChanged(SortOrder.NUMBER)
                },
                icon = Icons.Default.FormatListNumbered,
                label = "Number",
                isSelected = sortOrder == SortOrder.NUMBER,
                variant = GroupButtonVariant.Filled
            )
            Button(
                index = 1,
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    viewModel.onSortOrderChanged(SortOrder.METER)
                },
                icon = Icons.Default.MusicNote,
                label = "Meter",
                isSelected = sortOrder == SortOrder.METER,
                variant = GroupButtonVariant.Tonal
            )
            Button(
                index = 2,
                onClick = {
                    HapticFeedbackManager.smoothClick(context)
                    viewModel.refreshHymns()
                },
                icon = Icons.Default.Refresh,
                label = "Refresh",
                isSelected = isLoading,
                variant = GroupButtonVariant.Accent
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ExpressiveCircularProgress()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                if (sortOrder == SortOrder.METER) {
                    groupedHymns.forEach { (signature, hymns) ->
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (signature.isEmpty()) "(No meter)" else signature,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    hymns.forEach { hymn ->
                                        HymnListTile(hymn = hymn, onClick = { onHymnClick(hymn) })
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredHymns) { hymn ->
                        HymnListTile(hymn = hymn, onClick = { onHymnClick(hymn) })
                    }
                }
            }
        }
    }
}
}

@Composable
fun HymnListTile(hymn: Hymn, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = {
                HapticFeedbackManager.smoothClick(context)
                onClick()
            }),
        shape = RoundedCornerShape(24.dp), // Expressive corner
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                        painter = painterResource(id = R.drawable.hymn),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hymn ${hymn.number}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = hymn.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                if (hymn.signature.isNotEmpty()) {
                    Text(
                        text = hymn.signature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
