package com.reyzie.hymns.ui.christmas.motion

import kotlin.math.atan2
import kotlin.math.sqrt

private const val EARTH_GRAVITY = 9.81f

/**
 * Side-to-side tilt in degrees from a gravity reading. Positive means the right edge
 * of the phone is lower, so snow should slide to the right.
 */
fun rollDegrees(x: Float, y: Float, z: Float): Float =
    -Math.toDegrees(atan2(x.toDouble(), sqrt((y * y + z * z).toDouble()))).toFloat()

/**
 * Re-expresses a sensor reading in the screen's own directions. Sensors always report along
 * the phone's natural (portrait) sides, so when the screen is rotated the sideways axis
 * becomes the vertical one. Without this, a phone held upright in landscape reads as tilted
 * by 90 degrees. [quarterTurns] is the display rotation (0, 1, 2 or 3 quarter turns, as
 * `Surface.ROTATION_*`). Returns the reading's (screen-right, screen-up) parts.
 */
fun toScreenFrame(x: Float, y: Float, quarterTurns: Int): Pair<Float, Float> =
    when (((quarterTurns % 4) + 4) % 4) {
        1 -> -y to x
        2 -> -x to -y
        3 -> y to -x
        else -> x to y
    }

/**
 * Detects a deliberate shake from raw accelerometer readings (gravity included).
 * Fires at most once per [cooldownMillis] so one shake gives one flurry.
 */
class ShakeDetector(
    private val thresholdG: Float = 2.4f,
    private val cooldownMillis: Long = 1_500L,
) {
    private var lastShakeMillis: Long? = null

    fun onAcceleration(x: Float, y: Float, z: Float, timestampMillis: Long): Boolean {
        val gForce = sqrt(x * x + y * y + z * z) / EARTH_GRAVITY
        if (gForce < thresholdG) return false
        val last = lastShakeMillis
        if (last != null && timestampMillis - last < cooldownMillis) return false
        lastShakeMillis = timestampMillis
        return true
    }
}
