package com.reyzie.hymns.ui.christmas.motion

import kotlin.math.roundToInt

/** A vibration made of pulses: how long each step lasts and how strong it is (0 is a pause). */
data class HapticPattern(val timings: List<Long>, val amplitudes: List<Int>) {
    val totalMillis: Long get() = timings.sum()
}

/** Something the snow did that the phone should let you feel. */
sealed interface SnowHapticEvent {
    /** A clump slid off a card that had [depth] (0..1) of snow. */
    data class Slide(val depth: Float) : SnowHapticEvent

    /** The phone was shaken; [totalDepth] is all the snow knocked off, in full cards. */
    data class Shake(val totalDepth: Float) : SnowHapticEvent

    /** One of Santa's gifts was opened: a soft double tap. */
    data object GiftOpen : SnowHapticEvent

    /** A gift landed on a card: one small thud. */
    data object GiftLand : SnowHapticEvent
}

/**
 * Vibration patterns that give snow some weight: a soft tick when a clump slides off, and a
 * thump followed by a fading rumble when a shake knocks piles off. The more snow, the
 * longer and heavier it feels.
 */
object SnowHaptics {
    private const val MAX_AMPLITUDE = 255
    private const val MAX_SHAKE_DEPTH = 4f
    private const val MIN_RUMBLE_PULSES = 3
    private const val MAX_EXTRA_PULSES = 6
    private const val THUMP_MILLIS = 32L
    private const val PULSE_MILLIS = 16L
    private const val FIRST_GAP_MILLIS = 38L
    private const val GAP_GROWTH_MILLIS = 9L
    private const val SLIDE_MILLIS = 14L
    private const val SLIDE_MIN_AMPLITUDE = 45
    private const val SLIDE_AMPLITUDE_RANGE = 75
    private const val SHAKE_MIN_AMPLITUDE = 110
    private const val SHAKE_AMPLITUDE_RANGE = 145
    private const val FADE_PER_PULSE = 0.72f

    /** Two soft taps, the second a little firmer. */
    fun giftOpen() = HapticPattern(listOf(0L, 18L, 70L, 18L), listOf(0, 150, 0, 230))

    /** A single small thud. */
    fun giftLand() = HapticPattern(listOf(0L, 16L), listOf(0, 110))

    fun slide(depth: Float): HapticPattern {
        val amplitude = SLIDE_MIN_AMPLITUDE + (depth.coerceIn(0f, 1f) * SLIDE_AMPLITUDE_RANGE).roundToInt()
        return HapticPattern(listOf(0L, SLIDE_MILLIS), listOf(0, amplitude))
    }

    fun shake(totalDepth: Float): HapticPattern {
        val weight = (totalDepth / MAX_SHAKE_DEPTH).coerceIn(0f, 1f)
        val thump = SHAKE_MIN_AMPLITUDE + (weight * SHAKE_AMPLITUDE_RANGE).roundToInt()
        val pulses = MIN_RUMBLE_PULSES + (weight * MAX_EXTRA_PULSES).roundToInt()
        val timings = mutableListOf(0L, THUMP_MILLIS)
        val amplitudes = mutableListOf(0, thump)
        var strength = thump.toFloat()
        repeat(pulses) { i ->
            strength *= FADE_PER_PULSE
            timings += FIRST_GAP_MILLIS + i * GAP_GROWTH_MILLIS
            amplitudes += 0
            timings += PULSE_MILLIS
            amplitudes += strength.roundToInt().coerceIn(1, MAX_AMPLITUDE)
        }
        return HapticPattern(timings, amplitudes)
    }
}

/**
 * Lets a slide tick through at most once every [minGapMillis], so sliding feels like a
 * trickle, and at most [maxTicksPerWindow] within any [windowMillis]. The second limit is a
 * safety net: whatever sets the snow sliding, the motor never buzzes on and on.
 */
class SlideHapticThrottle(
    private val minGapMillis: Long,
    private val maxTicksPerWindow: Int = DEFAULT_MAX_TICKS,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
) {
    private val recent = ArrayDeque<Long>()

    fun tryFire(nowMillis: Long): Boolean {
        while (recent.isNotEmpty() && nowMillis - recent.first() >= windowMillis) recent.removeFirst()
        val last = recent.lastOrNull()
        if (last != null && nowMillis - last < minGapMillis) return false
        if (recent.size >= maxTicksPerWindow) return false
        recent.addLast(nowMillis)
        return true
    }

    private companion object {
        const val DEFAULT_MAX_TICKS = 5
        const val DEFAULT_WINDOW_MILLIS = 4_000L
    }
}
