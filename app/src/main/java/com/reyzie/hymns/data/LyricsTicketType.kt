package com.reyzie.hymns.data

/**
 * The book name written into a lyrics ticket summary ("<type> <id> Lyrics Issue").
 * The lyrics-triage automation (automation/n8n) parses these exact values to pick the vault file,
 * so changing one here requires the same change there.
 */
object LyricsTicketType {
    const val HYMN = "Hymn"
    const val KEERTHANE = "Keerthane"
    const val MT_HYMN = "M.T. Hymn"
    const val ORDER_REGULAR = "Order of Service Regular"
    const val ORDER_FESTIVAL = "Order of Service Festival"

    fun forSong(isKeerthane: Boolean, isMt: Boolean): String = when {
        isKeerthane -> KEERTHANE
        isMt -> MT_HYMN
        else -> HYMN
    }

    fun forOrderOfService(type: String): String =
        if (type == "festival") ORDER_FESTIVAL else ORDER_REGULAR

    fun orderOfServiceLabel(type: String, pageNo: Int): String {
        val name = if (type == "festival") "Festival" else "Regular"
        return "Order of Service ($name) page $pageNo"
    }
}
