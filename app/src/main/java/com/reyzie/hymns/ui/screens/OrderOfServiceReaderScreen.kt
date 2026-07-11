package com.reyzie.hymns.ui.screens

import com.reyzie.hymns.ui.motion.PredictiveExpressiveBackHandler
import com.reyzie.hymns.ui.motion.expressivePredictiveBackTransform
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Context
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.reyzie.hymns.utils.HapticFeedbackManager
import com.reyzie.hymns.data.OrderOfServiceRepository
import com.reyzie.hymns.data.OrderPage
import com.reyzie.hymns.data.ContentUpdateBus
import com.reyzie.hymns.ui.widgets.ExpressiveActionButton
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderOfServiceReaderScreen(
    type: String,
    onBackClick: () -> Unit
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var pages by remember { mutableStateOf<List<OrderPage>>(emptyList()) }
    var pageNoToIndex by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasSelectedPage by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { OrderOfServiceRepository(context) }

    val prefs = remember { context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) }
    var serviceFontSize by remember { mutableStateOf(prefs.getInt("global_service_font_size", 18)) }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(type) {
        loading = true
        error = null
        withContext(Dispatchers.IO) {
            val result = repository.loadPages(type)
            withContext(Dispatchers.Main) {
                pages = result.pages
                pageNoToIndex = result.pages.mapIndexed { idx, p -> p.pageNo to idx }.toMap()
                error = result.errorMessage?.takeIf { result.pages.isEmpty() }
                loading = false
            }
        }
    }

    LaunchedEffect(type) {
        ContentUpdateBus.orderUpdated.collect {
            val result = withContext(Dispatchers.IO) { repository.loadPages(type) }
            pages = result.pages
            pageNoToIndex = result.pages.mapIndexed { idx, p -> p.pageNo to idx }.toMap()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size.coerceAtLeast(1) }
    )

    fun jumpToPageNumber(pageNo: Int) {
        val idx = pageNoToIndex[pageNo]
        if (idx != null) {
            HapticFeedbackManager.mediumClick(context)
            hasSelectedPage = true
            scope.launch { pagerState.scrollToPage(idx) }
        }
    }

    var pageBackProgress by remember { mutableFloatStateOf(0f) }

    PredictiveExpressiveBackHandler(
        enabled = hasSelectedPage,
        onBack = {
            HapticFeedbackManager.smoothClick(context)
            hasSelectedPage = false
        },
        onProgress = { pageBackProgress = it }
    )

    val headerTitle = if (type == "regular") "Regular Sunday Order" else "Festival Order"
    val landingTitle = if (type == "festival") "Habbada Aaradhana Krama" else "Huduvada Aaradhana Krama"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                title = {
                    if (hasSelectedPage && pages.isNotEmpty()) {
                        val t = pages.getOrNull(pagerState.currentPage)?.title?.trim().orEmpty()
                        Text(
                            text = t.ifEmpty { headerTitle },
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    } else {
                        Text(headerTitle, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackManager.smoothClick(context)
                        if (hasSelectedPage) hasSelectedPage = false else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasSelectedPage && pages.isNotEmpty()) {
                        IconButton(onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            val newSize = (serviceFontSize - 2).coerceAtLeast(14)
                            serviceFontSize = newSize
                            prefs.edit().putInt("global_service_font_size", newSize).apply()
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Font Size")
                        }
                        
                        Text(
                            text = "$serviceFontSize",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        IconButton(onClick = {
                            HapticFeedbackManager.smoothClick(context)
                            val newSize = (serviceFontSize + 2).coerceAtMost(44)
                            serviceFontSize = newSize
                            prefs.edit().putInt("global_service_font_size", newSize).apply()
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Font Size")
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (!loading && pages.isNotEmpty()) {
                OrderReaderBottomBar(
                    hasSelectedPage = hasSelectedPage,
                    pages = pages,
                    currentIndex = pagerState.currentPage,
                    pageNoToIndex = pageNoToIndex,
                    onOpenAllPages = {
                        HapticFeedbackManager.smoothClick(context)
                        showBottomSheet = true
                    },
                    onJumpTo = { jumpToPageNumber(it) },
                    onPrev = {
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    },
                    onNext = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(
                    if (hasSelectedPage) {
                        Modifier.expressivePredictiveBackTransform(pageBackProgress)
                    } else {
                        Modifier
                    }
                )
        ) {
            when {
                loading -> ExpressiveCircularProgress(Modifier.align(Alignment.Center))
                error != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                pages.isEmpty() -> Text("No pages found", Modifier.align(Alignment.Center))
                !hasSelectedPage -> {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = if (isLandscape) 16.dp else 32.dp)
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            landingTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Enter a page number to jump directly to that page",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = jumpInput,
                            onValueChange = { jumpInput = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(onGo = {
                                jumpInput.toIntOrNull()?.let { jumpToPageNumber(it) }
                            }),
                            placeholder = { Text("Jump to page number (e.g., 1, 98, 100)") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(28.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        ExpressiveActionButton(
                            onClick = {
                                jumpInput.toIntOrNull()?.let { jumpToPageNumber(it) }
                            },
                            label = "Go to page",
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        ExpressiveActionButton(
                            onClick = {
                                HapticFeedbackManager.smoothClick(context)
                                hasSelectedPage = true
                                scope.launch { pagerState.scrollToPage(0) }
                            },
                            icon = Icons.Default.MenuBook,
                            label = "Open full book",
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { pageIndex ->
                        val pageData = pages[pageIndex]
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 640.dp)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                            Text(
                                "Page ${pageData.pageNo}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (!pageData.title.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    pageData.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                pageData.content,
                                fontSize = serviceFontSize.sp,
                                lineHeight = (serviceFontSize * 1.6).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(96.dp))
                        }
                    }
                    }
                }
            }
        }
    }

    if (showBottomSheet && pages.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text("All pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isLandscape) 8 else 4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = if (isLandscape) 180.dp else 400.dp)
                    ) {
                        itemsIndexed(pages) { index, page ->
                            val selected = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clickable {
                                        HapticFeedbackManager.smoothClick(context)
                                        hasSelectedPage = true
                                        scope.launch {
                                            pagerState.scrollToPage(index)
                                            sheetState.hide()
                                        }.invokeOnCompletion { showBottomSheet = false }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${page.pageNo}",
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
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
private fun OrderReaderBottomBar(
    hasSelectedPage: Boolean,
    pages: List<OrderPage>,
    currentIndex: Int,
    pageNoToIndex: Map<Int, Int>,
    onOpenAllPages: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val chipRadius = 3
    val start = (currentIndex - chipRadius).coerceAtLeast(0)
    val end = (currentIndex + chipRadius).coerceAtMost(pages.lastIndex)
    val visibleNumbers = if (pages.isNotEmpty()) pages.subList(start, end + 1).map { it.pageNo } else emptyList()

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!hasSelectedPage) {
                TextButton(onClick = onOpenAllPages) {
                    Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("All pages", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(4.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    visibleNumbers.forEach { no ->
                        val selected = false
                        FilterChip(
                            selected = selected,
                            onClick = { onJumpTo(no) },
                            label = { Text("$no") },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            } else {
                IconButton(onClick = onPrev, enabled = currentIndex > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                }
                OutlinedButton(
                    onClick = onOpenAllPages,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Page ${pages[currentIndex].pageNo}", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onNext, enabled = currentIndex < pages.lastIndex) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                }
            }
        }
    }
}
