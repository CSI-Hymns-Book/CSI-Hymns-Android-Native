package com.reyzie.hymns.ui.christmas.landing

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BerryRed = Color(0xFFD6493D)
private val Gold = Color(0xFFE9C27B)
private val CandleLight = Color(0xFFFFF4D6)
private val LanternGlow = Color(0xFFFFD98A)
private val TrunkBrown = Color(0xFF8A5A3B)
private val BulbColors = listOf(Color(0xFFFFD36E), Color(0xFFFF6B5B), Color(0xFF9FD8FF), Color(0xFFFFD36E), Color.White)

private fun svgPath(d: String): Path = PathParser().parsePathString(d).toPath()

/** A 0..1 phase that loops forever, or stays at 0 when [animate] is false. */
@Composable
private fun rememberLoop(animate: Boolean, periodMillis: Int): State<Float> {
    if (!animate) return remember { mutableFloatStateOf(0f) }
    val transition: InfiniteTransition = rememberInfiniteTransition(label = "christmasIcon")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "christmasIconLoop",
    )
}

/** Draws in a square [viewBox] coordinate space scaled to the canvas. */
private fun DrawScope.inViewBox(viewBox: Float, block: DrawScope.() -> Unit) {
    scale(size.width / viewBox, size.height / viewBox, pivot = Offset.Zero) { block() }
}

/**
 * Hymn book with a holly sprig. The book sways a little, a page turns over every few
 * seconds, a music note floats up out of it, and the holly berries twinkle.
 */
@Composable
fun HymnBookHollyIcon(tint: Color, leaf: Color, size: Dp, animate: Boolean) {
    val book = remember { svgPath("M3 10c4.5-2.2 9.5-2.2 15 .8 5.5-3 10.5-3 15-.8v19c-4.5-2.2-9.5-2.2-15 .8-5.5-3-10.5-3-15-.8z") }
    val lines = remember { svgPath("M7.5 16h6.5M7.5 20h6.5M7.5 24h4.5M22 16h6.5M22 20h6.5") }
    val page = remember { svgPath("M18 11.2c5-3 10-3 15-.8v18.8c-5-2.2-10-2.2-15 .8z") }
    val loop = rememberLoop(animate, periodMillis = HYMN_ICON_PERIOD_MILLIS)
    Canvas(Modifier.size(size)) {
        inViewBox(36f) {
            val phase = loop.value
            val sway = if (animate) 3f * sin(phase * 2 * PI.toFloat()) else 0f
            rotate(sway, Offset(18f, 30f)) {
                drawPath(book, tint, style = Stroke(2f, join = StrokeJoin.Round))
                drawLine(tint, Offset(18f, 11f), Offset(18f, 30f), 2f)
                drawPath(lines, tint.copy(alpha = 0.55f), style = Stroke(1.5f, cap = StrokeCap.Round))
                if (animate && phase < PAGE_TURN_SHARE) {
                    // The right page lifts, passes edge-on, and lands on the left.
                    val turn = phase / PAGE_TURN_SHARE
                    val width = cos(turn * PI.toFloat())
                    scale(scaleX = width, scaleY = 1f, pivot = Offset(18f, 0f)) {
                        drawPath(page, tint.copy(alpha = 0.22f))
                        drawPath(page, tint, style = Stroke(1.4f, join = StrokeJoin.Round))
                    }
                }
            }
            if (animate && phase > NOTE_START_PHASE) {
                val rise = (phase - NOTE_START_PHASE) / (1f - NOTE_START_PHASE)
                val noteAlpha = sin(rise * PI.toFloat()).coerceIn(0f, 1f)
                val x = 11f + 3f * sin(rise * 2f * PI.toFloat())
                val y = 12f - 10f * rise
                drawCircle(NoteGold.copy(alpha = noteAlpha), 1.7f, Offset(x, y))
                drawLine(NoteGold.copy(alpha = noteAlpha), Offset(x + 1.5f, y), Offset(x + 1.5f, y - 5f), 1.1f, StrokeCap.Round)
            }
            rotate(-25f, Offset(26f, 6f)) { drawOval(leaf, Offset(21f, 3.6f), Size(10f, 4.8f)) }
            rotate(35f, Offset(31.5f, 7.5f)) { drawOval(leaf, Offset(27f, 5.3f), Size(9f, 4.4f)) }
            listOf(Triple(28.6f, 9f, 2f), Triple(31.4f, 10.8f, 1.8f), Triple(26.4f, 11.2f, 1.6f)).forEachIndexed { i, (x, y, r) ->
                val twinkle = 0.75f + 0.25f * sin((phase * 2f + i * 0.2f) * 2 * PI.toFloat())
                drawCircle(BerryRed.copy(alpha = twinkle), r, Offset(x, y))
            }
        }
    }
}

private const val HYMN_ICON_PERIOD_MILLIS = 3_600
private const val PAGE_TURN_SHARE = 0.32f
private const val NOTE_START_PHASE = 0.45f
private val NoteGold = Color(0xFFE9C27B)

/**
 * A paper star lantern, the kind hung outside homes in Karnataka at Christmas.
 * It sways gently on its string.
 */
@Composable
fun StarLanternIcon(tint: Color, size: Dp, animate: Boolean) {
    val star = remember {
        svgPath("M20 9 L22.94 16.96 L31.41 17.29 L24.76 22.55 L27.06 30.71 L20 26 L12.94 30.71 L15.24 22.55 L8.59 17.29 L17.06 16.96Z")
    }
    val tassels = remember { svgPath("M13 30.7 L12 38 M27 30.7 L28 38 M20 26 L20 38") }
    val loop = rememberLoop(animate, periodMillis = 3_400)
    Canvas(Modifier.size(size)) {
        inViewBox(40f) {
            val sway = 7f * sin(loop.value * 2 * PI.toFloat())
            rotate(sway, pivot = Offset(20f, 0f)) {
                drawLine(tint, Offset(20f, 0f), Offset(20f, 9f), 1.4f)
                drawCircle(LanternGlow.copy(alpha = 0.35f), 10f, Offset(20f, 21f))
                drawPath(star, Gold)
                drawPath(star, tint, style = Stroke(1.3f, join = StrokeJoin.Round))
                drawCircle(CandleLight, 2.6f, Offset(20f, 20.5f))
                drawPath(tassels, BerryRed, style = Stroke(1.4f, cap = StrokeCap.Round))
            }
        }
    }
}

/** A Christmas tree with a gold star and blinking lights. */
@Composable
fun ChristmasTreeIcon(leaf: Color, size: Dp, animate: Boolean) {
    val tree = remember { svgPath("M16 4 L22.5 13 H19.5 L25.5 21 H21.5 L27.5 28 H4.5 L10.5 21 H6.5 L12.5 13 H9.5 Z") }
    val topStar = remember { svgPath("M16 .8l1.1 2.2 2.4.3-1.8 1.6.5 2.4-2.2-1.2-2.2 1.2.5-2.4-1.8-1.6 2.4-.3z") }
    val bulbs = remember { listOf(Offset(14f, 11f), Offset(18.5f, 17f), Offset(12f, 19.5f), Offset(20.5f, 24.5f), Offset(10.5f, 25.5f)) }
    val loop = rememberLoop(animate, periodMillis = 1_800)
    Canvas(Modifier.size(size)) {
        inViewBox(32f) {
            drawPath(tree, leaf)
            drawRect(TrunkBrown, Offset(14.2f, 28f), Size(3.6f, 3f))
            drawPath(topStar, Gold)
            bulbs.forEachIndexed { i, center ->
                val blink = 0.25f + 0.75f * (0.5f + 0.5f * sin((loop.value + i * 0.25f) * 2 * PI.toFloat()))
                drawCircle(BulbColors[i].copy(alpha = blink), 1.5f, center)
            }
        }
    }
}

/** Piano keys tied up like a present with a red bow. */
@Composable
fun PianoBowIcon(tint: Color, size: Dp) {
    val bow = remember { svgPath("M12 7C9 2.5 6.5 4 9 6.6M12 7c3-4.5 5.5-3 3-.4") }
    Canvas(Modifier.size(size)) {
        inViewBox(24f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(2.5f, 7f),
                size = Size(19f, 14f),
                cornerRadius = CornerRadius(2f),
                style = Stroke(1.7f),
            )
            listOf(7.5f, 12f, 16.5f).forEach { x -> drawLine(tint, Offset(x, 7f), Offset(x, 21f), 1.4f) }
            drawRect(tint, Offset(6.2f, 7f), Size(2.6f, 7.5f))
            drawRect(tint, Offset(15.2f, 7f), Size(2.6f, 7.5f))
            drawPath(bow, BerryRed, style = Stroke(1.6f, cap = StrokeCap.Round))
            drawCircle(BerryRed, 1.4f, Offset(12f, 7f))
        }
    }
}

/** A short music stave with three notes that bob up and down. */
@Composable
fun BobbingStave(tint: Color, modifier: Modifier, animate: Boolean) {
    val flag = remember { svgPath("M85.6 0 q8 6 4 14 q1 -7 -4 -9z") }
    val loop = rememberLoop(animate, periodMillis = 2_600)
    Canvas(modifier) {
        scale(size.width / 104f, size.height / 58f, pivot = Offset.Zero) {
            listOf(14f, 22f, 30f, 38f, 46f).forEach { y ->
                drawLine(tint.copy(alpha = 0.65f), Offset(0f, y), Offset(104f, y), 1.3f)
            }
            listOf(Triple(24f, 38f, 0f), Triple(52f, 30f, 0.13f), Triple(80f, 22f, 0.27f)).forEachIndexed { i, (x, y, delay) ->
                val bob = -3f * (0.5f - 0.5f * cos((loop.value - delay) * 2 * PI.toFloat()))
                translate(top = bob) {
                    rotate(-20f, Offset(x, y)) { drawOval(tint, Offset(x - 5f, y - 3.6f), Size(10f, 7.2f)) }
                    drawRect(tint, Offset(x + 4f, y - 26f), Size(1.6f, 26f))
                    if (i == 2) drawPath(flag, tint)
                }
            }
        }
    }
}
