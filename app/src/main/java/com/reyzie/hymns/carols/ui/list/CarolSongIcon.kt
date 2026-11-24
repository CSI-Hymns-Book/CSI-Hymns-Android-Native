package com.reyzie.hymns.carols.ui.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.ui.christmas.ChristmasEffectsLevel
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.LocalChristmasAmbience
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWINKLE_MILLIS = 2_600
private val LIGHT_SPOTS = listOf(
    0.50f to 0.30f, 0.37f to 0.50f, 0.63f to 0.53f, 0.27f to 0.75f, 0.50f to 0.70f, 0.73f to 0.77f,
)

/**
 * A little Christmas tree for song rows: its star glows and its lights twinkle in turn. It
 * stands still when Christmas effects are off or the phone's animations are off.
 */
@Composable
fun CarolSongIcon(palette: ChristmasPalette, modifier: Modifier = Modifier.size(30.dp)) {
    val level = LocalChristmasAmbience.current?.effectiveLevel
    val animate = level != null && level != ChristmasEffectsLevel.OFF
    val phase = if (animate) {
        rememberInfiniteTransition(label = "carolTree").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(TWINKLE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "carolTreeTwinkle",
        ).value
    } else {
        0f
    }
    val lightColors = listOf(palette.gold, palette.ember, palette.ice, Color.White, palette.gold, palette.ember)
    val leaf = lerp(palette.pine, Color.White, 0.12f)
    val leafDark = lerp(palette.pine, Color.Black, 0.18f)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Color(0xFF7A5638), topLeft = Offset(w * 0.44f, h * 0.84f), size = Size(w * 0.12f, h * 0.15f))
        fun tier(top: Float, bottom: Float, half: Float, color: Color) {
            val path = Path().apply {
                moveTo(w / 2, h * top)
                lineTo(w / 2 + w * half, h * bottom)
                lineTo(w / 2 - w * half, h * bottom)
                close()
            }
            drawPath(path, color)
        }
        tier(0.40f, 0.88f, 0.42f, leafDark)
        tier(0.24f, 0.68f, 0.34f, leaf)
        tier(0.10f, 0.50f, 0.25f, lerp(leaf, Color.White, 0.08f))

        // The star on top glows a little brighter and dimmer.
        val glow = if (animate) 0.75f + 0.25f * sin(phase * 2 * PI.toFloat()) else 1f
        val star = Path()
        val cx = w / 2
        val cy = h * 0.09f
        val outer = w * 0.1f
        for (i in 0 until 10) {
            val radius = if (i % 2 == 0) outer else outer * 0.45f
            val angle = -PI.toFloat() / 2 + i * PI.toFloat() / 5
            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle)
            if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, palette.gold.copy(alpha = glow))

        LIGHT_SPOTS.forEachIndexed { i, (fx, fy) ->
            val twinkle = if (animate) 0.3f + 0.7f * (0.5f + 0.5f * sin((phase + i / LIGHT_SPOTS.size.toFloat()) * 2 * PI.toFloat())) else 0.95f
            drawCircle(lightColors[i % lightColors.size].copy(alpha = twinkle), radius = w * 0.045f, center = Offset(w * fx, h * fy))
        }
    }
}
