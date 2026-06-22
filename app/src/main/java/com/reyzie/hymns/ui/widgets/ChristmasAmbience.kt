package com.reyzie.hymns.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.reyzie.hymns.ui.theme.LocalChristmasStyle
import kotlin.math.sin
import kotlin.random.Random

enum class SnowIntensity { Light, Medium, Heavy }

val ChristmasNightGradientColors = listOf(
    Color(0xFF0B1628),
    Color(0xFF152238),
    Color(0xFF1A2744),
    Color(0xFF0D1520),
)

/** Deep blue night gradient used on Christmas home and carols screens. */
@Composable
fun ChristmasScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(ChristmasNightGradientColors)),
        content = content,
    )
}

@Immutable
private data class SnowParticle(
    val xNorm: Float,
    val yNorm: Float,
    val radius: Float,
    val speed: Float,
    val driftAmp: Float,
    val driftFreq: Float,
    val phase: Float,
    val spin: Float,
    val alpha: Float,
    val style: Int, // 0 = flake, 1 = soft dot
)

@Immutable
private data class EasterEgg(
    val emoji: String,
    val xNorm: Float,
    val yNorm: Float,
    val speed: Float,
    val phase: Float,
    val sizeSp: Float,
)

private val easterEggEmojis = listOf("🎄", "⭐", "🔔", "🎁", "❄️", "🕯️", "🦌", "⛄")

/**
 * Subtle global Christmas overlay — snowfall + drifting emoji easter eggs.
 * Does not intercept touches (draw-only layer).
 */
@Composable
fun ChristmasAmbienceOverlay(
    modifier: Modifier = Modifier,
    intensity: SnowIntensity = SnowIntensity.Medium,
    showEasterEggs: Boolean = true,
) {
    val christmas = LocalChristmasStyle.current
    if (!christmas.isEnabled) return

    val snowColor = christmas.snowflakeColor
    val count = when (intensity) {
        SnowIntensity.Light -> 48
        SnowIntensity.Medium -> 72
        SnowIntensity.Heavy -> 110
    }

    val particles = remember(intensity) {
        List(count) { i ->
            val r = Random(i * 7919 + 31)
            SnowParticle(
                xNorm = r.nextFloat(),
                yNorm = r.nextFloat(),
                radius = r.nextFloat() * 2.8f + 1.2f,
                speed = r.nextFloat() * 0.14f + 0.06f,
                driftAmp = r.nextFloat() * 22f + 8f,
                driftFreq = r.nextFloat() * 2.5f + 1f,
                phase = r.nextFloat() * 6.28f,
                spin = r.nextFloat() * 360f,
                alpha = r.nextFloat() * 0.35f + 0.25f,
                style = if (r.nextFloat() > 0.35f) 0 else 1,
            )
        }
    }

    val eggs = remember(showEasterEggs) {
        if (!showEasterEggs) emptyList()
        else List(10) { i ->
            val r = Random(i * 3571 + 17)
            EasterEgg(
                emoji = easterEggEmojis[i % easterEggEmojis.size],
                xNorm = r.nextFloat(),
                yNorm = r.nextFloat(),
                speed = r.nextFloat() * 0.05f + 0.02f,
                phase = r.nextFloat() * 6.28f,
                sizeSp = r.nextFloat() * 4f + 10f,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "christmasAmbience")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "snowProgress",
    )

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val y = ((p.yNorm + progress * p.speed) % 1.05f) * size.height
            val drift = sin((progress * p.driftFreq * 6.28f) + p.phase) * p.driftAmp
            val x = p.xNorm * size.width + drift
            val rotation = p.spin + progress * 120f
            if (p.style == 0) {
                drawSnowflake(
                    center = Offset(x, y),
                    radius = p.radius * 2.2f,
                    color = snowColor.copy(alpha = p.alpha.coerceIn(0.15f, 0.85f)),
                    rotation = rotation,
                )
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = p.alpha * 0.55f),
                    radius = p.radius,
                    center = Offset(x, y),
                )
            }
        }

        eggs.forEach { egg ->
            val y = ((egg.yNorm + progress * egg.speed) % 1.08f) * size.height
            val x = egg.xNorm * size.width + sin(progress * 6.28f + egg.phase) * 12f
            val style = TextStyle(fontSize = egg.sizeSp.sp, color = Color.White.copy(alpha = 0.22f))
            val layout = textMeasurer.measure(egg.emoji, style)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(x - layout.size.width / 2f, y),
            )
        }
    }
}

@Composable
fun ChristmasAmbienceBox(
    modifier: Modifier = Modifier,
    intensity: SnowIntensity = SnowIntensity.Medium,
    showEasterEggs: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        ChristmasAmbienceOverlay(
            intensity = intensity,
            showEasterEggs = showEasterEggs,
        )
    }
}

private fun DrawScope.drawSnowflake(center: Offset, radius: Float, color: Color, rotation: Float) {
    withTransform({
        rotate(rotation, center)
    }) {
        val arm = radius
        val branch = arm * 0.38f
        for (i in 0 until 6) {
            rotate(i * 60f, center) {
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x, center.y - arm),
                    strokeWidth = (radius * 0.22f).coerceAtLeast(0.8f),
                )
                val mid = Offset(center.x, center.y - arm * 0.55f)
                drawLine(color, mid, Offset(mid.x - branch * 0.5f, mid.y + branch * 0.35f), strokeWidth = radius * 0.14f)
                drawLine(color, mid, Offset(mid.x + branch * 0.5f, mid.y + branch * 0.35f), strokeWidth = radius * 0.14f)
            }
        }
        drawCircle(color = color.copy(alpha = color.alpha * 0.9f), radius = radius * 0.18f, center = center)
    }
}
