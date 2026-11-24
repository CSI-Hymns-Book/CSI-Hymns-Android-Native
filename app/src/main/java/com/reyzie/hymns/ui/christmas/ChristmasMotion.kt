package com.reyzie.hymns.ui.christmas

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared timings for every Christmas screen, so entrances, presses and transitions
 * all move the same way.
 */
object ChristmasMotion {
    /** Fast start, long soft landing. Used for entrances and the card-to-screen expand. */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)

    const val ENTRANCE_MILLIS = 750
    const val ENTRANCE_START_DELAY_MILLIS = 150
    const val STAGGER_MILLIS = 75
    const val EXPAND_MILLIS = 550
    const val CROSSFADE_MILLIS = 450
    const val VERSE_ROTATE_MILLIS = 6_500L

    /** Scale a card shrinks to while pressed. */
    const val PRESSED_SCALE = 0.965f

    /** Bouncy release after a press. */
    fun <T> pressSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)

    /** Staggered entrance for the item at [index] in a list. */
    fun <T> entrance(index: Int): FiniteAnimationSpec<T> = tween(
        durationMillis = ENTRANCE_MILLIS,
        delayMillis = ENTRANCE_START_DELAY_MILLIS + index * STAGGER_MILLIS,
        easing = EmphasizedEasing,
    )

    /** Scale an item starts from when it zooms in. */
    const val POP_START_SCALE = 0.84f

    /** Springy zoom-in with a little overshoot, so items pop into place. */
    fun <T> popIn(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessLow)

    fun <T> expand(): FiniteAnimationSpec<T> =
        tween(durationMillis = EXPAND_MILLIS, easing = EmphasizedEasing)
}
