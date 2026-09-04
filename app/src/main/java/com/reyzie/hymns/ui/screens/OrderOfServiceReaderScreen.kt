package com.reyzie.hymns.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.data.ContentUpdateBus
import com.reyzie.hymns.data.OrderIndexEntry
import com.reyzie.hymns.data.OrderOfServiceLoadResult
import com.reyzie.hymns.data.OrderOfServiceRepository
import com.reyzie.hymns.data.OrderPage
import com.reyzie.hymns.data.OrderPageSection
import com.reyzie.hymns.data.groupOrderPagesByIndex
import com.reyzie.hymns.ui.motion.PredictiveExpressiveBackHandler
import com.reyzie.hymns.ui.motion.expressivePredictiveBackTransform
import com.reyzie.hymns.ui.widgets.ExpressiveActionButton
import com.reyzie.hymns.ui.widgets.ExpressiveCircularProgress
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAGE_UNAVAILABLE = "Page not available yet"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrderOfServiceReaderScreen(
    type: String,
    onBackClick: () -> Unit
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var pages by remember { mutableStateOf<List<OrderPage>>(emptyList()) }
    var indexEntries by remember { mutableStateOf<List<OrderIndexEntry>>(emptyList()) }
    var pageNoToIndex by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasSelectedPage by remember { mutableStateOf(false) }
    var jumpInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { OrderOfServiceRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val prefs = remember { context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) }
    var serviceFontSize by remember { mutableStateOf(prefs.getInt("global_service_font_size", 18)) }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun applyLoad(result: OrderOfServiceLoadResult, isInitial: Boolean = false) {
        pages = result.pages
        indexEntries = result.index
        pageNoToIndex = result.pages.mapIndexed { idx, p -> p.pageNo to idx }.toMap()
        if (isInitial) {
            error = result.errorMessage?.takeIf { result.pages.isEmpty() }
            loading = false
        }
    }

    LaunchedEffect(type) {
        loading = true
        error = null
        withContext(Dispatchers.IO) {
            val result = repository.loadPages(type)
            withContext(Dispatchers.Main) { applyLoad(result, isInitial = true) }
        }
    }

    LaunchedEffect(type) {
        ContentUpdateBus.orderUpdated.collect {
            val result = withContext(Dispatchers.IO) { repository.loadPages(type) }
            applyLoad(result)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size.coerceAtLeast(1) }
    )

    val sections = remember(indexEntries, pages) {
        groupOrderPagesByIndex(indexEntries, pages)
    }

    fun jumpToPageNumber(pageNo: Int) {
        val idx = pageNoToIndex[pageNo]
        if (idx != null) {
            HapticFeedbackManager.mediumClick(context)
            hasSelectedPage = true
            scope.launch { pagerState.scrollToPage(idx) }
        } else {
            HapticFeedbackManager.smoothClick(context)
            scope.launch { snackbarHostState.showSnackbar(PAGE_UNAVAILABLE) }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            .padding(horizontal = 24.dp, vertical = if (isLandscape) 16.dp else 24.dp)
                            .align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
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
                        if (sections.isNotEmpty()) {
                            Spacer(Modifier.height(28.dp))
                            OrderContentsList(
                                sections = sections,
                                currentPageNo = null,
                                onOpenSection = { section ->
                                    val target = section.pages.firstOrNull()?.pageNo ?: section.startPageNo
                                    jumpToPageNumber(target)
                                },
                                onOpenPage = { jumpToPageNumber(it) }
                            )
                        }
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
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .heightIn(max = if (isLandscape) 280.dp else 520.dp)
                        .padding(horizontal = 20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Text("All pages", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (sections.isNotEmpty()) {
                        items(sections, key = { "${it.startPageNo}-${it.title}" }) { section ->
                            OrderSectionBlock(
                                section = section,
                                currentPageNo = pages.getOrNull(pagerState.currentPage)?.pageNo,
                                onOpenSection = {
                                    val target = section.pages.firstOrNull()?.pageNo ?: section.startPageNo
                                    jumpToPageNumber(target)
                                    if (section.pages.isNotEmpty()) {
                                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                                            showBottomSheet = false
                                        }
                                    }
                                },
                                onOpenPage = { pageNo ->
                                    jumpToPageNumber(pageNo)
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        showBottomSheet = false
                                    }
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderContentsList(
    sections: List<OrderPageSection>,
    currentPageNo: Int?,
    onOpenSection: (OrderPageSection) -> Unit,
    onOpenPage: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text("ಪರಿವಿಡಿ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap a heading or page number to open that section",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        sections.forEach { section ->
            OrderSectionBlock(
                section = section,
                currentPageNo = currentPageNo,
                onOpenSection = { onOpenSection(section) },
                onOpenPage = onOpenPage
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderSectionBlock(
    section: OrderPageSection,
    currentPageNo: Int?,
    onOpenSection: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    val available = section.pages.isNotEmpty()
    val badgeColor = if (available) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val badgeContent = if (available) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleColor = if (available) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onOpenSection)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${section.startPageNo}",
                    color = badgeContent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                section.title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
        if (available) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                section.pages.forEach { page ->
                    val selected = page.pageNo == currentPageNo
                    OrderPageNumberChip(
                        pageNo = page.pageNo,
                        selected = selected,
                        onClick = { onOpenPage(page.pageNo) }
                    )
                }
            }
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                PAGE_UNAVAILABLE,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun OrderPageNumberChip(
    pageNo: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$pageNo",
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun OrderReaderBottomBar(
    hasSelectedPage: Boolean,
    pages: List<OrderPage>,
    currentIndex: Int,
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
                        FilterChip(
                            selected = false,
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
