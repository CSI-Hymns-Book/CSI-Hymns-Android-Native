package com.reyzie.hymns.ui.christmas.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowHapticsTest {
    @Test
    fun `timings and strengths line up`() {
        listOf(SnowHaptics.shake(0f), SnowHaptics.shake(3f), SnowHaptics.slide(0.5f)).forEach {
            assertEquals(it.timings.size, it.amplitudes.size)
            assertTrue(it.amplitudes.all { amplitude -> amplitude in 0..255 })
            assertTrue(it.timings.all { millis -> millis >= 0 })
        }
    }

    @Test
    fun `more snow gives a longer and stronger shake`() {
        val light = SnowHaptics.shake(0.3f)
        val heavy = SnowHaptics.shake(3f)

        assertTrue(heavy.totalMillis > light.totalMillis)
        assertTrue(heavy.amplitudes.max() > light.amplitudes.max())
    }

    @Test
    fun `a shake rumble fades out`() {
        val strengths = SnowHaptics.shake(3f).amplitudes.filter { it > 0 }

        assertTrue(strengths.first() > strengths.last())
    }

    @Test
    fun `deeper snow makes a firmer slide tick`() {
        assertTrue(SnowHaptics.slide(1f).amplitudes.max() > SnowHaptics.slide(0.1f).amplitudes.max())
    }

    @Test
    fun `slide ticks are spaced out`() {
        val throttle = SlideHapticThrottle(minGapMillis = 140)

        assertTrue(throttle.tryFire(nowMillis = 1_000))
        assertFalse(throttle.tryFire(nowMillis = 1_100))
        assertTrue(throttle.tryFire(nowMillis = 1_150))
    }
}
