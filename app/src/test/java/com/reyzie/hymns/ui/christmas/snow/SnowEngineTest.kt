package com.reyzie.hymns.ui.christmas.snow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SnowEngineTest {

    private fun engine(random: Random = Random(42)) = SnowEngine(random).apply { resize(400f, 800f, 1f) }

    @Test
    fun `seeds every layer at its full particle count`() {
        val engine = engine()

        SnowLayer.entries.forEach { layer ->
            assertEquals(layer.count, engine.particles.count { it.layer == layer })
        }
    }

    @Test
    fun `particles fall smoothly and only wrap after leaving the bottom`() {
        val engine = engine()
        val params = SnowStepParams(densityFraction = 1f)

        repeat(2_000) {
            val before = engine.particles.map { it.y }
            engine.step(1f / 60f, params)
            engine.particles.forEachIndexed { i, p ->
                val moved = p.y - before[i]
                val wrapped = moved < 0f
                if (wrapped) {
                    assertTrue("wrapped before reaching the bottom", before[i] > engine.height)
                } else {
                    assertTrue("jumped $moved px in one frame", moved < 3f)
                }
            }
        }
    }

    @Test
    fun `gentle density shows under half of each layer`() {
        val engine = engine()

        SnowLayer.entries.forEach { layer ->
            val inLayer = engine.particles.filter { it.layer == layer }
            val visible = inLayer.count { it.isVisible(0.45f) }
            assertTrue(visible in 1 until inLayer.size / 2 + 1)
        }
    }

    @Test
    fun `off density hides every particle`() {
        assertTrue(engine().particles.none { it.isVisible(0f) })
    }

    @Test
    fun `snow lands on a card top and melts into it`() {
        val engine = engine(Random(1))
        val surface = SnowSurface(key = "card", left = 0f, right = 400f, top = 400f)
        val absorbed = mutableListOf<Any>()
        engine.onAbsorb = { absorbed += it }

        repeat(600) { engine.step(1f / 60f, SnowStepParams(densityFraction = 1f), listOf(surface)) }

        assertTrue("nothing settled or melted", engine.settled.isNotEmpty() || absorbed.isNotEmpty())
        assertTrue(absorbed.all { it == "card" })
    }

    @Test
    fun `tilting slides settled snow off the card`() {
        val engine = engine(Random(1))
        val surface = SnowSurface(key = "card", left = 100f, right = 300f, top = 400f)
        repeat(300) { engine.step(1f / 60f, SnowStepParams(densityFraction = 1f), listOf(surface)) }
        val settledBefore = engine.settled.size
        assertTrue(settledBefore > 0)

        repeat(240) { engine.step(1f / 60f, SnowStepParams(densityFraction = 1f, tiltDegrees = 35f), listOf(surface)) }

        assertTrue(engine.settled.size < settledBefore)
    }

    @Test
    fun `knocking off a card throws chunks that fall away`() {
        val engine = engine()
        val surface = SnowSurface(key = "card", left = 0f, right = 300f, top = 300f)

        engine.knockOff(surface, depth = 1f)
        assertTrue(engine.chunks.isNotEmpty())

        repeat(200) { engine.step(1f / 60f, SnowStepParams(densityFraction = 0f)) }
        assertTrue(engine.chunks.isEmpty())
    }

    @Test
    fun `no landing on a card while the phone is tilted`() {
        val engine = engine(Random(1))
        val surface = SnowSurface(key = "card", left = 0f, right = 400f, top = 400f)

        repeat(300) { engine.step(1f / 60f, SnowStepParams(densityFraction = 1f, tiltDegrees = 30f), listOf(surface)) }

        assertTrue(engine.settled.isEmpty())
    }

    @Test
    fun `popping an emoji hides it briefly then drops it from the top`() {
        val engine = engine()
        val target = engine.emojis.first()

        val popped = engine.popEmojiAt(target.x, target.y, radius = 10f, visibleCount = 8)

        assertEquals(target, popped)
        assertTrue(target.isHidden)
        repeat(150) { engine.step(1f / 60f, SnowStepParams(densityFraction = 1f)) }
        assertTrue(!target.isHidden)
    }

    @Test
    fun `tapping empty sky pops nothing`() {
        val engine = engine()

        assertEquals(null, engine.popEmojiAt(-500f, -500f, radius = 10f, visibleCount = 8))
    }
}
