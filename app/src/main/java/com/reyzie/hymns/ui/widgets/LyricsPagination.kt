package com.reyzie.hymns.ui.widgets

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val LARGE_STANZA_MIN_LINES = 5
private const val LARGE_STANZA_MIN_CHARS = 220

data class LyricsPageLayout(
    val pages: List<String>,
    val stanzasPerPage: Int
)

/**
 * Builds flip-book pages with dynamic density:
 * - Hymns: 2 stanzas/page when paragraphs are large, 3 when small.
 * - Keerthane: chorus/refrain alone on page 1, then numbered stanzas with the same 2/3 rule.
 */
fun buildLyricsPages(
    lyrics: String,
    isKeerthane: Boolean,
    fontSize: TextUnit = 18.sp
): LyricsPageLayout {
    val paragraphs = splitParagraphs(lyrics)
    if (paragraphs.isEmpty()) {
        val trimmed = lyrics.trim()
        return LyricsPageLayout(
            pages = if (trimmed.isEmpty()) emptyList() else listOf(trimmed),
            stanzasPerPage = 2
        )
    }

    val pages = mutableListOf<String>()

    if (isKeerthane) {
        val (chorus, verses) = splitKeerthaneChorus(paragraphs)
        if (!chorus.isNullOrBlank()) {
            pages += chorus.trim()
        }
        if (verses.isNotEmpty()) {
            val perPage = recommendedStanzasPerPage(verses, fontSize)
            pages += chunkStanzas(verses, perPage, fontSize)
        }
    } else {
        val perPage = recommendedStanzasPerPage(paragraphs, fontSize)
        pages += chunkStanzas(paragraphs, perPage, fontSize)
    }

    return LyricsPageLayout(
        pages = pages.ifEmpty { listOf(lyrics.trim()) },
        stanzasPerPage = recommendedStanzasPerPage(paragraphs, fontSize)
    )
}

/** @deprecated Use [buildLyricsPages] */
fun splitLyricsIntoPages(lyrics: String, stanzasPerPage: Int): List<String> {
    return buildLyricsPages(lyrics, isKeerthane = stanzasPerPage >= 3, fontSize = 18.sp).pages
}

private fun splitParagraphs(lyrics: String): List<String> {
    return lyrics
        .split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun splitKeerthaneChorus(paragraphs: List<String>): Pair<String?, List<String>> {
    val firstNumberedIndex = paragraphs.indexOfFirst { isNumberedStanza(it) }
    return when {
        firstNumberedIndex <= 0 -> null to paragraphs
        else -> {
            val chorus = paragraphs.take(firstNumberedIndex).joinToString("\n\n")
            val verses = paragraphs.drop(firstNumberedIndex)
            chorus to verses
        }
    }
}

private fun isNumberedStanza(text: String): Boolean {
    val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
    return firstLine.trim().matches(Regex("""^\d+\.?\s*\S"""))
}

private fun isLargeStanza(stanza: String, fontSize: TextUnit): Boolean {
    val lines = stanza.lines().count { it.isNotBlank() }
    val chars = stanza.length
    val fontScale = (fontSize.value / 18f).coerceIn(0.85f, 1.35f)
    val lineThreshold = (LARGE_STANZA_MIN_LINES * fontScale).toInt().coerceAtLeast(4)
    val charThreshold = (LARGE_STANZA_MIN_CHARS * fontScale).toInt()
    return lines >= lineThreshold || chars >= charThreshold
}

private fun recommendedStanzasPerPage(stanzas: List<String>, fontSize: TextUnit): Int {
    if (stanzas.isEmpty()) return 1
    val large = stanzas.count { isLargeStanza(it, fontSize) }
    val largeRatio = large.toFloat() / stanzas.size
    return when {
        fontSize.value >= 20f -> 1
        largeRatio >= 0.25f || stanzas.any { it.length >= 280 } -> 1
        largeRatio >= 0.35f || stanzas.any { it.length >= 340 } -> 2
        else -> 3
    }
}

private fun chunkStanzas(
    stanzas: List<String>,
    stanzasPerPage: Int,
    fontSize: TextUnit
): List<String> {
    val chunk = stanzasPerPage.coerceAtLeast(1)
    val result = mutableListOf<String>()
    var index = 0
    while (index < stanzas.size) {
        val adaptive = when {
            index + 1 >= stanzas.size -> 1
            isLargeStanza(stanzas[index], fontSize) && isLargeStanza(stanzas[index + 1], fontSize) -> 1
            isLargeStanza(stanzas[index], fontSize) -> 1
            else -> chunk
        }
        val end = (index + adaptive).coerceAtMost(stanzas.size)
        result += stanzas.subList(index, end).joinToString("\n\n")
        index = end
    }
    return result
}
