package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.SortByAlpha
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.reyzie.hymns.R
import com.reyzie.hymns.data.Keerthane
import com.reyzie.hymns.utils.HapticFeedbackManager
import com.reyzie.hymns.utils.expressiveClick
import com.reyzie.hymns.utils.jiggle
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.ui.widgets.ExpressiveScreenTopBar
import com.reyzie.hymns.ui.widgets.GroupButtonVariant
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.ui.widgets.SyncStatusDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeerthaneScreen(
    viewModel: KeerthaneViewModel = viewModel(),
    onKeerthaneClick: (Keerthane) -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val filteredKeerthanes by viewModel.filteredKeerthanes.collectAsState()
    val groupedKeerthanes by viewModel.groupedKeerthanes.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isLandscape) {
                ExpressiveScreenTopBar(
                    title = "CSI Kannada Keerthanes",
                    onMenuClick = onMenuClick
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
                if (!isLandscape) {
                    // Modern Search Bar (Portrait Static)
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
                            placeholder = { Text("Search number, title, meter…", style = MaterialTheme.typography.bodyMedium, maxLines = 1) },
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
                            maxLines = 1,
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
                                viewModel.onSortOrderChanged(SortOrder.TITLE)
                            },
                            icon = Icons.Default.SortByAlpha,
                            label = "Order",
                            isSelected = sortOrder == SortOrder.TITLE,
                            variant = GroupButtonVariant.Tonal
                        )
                        Button(
                            index = 2,
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                viewModel.refreshKeerthanes()
                            },
                            icon = Icons.Default.Refresh,
                            label = "Refresh",
                            isSelected = isLoading,
                            variant = GroupButtonVariant.Accent
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = if (isLandscape) 60.dp else 80.dp)
                ) {
                    if (isLandscape) {
                        item {
                            ExpressiveScreenTopBar(
                                title = "CSI Kannada Keerthanes",
                                onMenuClick = onMenuClick
                            )
                        }
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(28.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search number, title, meter…", style = MaterialTheme.typography.bodyMedium, maxLines = 1) },
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
                                    maxLines = 1,
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        item {
                            StandardButtonGroup(
                                buttonCount = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
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
                                        viewModel.onSortOrderChanged(SortOrder.TITLE)
                                    },
                                    icon = Icons.Default.SortByAlpha,
                                    label = "Order",
                                    isSelected = sortOrder == SortOrder.TITLE,
                                    variant = GroupButtonVariant.Tonal
                                )
                                Button(
                                    index = 2,
                                    onClick = {
                                        HapticFeedbackManager.smoothClick(context)
                                        viewModel.refreshKeerthanes()
                                    },
                                    icon = Icons.Default.Refresh,
                                    label = "Refresh",
                                    isSelected = isLoading,
                                    variant = GroupButtonVariant.Accent
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ExpressiveCircularProgress()
                            }
                        }
                    } else {
                        items(filteredKeerthanes) { k ->
                            KeerthaneListTile(keerthane = k, onClick = { onKeerthaneClick(k) })
                        }
                    }
                }
            }
        }
    }

    SyncStatusDialog(
        syncState = syncState,
        onDismiss = { viewModel.dismissSyncDialog() }
    )
}

@Composable
fun KeerthaneListTile(keerthane: Keerthane, onClick: () -> Unit) {
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
                        painter = painterResource(id = R.drawable.keerthane),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keerthane ${keerthane.number}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = keerthane.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                if (keerthane.signature.isNotEmpty()) {
                    Text(
                        text = keerthane.signature,
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
