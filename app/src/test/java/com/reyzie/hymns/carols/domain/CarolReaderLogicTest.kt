package com.reyzie.hymns.carols.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarolRecentsTest {
    @Test
    fun `newly viewed carol goes to the front`() {
        assertEquals(listOf("c", "a", "b"), CarolRecents.push(listOf("a", "b"), "c"))
    }

    @Test
    fun `viewing a carol again moves it to the front without duplicating it`() {
        assertEquals(listOf("b", "a", "c"), CarolRecents.push(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun `list keeps only the most recent carols`() {
        val result = CarolRecents.push(listOf("1", "2", "3"), "4", max = 3)
        assertEquals(listOf("4", "1", "2"), result)
    }

    @Test
    fun `stored text round trips and ignores blanks`() {
        val ids = listOf("x", "y")
        assertEquals(ids, CarolRecents.decode(CarolRecents.encode(ids)))
        assertEquals(emptyList<String>(), CarolRecents.decode(""))
        assertEquals(listOf("a"), CarolRecents.decode(",a,,"))
    }
}

class CarolTextScaleTest {
    @Test
    fun `bigger and smaller step within limits`() {
        assertEquals(1.1f, CarolTextScale.bigger(1f), 0.001f)
        assertEquals(0.9f, CarolTextScale.smaller(1f), 0.001f)
        assertEquals(CarolTextScale.MAX, CarolTextScale.bigger(CarolTextScale.MAX), 0.001f)
        assertEquals(CarolTextScale.MIN, CarolTextScale.smaller(CarolTextScale.MIN), 0.001f)
    }

    @Test
    fun `out of range stored values are clamped`() {
        assertEquals(CarolTextScale.MAX, CarolTextScale.clamp(9f), 0.001f)
        assertEquals(CarolTextScale.MIN, CarolTextScale.clamp(0f), 0.001f)
    }
}

class ChurchInitialsTest {
    @Test
    fun `uses the first letters of the first two words`() {
        assertEquals("SP", churchInitials("St. Paul's Church"))
        assertEquals("CC", churchInitials("  christ   church "))
    }

    @Test
    fun `single word gives one letter and blank gives a star`() {
        assertEquals("B", churchInitials("Bethel"))
        assertEquals("✦", churchInitials("   "))
    }

    @Test
    fun `same name always gets the same colour slot`() {
        assertEquals(churchColorSlot("Bethel", 5), churchColorSlot("Bethel", 5))
        assertTrue(churchColorSlot("Anything", 5) in 0 until 5)
    }
}

class CarolTextScaleSliderTest {
    @Test
    fun `snaps to the nearest five percent`() {
        assertEquals(1.05f, CarolTextScale.snap(1.049f), 0.0001f)
        assertEquals(1.1f, CarolTextScale.snap(1.08f), 0.0001f)
        assertEquals(1.0f, CarolTextScale.snap(1.02f), 0.0001f)
    }

    @Test
    fun `snapping keeps the size within limits`() {
        assertEquals(CarolTextScale.MIN, CarolTextScale.snap(0.1f), 0.0001f)
        assertEquals(CarolTextScale.MAX, CarolTextScale.snap(5f), 0.0001f)
    }

    @Test
    fun `the slider has a stop for every five percent`() {
        val span = CarolTextScale.MAX - CarolTextScale.MIN
        // 20 intervals means 19 stops between the two ends.
        assertEquals(20, Math.round(span / CarolTextScale.SLIDER_STEP))
        assertEquals(19, CarolTextScale.SLIDER_STOPS)
    }

    @Test
    fun `shows the size as a percentage`() {
        assertEquals("100%", CarolTextScale.percentLabel(1f))
        assertEquals("115%", CarolTextScale.percentLabel(1.15f))
        assertEquals("180%", CarolTextScale.percentLabel(9f))
    }
}

class SplitStanzasTest {
    @Test
    fun `splits at blank lines and trims each verse`() {
        val lyrics = "1. Joy to the world\nThe Lord is come\n\n  \n2. Let earth receive\r\n\r\nChorus "
        assertEquals(
            listOf("1. Joy to the world\nThe Lord is come", "2. Let earth receive", "Chorus"),
            splitStanzas(lyrics),
        )
    }

    @Test
    fun `blank lyrics give no verses`() {
        assertEquals(emptyList<String>(), splitStanzas("  \n\n "))
    }
}
