package com.reyzie.hymns.carols.domain

private const val BILINGUAL_SEPARATOR = "\n\n---\n\nEnglish Translation:\n"

fun buildBilingualLyrics(kannada: String, english: String?): String {
    val kn = kannada.trim()
    val en = english?.trim().orEmpty()
    return if (en.isEmpty()) kn else "$kn$BILINGUAL_SEPARATOR$en"
}

fun splitBilingualLyrics(lyrics: String): Pair<String, String?> {
    val idx = lyrics.indexOf(BILINGUAL_SEPARATOR)
    return if (idx < 0) {
        lyrics to null
    } else {
        lyrics.substring(0, idx).trim() to lyrics.substring(idx + BILINGUAL_SEPARATOR.length).trim()
    }
}
