package com.reyzie.hymns.ui.christmas

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.reyzie.hymns.ui.christmas.snow.CapCommand
import com.reyzie.hymns.ui.christmas.snow.SnowCapMetrics
import com.reyzie.hymns.ui.christmas.snow.SnowCapValues
import com.reyzie.hymns.ui.christmas.snow.advanceSnowCap
import com.reyzie.hymns.ui.christmas.snow.buildSnowCapOutline
import kotlinx.coroutines.isActive
import kotlin.random.Random

/**
 * False while this part of the screen is covered by another screen (for example a song list
 * under an open song). Snow caps underneath then stop catching snow, so flakes never appear
 * to land on something the user cannot see.
 */
val LocalSnowCapsVisible = compositionLocalOf { true }

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val MAX_FRAME_SECONDS = 0.05f
private const val SHADOW_OFFSET_DP = 1.5f
private const val FADE_IN_DEPTH = 6f

/**
 * Puts a pile of snow on top of this element. Falling snow lands on it and the pile slowly
 * grows; tilting the phone slides it off, and shaking knocks it all off.
 *
 * Put this before any `clip` or `background` in the modifier chain: the cap draws a little
 * outside the element's bounds. Pass the element's corner radius as [cornerDp] so the snow
 * follows its rounded corners instead of sticking out beside them. Pass [enabled] = false while the element is hidden behind
 * another screen, so snow does not land on something the user cannot see.
 */
fun Modifier.snowCap(
    enabled: Boolean = true,
    initialDepth: Float = 0.6f,
    cornerDp: Float = SnowCapMetrics.DEFAULT_CORNER,
): Modifier = composed {
    val controller = LocalChristmasAmbience.current
    if (controller == null || controller.effectiveLevel == ChristmasEffectsLevel.OFF) return@composed Modifier
    val isCatching = enabled && LocalSnowCapsVisible.current

    val palette = rememberChristmasPalette()
    val pxPerDp = LocalDensity.current.density
    val key = remember { Any() }
    val seed = remember { Random.nextInt() }
    var values by remember { mutableStateOf(SnowCapValues(initialDepth.coerceIn(0f, 1f))) }

    DisposableEffect(controller, key) {
        controller.registerCap(key) { amount ->
            values = values.copy(depth = (values.depth + amount).coerceAtMost(1f))
        }
        onDispose { controller.unregisterCap(key) }
    }
    LaunchedEffect(controller, isCatching) { controller.updateCap(key, visible = isCatching) }

    val startGeneration = remember(controller) { controller.shakeGeneration }
    val generation = controller.shakeGeneration
    LaunchedEffect(generation) {
        if (generation != startGeneration && isCatching) {
            controller.knockOffCap(key, values.depth)
            values = values.copy(depth = 0f)
        }
    }

    LaunchedEffect(controller, isCatching) {
        if (!isCatching) return@LaunchedEffect
        var last = -1L
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (last < 0) 0f else ((now - last) / NANOS_PER_SECOND).coerceAtMost(MAX_FRAME_SECONDS)
                last = now
                val step = advanceSnowCap(values, dt, controller.tiltDegrees, controller.isSnowing, Random.nextFloat())
                if (step.values != values) values = step.values
                if (step.spillDirection != 0) controller.spillFromCap(key, step.spillDirection)
                controller.updateCap(key, depth = values.depth)
            }
        }
    }

    this
        .onGloballyPositioned { controller.updateCap(key, bounds = it.boundsInRoot()) }
        .drawWithCache {
            val path = buildSnowCapOutline(size.width / pxPerDp, seed, cornerDp).toPath(pxPerDp)
            val capWidth = size.width + 2 * SnowCapMetrics.OVERHANG * pxPerDp
            val pivot = Offset(capWidth / 2f, SnowCapMetrics.BASELINE * pxPerDp)
            onDrawWithContent {
                drawContent()
                val current = values
                if (current.depth < 0.01f) return@onDrawWithContent
                val alpha = (current.depth * FADE_IN_DEPTH).coerceAtMost(1f)
                translate(
                    left = (current.slideDp - SnowCapMetrics.OVERHANG) * pxPerDp,
                    top = -SnowCapMetrics.RISE * pxPerDp,
                ) {
                    scale(scaleX = 1f, scaleY = current.depth, pivot = pivot) {
                        translate(top = SHADOW_OFFSET_DP * pxPerDp) { drawPath(path, palette.capShadow, alpha = alpha) }
                        drawPath(path, palette.capSnow, alpha = alpha)
                    }
                }
            }
        }
}

private fun List<CapCommand>.toPath(pxPerDp: Float): Path = Path().also { path ->
    forEach { command ->
        when (command) {
            is CapCommand.MoveTo -> path.moveTo(command.x * pxPerDp, command.y * pxPerDp)
            is CapCommand.LineTo -> path.lineTo(command.x * pxPerDp, command.y * pxPerDp)
            is CapCommand.QuadTo -> path.quadraticTo(
                command.controlX * pxPerDp,
                command.controlY * pxPerDp,
                command.x * pxPerDp,
                command.y * pxPerDp,
            )
        }
    }
    path.close()
}
