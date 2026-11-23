package com.reyzie.hymns.ui.christmas.snow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowCapGeometryTest {

    private fun CapCommand.points(): List<Pair<Float, Float>> = when (this) {
        is CapCommand.MoveTo -> listOf(x to y)
        is CapCommand.LineTo -> listOf(x to y)
        is CapCommand.QuadTo -> listOf(controlX to controlY, x to y)
    }

    @Test
    fun `outline stays inside the cap box`() {
        val width = 170f
        val outline = buildSnowCapOutline(width, seed = 7)

        assertTrue(outline.first() is CapCommand.MoveTo)
        outline.flatMap { it.points() }.forEach { (x, y) ->
            assertTrue("x=$x", x in 0f..(width + 2 * SnowCapMetrics.OVERHANG))
            assertTrue("y=$y", y in 0f..SnowCapMetrics.HEIGHT)
        }
    }

    @Test
    fun `snow never sticks out past the card edges`() {
        listOf(100f to 18f, 170f to 24f, 360f to 22f, 60f to 28f).forEach { (width, corner) ->
            val points = buildSnowCapOutline(width, seed = 5, cornerDp = corner).flatMap { it.points() }

            assertTrue("min x for $width", points.minOf { it.first } >= SnowCapMetrics.OVERHANG)
            assertTrue("max x for $width", points.maxOf { it.first } <= width + SnowCapMetrics.OVERHANG)
        }
    }

    @Test
    fun `larger corners pull the ends further in`() {
        fun leftEnd(corner: Float) = (buildSnowCapOutline(200f, 1, corner).first() as CapCommand.MoveTo).x

        assertTrue(leftEnd(28f) > leftEnd(10f))
    }

    @Test
    fun `same seed gives the same cap`() {
        assertEquals(buildSnowCapOutline(200f, 3), buildSnowCapOutline(200f, 3))
    }

    @Test
    fun `wider cards get more icicles`() {
        fun icicles(width: Float) = buildSnowCapOutline(width, 1).count { it is CapCommand.LineTo && it.y > 14f }

        assertTrue(icicles(350f) > icicles(100f))
    }

    @Test
    fun `snow builds up while it is snowing`() {
        val step = advanceSnowCap(SnowCapValues(depth = 0.5f), dt = 1f, tiltDegrees = 0f, isSnowing = true, roll = 1f)

        assertTrue(step.values.depth > 0.5f)
        assertEquals(0, step.spillDirection)
    }

    @Test
    fun `snow never grows past full`() {
        val step = advanceSnowCap(SnowCapValues(depth = 1f), dt = 10f, tiltDegrees = 0f, isSnowing = true, roll = 1f)

        assertEquals(1f, step.values.depth)
    }

    @Test
    fun `tilting right melts the cap and spills to the right`() {
        val step = advanceSnowCap(SnowCapValues(depth = 1f), dt = 0.1f, tiltDegrees = 25f, isSnowing = true, roll = 0f)

        assertTrue(step.values.depth < 1f)
        assertEquals(1, step.spillDirection)
        assertTrue(step.values.slideDp > 0f)
    }

    @Test
    fun `tilting left spills to the left`() {
        val step = advanceSnowCap(SnowCapValues(depth = 1f), dt = 0.1f, tiltDegrees = -25f, isSnowing = true, roll = 0f)

        assertEquals(-1, step.spillDirection)
        assertTrue(step.values.slideDp < 0f)
    }

    @Test
    fun `a small tilt does nothing`() {
        val step = advanceSnowCap(SnowCapValues(depth = 0.8f), dt = 0.1f, tiltDegrees = 6f, isSnowing = false, roll = 0f)

        assertEquals(SnowCapValues(depth = 0.8f), step.values)
    }

    @Test
    fun `slide returns to centre once level`() {
        var values = SnowCapValues(depth = 0.4f, slideDp = 10f)
        repeat(120) { values = advanceSnowCap(values, 1f / 60f, 0f, isSnowing = false, roll = 1f).values }

        assertEquals(0f, values.slideDp)
    }
}
