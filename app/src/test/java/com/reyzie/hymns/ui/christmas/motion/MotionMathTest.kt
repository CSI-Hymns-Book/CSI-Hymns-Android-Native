package com.reyzie.hymns.ui.christmas.motion

import com.reyzie.hymns.ui.christmas.ChristmasEffectsLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMathTest {

    @Test
    fun `phone held upright has no tilt`() {
        assertEquals(0f, rollDegrees(0f, 9.81f, 0f), 0.01f)
    }

    @Test
    fun `right edge down gives positive tilt`() {
        assertEquals(90f, rollDegrees(-9.81f, 0f, 0f), 0.01f)
    }

    @Test
    fun `left edge down gives negative tilt`() {
        assertTrue(rollDegrees(4f, 8f, 0f) < 0f)
    }

    @Test
    fun `resting phone is not a shake`() {
        assertFalse(ShakeDetector().onAcceleration(0f, 9.81f, 0f, 0L))
    }

    @Test
    fun `strong jolt is a shake`() {
        assertTrue(ShakeDetector().onAcceleration(25f, 9.81f, 5f, 0L))
    }

    @Test
    fun `one shake fires once within the cooldown`() {
        val detector = ShakeDetector(cooldownMillis = 1_500L)

        assertTrue(detector.onAcceleration(30f, 0f, 0f, 1_000L))
        assertFalse(detector.onAcceleration(30f, 0f, 0f, 1_400L))
        assertTrue(detector.onAcceleration(30f, 0f, 0f, 2_600L))
    }

    @Test
    fun `unknown or missing effects setting falls back to full`() {
        assertEquals(ChristmasEffectsLevel.GENTLE, ChristmasEffectsLevel.fromKey(null))
        assertEquals(ChristmasEffectsLevel.GENTLE, ChristmasEffectsLevel.fromKey("SPARKLY"))
        assertEquals(ChristmasEffectsLevel.GENTLE, ChristmasEffectsLevel.fromKey("FULL"))
        assertEquals(ChristmasEffectsLevel.GENTLE, ChristmasEffectsLevel.fromKey("GENTLE"))
    }
}
