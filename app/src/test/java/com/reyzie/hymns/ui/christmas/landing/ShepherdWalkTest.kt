package com.reyzie.hymns.ui.christmas.landing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShepherdWalkTest {
    @Test
    fun `nobody walks before the first start`() {
        assertTrue(ShepherdWalk.walkers(0f).isEmpty())
        assertTrue(ShepherdWalk.walkers(ShepherdWalk.FIRST_START_SECONDS - 0.1f).isEmpty())
    }

    @Test
    fun `the first shepherd sets off alone`() {
        val walkers = ShepherdWalk.walkers(ShepherdWalk.FIRST_START_SECONDS)

        assertEquals(1, walkers.size)
        assertEquals(0, walkers.single().index)
        assertEquals(0f, walkers.single().progress, 0.001f)
    }

    @Test
    fun `the next shepherd follows after a fixed gap, never all together`() {
        val first = ShepherdWalk.FIRST_START_SECONDS
        val gap = ShepherdWalk.GAP_SECONDS

        assertEquals(1, ShepherdWalk.walkers(first + gap - 0.1f).size)
        assertEquals(listOf(0, 1), ShepherdWalk.walkers(first + gap + 1f).map { it.index })
    }

    @Test
    fun `never more than two are on screen and each one leaves`() {
        (0..2_000).map { it * 0.5f }.forEach { t ->
            assertTrue("at $t", ShepherdWalk.walkers(t).size <= 2)
            assertTrue(ShepherdWalk.walkers(t).all { it.progress in 0f..1f })
        }
        val after = ShepherdWalk.FIRST_START_SECONDS + ShepherdWalk.WALK_SECONDS + 0.1f
        assertTrue(0 !in ShepherdWalk.walkers(after).map { it.index })
    }

    @Test
    fun `looks rotate through the variants`() {
        val variants = (0 until ShepherdWalk.VARIANTS * 2).map { ShepherdWalk.Walker(it, 0.5f).variant }

        assertEquals(listOf(0, 1, 2, 0, 1, 2), variants)
    }
}
