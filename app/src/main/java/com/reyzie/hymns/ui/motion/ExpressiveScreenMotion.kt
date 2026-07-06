package com.reyzie.hymns.ui.motion

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.reyzie.hymns.utils.MotionSpecs
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue

/** Slide + scale preview while the system back gesture is in progress. */
fun Modifier.expressivePredictiveBackTransform(progress: Float): Modifier = graphicsLayer {
    val p = progress.coerceIn(0f, 1f)
    translationX = size.width * p * 0.28f
    val scale = lerp(1f, 0.94f, p)
    scaleX = scale
    scaleY = scale
    alpha = lerp(1f, 0.88f, p)
}

/**
 * Registers predictive back and reports swipe progress for custom in-app animations.
 * Falls back to an immediate [onBack] when the gesture completes.
 */
@Composable
fun PredictiveExpressiveBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
    onProgress: (Float) -> Unit = {}
) {
    PredictiveBackHandler(enabled = enabled) { progressEvents ->
        try {
            progressEvents.collect { event ->
                onProgress(event.progress)
            }
            onProgress(0f)
            onBack()
        } catch (_: CancellationException) {
            onProgress(0f)
        }
    }
}

/** M3 expressive forward navigation — shared axis + fade + subtle scale. */
fun expressiveForwardEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { (it * 0.18f).toInt() },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(
        animationSpec = tween(MotionSpecs.DurationMedium, easing = MotionSpecs.EmphasizedDecelerate)
    ) + scaleIn(
        initialScale = 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

fun expressiveForwardExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { -(it * 0.12f).toInt() },
        animationSpec = tween(MotionSpecs.DurationMedium, easing = MotionSpecs.Emphasized)
    ) + fadeOut(tween(MotionSpecs.DurationShort, easing = MotionSpecs.Emphasized)) +
        scaleOut(
            targetScale = 0.96f,
            animationSpec = tween(MotionSpecs.DurationShort, easing = MotionSpecs.Emphasized)
        )

fun expressiveBackEnter(): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { -(it * 0.12f).toInt() },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(tween(MotionSpecs.DurationMedium, easing = MotionSpecs.EmphasizedDecelerate))

fun expressiveBackExit(): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { (it * 0.18f).toInt() },
        animationSpec = tween(MotionSpecs.DurationMedium, easing = MotionSpecs.Emphasized)
    ) + fadeOut(tween(MotionSpecs.DurationShort)) +
        scaleOut(targetScale = 0.94f, animationSpec = tween(MotionSpecs.DurationShort))

/** Bottom audio panel — slide up + fade + subtle scale. */
fun expressiveAudioPlayerEnter(): EnterTransition =
    slideInVertically(
        initialOffsetY = { (it * 0.4f).toInt() },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(
        animationSpec = tween(MotionSpecs.DurationMedium, easing = MotionSpecs.EmphasizedDecelerate)
    ) + scaleIn(
        initialScale = 0.96f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

fun expressiveAudioPlayerExit(): ExitTransition =
    slideOutVertically(
        targetOffsetY = { (it * 0.3f).toInt() },
        animationSpec = tween(MotionSpecs.DurationMedium, easing = MotionSpecs.Emphasized)
    ) + fadeOut(tween(MotionSpecs.DurationShort, easing = MotionSpecs.Emphasized)) +
        scaleOut(targetScale = 0.96f, animationSpec = tween(MotionSpecs.DurationShort))

/**
 * Full-screen overlay with enter/exit motion (hymn detail, keerthane, order reader, etc.).
 */
@Composable
fun <T> ExpressiveOverlayScreen(
    item: T?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    var displayed by remember { mutableStateOf<T?>(item) }
    var visible by remember { mutableStateOf(item != null) }

    LaunchedEffect(item) {
        if (item != null) {
            displayed = item
            visible = true
        } else if (displayed != null) {
            visible = false
        }
    }

    LaunchedEffect(visible) {
        if (!visible && displayed != null) {
            kotlinx.coroutines.delay(MotionSpecs.DurationMedium.toLong())
            displayed = null
        }
    }

    val show = displayed != null && visible
    var swipeProgress by remember { mutableFloatStateOf(0f) }
    var swipeActive by remember { mutableStateOf(false) }

    val animatedSwipe by animateFloatAsState(
        targetValue = swipeProgress,
        animationSpec = if (swipeActive) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "overlayPredictiveBack"
    )

    if (displayed != null) {
        PredictiveExpressiveBackHandler(
            enabled = show,
            onBack = {
                visible = false
                onDismiss()
            },
            onProgress = { progress ->
                swipeActive = progress > 0f
                swipeProgress = progress
            }
        )
    }

    AnimatedVisibility(
        visible = show,
        enter = expressiveForwardEnter(),
        exit = expressiveBackExit(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .expressivePredictiveBackTransform(animatedSwipe)
                .background(MaterialTheme.colorScheme.background)
        ) {
            displayed?.let { content(it) }
        }
    }
}

/** Boolean overload for simple modal overlays. */
@Composable
fun ExpressiveOverlayScreen(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ExpressiveOverlayScreen(
        item = visible.takeIf { it },
        onDismiss = onDismiss,
        modifier = modifier
    ) {
        content()
    }
}

/** Horizontal pager page emphasis — scale + fade on off-screen pages. */
fun Modifier.expressivePagerPage(
    page: Int,
    currentPage: Int,
    pageOffsetFraction: Float
): Modifier {
    val offset = (currentPage - page) + pageOffsetFraction
    val absOffset = offset.absoluteValue.coerceIn(0f, 1f)
    val scale = lerp(0.92f, 1f, 1f - absOffset)
    val alpha = lerp(0.55f, 1f, 1f - absOffset)
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
