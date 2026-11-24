package com.reyzie.hymns.carols.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.carols.data.local.CarolReaderStore
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.CarolTextScale
import com.reyzie.hymns.carols.domain.splitBilingualLyrics
import com.reyzie.hymns.carols.domain.splitStanzas
import com.reyzie.hymns.interop.ReturnToCallerChip
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.DimChristmasSnow
import com.reyzie.hymns.ui.christmas.lists.christmasReaderFrame
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

private val ArrowSize = 40.dp
private val ArrowEdge = 4.dp

/**
 * The carol reader. Swipe, or use the arrows at either side, to move through the church's songs.
 * The top bar stars a song, sets the text size with a slider, starts Present mode and, for people
 * allowed to, edits the song. The screen stays awake while a carol is open.
 */
@Composable
fun CarolSongDetailScreen(
    songs: List<CarolSong>,
    startSongId: String,
    churchName: String,
    onBackClick: () -> Unit,
    canEdit: (CarolSong) -> Boolean = { false },
    onEdit: (CarolSong) -> Unit = {},
) {
    DimChristmasSnow()
    KeepScreenOn()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = rememberChristmasPalette()
    val store = remember { CarolReaderStore.getInstance(context) }
    val favourites by store.favourites.collectAsState()
    val textScale by store.textScale.collectAsState()
    val pages = songs.ifEmpty { return }
    val startIndex = pages.indexOfFirst { it.id == startSongId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = startIndex) { pages.size }
    val currentIndex = pager.currentPage.coerceIn(0, pages.lastIndex)
    val current = pages[currentIndex]
    val languages = remember { mutableStateMapOf<String, CarolLanguage>() }
    var presenting by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(current.id) { store.markViewed(current.id) }

    fun goTo(index: Int) {
        HapticFeedbackManager.smoothClick(context)
        scope.launch { pager.animateScrollToPage(index.coerceIn(0, pages.lastIndex)) }
    }

    ChristmasScreenBackground {
        androidx.compose.material3.Scaffold(
            modifier = Modifier.christmasReaderFrame(),
            containerColor = Color.Transparent,
            topBar = {
                CarolReaderTopBar(
                    title = current.title,
                    palette = palette,
                    isFavourite = current.id in favourites,
                    textScale = textScale,
                    previewLine = previewLine(current),
                    canEdit = canEdit(current),
                    onBack = onBackClick,
                    onToggleFavourite = {
                        HapticFeedbackManager.smoothClick(context)
                        store.toggleFavourite(current.id)
                    },
                    onTextScale = { store.setTextScale(it) },
                    onPresent = { presenting = true },
                    onEdit = { onEdit(current) },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    val song = pages[page]
                    CarolLyricsPage(
                        song = song,
                        churchName = churchName,
                        palette = palette,
                        textScale = textScale,
                        language = languages[song.id] ?: CarolLanguage.KANNADA,
                        onLanguageChange = { languages[song.id] = it },
                    )
                }
                if (pages.size > 1) {
                    PagerArrow(
                        forward = false,
                        enabled = currentIndex > 0,
                        palette = palette,
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = ArrowEdge),
                        onClick = { goTo(currentIndex - 1) },
                    )
                    PagerArrow(
                        forward = true,
                        enabled = currentIndex < pages.lastIndex,
                        palette = palette,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = ArrowEdge),
                        onClick = { goTo(currentIndex + 1) },
                    )
                    Text(
                        "${currentIndex + 1} of ${pages.size}",
                        color = palette.inkMuted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .navigationBarsPadding()
                            .padding(start = 20.dp, bottom = 22.dp),
                    )
                }
                ReturnToCallerChip(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 12.dp),
                )
            }
        }
        if (presenting) {
            val stanzas = remember(current.id, current.lyrics, languages[current.id]) { presentStanzas(current, languages[current.id]) }
            CarolPresenter(current.title, stanzas, palette, onClose = { presenting = false })
        }
    }
}

/** A round frosted-glass arrow at the side of the reader. Dimmed and inactive at the first or last song. */
@Composable
private fun PagerArrow(forward: Boolean, enabled: Boolean, palette: ChristmasPalette, modifier: Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(ArrowSize)
            .alpha(if (enabled) 1f else 0.35f)
            .background(palette.glassHigh, CircleShape)
            .border(1.dp, palette.edge, CircleShape),
    ) {
        Icon(
            if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = if (forward) "Next carol" else "Previous carol",
            tint = palette.ink,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarolReaderTopBar(
    title: String,
    palette: ChristmasPalette,
    isFavourite: Boolean,
    textScale: Float,
    previewLine: String,
    canEdit: Boolean,
    onBack: () -> Unit,
    onToggleFavourite: () -> Unit,
    onTextScale: (Float) -> Unit,
    onPresent: () -> Unit,
    onEdit: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, color = palette.ink, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
            }
        },
        actions = {
            TextSizeButton(palette, textScale, previewLine, onTextScale)
            IconButton(onClick = onToggleFavourite) {
                Icon(
                    if (isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = palette.gold,
                )
            }
            IconButton(onClick = onPresent) {
                Icon(Icons.Default.Slideshow, contentDescription = "Present verse by verse", tint = palette.ink)
            }
            if (canEdit) ReaderOverflow(palette, onEdit)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

/** "Aa" button that opens a small panel with a size slider and a live preview line. */
@Composable
private fun TextSizeButton(palette: ChristmasPalette, scale: Float, previewLine: String, onScale: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Text("Aa", color = palette.ink, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.semantics { contentDescription = "Text size" })
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(Modifier.width(290.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Text size", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(CarolTextScale.percentLabel(scale), color = palette.inkMuted, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", fontSize = 13.sp)
                    Slider(
                        value = scale,
                        onValueChange = { onScale(CarolTextScale.snap(it)) },
                        valueRange = CarolTextScale.MIN..CarolTextScale.MAX,
                        steps = CarolTextScale.SLIDER_STOPS,
                        colors = SliderDefaults.colors(thumbColor = palette.gold, activeTrackColor = palette.gold),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { contentDescription = "Text size slider" },
                    )
                    Text("A", fontSize = 26.sp)
                }
                Text(
                    previewLine,
                    fontSize = (BASE_LYRIC_SP * scale).sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(258.dp).padding(vertical = 4.dp),
                )
                TextButton(onClick = { onScale(CarolTextScale.DEFAULT) }, enabled = scale != CarolTextScale.DEFAULT) { Text("Reset to normal") }
            }
        }
    }
}

/** Three-dot menu in the reader's top bar, shown only to people who may edit the song. */
@Composable
private fun ReaderOverflow(palette: ChristmasPalette, onEdit: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = palette.ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Edit song") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    open = false
                    onEdit()
                },
            )
        }
    }
}

/** Keeps the screen awake while this composable is shown, so lyrics don't go dark mid-song. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

private fun lyricsFor(song: CarolSong, language: CarolLanguage?): String {
    val (kannada, english) = splitBilingualLyrics(song.lyrics)
    return if (language == CarolLanguage.ENGLISH && english != null) english else kannada
}

/** The first line of a song's lyrics, for the text-size preview. */
private fun previewLine(song: CarolSong): String =
    lyricsFor(song, null).lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: song.title

private fun presentStanzas(song: CarolSong, language: CarolLanguage?): List<String> =
    splitStanzas(lyricsFor(song, language))
