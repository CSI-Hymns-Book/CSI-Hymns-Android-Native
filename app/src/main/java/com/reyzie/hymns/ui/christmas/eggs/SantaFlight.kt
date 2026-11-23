package com.reyzie.hymns.ui.christmas.eggs

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Where the sleigh is, in pixels. Reindeer run ahead to the right of [x]. */
data class SantaPose(val x: Float, val y: Float, val progress: Float)

/**
 * Santa's path across the top of the screen: in from the left, gently rising and bobbing,
 * out on the right. All sizes are in dp and scaled by [unit] (pixels per dp).
 */
object SantaFlight {
    const val DURATION_SECONDS = 11f
    private const val START_X_DP = -150f
    private const val EXTRA_TRAVEL_DP = 320f
    private const val START_Y_DP = 100f
    private const val RISE_DP = 44f
    private const val BOB_DP = 9f
    private const val BOBS = 1.5f
    private const val TEAM_LENGTH_DP = 135f
    private const val TAIL_DP = 25f
    private const val HIT_HEIGHT_DP = 26f

    /** Pose at [elapsedSeconds] into the flight, or null once he has flown off screen. */
    fun pose(elapsedSeconds: Float, widthPx: Float, unit: Float): SantaPose? {
        val progress = elapsedSeconds / DURATION_SECONDS
        if (progress < 0f || progress > 1f) return null
        val x = (START_X_DP * unit) + (widthPx + EXTRA_TRAVEL_DP * unit) * progress
        val y = (START_Y_DP - RISE_DP * progress + BOB_DP * sin(progress * 2 * PI.toFloat() * BOBS)) * unit
        return SantaPose(x, y, progress)
    }

    /** True when a tap at ([tapX], [tapY]) lands on the sleigh or the reindeer team. */
    fun isHit(pose: SantaPose, tapX: Float, tapY: Float, unit: Float): Boolean =
        tapX >= pose.x - TAIL_DP * unit &&
            tapX <= pose.x + TEAM_LENGTH_DP * unit &&
            abs(tapY - pose.y) <= HIT_HEIGHT_DP * unit
}
