package com.reyzie.hymns.carols.domain

import com.reyzie.hymns.carols.data.model.CarolSong
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Search across carols in English and Kannada. Typing English finds the Kannada songs and the
 * other way round: both sides are reduced to the same rough roman spelling ("ಯೇಸು" and "yesu"
 * both become "yesu"), then compared. Pure functions, no UI, so they are easy to test.
 */
object CarolSearch {
    private const val MIN_CONSONANTS = 3
    private const val MAX_CACHED = 2_000

    /** Everything searchable about one song, prepared once. */
    private class Entry(val plain: List<String>, val keys: List<String>, val consonantKeys: List<String>)

    private val cache = ConcurrentHashMap<String, Entry>()

    /** True when [song] matches [query]; a blank query matches everything. */
    fun matches(song: CarolSong, query: String): Boolean {
        if (query.isBlank()) return true
        return matchEntry(entryFor(song), query)
    }

    /** The songs that match [query], in their original order. */
    fun filterSongs(songs: List<CarolSong>, query: String): List<CarolSong> =
        if (query.isBlank()) songs else songs.filter { matches(it, query) }

    /** True when any of [fields] (a title, a number, a church name...) matches [query]. */
    fun matchesFields(fields: List<String?>, query: String): Boolean {
        if (query.isBlank()) return true
        return matchEntry(entryOf(fields.filterNotNull()), query)
    }

    /**
     * The rough roman spelling of [text]: Kannada letters transliterated, lower case, no accents,
     * no spaces or punctuation, aspirates folded (sh/kh/th...), doubled letters collapsed.
     */
    fun skeleton(text: String): String {
        var s = KannadaTransliterator.transliterate(text).lowercase(Locale.ROOT)
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replace(MARKS, "")
        s = s.replace(NOT_ALPHANUMERIC, "")
        s = s.replace(ASPIRATE, "$1")
        s = s.replace(DOUBLED, "$1")
        return s.replace('w', 'v')
    }

    /** [skeleton] with its vowels removed: a looser key, so "yesu" and "yisu" meet. */
    fun consonantSkeleton(text: String): String = skeleton(text).replace(VOWELS, "")

    private fun matchEntry(entry: Entry, query: String): Boolean {
        val q = query.trim().lowercase(Locale.ROOT)
        if (entry.plain.any { it.contains(q) }) return true
        val key = skeleton(query)
        if (key.isNotEmpty() && entry.keys.any { it.contains(key) }) return true
        val consonants = key.replace(VOWELS, "")
        return consonants.length >= MIN_CONSONANTS && entry.consonantKeys.any { it.contains(consonants) }
    }

    private fun entryFor(song: CarolSong): Entry {
        val id = "${song.id}|${song.updatedAt ?: song.createdAt}|${song.lyrics.hashCode()}|${song.title.hashCode()}|${song.scale}|${song.songNumber}"
        cache[id]?.let { return it }
        val (kannada, english) = splitBilingualLyrics(song.lyrics)
        val entry = entryOf(listOfNotNull(song.title, song.songNumber, song.scale, kannada, english))
        if (cache.size >= MAX_CACHED) cache.clear()
        cache[id] = entry
        return entry
    }

    private fun entryOf(fields: List<String>): Entry {
        val keys = fields.map { skeleton(it) }
        return Entry(
            plain = fields.map { it.lowercase(Locale.ROOT) },
            keys = keys,
            consonantKeys = keys.map { it.replace(VOWELS, "") },
        )
    }

    private val MARKS = Regex("\\p{Mn}+")
    private val NOT_ALPHANUMERIC = Regex("[^a-z0-9]")
    private val ASPIRATE = Regex("([kgcsjtdpb])h")
    private val DOUBLED = Regex("([a-z])\\1+")
    private val VOWELS = Regex("[aeiou]")
}

/** Kannada script to a simple roman spelling. Anything that is not Kannada passes through. */
internal object KannadaTransliterator {
    private val independentVowels = mapOf(
        0x0C85 to "a", 0x0C86 to "aa", 0x0C87 to "i", 0x0C88 to "ii", 0x0C89 to "u", 0x0C8A to "uu",
        0x0C8B to "ru", 0x0C8C to "lu", 0x0C8E to "e", 0x0C8F to "ee", 0x0C90 to "ai",
        0x0C92 to "o", 0x0C93 to "oo", 0x0C94 to "au",
    )
    private val consonants = mapOf(
        0x0C95 to "k", 0x0C96 to "kh", 0x0C97 to "g", 0x0C98 to "gh", 0x0C99 to "ng",
        0x0C9A to "ch", 0x0C9B to "chh", 0x0C9C to "j", 0x0C9D to "jh", 0x0C9E to "ny",
        0x0C9F to "t", 0x0CA0 to "th", 0x0CA1 to "d", 0x0CA2 to "dh", 0x0CA3 to "n",
        0x0CA4 to "t", 0x0CA5 to "th", 0x0CA6 to "d", 0x0CA7 to "dh", 0x0CA8 to "n",
        0x0CAA to "p", 0x0CAB to "ph", 0x0CAC to "b", 0x0CAD to "bh", 0x0CAE to "m",
        0x0CAF to "y", 0x0CB0 to "r", 0x0CB1 to "r", 0x0CB2 to "l", 0x0CB3 to "l",
        0x0CB5 to "v", 0x0CB6 to "sh", 0x0CB7 to "sh", 0x0CB8 to "s", 0x0CB9 to "h", 0x0CDE to "l",
    )
    private val vowelSigns = mapOf(
        0x0CBE to "aa", 0x0CBF to "i", 0x0CC0 to "ii", 0x0CC1 to "u", 0x0CC2 to "uu", 0x0CC3 to "ru", 0x0CC4 to "ruu",
        0x0CC6 to "e", 0x0CC7 to "ee", 0x0CC8 to "ai", 0x0CCA to "o", 0x0CCB to "oo", 0x0CCC to "au",
    )
    private const val ANUSVARA = 0x0C82
    private const val CANDRABINDU = 0x0C81
    private const val VISARGA = 0x0C83
    private const val VIRAMA = 0x0CCD
    private const val DIGIT_ZERO = 0x0CE6
    private const val DIGIT_NINE = 0x0CEF
    private const val ZWNJ = 0x200C
    private const val ZWJ = 0x200D
    private const val FIRST_CONSONANT = 0x0C95
    private const val LAST_NASAL_CLASS_CONSONANT = 0x0CA8

    fun transliterate(text: String): String {
        val out = StringBuilder(text.length * 2)
        var pendingA = false
        val points = text.codePoints().toArray()
        fun flushA() { if (pendingA) { out.append('a'); pendingA = false } }
        for ((index, cp) in points.withIndex()) {
            when {
                cp == ZWNJ || cp == ZWJ -> Unit
                cp in consonants -> { flushA(); out.append(consonants.getValue(cp)); pendingA = true }
                cp in vowelSigns -> { pendingA = false; out.append(vowelSigns.getValue(cp)) }
                cp == VIRAMA -> pendingA = false
                cp == ANUSVARA -> {
                    flushA()
                    val next = points.getOrNull(index + 1) ?: 0
                    out.append(if (next in FIRST_CONSONANT..LAST_NASAL_CLASS_CONSONANT) 'n' else 'm')
                }
                cp == CANDRABINDU -> { flushA(); out.append('m') }
                cp == VISARGA -> { flushA(); out.append('h') }
                cp in independentVowels -> { flushA(); out.append(independentVowels.getValue(cp)) }
                cp in DIGIT_ZERO..DIGIT_NINE -> { flushA(); out.append(('0'.code + (cp - DIGIT_ZERO)).toChar()) }
                else -> { flushA(); out.appendCodePoint(cp) }
            }
        }
        flushA()
        return out.toString()
    }
}
