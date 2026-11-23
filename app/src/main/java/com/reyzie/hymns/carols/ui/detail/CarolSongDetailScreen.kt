package com.reyzie.hymns.carols.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.carols.data.local.CarolReaderStore
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.CarolTextScale
import com.reyzie.hymns.carols.domain.carolShareText
import com.reyzie.hymns.carols.domain.splitBilingualLyrics
import com.reyzie.hymns.carols.domain.splitStanzas
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.DimChristmasSnow
import com.reyzie.hymns.ui.christmas.lists.christmasReaderFrame
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.ui.widgets.ChristmasScreenBackground
import com.reyzie.hymns.utils.HapticFeedbackManager

/**
 * The carol reader. Swipe left or right to move through the church's songs. The top bar
 * stars a song, shares its lyrics, changes the text size and starts Present mode. The
 * screen stays awake while a carol is open.
 */
@Composable
fun CarolSongDetailScreen(
    songs: List<CarolSong>,
    startSongId: String,
    churchName: String,
    onBackClick: () -> Unit,
) {
    DimChristmasSnow()
    KeepScreenOn()
    val context = LocalContext.current
    val palette = rememberChristmasPalette()
    val store = remember { CarolReaderStore.getInstance(context) }
    val favourites by store.favourites.collectAsState()
    val textScale by store.textScale.collectAsState()
    val pages = songs.ifEmpty { return }
    val startIndex = pages.indexOfFirst { it.id == startSongId }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = startIndex) { pages.size }
    val current = pages[pager.currentPage.coerceIn(0, pages.lastIndex)]
    val languages = remember { mutableStateMapOf<String, CarolLanguage>() }
    var presenting by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(current.id) { store.markViewed(current.id) }

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
                    onBack = onBackClick,
                    onToggleFavourite = {
                        HapticFeedbackManager.smoothClick(context)
                        store.toggleFavourite(current.id)
                    },
                    onShare = { shareCarol(context, current, churchName, languages[current.id]) },
                    onTextScale = { store.setTextScale(it) },
                    onPresent = { presenting = true },
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
                    Text(
                        "${pager.currentPage + 1} of ${pages.size}  ·  swipe for the next carol",
                        color = palette.inkMuted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                    )
                }
            }
        }
        if (presenting) {
            val stanzas = remember(current.id, languages[current.id]) { presentStanzas(current, languages[current.id]) }
            CarolPresenter(current.title, stanzas, palette, onClose = { presenting = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarolReaderTopBar(
    title: String,
    palette: ChristmasPalette,
    isFavourite: Boolean,
    textScale: Float,
    onBack: () -> Unit,
    onToggleFavourite: () -> Unit,
    onShare: () -> Unit,
    onTextScale: (Float) -> Unit,
    onPresent: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, color = palette.ink, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.ink)
            }
        },
        actions = {
            IconButton(onClick = { onTextScale(CarolTextScale.smaller(textScale)) }, enabled = textScale > CarolTextScale.MIN) {
                Icon(Icons.Default.TextDecrease, contentDescription = "Smaller text", tint = palette.ink)
            }
            IconButton(onClick = { onTextScale(CarolTextScale.bigger(textScale)) }, enabled = textScale < CarolTextScale.MAX) {
                Icon(Icons.Default.TextIncrease, contentDescription = "Bigger text", tint = palette.ink)
            }
            IconButton(onClick = onToggleFavourite) {
                Icon(
                    if (isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites",
                    tint = palette.gold,
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share lyrics", tint = palette.ink)
            }
            IconButton(onClick = onPresent) {
                Icon(Icons.Default.Slideshow, contentDescription = "Present verse by verse", tint = palette.ink)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
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

private fun presentStanzas(song: CarolSong, language: CarolLanguage?): List<String> =
    splitStanzas(lyricsFor(song, language))

private fun shareCarol(context: android.content.Context, song: CarolSong, churchName: String, language: CarolLanguage?) {
    val text = carolShareText(song.title, song.songNumber, churchName, lyricsFor(song, language))
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, song.title)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share carol"))
}
