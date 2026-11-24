package com.reyzie.hymns.ui.christmas.eggs

import com.reyzie.hymns.ui.christmas.snow.SnowSurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftDropsTest {
    private val shelf = SnowSurface("card", 0f, 400f, 300f)

    private fun GiftDrops.run(seconds: Float, surfaces: List<SnowSurface>, onLanded: (Float, Float) -> Unit = { _, _ -> }) {
        repeat((seconds * 30).toInt()) { step(1f / 30f, surfaces, 800f, 1f, onLanded) }
    }

    private fun dropOne(x: Float = 200f, y: Float = 80f): GiftDrops =
        GiftDrops().apply { startFlight(); flight(0.23f, x, y, allowed = true) }

    @Test
    fun `surprise odds match their weights`() {
        val counts = HashMap<GiftSurprise, Int>()
        for (percent in 0 until 100) {
            val surprise = GiftSurprise.pick(percent / 100f + 0.001f)
            counts[surprise] = (counts[surprise] ?: 0) + 1
        }
        GiftSurprise.entries.forEach { assertEquals(it.name, it.weight, counts[it]) }
        assertEquals(100, GiftSurprise.entries.sumOf { it.weight })
    }

    @Test
    fun `roll edges pick the first and last surprise`() {
        assertEquals(GiftSurprise.CONFETTI, GiftSurprise.pick(0f))
        assertEquals(GiftSurprise.STAR, GiftSurprise.pick(0.9999f))
    }

    @Test
    fun `one gift per drop point and three over a full flight`() {
        val drops = GiftDrops()
        drops.startFlight()
        drops.flight(0.1f, 50f, 50f, allowed = true)
        assertTrue(drops.isEmpty)
        drops.flight(0.23f, 50f, 50f, allowed = true)
        drops.flight(0.22f, 60f, 50f, allowed = true)
        assertEquals(1, drops.gifts.size)
        drops.flight(0.5f, 120f, 50f, allowed = true)
        drops.flight(0.9f, 220f, 50f, allowed = true)
        assertEquals(3, drops.gifts.size)
    }

    @Test
    fun `never more than the cap on screen`() {
        val drops = GiftDrops()
        drops.startFlight()
        drops.flight(0.9f, 220f, 50f, allowed = true)
        drops.startFlight()
        drops.flight(0.9f, 220f, 50f, allowed = true)
        assertEquals(GiftDrops.MAX_GIFTS, drops.gifts.size)
    }

    @Test
    fun `no gifts when not allowed`() {
        val drops = GiftDrops()
        drops.startFlight()
        drops.flight(1f, 10f, 10f, allowed = false)
        assertTrue(drops.isEmpty)
    }

    @Test
    fun `a gift lands once on the card it crosses then sinks away`() {
        val drops = dropOne()
        var landings = 0
        drops.run(8f, listOf(shelf)) { _, _ -> landings += 1 }
        assertEquals(1, landings)
        drops.run(6f, listOf(shelf))
        assertTrue(drops.isEmpty)
    }

    @Test
    fun `a landed gift rides its card when it scrolls`() {
        val drops = dropOne()
        drops.run(4f, listOf(shelf))
        drops.step(1f / 30f, listOf(SnowSurface("card", 0f, 400f, 180f)), 800f)
        assertEquals(180f - GiftDrops.HALF_SIZE, drops.gifts.first().y, 0.01f)
    }

    @Test
    fun `with no card below a gift falls off the screen`() {
        val drops = dropOne()
        drops.run(12f, emptyList())
        assertTrue(drops.isEmpty)
    }

    @Test
    fun `a tap near a gift opens it once and it disappears`() {
        val drops = dropOne(y = 200f)
        assertNull(drops.open(400f, 600f))
        assertNotNull(drops.open(215f, 190f))
        assertEquals(GiftState.OPENING, drops.gifts.first().state)
        assertNull(drops.open(215f, 190f))
        drops.run(1.5f, emptyList())
        assertTrue(drops.isEmpty)
    }
}
