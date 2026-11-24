package com.reyzie.hymns.ui.christmas.lists

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.reyzie.hymns.ui.christmas.ChristmasEffectsLevel
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.LocalChristmasAmbience
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import kotlin.math.PI
import kotlin.math.sin

private const val LIGHT_SPACING_DP = 30f
private const val LIGHT_EDGE_INSET_DP = 5f
private const val LIGHT_TOP_MARGIN_DP = 28f
private const val LIGHT_BOTTOM_MARGIN_DP = 30f
private const val LIGHT_RADIUS_DP = 2.1f
private const val GLOW_RADIUS_DP = 5.5f
private const val TWINKLE_MILLIS = 3_600
private const val PHASE_STEP = 0.137f
private const val STILL_PHASE = 0.5f
private const val DRIFT_HEIGHT_DP = 7f
private const val DRIFT_LUMP_DP = 46f

/**
 * Gives a reading screen (hymn or carol lyrics) the Christmas look without covering any
 * text: a night-sky backdrop, a thin string of twinkling lights down both side edges, and
 * a low drift of snow along the very bottom. Everything is drawn in the outer margins
 * only and never takes touches. Does nothing when Christmas mode is off.
 */
@Composable
fun Modifier.christmasReaderFrame(): Modifier {
    val controller = LocalChristmasAmbience.current ?: return this
    val palette = rememberChristmasPalette()
    val animate = controller.effectiveLevel != ChristmasEffectsLevel.OFF
    val twinkle = if (animate) {
        rememberInfiniteTransition(label = "readerLights").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(TWINKLE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "readerLightsPhase",
        )
    } else {
        null
    }
    return this
        .background(Brush.verticalGradient(palette.screenGradient))
        .drawWithContent {
            drawContent()
            drawSideLights(palette, twinkle?.value ?: STILL_PHASE)
            drawBottomDrift(palette)
        }
}

private fun DrawScope.drawSideLights(palette: ChristmasPalette, phase: Float) {
    val u = density
    val colors = listOf(palette.gold, palette.holly, palette.pine, palette.ice)
    val top = LIGHT_TOP_MARGIN_DP * u
    val bottom = size.height - LIGHT_BOTTOM_MARGIN_DP * u
    if (bottom <= top) return
    val count = ((bottom - top) / (LIGHT_SPACING_DP * u)).toInt() + 1
    listOf(LIGHT_EDGE_INSET_DP * u, size.width - LIGHT_EDGE_INSET_DP * u).forEach { x ->
        drawLine(palette.gold.copy(alpha = 0.16f), Offset(x, top), Offset(x, bottom), strokeWidth = 0.8f * u)
        repeat(count) { i ->
            val y = top + i * LIGHT_SPACING_DP * u
            val glow = 0.35f + 0.45f * (0.5f + 0.5f * sin(2f * PI.toFloat() * (phase + i * PHASE_STEP)))
            val color = colors[i % colors.size]
            drawCircle(color.copy(alpha = glow * 0.22f), GLOW_RADIUS_DP * u, Offset(x, y))
            drawCircle(color.copy(alpha = glow), LIGHT_RADIUS_DP * u, Offset(x, y))
        }
    }
}

private fun DrawScope.drawBottomDrift(palette: ChristmasPalette) {
    val u = density
    val lump = DRIFT_LUMP_DP * u
    val height = DRIFT_HEIGHT_DP * u
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height - height * 0.5f)
        var x = 0f
        while (x < size.width) {
            val next = minOf(size.width, x + lump)
            quadraticTo((x + next) / 2f, size.height - height * 1.5f, next, size.height - height * 0.5f)
            x = next
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, Color.White.copy(alpha = if (palette.isNight) 0.5f else 0.8f))
}
