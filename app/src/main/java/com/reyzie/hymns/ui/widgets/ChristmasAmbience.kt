package com.reyzie.hymns.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.reyzie.hymns.ui.christmas.ChristmasNightPalette
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette

/**
 * Colours for Christmas screens (carols list, carol reader, PDF viewer).
 * Built from [ChristmasPalette] so these screens match the new Christmas look.
 * The snowfall itself now lives in [com.reyzie.hymns.ui.christmas.ChristmasSnowfall].
 */
@Immutable
data class ChristmasScreenColors(
    val gradient: List<Color>,
    val onBackground: Color,
    val onBackgroundMuted: Color,
    val accent: Color,
)

private fun ChristmasPalette.toScreenColors() = ChristmasScreenColors(
    gradient = screenGradient,
    onBackground = ink,
    onBackgroundMuted = inkMuted,
    accent = gold,
)

val LocalChristmasScreenColors = staticCompositionLocalOf { ChristmasNightPalette.toScreenColors() }

@Composable
fun rememberChristmasScreenColors(): ChristmasScreenColors = rememberChristmasPalette().toScreenColors()

@Composable
fun christmasContentColor(): Color = LocalChristmasScreenColors.current.onBackground

@Composable
fun christmasContentColorMuted(alpha: Float = 0.78f): Color =
    LocalChristmasScreenColors.current.onBackground.copy(alpha = alpha)

/** Christmas screen backdrop: night sky in the dark theme, frosty dawn in the light theme. */
@Composable
fun ChristmasScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = rememberChristmasScreenColors()
    CompositionLocalProvider(LocalChristmasScreenColors provides colors) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors.gradient)),
            content = content,
        )
    }
}
