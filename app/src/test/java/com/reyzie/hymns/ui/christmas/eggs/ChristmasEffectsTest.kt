package com.reyzie.hymns.ui.christmas.eggs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChristmasEffectsTest {

    private fun effects() = ChristmasEffects(Random(3))

    private fun ChristmasEffects.run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            step(1f / 60f)
            t += 1f / 60f
        }
    }

    @Test
    fun `sparkles fade away`() {
        val fx = effects()
        fx.sparkle(100f, 100f, count = 12)
        assertEquals(12, fx.particles.size)

        fx.run(1.2f)

        assertTrue(fx.particles.isEmpty())
    }

    @Test
    fun `bell rings start one after another`() {
        val fx = effects()
        fx.rings(50f, 50f)

        assertEquals(1, fx.particles.count { !it.isWaiting })
        fx.run(0.5f)
        assertEquals(3, fx.particles.count { !it.isWaiting })
    }

    @Test
    fun `a firework rocket bursts into sparks`() {
        val fx = effects()
        fx.rocket(width = 400f, launchY = 330f)

        fx.run(1.05f)

        assertTrue(fx.particles.isNotEmpty())
        assertTrue(fx.particles.all { it.kind == EffectKind.SPARK })
    }

    @Test
    fun `confetti falls from above the screen`() {
        val fx = effects()
        fx.confetti(width = 400f)

        assertTrue(fx.particles.all { it.y < 0f })
        fx.run(2f)
        assertTrue(fx.particles.any { it.y > 0f })
    }

    @Test
    fun `Santa flies left to right and then leaves`() {
        val start = requireNotNull(SantaFlight.pose(0f, widthPx = 400f, unit = 1f))
        val middle = requireNotNull(SantaFlight.pose(5f, widthPx = 400f, unit = 1f))

        assertTrue(middle.x > start.x)
        assertNull(SantaFlight.pose(SantaFlight.DURATION_SECONDS + 1f, widthPx = 400f, unit = 1f))
    }

    @Test
    fun `tapping the reindeer or sleigh catches Santa but the sky beside him does not`() {
        val pose = SantaPose(x = 200f, y = 80f, progress = 0.5f)

        assertTrue(SantaFlight.isHit(pose, 200f, 80f, unit = 1f))
        assertTrue(SantaFlight.isHit(pose, 300f, 85f, unit = 1f))
        assertTrue(!SantaFlight.isHit(pose, 200f, 200f, unit = 1f))
        assertTrue(!SantaFlight.isHit(pose, 50f, 80f, unit = 1f))
    }
}
