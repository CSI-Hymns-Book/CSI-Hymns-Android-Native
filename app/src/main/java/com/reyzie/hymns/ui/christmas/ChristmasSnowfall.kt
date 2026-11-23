package com.reyzie.hymns.ui.christmas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.ui.christmas.eggs.drawChristmasEffects
import com.reyzie.hymns.ui.christmas.eggs.drawSanta
import com.reyzie.hymns.ui.christmas.snow.SnowEngine
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val MAX_FRAME_SECONDS = 0.05f
private const val FLAKE_STROKE = 0.13f
private const val SANTA_MIN_ALPHA = 0.4f

/** The drifting emoji easter eggs from the original Christmas mode. */
private val EasterEggEmojis = listOf("🎄", "⭐", "🔔", "🎁", "❄️", "🕯️", "🦌", "⛄")

/**
 * Full-screen snowfall drawn over the app. Draw-only: it never takes touches.
 * Runs on a frame clock, so it pauses by itself when the app is in the background.
 */
@Composable
fun ChristmasSnowfall(controller: ChristmasAmbienceController, modifier: Modifier = Modifier) {
    val level = controller.effectiveLevel
    if (level == ChristmasEffectsLevel.OFF) return

    val palette = rememberChristmasPalette()
    val density = LocalDensity.current.density
    var frameNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(controller) {
        var last = -1L
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (last < 0) 0f else ((now - last) / NANOS_PER_SECOND).coerceAtMost(MAX_FRAME_SECONDS)
                last = now
                controller.advance(dt)
                frameNanos = now
            }
        }
    }

    val textMeasurer = rememberTextMeasurer()
    val pxDensity = LocalDensity.current
    val emojiImages = remember(textMeasurer, pxDensity) {
        EasterEggEmojis.map { textMeasurer.renderEmoji(it, pxDensity) }
    }
    val giftImage = remember(textMeasurer, pxDensity) { textMeasurer.renderEmoji("🎁", pxDensity) }
    val flakePath = remember { buildFlakePath() }
    val engine = controller.engine

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { engine.resize(it.width.toFloat(), it.height.toFloat(), density) },
    ) {
        if (frameNanos == 0L) return@Canvas
        val alpha = controller.snowAlpha
        val snowDensity = if (controller.isFlurry) 1f else level.snowDensity
        engine.particles.forEach { p ->
            if (!p.isVisible(snowDensity)) return@forEach
            drawSnowPiece(flakePath, p.layer.isFlake, p.x, p.y, p.radius, p.rotation, palette.snow, p.alpha * alpha)
        }
        engine.settled.forEach { s ->
            drawSnowPiece(flakePath, s.isFlake, s.x, s.y, s.radius, s.rotation, palette.snow, s.alpha * 0.9f * alpha)
        }
        engine.chunks.forEach { c ->
            drawCircle(palette.capSnow.copy(alpha = c.alpha * 0.95f), c.radius, Offset(c.x, c.y))
        }
        if (controller.easterEggsEnabled) {
            drawEmojis(engine, emojiImages, level.emojiCount, palette.emojiAlpha * alpha)
        }
        controller.santaPose()?.let { drawSanta(it, controller.clockSeconds, palette.isNight, alpha.coerceAtLeast(SANTA_MIN_ALPHA)) }
        drawChristmasEffects(controller.effects, giftImage)
    }
}

private fun DrawScope.drawSnowPiece(
    flakePath: Path,
    isFlake: Boolean,
    x: Float,
    y: Float,
    radius: Float,
    rotation: Float,
    color: Color,
    alpha: Float,
) {
    if (alpha <= 0.01f) return
    if (!isFlake) {
        drawCircle(color.copy(alpha = alpha), radius, Offset(x, y))
        return
    }
    translate(x, y) {
        rotateRad(rotation, pivot = Offset.Zero) {
            scale(radius, radius, pivot = Offset.Zero) {
                drawPath(flakePath, color.copy(alpha = alpha), style = Stroke(width = FLAKE_STROKE, cap = StrokeCap.Round))
            }
        }
    }
}

private fun DrawScope.drawEmojis(engine: SnowEngine, images: List<ImageBitmap>, count: Int, alpha: Float) {
    if (count == 0 || alpha <= 0.01f) return
    engine.emojis.take(count).forEach { e ->
        if (e.isHidden) return@forEach
        val image = images[e.index % images.size]
        translate(e.x, e.y) {
            rotateRad(e.tilt, pivot = Offset.Zero) {
                drawImage(image, topLeft = Offset(-image.width / 2f, -image.height / 2f), alpha = alpha)
            }
        }
    }
}

/**
 * Draws an emoji once into a small bitmap. Colour emoji ignore text alpha, and drawing
 * bitmaps with alpha each frame is far cheaper than a translucent layer over the screen.
 */
private fun TextMeasurer.renderEmoji(emoji: String, density: Density): ImageBitmap {
    val layout = measure(emoji, TextStyle(fontSize = 18.sp), density = density)
    val image = ImageBitmap(layout.size.width.coerceAtLeast(1), layout.size.height.coerceAtLeast(1))
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
        drawText(layout)
    }
    return image
}

/** A six-armed snowflake with side branches, radius 1, centred on the origin. */
private fun buildFlakePath(): Path = Path().apply {
    repeat(6) { arm ->
        val angle = arm * PI / 3
        fun point(px: Float, py: Float): Offset {
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            return Offset(px * c - py * s, px * s + py * c)
        }
        val tip = point(0f, -1f)
        val mid = point(0f, -0.55f)
        val left = point(-0.28f, -0.78f)
        val right = point(0.28f, -0.78f)
        moveTo(0f, 0f); lineTo(tip.x, tip.y)
        moveTo(mid.x, mid.y); lineTo(left.x, left.y)
        moveTo(mid.x, mid.y); lineTo(right.x, right.y)
    }
}
