@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.reyzie.hymns.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.reyzie.hymns.ui.theme.contentOn

/**
 * Proper Semantic Material 3 Theme with Dynamic Color support.
 * Colors are derived from tonal palettes to ensure consistent contrast and accessibility.
 */
private val BaselineBlueSeed = Color(0xFF0061A4)

@Immutable
data class ChristmasStyle(
    val isEnabled: Boolean,
    val ornamentPrimary: Color,
    val ornamentSecondary: Color,
    val garlandColor: Color,
    val starColor: Color,
    val snowflakeColor: Color
)

val LocalChristmasStyle = staticCompositionLocalOf {
    ChristmasStyle(
        isEnabled = false,
        ornamentPrimary = Color.Unspecified,
        ornamentSecondary = Color.Unspecified,
        garlandColor = Color.Unspecified,
        starColor = Color.Unspecified,
        snowflakeColor = Color.Unspecified
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD1E4FF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFD7E3F7),
    onSecondary = Color(0xFF243140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFF2B8B5),
    onTertiary = Color(0xFF601410),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF111D2B),
    tertiary = Color(0xFFB3261E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF111318),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFD6E4F5),
    onSurfaceVariant = Color(0xFF111318),
    outline = Color(0xFF74777F)
)

internal fun generateSeedColorScheme(seedColor: Color, darkTheme: Boolean): ColorScheme {
    // We compute proper contrasting colors using luminance.
    val isLightSeed = seedColor.luminance() > 0.5f
    
    // For primary, if we are in darkTheme, we want a lighter/softer version of the color.
    val primary = if (darkTheme) {
        if (isLightSeed) seedColor else blendColor(seedColor, Color.White, 0.4f)
    } else {
        if (isLightSeed) blendColor(seedColor, Color.Black, 0.3f) else seedColor
    }

    val onPrimary = primary.contentOn()
    
    val primaryContainer = if (darkTheme) {
        blendColor(primary, Color.Black, 0.6f)
    } else {
        blendColor(primary, Color.White, 0.85f)
    }

    val onPrimaryContainer = primaryContainer.contentOn()

    val secondary = if (darkTheme) {
        blendColor(primary, Color.White, 0.2f)
    } else {
        blendColor(primary, Color.Black, 0.2f)
    }
    val onSecondary = secondary.contentOn()
    
    val secondaryContainer = if (darkTheme) {
        blendColor(secondary, Color.Black, 0.7f)
    } else {
        blendColor(secondary, Color.White, 0.8f)
    }
    val onSecondaryContainer = secondaryContainer.contentOn()

    val background = if (darkTheme) Color(0xFF111318) else Color(0xFFF9F9FF)
    val onBackground = if (darkTheme) Color(0xFFE2E2E9) else Color(0xFF111318)
    val surface = if (darkTheme) Color(0xFF111318) else Color(0xFFF9F9FF)
    val onSurface = if (darkTheme) Color(0xFFE2E2E9) else Color(0xFF111318)

    val surfaceVariant = if (darkTheme) Color(0xFF32353B) else Color(0xFFD6E4F5)
    val onSurfaceVariant = if (darkTheme) Color(0xFFC3C7CF) else Color(0xFF111318)
    val outline = if (darkTheme) Color(0xFF8D9199) else Color(0xFF74777F)

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    }
}

private fun blendColor(color: Color, with: Color, fraction: Float): Color {
    val r = color.red + (with.red - color.red) * fraction
    val g = color.green + (with.green - color.green) * fraction
    val b = color.blue + (with.blue - color.blue) * fraction
    return Color(r, g, b, color.alpha)
}

/** Ensures distinct surface-container roles — wider steps so layered controls stay readable. */
internal fun ColorScheme.withExpressiveSurfaceTones(darkTheme: Boolean): ColorScheme {
    val s = surface
    return if (darkTheme) {
        copy(
            surfaceDim = blendColor(s, Color.Black, 0.22f),
            surfaceBright = blendColor(s, Color.White, 0.14f),
            surfaceContainerLowest = blendColor(s, Color.Black, 0.06f),
            surfaceContainerLow = blendColor(s, Color.White, 0.06f),
            surfaceContainer = blendColor(s, Color.White, 0.10f),
            surfaceContainerHigh = blendColor(s, Color.White, 0.16f),
            surfaceContainerHighest = blendColor(s, Color.White, 0.22f),
            surfaceVariant = if (surfaceVariant == s) blendColor(s, Color.White, 0.12f) else surfaceVariant
        )
    } else {
        copy(
            surfaceDim = blendColor(s, Color.Black, 0.04f),
            surfaceBright = Color.White,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFEEF4FC),
            surfaceContainer = Color(0xFFE3EDF8),
            surfaceContainerHigh = Color(0xFFD6E4F5),
            surfaceContainerHighest = Color(0xFFC8DBF2),
            surfaceVariant = if (surfaceVariant == s) Color(0xFFD6E4F5) else surfaceVariant
        )
    }
}

private val ReadableLightText = Color(0xFF111318)
private val ReadableDarkText = Color(0xFFE8EAED)
private val ReadableLightMuted = Color(0xFF2B3138)

/** Pairs every container/surface tone with readable foreground (dark on light, light on dark). */
internal fun ColorScheme.withReadableContrast(darkTheme: Boolean): ColorScheme {
    val body = if (darkTheme) ReadableDarkText else ReadableLightText
    val muted = if (darkTheme) Color(0xFFC3C7CF) else ReadableLightMuted

    return copy(
        onBackground = body,
        onSurface = body,
        onSurfaceVariant = muted,
        onPrimary = primary.contentOn(),
        onSecondary = secondary.contentOn(),
        onTertiary = tertiary.contentOn(),
        onPrimaryContainer = primaryContainer.contentOn(),
        onSecondaryContainer = secondaryContainer.contentOn(),
        onTertiaryContainer = tertiaryContainer.contentOn(),
    )
}

private fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else Math.pow(((red + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else Math.pow(((green + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else Math.pow(((blue + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

@Composable
fun CSIHymnsBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    seedColor: Color? = null,
    dynamicColor: Boolean = true,
    isChristmasMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        // 0. Christmas override
        isChristmasMode -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFDC143C), // Crimson / Candy Cane Red
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFF8B0000),
                    onPrimaryContainer = Color(0xFFFFDAD6),
                    secondary = Color(0xFF228B22), // Forest Green
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFF005300),
                    onSecondaryContainer = Color(0xFFB8FFA9),
                    tertiary = Color(0xFFFFD700), // Gold
                    onTertiary = Color(0xFF3A3000),
                    background = Color(0xFF0F0F1A), // Deep Christmas Night
                    onBackground = Color(0xFFE2E2E9),
                    surface = Color(0xFF1A1A2E),
                    onSurface = Color(0xFFE2E2E9),
                    surfaceVariant = Color(0xFF16213E),
                    onSurfaceVariant = Color(0xFFC3C7CF),
                    outline = Color(0xFF8D9199)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFB22222), // Deep Red
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFDAD6),
                    onPrimaryContainer = Color(0xFF410002),
                    secondary = Color(0xFF228B22), // Forest Green
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFB8FFA9),
                    onSecondaryContainer = Color(0xFF002200),
                    tertiary = Color(0xFFDAA520), // Goldenrod
                    onTertiary = Color(0xFFFFFFFF),
                    background = Color(0xFFFFF8F0), // Warm white
                    onBackground = Color(0xFF1A1A1A),
                    surface = Color(0xFFFFFAFA), // Snow
                    onSurface = Color(0xFF1A1A1A),
                    surfaceVariant = Color(0xFFF5F5F5),
                    onSurfaceVariant = Color(0xFF43474E),
                    outline = Color(0xFF74777F)
                )
            }
        }
        // 1. Seed Color (Custom Accent Color) is provided and takes precedence over default System dynamic scheme if selected
        seedColor != null -> {
            ExpressivePalettes.schemeForSeed(seedColor.toArgb(), darkTheme)
                ?: generateSeedColorScheme(seedColor, darkTheme)
        }
        // 2. Dynamic Color (Android 12+) - Official Tonal Palette Generation
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 3. Fallback to Baseline Blue or Default ColorScheme
        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    // Apply AMOLED black optimization properly by strictly using surface/background roles
    val finalColorScheme = (if (darkTheme && amoledBlack) {
        colorScheme.copy(
            surface = Color.Black,
            background = Color.Black,
            onSurface = colorScheme.onSurface,
            onBackground = colorScheme.onBackground
        )
    } else {
        colorScheme
    }).withExpressiveSurfaceTones(darkTheme).withReadableContrast(darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val christmasStyle = if (isChristmasMode) {
        if (darkTheme) {
            ChristmasStyle(
                isEnabled = true,
                ornamentPrimary = Color(0xFFDC143C),
                ornamentSecondary = Color(0xFFFFD700),
                garlandColor = Color(0xFF006400),
                starColor = Color(0xFFDAA520),
                snowflakeColor = Color(0xB3FFFFFF)
            )
        } else {
            ChristmasStyle(
                isEnabled = true,
                ornamentPrimary = Color(0xFFB22222),
                ornamentSecondary = Color(0xFFFFD700),
                garlandColor = Color(0xFF228B22),
                starColor = Color(0xFFDAA520),
                snowflakeColor = Color(0xE6FFFFFF)
            )
        }
    } else {
        LocalChristmasStyle.current.copy(isEnabled = false)
    }

    CompositionLocalProvider(LocalChristmasStyle provides christmasStyle) {
        MaterialExpressiveTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}
