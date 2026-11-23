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
