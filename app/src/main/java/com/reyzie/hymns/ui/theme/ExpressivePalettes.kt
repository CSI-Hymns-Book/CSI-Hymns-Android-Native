package com.reyzie.hymns.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Per accent seed: light + dark expressive palettes (primary / secondary / tertiary roles).
 * Tertiary is tuned per family so Refresh & accent controls stay distinct from primary.
 */
object ExpressivePalettes {
    private data class PalettePair(val light: ColorScheme, val dark: ColorScheme)

    private val bySeed = mapOf(
        0xFF6750A4.toInt() to pair(
            seed = Color(0xFF6750A4),
            tertiary = Color(0xFF7D5260),
            tertiaryContainer = Color(0xFFFFD8E4)
        ),
        0xFFD32F2F.toInt() to pair(
            seed = Color(0xFFD32F2F),
            tertiary = Color(0xFFE65100),
            tertiaryContainer = Color(0xFFFFCCBC)
        ),
        0xFFC62828.toInt() to pair(
            seed = Color(0xFFC62828),
            tertiary = Color(0xFF6D4C41),
            tertiaryContainer = Color(0xFFD7CCC8)
        ),
        0xFFE91E63.toInt() to pair(
            seed = Color(0xFFE91E63),
            tertiary = Color(0xFF00838F),
            tertiaryContainer = Color(0xFFB2EBF2)
        ),
        0xFF9C27B0.toInt() to pair(
            seed = Color(0xFF9C27B0),
            tertiary = Color(0xFF5C6BC0),
            tertiaryContainer = Color(0xFFC5CAE9)
        ),
        0xFF673AB7.toInt() to pair(
            seed = Color(0xFF673AB7),
            tertiary = Color(0xFF00796B),
            tertiaryContainer = Color(0xFFB2DFDB)
        ),
        0xFF3F51B5.toInt() to pair(
            seed = Color(0xFF3F51B5),
            tertiary = Color(0xFFFF6F00),
            tertiaryContainer = Color(0xFFFFE0B2)
        ),
        0xFF2196F3.toInt() to pair(
            seed = Color(0xFF2196F3),
            tertiary = Color(0xFF43A047),
            tertiaryContainer = Color(0xFFC8E6C9)
        ),
        0xFF03A9F4.toInt() to pair(
            seed = Color(0xFF03A9F4),
            tertiary = Color(0xFF8E24AA),
            tertiaryContainer = Color(0xFFE1BEE7)
        ),
        0xFF00BCD4.toInt() to pair(
            seed = Color(0xFF00BCD4),
            tertiary = Color(0xFFFF5722),
            tertiaryContainer = Color(0xFFFFCCBC)
        ),
        0xFF009688.toInt() to pair(
            seed = Color(0xFF009688),
            tertiary = Color(0xFF3949AB),
            tertiaryContainer = Color(0xFFC5CAE9)
        ),
        0xFF4CAF50.toInt() to pair(
            seed = Color(0xFF4CAF50),
            tertiary = Color(0xFF6A1B9A),
            tertiaryContainer = Color(0xFFE1BEE7)
        ),
        0xFF8BC34A.toInt() to pair(
            seed = Color(0xFF8BC34A),
            tertiary = Color(0xFF00897B),
            tertiaryContainer = Color(0xFFB2DFDB)
        ),
        0xFFCDDC39.toInt() to pair(
            seed = Color(0xFFCDDC39),
            tertiary = Color(0xFF5D4037),
            tertiaryContainer = Color(0xFFD7CCC8)
        ),
        0xFFFFEB3B.toInt() to pair(
            seed = Color(0xFFF9A825),
            tertiary = Color(0xFF1565C0),
            tertiaryContainer = Color(0xFFBBDEFB)
        ),
        0xFFFFC107.toInt() to pair(
            seed = Color(0xFFFFC107),
            tertiary = Color(0xFF6A1B9A),
            tertiaryContainer = Color(0xFFE1BEE7)
        ),
        0xFFFF9800.toInt() to pair(
            seed = Color(0xFFFF9800),
            tertiary = Color(0xFF00838F),
            tertiaryContainer = Color(0xFFB2EBF2)
        ),
        0xFFFF5722.toInt() to pair(
            seed = Color(0xFFFF5722),
            tertiary = Color(0xFF303F9F),
            tertiaryContainer = Color(0xFFC5CAE9)
        ),
        0xFF795548.toInt() to pair(
            seed = Color(0xFF795548),
            tertiary = Color(0xFF546E7A),
            tertiaryContainer = Color(0xFFCFD8DC)
        ),
        0xFF9E9E9E.toInt() to pair(
            seed = Color(0xFF616161),
            tertiary = Color(0xFF455A64),
            tertiaryContainer = Color(0xFFCFD8DC)
        ),
        0xFF607D8B.toInt() to pair(
            seed = Color(0xFF607D8B),
            tertiary = Color(0xFFFF7043),
            tertiaryContainer = Color(0xFFFFCCBC)
        )
    )

    fun schemeForSeed(seedArgb: Int, darkTheme: Boolean): ColorScheme? {
        val pair = bySeed[seedArgb] ?: return null
        return if (darkTheme) pair.dark else pair.light
    }

    private fun pair(seed: Color, tertiary: Color, tertiaryContainer: Color): PalettePair {
        val lightBase = generateSeedColorScheme(seed, darkTheme = false)
        val darkBase = generateSeedColorScheme(seed, darkTheme = true)
        return PalettePair(
            light = lightBase.copy(
                tertiary = tertiary,
                onTertiary = Color.White,
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = Color(0xFF1A1C1E)
            ),
            dark = darkBase.copy(
                tertiary = blendForDark(tertiary),
                onTertiary = Color(0xFF1A1C1E),
                tertiaryContainer = blendForDark(tertiaryContainer),
                onTertiaryContainer = Color(0xFFE2E2E9)
            )
        )
    }

    private fun blendForDark(c: Color): Color = Color(
        red = c.red * 0.55f + 0.12f,
        green = c.green * 0.55f + 0.12f,
        blue = c.blue * 0.55f + 0.14f,
        alpha = 1f
    )
}
