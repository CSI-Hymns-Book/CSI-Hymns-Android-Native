package com.reyzie.hymns.carols.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.domain.splitBilingualLyrics
import com.reyzie.hymns.ui.christmas.ChristmasDisplayFont
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.widgets.StandardButtonGroup
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.launch

/** Which language of a bilingual carol is shown. */
enum class CarolLanguage { KANNADA, ENGLISH }

private const val BASE_LYRIC_SP = 17f
private const val LYRIC_LINE_HEIGHT = 1.5f
private const val PRESENT_LYRIC_SP = 30f

/**
 * One carol in the reader: church and number, the Kannada/English switch for bilingual
 * songs, and the lyrics at the reader's chosen size. Landscape shows both languages side
 * by side.
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
    val (kannada, english) = remember(song.id) { splitBilingualLyrics(song.lyrics) }
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val lyricSize = (BASE_LYRIC_SP * textScale).sp
    val lyricStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = lyricSize,
        lineHeight = lyricSize * LYRIC_LINE_HEIGHT,
        color = palette.ink,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 96.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (churchName.isNotBlank()) {
                    Text(churchName.uppercase(), color = palette.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                }
                Text(
                    song.title,
                    color = palette.ink,
                    fontFamily = ChristmasDisplayFont,
                    fontSize = 26.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    listOfNotNull(song.songNumber?.let { "No. $it" }, song.scale.takeIf { it.isNotBlank() }).joinToString(" · "),
                    color = palette.inkMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (english != null && !isLandscape) {
            LanguageSwitch(language, onLanguageChange, Modifier.padding(top = 14.dp))
        }
        Spacer(Modifier.height(18.dp))
        if (english != null && isLandscape) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LyricColumn("ಕನ್ನಡ", kannada, palette, lyricStyle, Modifier.weight(1f))
                LyricColumn("English", english, palette, lyricStyle, Modifier.weight(1f))
            }
        } else {
            val text = if (language == CarolLanguage.ENGLISH && english != null) english else kannada
            Text(text, style = lyricStyle)
        }
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
    modifier: Modifier,
) {
    Column(modifier) {
        Text(heading, color = palette.gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text, style = style)
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
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060F)),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
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
        Row(Modifier.fillMaxSize()) {
            listOf(-1, 1).forEach { step ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = if (step < 0) "Previous verse" else "Next verse",
                        ) {
                            val target = (pager.currentPage + step).coerceIn(0, pages.lastIndex)
                            scope.launch { pager.animateScrollToPage(target) }
                        },
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
