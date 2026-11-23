package com.reyzie.hymns.ui.christmas.landing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChristmasCalendarTest {

    @Test
    fun `counts down the days to Christmas`() {
        assertEquals("32 days to Christmas", ChristmasCalendar.countdownLabel(LocalDate.of(2026, 11, 23)))
    }

    @Test
    fun `says tomorrow on Christmas Eve`() {
        assertEquals("Christmas is tomorrow", ChristmasCalendar.countdownLabel(LocalDate.of(2026, 12, 24)))
    }

    @Test
    fun `greets on Christmas Day`() {
        assertEquals("Merry Christmas!", ChristmasCalendar.countdownLabel(LocalDate.of(2026, 12, 25)))
    }

    @Test
    fun `shows Christmastide after Christmas and into early January`() {
        assertEquals("Christmastide", ChristmasCalendar.countdownLabel(LocalDate.of(2026, 12, 28)))
        assertEquals("Christmastide", ChristmasCalendar.countdownLabel(LocalDate.of(2027, 1, 3)))
    }

    @Test
    fun `Advent starts four Sundays before Christmas`() {
        assertEquals(LocalDate.of(2026, 11, 29), ChristmasCalendar.adventStart(2026))
        assertEquals(LocalDate.of(2025, 11, 30), ChristmasCalendar.adventStart(2025))
        // Christmas 2022 fell on a Sunday, so the fourth Sunday of Advent was 18 December.
        assertEquals(LocalDate.of(2022, 11, 27), ChristmasCalendar.adventStart(2022))
    }

    @Test
    fun `announces when Advent begins`() {
        assertEquals("Advent begins Sun 29 Nov", ChristmasCalendar.adventLabel(LocalDate.of(2026, 11, 23)))
    }

    @Test
    fun `shows Advent season once it has started`() {
        assertEquals("Advent season", ChristmasCalendar.adventLabel(LocalDate.of(2026, 12, 10)))
    }

    @Test
    fun `shows nothing about Advent in mid-year`() {
        assertEquals("", ChristmasCalendar.adventLabel(LocalDate.of(2026, 7, 1)))
    }
}

class ChristmasMomentTest {

    @Test
    fun `recognises each special day`() {
        assertEquals(ChristmasMoment.CHRISTMAS_EVE, ChristmasCalendar.moment(LocalDate.of(2026, 12, 24)))
        assertEquals(ChristmasMoment.CHRISTMAS_DAY, ChristmasCalendar.moment(LocalDate.of(2026, 12, 25)))
        assertEquals(ChristmasMoment.NEW_YEAR, ChristmasCalendar.moment(LocalDate.of(2027, 1, 1)))
        assertEquals(ChristmasMoment.EPIPHANY, ChristmasCalendar.moment(LocalDate.of(2027, 1, 6)))
        assertEquals(ChristmasMoment.NONE, ChristmasCalendar.moment(LocalDate.of(2026, 11, 23)))
    }

    @Test
    fun `chip shows the countdown on ordinary days and a greeting on special ones`() {
        assertEquals("32 days to Christmas", ChristmasCalendar.chipLabel(LocalDate.of(2026, 11, 23)))
        assertEquals("Christmas Eve · Santa is on his way", ChristmasCalendar.chipLabel(LocalDate.of(2026, 12, 24)))
        assertEquals("Epiphany · the wise men arrive", ChristmasCalendar.chipLabel(LocalDate.of(2027, 1, 6)))
    }
}
