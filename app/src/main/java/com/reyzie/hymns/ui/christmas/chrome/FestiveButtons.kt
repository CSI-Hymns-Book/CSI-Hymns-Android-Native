package com.reyzie.hymns.ui.christmas.chrome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.ui.christmas.ChristmasEffectsLevel
import com.reyzie.hymns.ui.christmas.LocalChristmasAmbience
import com.reyzie.hymns.ui.christmas.rememberChristmasPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** What a button is for, which decides how it is dressed. */
enum class FestiveButtonRole {
    /** The main action: a bow, and a burst of sparkles when pressed. */
    MAIN,

    /** A quieter choice such as Cancel: a sprig of holly. */
    SECONDARY,

    /** Delete and similar: plain, so it never reads as decoration. */
    DESTRUCTIVE,
}

private const val PRESSED_SCALE_X = 0.93f
private const val PRESSED_SCALE_Y = 0.9f
private const val SPARKLES = 8
private const val SPARKLE_MILLIS = 600
private const val BOW_WIDTH_DP = 30f
private const val BOW_TILT = 14f
private const val HOLLY_WIDTH_DP = 24f
private const val HOLLY_TILT = -18f

/**
 * Squashes the element a little while a finger is down and springs it back on release. It only
 * watches the touch, so the button inside still gets its click. [onRelease] runs when the
 * finger lifts on the element.
 */
fun Modifier.festivePress(enabled: Boolean = true, onRelease: () -> Unit = {}): Modifier = composed {
    if (!enabled) return@composed Modifier
    var pressed by remember { mutableStateOf(false) }
    val release by rememberUpdatedState(onRelease)
    val scaleX by animateFloatAsState(if (pressed) PRESSED_SCALE_X else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow), label = "pressX")
    val scaleY by animateFloatAsState(if (pressed) PRESSED_SCALE_Y else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow), label = "pressY")
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                val up = waitForUpOrCancellation(PointerEventPass.Initial)
                pressed = false
                if (up != null) release()
            }
        }
        .graphicsLayer { this.scaleX = scaleX; this.scaleY = scaleY }
}

/**
 * Dresses a button for Christmas: squash on press, plus a bow, holly or nothing by [role].
 * The main button also throws sparkles when it is pressed. Outside Christmas mode this is just
 * [content]. Put the real button inside; its own click and colours are untouched.
 */
@Composable
fun FestiveButtonFrame(role: FestiveButtonRole, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val ambience = LocalChristmasAmbience.current
    if (ambience == null) {
        Box(modifier) { content() }
        return
    }
    val animated = ambience.effectiveLevel != ChristmasEffectsLevel.OFF
    val palette = rememberChristmasPalette()
    var bursts by remember { mutableIntStateOf(0) }
    val sparkle = remember { Animatable(1f) }
    LaunchedEffect(bursts) {
        if (bursts == 0) return@LaunchedEffect
        sparkle.snapTo(0f)
        sparkle.animateTo(1f, tween(SPARKLE_MILLIS))
    }
    Box(
        modifier
            .festivePress(enabled = animated) { if (role == FestiveButtonRole.MAIN) bursts += 1 }
            .drawWithContent {
                drawContent()
                when (role) {
                    FestiveButtonRole.MAIN -> {
                        val w = BOW_WIDTH_DP.dp.toPx()
                        val left = size.width - w + 9.dp.toPx()
                        val top = (-12).dp.toPx()
                        rotate(BOW_TILT, Offset(left + w / 2f, top + w * 0.35f)) { drawBow(left, top, w) }
                    }
                    FestiveButtonRole.SECONDARY -> {
                        val w = HOLLY_WIDTH_DP.dp.toPx()
                        val left = (-6).dp.toPx()
                        val top = (-9).dp.toPx()
                        rotate(HOLLY_TILT, Offset(left + w / 2f, top + w * 0.4f)) { drawHolly(left, top, w) }
                    }
                    FestiveButtonRole.DESTRUCTIVE -> Unit
                }
                val t = sparkle.value
                if (t < 1f) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    repeat(SPARKLES) { i ->
                        val angle = i * 2f * PI.toFloat() / SPARKLES
                        val distance = (18f + 26f * t).dp.toPx()
                        drawCircle(
                            palette.gold.copy(alpha = 1f - t),
                            2.2.dp.toPx(),
                            Offset(center.x + cos(angle) * distance, center.y + sin(angle) * distance),
                        )
                    }
                }
            },
    ) { content() }
}
