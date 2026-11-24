package com.reyzie.hymns.ui.christmas.landing

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Days that get their own celebration on the Christmas screens. */
enum class ChristmasMoment { NONE, CHRISTMAS_EVE, CHRISTMAS_DAY, NEW_YEAR, EPIPHANY }

/** Date-based text for the Christmas landing screen. All functions are pure. */
object ChristmasCalendar {
    private const val EPIPHANY_DAY = 6
    private val adventDateFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    /**
     * Today's date on the device's calendar. The clock is read on every call, so it follows the
     * time zone the phone is in right now. Tests pass a fixed clock to try other zones.
     */
    fun today(clock: Clock = Clock.systemDefaultZone()): LocalDate = LocalDate.now(clock)

    private fun christmas(year: Int): LocalDate = LocalDate.of(year, Month.DECEMBER, 25)

    /** Christmastide runs from Christmas Day to the day before Epiphany (6 January). */
    fun isChristmastide(date: LocalDate): Boolean =
        (date.month == Month.DECEMBER && date.dayOfMonth > 25) ||
            (date.month == Month.JANUARY && date.dayOfMonth < EPIPHANY_DAY)

    /** "32 days to Christmas", "Christmas is tomorrow", "Merry Christmas!" or "Christmastide". */
    fun countdownLabel(today: LocalDate): String {
        if (isChristmastide(today)) return "Christmastide"
        val days = ChronoUnit.DAYS.between(today, christmas(today.year))
        return when {
            days > 1 -> "$days days to Christmas"
            days == 1L -> "Christmas is tomorrow"
            else -> "Merry Christmas!"
        }
    }

    /** First Sunday of Advent: four Sundays before Christmas Day. */
    fun adventStart(year: Int): LocalDate {
        val fourthSunday = christmas(year).minusDays(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return fourthSunday.minusWeeks(3)
    }

    /** "Advent begins Sun 29 Nov", "Advent season" or "Christmastide"; empty outside the season. */
    fun adventLabel(today: LocalDate): String {
        if (isChristmastide(today) || today == christmas(today.year)) return "Christmastide"
        val start = adventStart(today.year)
        return when {
            today.isBefore(start) && today.month >= Month.OCTOBER -> "Advent begins ${start.format(adventDateFormat)}"
            today.isBefore(start) -> ""
            else -> "Advent season"
        }
    }

    fun moment(date: LocalDate): ChristmasMoment = when {
        date.month == Month.DECEMBER && date.dayOfMonth == 24 -> ChristmasMoment.CHRISTMAS_EVE
        date.month == Month.DECEMBER && date.dayOfMonth == 25 -> ChristmasMoment.CHRISTMAS_DAY
        date.month == Month.JANUARY && date.dayOfMonth == 1 -> ChristmasMoment.NEW_YEAR
        date.month == Month.JANUARY && date.dayOfMonth == EPIPHANY_DAY -> ChristmasMoment.EPIPHANY
        else -> ChristmasMoment.NONE
    }

    /** Text for the chip at the top of the landing screen. */
    fun chipLabel(date: LocalDate): String = when (moment(date)) {
        ChristmasMoment.CHRISTMAS_EVE -> "Christmas Eve · Santa is on his way"
        ChristmasMoment.CHRISTMAS_DAY -> "Merry Christmas!"
        ChristmasMoment.NEW_YEAR -> "Happy New Year"
        ChristmasMoment.EPIPHANY -> "Epiphany · the wise men arrive"
        ChristmasMoment.NONE -> countdownLabel(date)
    }
}
