package com.reyzie.hymns.ui.christmas.chrome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.ui.christmas.LocalChristmasAmbience
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import com.reyzie.hymns.ui.christmas.snow.SnowCapMetrics
import com.reyzie.hymns.ui.christmas.snow.buildSnowCapOutline
import com.reyzie.hymns.ui.christmas.toPath

/*
 * Small hand-drawn Christmas decorations: a Santa cap, a bow, a sprig of holly and a snow trim.
 * Each is drawn on a fixed grid (the same grid as the mockup) and scaled to the width asked for.
 */

private val CapRed = Color(0xFFD6342B)
private val CapShade = Color(0xFFA81F18)
private val CapWhite = Color(0xFFF4F7FF)
private val BowRed = Color(0xFFD6342B)
private val BowEdge = Color(0xFF8E1B14)
private val BowTail = Color(0xFFB02A22)
private val HollyGreen = Color(0xFF2F7D55)

private const val CAP_GRID_WIDTH = 68f
private const val CAP_GRID_HEIGHT = 48f
private const val BOW_GRID_WIDTH = 40f
private const val HOLLY_GRID_WIDTH = 32f
private const val TRIM_SEED = 7
private const val TRIM_DEPTH = 0.85f
private const val TRIM_SHADOW_DP = 1.5f

private val CapBody = Path().apply {
    moveTo(6f, 37f)
    cubicTo(8f, 15f, 26f, 4f, 44f, 8f)
    cubicTo(54f, 10f, 60f, 16f, 60f, 25f)
    cubicTo(56f, 21f, 50f, 21f, 47f, 27f)
    cubicTo(45f, 32f, 46f, 36f, 48f, 37f)
    close()
}
private val CapShadeShape = Path().apply {
    moveTo(6f, 37f)
    cubicTo(8f, 22f, 18f, 11f, 32f, 8f)
    cubicTo(22f, 14f, 16f, 24f, 16f, 37f)
    close()
}
private val BowLoops = Path().apply {
    moveTo(20f, 16f); cubicTo(12f, 2f, 2f, 4f, 3f, 13f); cubicTo(4f, 21f, 14f, 18f, 20f, 16f); close()
    moveTo(20f, 16f); cubicTo(28f, 2f, 38f, 4f, 37f, 13f); cubicTo(36f, 21f, 26f, 18f, 20f, 16f); close()
}
private val HollyLeaves = Path().apply {
    moveTo(4f, 14f); cubicTo(8f, 4f, 18f, 4f, 20f, 12f); cubicTo(14f, 10f, 9f, 12f, 4f, 14f); close()
    moveTo(14f, 18f); cubicTo(18f, 9f, 28f, 9f, 30f, 17f); cubicTo(24f, 16f, 19f, 17f, 14f, 18f); close()
}

/** A Santa cap whose top-left corner is ([left], [top]) and that is [widthPx] wide. */
fun DrawScope.drawSantaCap(left: Float, top: Float, widthPx: Float) {
    val k = widthPx / CAP_GRID_WIDTH
    withTransform({ translate(left, top); scale(k, k, Offset.Zero) }) {
        drawPath(CapBody, CapRed)
        drawPath(CapShadeShape, CapShade, alpha = 0.55f)
        drawRoundRect(CapWhite, Offset(2f, 33f), Size(50f, 12f), CornerRadius(6f))
        drawCircle(CapWhite, 6.5f, Offset(60f, 26f))
    }
}

/** A red bow with two tails. */
fun DrawScope.drawBow(left: Float, top: Float, widthPx: Float) {
    val k = widthPx / BOW_GRID_WIDTH
    withTransform({ translate(left, top); scale(k, k, Offset.Zero) }) {
        drawPath(BowLoops, BowRed)
        drawPath(BowLoops, BowEdge, style = Stroke(1.2f))
        drawLine(BowTail, Offset(20f, 16f), Offset(12f, 29f), 3.4f, StrokeCap.Round)
        drawLine(BowTail, Offset(20f, 16f), Offset(28f, 29f), 3.4f, StrokeCap.Round)
        drawCircle(BowTail, 4f, Offset(20f, 15f))
    }
}

/** Two holly leaves and two berries. */
fun DrawScope.drawHolly(left: Float, top: Float, widthPx: Float) {
    val k = widthPx / HOLLY_GRID_WIDTH
    withTransform({ translate(left, top); scale(k, k, Offset.Zero) }) {
        drawPath(HollyLeaves, HollyGreen)
        drawCircle(BowRed, 3f, Offset(14f, 17f))
        drawCircle(BowRed, 3f, Offset(19f, 19f))
    }
}

/** A Santa cap as its own element, [width] wide. */
@Composable
fun SantaCap(modifier: Modifier = Modifier, width: Dp = 64.dp) {
    Canvas(modifier.size(width, width * (CAP_GRID_HEIGHT / CAP_GRID_WIDTH))) { drawSantaCap(0f, 0f, size.width) }
}

/**
 * Perches a small Santa cap on the corner of a floating button. Does nothing outside Christmas
 * mode. Draws outside the element's bounds, so the parent must not clip.
 */
fun Modifier.festiveCap(
    width: Dp = 36.dp,
    offsetX: Dp = (-9).dp,
    offsetY: Dp = (-19).dp,
    tiltDegrees: Float = -14f,
): Modifier = composed {
    if (LocalChristmasAmbience.current == null) return@composed Modifier
    this.drawWithContent {
        drawContent()
        val w = width.toPx()
        val left = offsetX.toPx()
        val top = offsetY.toPx()
        rotate(tiltDegrees, Offset(left + w * 0.22f, top + w * (CAP_GRID_HEIGHT / CAP_GRID_WIDTH) * 0.92f)) {
            drawSantaCap(left, top, w)
        }
    }
}

/**
 * Draws a fixed snow trim along the top edge of this element, in the same style as the snow
 * on the cards but without the physics. [cornerDp] is the element's corner radius.
 */
fun Modifier.festiveSnowTrim(cornerDp: Float): Modifier = composed {
    val palette = rememberChristmasPalette()
    this.drawWithCache {
        val pxPerDp = density
        val path = buildSnowCapOutline(size.width / pxPerDp, TRIM_SEED, cornerDp).toPath(pxPerDp)
        val capWidth = size.width + 2 * SnowCapMetrics.OVERHANG * pxPerDp
        val pivot = Offset(capWidth / 2f, SnowCapMetrics.BASELINE * pxPerDp)
        onDrawWithContent {
            drawContent()
            translate(left = -SnowCapMetrics.OVERHANG * pxPerDp, top = -SnowCapMetrics.RISE * pxPerDp) {
                scale(scaleX = 1f, scaleY = TRIM_DEPTH, pivot = pivot) {
                    translate(top = TRIM_SHADOW_DP * pxPerDp) { drawPath(path, palette.capShadow) }
                    drawPath(path, palette.capSnow)
                }
            }
        }
    }
}
