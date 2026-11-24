package com.reyzie.hymns.ui.christmas.landing

import kotlin.math.ceil

/**
 * Timing for the shepherds who walk across the hills toward the stable, one after another
 * on a fixed schedule rather than all at once. A new shepherd sets off every
 * [GAP_SECONDS]; each one takes [WALK_SECONDS] to cross, so at most two are visible.
 */
object ShepherdWalk {
    const val FIRST_START_SECONDS = 3f
    const val GAP_SECONDS = 14f
    const val WALK_SECONDS = 26f

    /** Number of different shepherd looks, used in turn. */
    const val VARIANTS = 3

    /** The [index]-th shepherd to set off, [progress] of the way across (0..1). */
    data class Walker(val index: Int, val progress: Float) {
        val variant: Int get() = index % VARIANTS
    }

    /** Everyone who is walking at [seconds] on the scene clock, earliest shepherd first. */
    fun walkers(seconds: Float): List<Walker> {
        if (seconds < FIRST_START_SECONDS) return emptyList()
        val newest = ((seconds - FIRST_START_SECONDS) / GAP_SECONDS).toInt()
        val oldest = (newest - ceil(WALK_SECONDS / GAP_SECONDS).toInt()).coerceAtLeast(0)
        return (oldest..newest).mapNotNull { index ->
            val progress = (seconds - (FIRST_START_SECONDS + index * GAP_SECONDS)) / WALK_SECONDS
            if (progress in 0f..1f) Walker(index, progress) else null
        }
    }
}
