package com.reyzie.hymns.carols.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/** Keeps the "recently viewed" carols list: newest first, no duplicates, limited length. */
object CarolRecents {
    const val DEFAULT_MAX = 6
    private const val SEPARATOR = ","

    fun push(ids: List<String>, id: String, max: Int = DEFAULT_MAX): List<String> =
        (listOf(id) + ids.filterNot { it == id }).take(max)

    fun encode(ids: List<String>): String = ids.joinToString(SEPARATOR)

    fun decode(stored: String?): List<String> =
        stored.orEmpty().split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
}

/** Lyric text size in the carol reader, as a multiple of the normal size. */
object CarolTextScale {
    const val MIN = 0.8f
    const val MAX = 1.8f
    const val DEFAULT = 1f
    private const val STEP = 0.1f
    private const val PRECISION = 10f

    /** The slider moves in steps of 0.05, which is 19 stops between [MIN] and [MAX]. */
    const val SLIDER_STEP = 0.05f
    const val SLIDER_STOPS = 19

    fun bigger(scale: Float): Float = clamp(round(scale + STEP))

    fun smaller(scale: Float): Float = clamp(round(scale - STEP))

    fun clamp(scale: Float): Float = scale.coerceIn(MIN, MAX)

    /** The slider position rounded to the nearest 0.05 and kept within limits. */
    fun snap(scale: Float): Float = clamp((scale / SLIDER_STEP).roundToInt() * SLIDER_STEP)

    /** "110%" for a scale of 1.1. */
    fun percentLabel(scale: Float): String = "${(clamp(scale) * 100).roundToInt()}%"

    private fun round(value: Float): Float = (value * PRECISION).roundToInt() / PRECISION
}

private const val FALLBACK_INITIAL = "✦"
private const val MAX_INITIALS = 2

/** Up to two capital letters for a church's avatar, e.g. "St. Paul's Church" → "SP". */
fun churchInitials(name: String): String {
    val letters = name.split(" ")
        .mapNotNull { word -> word.firstOrNull { it.isLetter() } }
        .take(MAX_INITIALS)
    return if (letters.isEmpty()) FALLBACK_INITIAL else letters.joinToString("").uppercase()
}

/** A stable colour slot for a church, so its avatar keeps the same colour every time. */
fun churchColorSlot(name: String, slotCount: Int): Int = abs(name.trim().lowercase().hashCode() % slotCount)

/** Splits lyrics into verses at blank lines, for showing one verse at a time. */
fun splitStanzas(lyrics: String): List<String> =
    lyrics.replace("\r\n", "\n")
        .split(Regex("\n\\s*\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
