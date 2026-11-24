package com.reyzie.hymns.ui.christmas.landing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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


class ChristmasCountdownZoneTest {
    private fun todayAt(instant: String, zone: String) =
        ChristmasCalendar.today(Clock.fixed(Instant.parse(instant), ZoneId.of(zone)))

    private fun daysAt(instant: String, zone: String) =
        ChristmasCalendar.countdownLabel(todayAt(instant, zone))

    @Test
    fun `the same moment is Christmas Eve in New York and Christmas Day in India`() {
        val eve = "2026-12-24T20:00:00Z"
        assertEquals(ChristmasMoment.CHRISTMAS_EVE, ChristmasCalendar.moment(todayAt(eve, "America/New_York")))
        assertEquals(ChristmasMoment.CHRISTMAS_EVE, ChristmasCalendar.moment(todayAt(eve, "America/Los_Angeles")))
        assertEquals(ChristmasMoment.CHRISTMAS_DAY, ChristmasCalendar.moment(todayAt(eve, "Asia/Kolkata")))
        assertEquals(ChristmasMoment.CHRISTMAS_DAY, ChristmasCalendar.moment(todayAt(eve, "Pacific/Kiritimati")))
    }

    @Test
    fun `the day count follows the local date in every zone`() {
        val nov24 = "2026-11-24T10:00:00Z"
        assertEquals("31 days to Christmas", daysAt(nov24, "America/New_York"))
        assertEquals("31 days to Christmas", daysAt(nov24, "Asia/Kolkata"))
        assertEquals("30 days to Christmas", daysAt(nov24, "Pacific/Kiritimati"))
        assertEquals("32 days to Christmas", daysAt(nov24, "Pacific/Pago_Pago"))
    }

    @Test
    fun `daylight saving changes do not shift the count`() {
        assertEquals("55 days to Christmas", daysAt("2026-10-31T16:00:00Z", "America/New_York"))
        assertEquals("299 days to Christmas", daysAt("2026-03-01T12:00:00Z", "Europe/London"))
    }

    @Test
    fun `the day after Christmas counts to next year only once Christmastide ends`() {
        assertEquals("Christmastide", daysAt("2026-12-26T12:00:00Z", "Asia/Kolkata"))
        assertEquals("353 days to Christmas", daysAt("2027-01-06T12:00:00Z", "Asia/Kolkata"))
    }

    @Test
    fun `advent text is the same wherever the phone is`() {
        val nov24 = "2026-11-24T10:00:00Z"
        assertEquals("Advent begins Sun 29 Nov", ChristmasCalendar.adventLabel(todayAt(nov24, "Asia/Kolkata")))
        assertEquals("Advent begins Sun 29 Nov", ChristmasCalendar.adventLabel(todayAt(nov24, "Pacific/Kiritimati")))
    }
}
