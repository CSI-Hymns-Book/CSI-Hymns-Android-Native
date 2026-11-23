package com.reyzie.hymns.ui.christmas.eggs

import org.junit.Assert.assertEquals
import org.junit.Test

class ChristmasSecretsTest {

    @Test
    fun `seven quick taps fire the shooting star with a hint on the fourth`() {
        val counter = StarTapCounter()
        val results = (1..7).map { counter.tap(it * 0.3f) }

        assertEquals(StarTapResult.HINT, results[3])
        assertEquals(StarTapResult.SHOOTING_STAR, results[6])
        assertEquals(5, results.count { it == StarTapResult.TAP })
    }

    @Test
    fun `a long pause starts the count again`() {
        val counter = StarTapCounter(windowSeconds = 2.5f)
        repeat(6) { counter.tap(it * 0.3f) }

        val afterPause = counter.tap(10f)

        assertEquals(StarTapResult.TAP, afterPause)
    }

    @Test
    fun `count starts over after the shooting star`() {
        val counter = StarTapCounter()
        repeat(7) { counter.tap(it * 0.2f) }

        val results = (1..7).map { counter.tap(2f + it * 0.2f) }

        assertEquals(StarTapResult.SHOOTING_STAR, results.last())
    }

    @Test
    fun `stored secrets ignore unknown names`() {
        val parsed = ChristmasSecret.fromStored(setOf("STAR", "SANTA", "UNICORN"))

        assertEquals(setOf(ChristmasSecret.STAR, ChristmasSecret.SANTA), parsed)
    }
}
