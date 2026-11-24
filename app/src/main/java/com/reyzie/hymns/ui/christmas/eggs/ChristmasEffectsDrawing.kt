package com.reyzie.hymns.ui.christmas.eggs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val RING_GROWTH_DP = 60f
private const val ROCKET_TRAIL_SECONDS = 0.05f
/** Bright warm gold edge that lifts Santa off the dark sky. */
private val RimGold = Color(0xFFFFDE9A)
private const val SANTA_GLOW_RADIUS_DP = 100f
private const val SANTA_GLOW_SQUASH = 0.42f
private val SantaRed = Color(0xFFD6493D)
private val SantaWhite = Color(0xFFFFF8EA)
private val DayRim = Color(0xFF33405F)
private val RudolphNose = Color(0xFFFF3B30)

/** Draws sparkles, confetti, bell rings, fireworks and falling gifts. */
fun DrawScope.drawChristmasEffects(effects: ChristmasEffects, gift: ImageBitmap, surpriseImages: Map<String, ImageBitmap> = emptyMap()) {
    effects.particles.forEach { p ->
        if (p.isWaiting) return@forEach
        val color = Color(p.color)
        when (p.kind) {
            EffectKind.SPARK -> drawCircle(color.copy(alpha = p.fade), p.size * (0.5f + 0.5f * p.fade), Offset(p.x, p.y))
            EffectKind.CONFETTI -> rotateRad(p.rotation, pivot = Offset(p.x, p.y)) {
                val height = p.size * 0.5f * abs(cos(p.rotation * 1.3f))
                drawRect(color.copy(alpha = (p.fade * 2f).coerceAtMost(1f)), Offset(p.x - p.size / 2f, p.y - height / 2f), Size(p.size, height))
            }
            EffectKind.RING -> {
                val radius = p.size + (p.maxLife - p.life) * RING_GROWTH_DP * density
                drawCircle(color.copy(alpha = p.fade * 0.8f), radius, Offset(p.x, p.y), style = Stroke(1.6f * density))
            }
            EffectKind.ROCKET -> drawLine(
                color,
                Offset(p.x, p.y),
                Offset(p.x - p.vx * ROCKET_TRAIL_SECONDS, p.y - p.vy * ROCKET_TRAIL_SECONDS),
                p.size,
                StrokeCap.Round,
            )
            EffectKind.EMOJI -> surpriseImages[p.emoji]?.let { drawSurpriseEmoji(p, it) }
            EffectKind.GIFT -> translate(p.x, p.y) {
                rotateRad(p.rotation, pivot = Offset.Zero) {
                    drawImage(gift, topLeft = Offset(-gift.width / 2f, -gift.height / 2f), alpha = (p.fade * 3f).coerceAtMost(1f))
                }
            }
        }
    }
}

private const val TWO_PI = (2 * PI).toFloat()
private val StarGlow = Color(0xFFFFD36B)

private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t.coerceIn(0f, 1f)

/** Fades in over the first [inEnd] of its life and out over the last [outStart]..1. */
private fun lifeAlpha(t: Float, inEnd: Float, outStart: Float): Float = when {
    t < inEnd -> t / inEnd
    t > outStart -> (1f - t) / (1f - outStart)
    else -> 1f
}.coerceIn(0f, 1f)

/** One gift surprise emoji. Falling ones are integrated; the rest are drawn from their age. */
private fun DrawScope.drawSurpriseEmoji(p: EffectParticle, image: ImageBitmap) {
    val t = 1f - p.fade
    val u = density
    var x = p.x
    var y = p.y
    var scaleX = 1f
    var scaleY = 1f
    var rotation = p.rotation
    var alpha = 1f
    when (p.style) {
        EmojiStyle.FALL -> alpha = (p.life * 3f).coerceIn(0f, 1f)
        EmojiStyle.SWING -> {
            rotation = sin(t * 3f * TWO_PI) * 0.5f * (1f - t * 0.6f) * p.spin
            alpha = lifeAlpha(t, 0.1f, 0.85f)
        }
        EmojiStyle.POP -> {
            val s = when {
                t < 0.3f -> lerp(0.3f, 1.15f, t / 0.3f)
                t < 0.5f -> lerp(1.15f, 0.95f, (t - 0.3f) / 0.2f)
                else -> lerp(0.95f, 1f, (t - 0.5f) / 0.2f)
            }
            scaleX = s; scaleY = s
            y = p.y - 34f * u * (t / 0.35f).coerceIn(0f, 1f)
            alpha = lifeAlpha(t, 0.08f, 0.85f)
        }
        EmojiStyle.RISE -> {
            val rise = p.vy
            val drift = p.vx
            val landing = (t / 0.7f).coerceIn(0f, 1f)
            val eased = landing * landing * (3f - 2f * landing)
            val after = ((t - 0.7f) / 0.3f).coerceIn(0f, 1f)
            x = p.x + drift * after
            y = p.y + rise * eased - 40f * u * after
            val s = when {
                t < 0.25f -> lerp(0.3f, 1.5f, t / 0.25f)
                t < 0.7f -> lerp(1.5f, 1.2f, (t - 0.25f) / 0.45f)
                else -> lerp(1.2f, 0.5f, after)
            }
            scaleX = s; scaleY = s
            rotation = t * 3f * PI.toFloat()
            alpha = lifeAlpha(t, 0.1f, 0.85f)
            drawCircle(StarGlow.copy(alpha = 0.3f * alpha), p.size * 0.75f * s, Offset(x, y))
        }
    }
    val k = p.size / image.height
    translate(x, y) {
        rotateRad(rotation, pivot = Offset.Zero) {
            scale(scaleX * k, scaleY * k, pivot = Offset.Zero) {
                drawImage(image, topLeft = Offset(-image.width / 2f, -image.height / 2f), alpha = alpha)
            }
        }
    }
}

/**
 * Santa's sleigh with four reindeer pulling ahead, Rudolph's nose glowing at the front.
 * Drawn as moonlit silhouettes with a thin gold rim so they read against the night sky.
 */
fun DrawScope.drawSanta(pose: SantaPose, timeSeconds: Float, isNight: Boolean, alpha: Float) {
    val u = density
    // White reindeer and sleigh: they glow against the night sky. In daylight they get a
    // navy outline instead, so white still shows on a pale background.
    val fill = (if (isNight) SantaWhite else Color.White).copy(alpha = alpha)
    val rim = (if (isNight) RimGold else DayRim).copy(alpha = alpha)
    val x = pose.x
    val y = pose.y
    // A soft warm glow behind the whole team (white-ish in daylight), flattened to fit its shape.
    val glowCenter = Offset(x + 66f * u, y - 3f * u)
    val glowColor = if (isNight) Color(0xFFFFCB6B) else Color.White
    val glowStrength = (if (isNight) 0.30f else 0.55f) * alpha
    scale(scaleX = 1f, scaleY = SANTA_GLOW_SQUASH, pivot = glowCenter) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(glowColor.copy(alpha = glowStrength), glowColor.copy(alpha = glowStrength * 0.35f), Color.Transparent),
                glowCenter,
                SANTA_GLOW_RADIUS_DP * u,
            ),
            radius = SANTA_GLOW_RADIUS_DP * u,
            center = glowCenter,
        )
    }
    drawLine(rim, Offset(x + 16f * u, y - 9f * u), Offset(x + 112f * u, y + (-4f + sin(timeSeconds * 8f + 3f) * 1.2f) * u), 1.1f * u)
    repeat(4) { i ->
        val deerX = x + (40f + i * 24f) * u
        val deerY = y + (-2f + sin(timeSeconds * 8f + i) * 1.4f) * u
        drawReindeer(deerX, deerY, timeSeconds, i, lead = i == 3, fill = fill, rim = rim, alpha = alpha)
    }
    drawSleigh(x, y, timeSeconds, fill, rim, alpha)
}

private fun DrawScope.drawReindeer(rx: Float, ry: Float, t: Float, index: Int, lead: Boolean, fill: Color, rim: Color, alpha: Float) {
    val u = density
    val gallop = sin(t * 12f + index * 1.3f) * 3f * u
    drawLine(fill, Offset(rx + 4f * u, ry + 2f * u), Offset(rx + 5f * u + gallop, ry + 8f * u), 1.3f * u, StrokeCap.Round)
    drawLine(fill, Offset(rx - 5f * u, ry + 2f * u), Offset(rx - 6f * u - gallop, ry + 8f * u), 1.3f * u, StrokeCap.Round)
    rotate(-6f, Offset(rx, ry)) {
        drawOval(fill, Offset(rx - 8f * u, ry - 3.3f * u), Size(16f * u, 6.6f * u))
        drawOval(rim, Offset(rx - 8f * u, ry - 3.3f * u), Size(16f * u, 6.6f * u), style = Stroke(1.2f * u))
    }
    drawLine(fill, Offset(rx + 6f * u, ry - 1f * u), Offset(rx + 9f * u, ry - 6f * u), 2f * u, StrokeCap.Round)
    drawOval(fill, Offset(rx + 7.7f * u, ry - 8.3f * u), Size(5.6f * u, 3.6f * u))
    listOf(
        Offset(9f, -8f) to Offset(7f, -13f),
        Offset(8f, -11f) to Offset(5.5f, -11.5f),
        Offset(10.5f, -8f) to Offset(12f, -13f),
    ).forEach { (from, to) ->
        drawLine(fill, Offset(rx + from.x * u, ry + from.y * u), Offset(rx + to.x * u, ry + to.y * u), 1f * u, StrokeCap.Round)
    }
    if (lead) {
        val nose = Offset(rx + 13.4f * u, ry - 6.6f * u)
        drawCircle(RudolphNose.copy(alpha = 0.45f * alpha), 6f * u, nose)
        drawCircle(RudolphNose.copy(alpha = alpha), 1.5f * u, nose)
    }
}

private fun DrawScope.drawSleigh(x: Float, y: Float, t: Float, fill: Color, rim: Color, alpha: Float) {
    val u = density
    fun at(dx: Float, dy: Float) = Offset(x + dx * u, y + dy * u)
    drawCircle(SantaRed.copy(alpha = alpha), 5.5f * u, at(-11f, -10f))
    drawCircle(rim, 5.5f * u, at(-11f, -10f), style = Stroke(1.4f * u))
    drawCircle(SantaRed.copy(alpha = alpha), 5f * u, at(-3f, -10f))
    drawCircle(fill, 3f * u, at(-2f, -17f))
    val hat = Path().apply {
        moveTo(x - 5f * u, y - 18.5f * u)
        lineTo(x + 1f * u, y - 18.5f * u)
        lineTo(x - 6f * u, y - 25f * u)
        close()
    }
    drawPath(hat, SantaRed.copy(alpha = alpha))
    drawCircle(Color.White.copy(alpha = alpha), 1.3f * u, at(-6.2f, -25f))
    val wave = -1.9f + sin(t * 7f) * 0.35f
    drawLine(SantaRed.copy(alpha = alpha), at(-1f, -12f), Offset(x + (-1f + cos(wave) * 7f) * u, y + (-12f + sin(wave) * 7f) * u), 1.8f * u, StrokeCap.Round)
    val body = Path().apply {
        moveTo(x - 15f * u, y - 7f * u)
        lineTo(x + 10f * u, y - 7f * u)
        quadraticTo(x + 16f * u, y - 7f * u, x + 16f * u, y - 13f * u)
        lineTo(x + 19f * u, y - 13f * u)
        quadraticTo(x + 19f * u, y + 2f * u, x + 8f * u, y + 2f * u)
        lineTo(x - 15f * u, y + 2f * u)
        close()
    }
    drawPath(body, fill)
    drawPath(body, rim, style = Stroke(1.4f * u))
    val runner = Path().apply {
        moveTo(x - 17f * u, y + 6f * u)
        lineTo(x + 15f * u, y + 6f * u)
        quadraticTo(x + 21f * u, y + 6f * u, x + 20f * u, y + 1f * u)
    }
    drawPath(runner, rim, style = Stroke(1.9f * u, cap = StrokeCap.Round))
    drawLine(rim, at(-9f, 2f), at(-9f, 6f), 0.9f * u)
    drawLine(rim, at(6f, 2f), at(6f, 6f), 0.9f * u)
}
