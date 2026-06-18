package com.reyzie.hymns.utils

import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * Material 3 Expressive motion tokens — aligned with Essentials (medium-bouncy springs).
 */
object MotionSpecs {
    const val DurationTouch = 120
    const val DurationShort = 200
    const val DurationMedium = 300
    const val DurationLong = 450

    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val StandardDecelerate = CubicBezierEasing(0.0f, 0.0f, 1.0f, 1.0f)

    /** Essentials tab / toolbar spring */
    val ExpressiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val ExpressiveDpSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val PressScaleSpec = tween<Float>(durationMillis = 100, easing = LinearOutSlowInEasing)
    val ReleaseScaleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 500f
    )

    val SiblingSpec = tween<Float>(durationMillis = 180, easing = EmphasizedDecelerate)

    val ColorSpec = tween<Color>(durationMillis = DurationMedium, easing = Emphasized)
    val DpSpec = tween<Dp>(durationMillis = DurationMedium, easing = Emphasized)
    val WeightSpec = tween<Float>(durationMillis = DurationMedium, easing = Emphasized)
}
