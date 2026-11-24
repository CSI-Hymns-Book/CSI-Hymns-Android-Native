package com.reyzie.hymns.carols.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.splitBilingualLyrics
import com.reyzie.hymns.carols.domain.splitStanzas
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

/** Which language of a bilingual carol is shown. */
enum class CarolLanguage { KANNADA, ENGLISH }

internal const val BASE_LYRIC_SP = 17f
private const val LYRIC_LINE_HEIGHT = 1.6f
private const val PRESENT_LYRIC_SP = 30f
private const val VERSE_GAP_DP = 22f
private val ReadingWidth = 560.dp
private val LandscapeReadingWidth = 920.dp

/** Room kept clear at each side for the previous and next arrows. */
private val ArrowGutter = 48.dp

/**
 * One carol in the reader, centred: church, a gold number chip, the title, the scale, the
 * Kannada/English switch for bilingual songs and then the lyrics verse by verse at the reader's
 * chosen size. Landscape shows both languages side by side.
 */
@Composable
fun CarolLyricsPage(
    song: CarolSong,
    churchName: String,
    palette: ChristmasPalette,
    textScale: Float,
    language: CarolLanguage,
    onLanguageChange: (CarolLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (kannada, english) = remember(song.id, song.lyrics) { splitBilingualLyrics(song.lyrics) }
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val lyricSize = (BASE_LYRIC_SP * textScale).sp
    val lyricStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = lyricSize,
        lineHeight = lyricSize * LYRIC_LINE_HEIGHT,
        color = palette.ink,
        textAlign = TextAlign.Center,
    )
    val sideBySide = english != null && isLandscape
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (sideBySide) LandscapeReadingWidth else ReadingWidth)
                .fillMaxWidth()
                .padding(horizontal = ArrowGutter)
                .padding(top = 8.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SongHeader(song, churchName, palette)
            if (english != null && !isLandscape) {
                LanguageSwitch(language, onLanguageChange, Modifier.padding(top = 16.dp))
            }
            Ornament(palette)
            if (sideBySide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    LyricColumn("ಕನ್ನಡ", kannada, palette, lyricStyle, textScale, Modifier.weight(1f))
                    LyricColumn("English", english.orEmpty(), palette, lyricStyle, textScale, Modifier.weight(1f))
                }
            } else {
                Verses(if (language == CarolLanguage.ENGLISH && english != null) english else kannada, lyricStyle, textScale)
            }
        }
    }
}

/** Church, number chip, title and scale, all centred. */
@Composable
private fun SongHeader(song: CarolSong, churchName: String, palette: ChristmasPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (churchName.isNotBlank()) {
            Text(
                churchName.uppercase(),
                color = palette.gold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
                textAlign = TextAlign.Center,
            )
        }
        song.songNumber?.takeIf { it.isNotBlank() }?.let { number ->
            Text(
                "No. $number",
                color = palette.gold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(palette.gold.copy(alpha = 0.16f), RoundedCornerShape(50))
                    .border(1.dp, palette.gold.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
        Text(
            song.title,
            color = palette.ink,
            fontFamily = ChristmasDisplayFont,
            fontSize = 28.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        song.scale.takeIf { it.isNotBlank() }?.let { scale ->
            Text(
                "Scale · $scale",
                color = palette.inkMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** A small gold flourish between the heading and the lyrics. */
@Composable
private fun Ornament(palette: ChristmasPalette) {
    Text("✦", color = palette.gold.copy(alpha = 0.85f), fontSize = 14.sp, modifier = Modifier.padding(vertical = 18.dp))
}

/** The lyrics as separate verses, centred, with a clear gap between them. */
@Composable
private fun Verses(text: String, style: androidx.compose.ui.text.TextStyle, textScale: Float) {
    val verses = remember(text) { splitStanzas(text).ifEmpty { listOf(text) } }
    Column(
        verticalArrangement = Arrangement.spacedBy((VERSE_GAP_DP * textScale).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        verses.forEach { Text(it, style = style, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun LanguageSwitch(language: CarolLanguage, onChange: (CarolLanguage) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    StandardButtonGroup(buttonCount = 2, modifier = modifier.width(220.dp), highContrast = true) {
        Button(
            index = 0,
            onClick = {
                HapticFeedbackManager.smoothClick(context)
                onChange(CarolLanguage.KANNADA)
            },
            label = "ಕನ್ನಡ",
            isSelected = language == CarolLanguage.KANNADA,
        )
        Button(
            index = 1,
            onClick = {
                HapticFeedbackManager.smoothClick(context)
                onChange(CarolLanguage.ENGLISH)
            },
            label = "English",
            isSelected = language == CarolLanguage.ENGLISH,
        )
    }
}

@Composable
private fun LyricColumn(
    heading: String,
    text: String,
    palette: ChristmasPalette,
    style: androidx.compose.ui.text.TextStyle,
    textScale: Float,
    modifier: Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(heading, color = palette.gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Verses(text, style, textScale)
    }
}

/**
 * Full-screen "present" mode for carol rounds: one verse at a time in large type on a dark
 * background. Swipe or tap the right half for the next verse, the left half to go back.
 */
@Composable
fun CarolPresenter(title: String, stanzas: List<String>, palette: ChristmasPalette, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val pages = stanzas.ifEmpty { listOf(title) }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    fun step(delta: Int) {
        val target = (pager.currentPage + delta).coerceIn(0, pages.lastIndex)
        scope.launch { pager.animateScrollToPage(target) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060F)),
    ) {
        // Taps are read on the pager itself instead of by full-screen boxes laid over it: a box on
        // top takes every touch that lands on it, so swipes never reached the pager.
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pages.size) {
                    detectTapGestures { tap -> step(presenterStepFor(tap.x, size.width.toFloat())) }
                }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("Previous verse") { step(-1); true },
                        CustomAccessibilityAction("Next verse") { step(1); true },
                    )
                },
        ) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = 28.dp, vertical = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    pages[page],
                    color = Color(0xFFF5EFE4),
                    fontSize = PRESENT_LYRIC_SP.sp,
                    lineHeight = (PRESENT_LYRIC_SP * 1.35f).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$title  ·  ${pager.currentPage + 1} of ${pages.size}",
                color = palette.gold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Stop presenting", tint = Color(0xFFF5EFE4))
            }
        }
    }
}

/** Which way a tap in Present mode turns the page: the left half goes back, the right half forward. */
internal fun presenterStepFor(tapX: Float, width: Float): Int = if (tapX < width / 2f) -1 else 1
