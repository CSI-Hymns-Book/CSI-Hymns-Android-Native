package com.reyzie.hymns.carols.domain

import com.reyzie.hymns.carols.data.model.CarolSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarolSearchTest {
    private fun song(
        id: String,
        title: String,
        number: String? = null,
        scale: String = "C Major",
        kannada: String = "ಜೈ ಜೈ",
        english: String? = null,
    ) = CarolSong(
        id = id, churchId = "church", title = title, songNumber = number,
        lyrics = buildBilingualLyrics(kannada, english), scale = scale,
        createdByUserId = "u", createdAt = "2026-11-24T00:00:00Z",
    )

    private val mangala = song("1", "ಮಂಗಳ ಶ್ರೀರಾತ್ರಿಯಲ್ಲಿ", "1", "G Major", "ಮಂಗಳ ಶ್ರೀರಾತ್ರಿಯಲ್ಲಿ\nಬೇತ್ಲೆಹೇಮ್ ಛತ್ರದಿ", "Blessed holy night in Bethlehem")
    private val yesu = song("2", "ಯೇಸು ಜನನ", "12", "D Major", "ಯೇಸು ಹುಟ್ಟಿದನು", "Joy to the world, the Lord is come")
    private val english = song("3", "Hosanna", "30", "A Minor", "ಹೊಸನ್ನ ಹೊಸನ್ನ")
    private val all = listOf(mangala, yesu, english)

    private fun ids(query: String) = CarolSearch.filterSongs(all, query).map { it.id }

    @Test
    fun `transliterates Kannada to a roman skeleton`() {
        assertEquals("yesu", CarolSearch.skeleton("ಯೇಸು"))
        assertEquals("sri", CarolSearch.skeleton("ಶ್ರೀ"))
        assertEquals("mangala", CarolSearch.skeleton("ಮಂಗಳ"))
        assertEquals("sambrama", CarolSearch.skeleton("ಸಂಭ್ರಮ")) // bh folds to b
        assertEquals("12", CarolSearch.skeleton("೧೨"))
    }

    @Test
    fun `skeleton drops spaces accents doubling and aspiration`() {
        assertEquals("yesu", CarolSearch.skeleton("Yeeesu"))
        assertEquals("yesu", CarolSearch.skeleton("Yeshu"))
        assertEquals("yesu", CarolSearch.skeleton("  Y-é-s-u!  "))
        assertEquals("hosana", CarolSearch.skeleton("Hosanna"))
    }

    @Test
    fun `an English title finds the Kannada song`() {
        assertEquals(listOf("2"), ids("yesu"))
        assertEquals(listOf("2"), ids("Yeshu"))
        assertEquals(listOf("1"), ids("mangala shri"))
    }

    @Test
    fun `a Kannada query finds English text`() {
        assertEquals(listOf("3"), ids("ಹೊಸನ್ನ")) // the Kannada lyrics match directly
        val withEnglishTitle = song("4", "Yesu Janiside", kannada = "x")
        assertEquals(listOf("4"), CarolSearch.filterSongs(listOf(withEnglishTitle, english), "ಯೇಸು").map { it.id })
    }

    @Test
    fun `finds by song number and by scale`() {
        assertEquals(listOf("2"), ids("12"))
        assertEquals(listOf("1"), ids("g major"))
        assertEquals(listOf("3"), ids("a minor"))
    }

    @Test
    fun `finds text inside the English translation`() {
        assertEquals(listOf("2"), ids("joy to the world"))
        assertEquals(listOf("1"), ids("bethlehem"))
    }

    @Test
    fun `finds text inside the Kannada lyrics`() {
        assertEquals(listOf("1"), ids("ಬೇತ್ಲೆಹೇಮ್"))
        assertEquals(listOf("1"), ids("bethlehem"))
    }

    @Test
    fun `a blank query returns every song unchanged`() {
        assertEquals(all, CarolSearch.filterSongs(all, ""))
        assertEquals(all, CarolSearch.filterSongs(all, "   "))
        assertTrue(CarolSearch.matches(mangala, ""))
    }

    @Test
    fun `an unrelated query matches nothing`() {
        assertEquals(emptyList<String>(), ids("zzzqx"))
        assertFalse(CarolSearch.matches(yesu, "zzzqx"))
    }

    @Test
    fun `a looser consonant match bridges e and i`() {
        val hymn = song("5", "ಕರ್ತನೇ ಕೇಳು", kannada = "x")
        assertTrue(CarolSearch.matches(hymn, "kirtane")) // consonants k-r-t-n are the same
    }

    @Test
    fun `short consonant guesses do not match by chance`() {
        assertFalse(CarolSearch.matches(yesu, "ys")) // two letters is too little to guess from
    }

    @Test
    fun `matchesFields works for a church name or a pdf title`() {
        assertTrue(CarolSearch.matchesFields(listOf("Yesukrupalaya Church, Mysore", null), "yesukrupalaya"))
        assertTrue(CarolSearch.matchesFields(listOf("ಯೇಸುಕೃಪಾಲಯ"), "yesu"))
        assertFalse(CarolSearch.matchesFields(listOf("Wesley Church"), "yesu"))
        assertTrue(CarolSearch.matchesFields(emptyList(), ""))
    }

    @Test
    fun `an edited song is searched by its new text`() {
        val before = song("9", "Old title", kannada = "ಹಳೆಯ")
        assertTrue(CarolSearch.matches(before, "old title"))
        val after = before.copy(title = "New title", updatedAt = "2026-11-25T00:00:00Z")
        assertTrue(CarolSearch.matches(after, "new title"))
        assertFalse(CarolSearch.matches(after, "old title"))
    }
}
