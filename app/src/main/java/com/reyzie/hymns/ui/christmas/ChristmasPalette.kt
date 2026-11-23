package com.reyzie.hymns.ui.christmas

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily

/**
 * Colour tokens for the "candlelit night" Christmas look.
 *
 * Night is used with the dark theme and Dawn with the light theme. Screens should read
 * colours from here instead of hard-coding them, so both themes stay in step.
 */
@Immutable
data class ChristmasPalette(
    val isNight: Boolean,
    // Sky, top to bottom, then the ground the content sits on.
    val skyTop: Color,
    val skyMid: Color,
    val skyLow: Color,
    val horizon: Color,
    val ground: Color,
    val groundDeep: Color,
    // Text and accents.
    val ink: Color,
    val inkMuted: Color,
    val gold: Color,
    val holly: Color,
    // Frosted-glass cards.
    val glass: Color,
    val glassHigh: Color,
    val edge: Color,
    val featureStart: Color,
    val featureEnd: Color,
    // One tint per collection.
    val pine: Color,
    val pineBackground: Color,
    val ice: Color,
    val iceBackground: Color,
    val ember: Color,
    val emberBackground: Color,
    // Snow.
    val snow: Color,
    val capSnow: Color,
    val capShadow: Color,
    val hollyLeaf: Color,
    val emojiAlpha: Float,
) {
    /** Background for ordinary Christmas screens (lists, readers). */
    val screenGradient: List<Color> get() = listOf(skyTop, skyMid, ground, groundDeep)
}

val ChristmasNightPalette = ChristmasPalette(
    isNight = true,
    skyTop = Color(0xFF050919),
    skyMid = Color(0xFF10183A),
    skyLow = Color(0xFF261C45),
    horizon = Color(0xFF4B2A4C),
    ground = Color(0xFF0E1124),
    groundDeep = Color(0xFF080A16),
    ink = Color(0xFFF5EFE4),
    inkMuted = Color(0xFFF5EFE4).copy(alpha = 0.7f),
    gold = Color(0xFFE9C27B),
    holly = Color(0xFFD6493D),
    glass = Color.White.copy(alpha = 0.06f),
    glassHigh = Color.White.copy(alpha = 0.11f),
    edge = Color(0xFFE9C27B).copy(alpha = 0.28f),
    featureStart = Color(0xFFD6493D).copy(alpha = 0.30f),
    featureEnd = Color(0xFF5C1834).copy(alpha = 0.32f),
    pine = Color(0xFF8FD3AA),
    pineBackground = Color(0xFF6EBE8C).copy(alpha = 0.14f),
    ice = Color(0xFF9CC8FF),
    iceBackground = Color(0xFF82B4FF).copy(alpha = 0.14f),
    ember = Color(0xFFF3B26B),
    emberBackground = Color(0xFFF3A05A).copy(alpha = 0.15f),
    snow = Color.White,
    capSnow = Color(0xFFEEF3FC),
    capShadow = Color.Black.copy(alpha = 0.35f),
    hollyLeaf = Color(0xFF4FAE74),
    emojiAlpha = 0.28f,
)

val ChristmasDawnPalette = ChristmasPalette(
    isNight = false,
    skyTop = Color(0xFF87A2CF),
    skyMid = Color(0xFFBCCDE8),
    skyLow = Color(0xFFE7D7D8),
    horizon = Color(0xFFF4CDAA),
    ground = Color(0xFFF1F4F9),
    groundDeep = Color(0xFFE1E8F2),
    ink = Color(0xFF17203A),
    inkMuted = Color(0xFF17203A).copy(alpha = 0.7f),
    gold = Color(0xFF9C6A17),
    holly = Color(0xFFB5362C),
    glass = Color.White.copy(alpha = 0.62f),
    glassHigh = Color.White.copy(alpha = 0.85f),
    edge = Color(0xFF9C6A17).copy(alpha = 0.3f),
    featureStart = Color(0xFFD6493D).copy(alpha = 0.16f),
    featureEnd = Color.White.copy(alpha = 0.7f),
    pine = Color(0xFF2D7A4F),
    pineBackground = Color(0xFF2D7A4F).copy(alpha = 0.12f),
    ice = Color(0xFF2B62A8),
    iceBackground = Color(0xFF2B62A8).copy(alpha = 0.11f),
    ember = Color(0xFFB4601A),
    emberBackground = Color(0xFFB4601A).copy(alpha = 0.12f),
    snow = Color(0xFF6E88B6),
    capSnow = Color.White,
    capShadow = Color(0xFF3C5582).copy(alpha = 0.3f),
    hollyLeaf = Color(0xFF2F8A55),
    emojiAlpha = 0.38f,
)

/** Background luminance below this counts as a dark theme. */
private const val DARK_BACKGROUND_LUMINANCE = 0.45f

/** Picks Night for the dark theme and Dawn for the light theme. */
@Composable
fun rememberChristmasPalette(): ChristmasPalette =
    if (MaterialTheme.colorScheme.background.luminance() < DARK_BACKGROUND_LUMINANCE) {
        ChristmasNightPalette
    } else {
        ChristmasDawnPalette
    }

/**
 * Display face for Christmas headings. Uses the system serif until a bundled display
 * font is added under res/font; swapping it here updates every Christmas heading.
 */
val ChristmasDisplayFont: FontFamily = FontFamily.Serif
