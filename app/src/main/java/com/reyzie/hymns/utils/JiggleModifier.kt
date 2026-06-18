package com.reyzie.hymns.utils

import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.collectLatest

/**
 * Connected Expressive Interaction Modifier.
 * Allows buttons to react to their own presses AND broadcast state to siblings.
 */
fun Modifier.expressiveClick(
    interactionSource: MutableInteractionSource? = null,
    onPressedChange: ((Boolean) -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    // Internal scale animation (Restrained Pinch)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1.0f,
        animationSpec = if (isPressed) MotionSpecs.PressScaleSpec else MotionSpecs.ReleaseScaleSpec,
        label = "tactilePinch"
    )

    // Sync with provided interaction source if any
    LaunchedEffect(interactionSource) {
        interactionSource?.interactions?.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(false)
                    isPressed = true
                    onPressedChange?.invoke(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    onPressedChange?.invoke(false)
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Modifier for Sibling/Neighbor response to a nearby interaction.
 */
fun Modifier.connectedResponse(
    isNeighborPressed: Boolean
): Modifier = composed {
    val neighborScaleX by animateFloatAsState(
        targetValue = if (isNeighborPressed) 0.9f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "neighborScaleX"
    )
    val neighborScaleY by animateFloatAsState(
        targetValue = if (isNeighborPressed) 0.97f else 1.0f,
        animationSpec = MotionSpecs.SiblingSpec,
        label = "neighborScaleY"
    )

    this.graphicsLayer {
        scaleX = neighborScaleX
        scaleY = neighborScaleY
    }
}

// Legacy alias
fun Modifier.jiggle(trigger: Any? = null): Modifier = this.expressiveClick()
