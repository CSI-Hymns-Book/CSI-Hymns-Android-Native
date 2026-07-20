package com.reyzie.hymns.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reyzie.hymns.data.AppSection
import com.reyzie.hymns.data.Hymn
import com.reyzie.hymns.data.Keerthane
import androidx.compose.runtime.LaunchedEffect
import com.reyzie.hymns.ui.viewmodels.FavoritesViewModel
import com.reyzie.hymns.ui.widgets.ExpressiveScreenTopBar
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = viewModel(),
    onHymnClick: (Hymn) -> Unit = {},
    onKeerthaneClick: (Keerthane) -> Unit = {},
    onMenuClick: () -> Unit = {},
    activeSection: AppSection = AppSection.CSI
) {
    LaunchedEffect(activeSection) {
        viewModel.setSection(activeSection)
    }
    val favoriteHymns by viewModel.favoriteHymns.collectAsState()
    val favoriteKeerthanes by viewModel.favoriteKeerthanes.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isLandscape) {
                ExpressiveScreenTopBar(
                    title = "My Favorites",
                    onMenuClick = onMenuClick
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
            ) {
            if (activeSection == AppSection.MT) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = if (isLandscape) 60.dp else 100.dp
                    )
                ) {
                    if (isLandscape) {
                        item {
                            ExpressiveScreenTopBar(
                                title = "My Favorites",
                                onMenuClick = onMenuClick
                            )
                        }
                    }

                    if (favoriteHymns.isEmpty()) {
                        item {
                            FavoritesEmptyState(
                                title = "No Favorite Hymns",
                                hint = "Tap the heart on any hymn detail screen to save it here."
                            )
                        }
                    } else {
                        items(favoriteHymns, key = { "hymn-${it.number}" }) { hymn ->
                            HymnListTile(hymn = hymn, isMt = activeSection == AppSection.MT, onClick = { onHymnClick(hymn) })
                        }
                    }
                }
            } else {
                if (!isLandscape) {
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
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp,
                                    bottom = if (isLandscape) 60.dp else 100.dp
                                )
                            ) {
                                if (isLandscape) {
                                    item {
                                        ExpressiveScreenTopBar(
                                            title = "My Favorites",
                                            onMenuClick = onMenuClick
                                        )
                                    }
                                    item {
                                        StandardButtonGroup(
                                            buttonCount = 2,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
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
                                    }
                                }

                                if (favoriteHymns.isEmpty()) {
                                    item {
                                        FavoritesEmptyState(
                                            title = "No Favorite Hymns",
                                            hint = "Tap the heart on any hymn detail screen to save it here."
                                        )
                                    }
                                } else {
                                    items(favoriteHymns, key = { "hymn-${it.number}" }) { hymn ->
                                        HymnListTile(hymn = hymn, isMt = activeSection == AppSection.MT, onClick = { onHymnClick(hymn) })
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 12.dp,
                                    bottom = if (isLandscape) 60.dp else 100.dp
                                )
                            ) {
                                if (isLandscape) {
                                    item {
                                        ExpressiveScreenTopBar(
                                            title = "My Favorites",
                                            onMenuClick = onMenuClick
                                        )
                                    }
                                    item {
                                        StandardButtonGroup(
                                            buttonCount = 2,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
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
                                    }
                                }

                                if (favoriteKeerthanes.isEmpty()) {
                                    item {
                                        FavoritesEmptyState(
                                            title = "No Favorite Keerthanes",
                                            hint = "Tap the heart on any keerthane detail screen to save it here."
                                        )
                                    }
                                } else {
                                    items(favoriteKeerthanes, key = { "k-${it.number}" }) { keerthane ->
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
}
}

@Composable
private fun FavoritesEmptyState(title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
