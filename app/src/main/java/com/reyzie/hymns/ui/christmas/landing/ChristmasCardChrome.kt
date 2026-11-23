package com.reyzie.hymns.ui.christmas.landing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.reyzie.hymns.ui.christmas.ChristmasMotion
import com.reyzie.hymns.ui.christmas.ChristmasPalette
import com.reyzie.hymns.ui.christmas.snowCap
import com.reyzie.hymns.utils.HapticFeedbackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FADE_MILLIS = 260

/**
 * Zooms this element in when it first appears: it grows from a smaller size with a little
 * overshoot and fades in, delayed by its [index] so items pop in one after another.
 * Shows straight away when [animate] is false.
 */
@Composable
fun Modifier.christmasEntrance(index: Int, animate: Boolean): Modifier {
    val zoom = remember { Animatable(if (animate) ChristmasMotion.POP_START_SCALE else 1f) }
    val fade = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (zoom.value >= 1f) return@LaunchedEffect
        delay((ChristmasMotion.ENTRANCE_START_DELAY_MILLIS + index * ChristmasMotion.STAGGER_MILLIS).toLong())
        launch { fade.animateTo(1f, tween(FADE_MILLIS)) }
        zoom.animateTo(1f, ChristmasMotion.popIn())
    }
    return graphicsLayer {
        alpha = fade.value
        scaleX = zoom.value
        scaleY = zoom.value
    }
}

/**
 * Frosted-glass card chrome shared by every collection card: staggered entrance, a springy
 * press, a gold edge while pressed, a snow cap on top, and a haptic tap on open.
 */
@Composable
fun Modifier.christmasCard(
    palette: ChristmasPalette,
    shape: Shape,
    cornerDp: Float,
    index: Int,
    animate: Boolean,
    capsEnabled: Boolean,
    tint: Brush?,
    contentLabel: String,
    onClick: () -> Unit,
): Modifier {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) ChristmasMotion.PRESSED_SCALE else 1f,
        animationSpec = ChristmasMotion.pressSpring(),
        label = "christmasCardPress",
    )
    val edge = if (pressed) palette.gold else palette.edge
    return this
        .christmasEntrance(index, animate)
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .snowCap(enabled = capsEnabled, cornerDp = cornerDp)
        .clip(shape)
        .background(palette.glass)
        .then(if (tint != null) Modifier.background(tint) else Modifier)
        .border(1.dp, edge, shape)
        .clickable(
            interactionSource = interaction,
            indication = ripple(color = palette.gold),
            role = Role.Button,
            onClickLabel = "Open $contentLabel",
        ) {
            HapticFeedbackManager.smoothClick(context)
            onClick()
        }
}
