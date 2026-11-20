package com.reyzie.hymns.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTicketTypeTest {

    @Test
    fun `hymn screen reports plain hymns as Hymn`() {
        assertEquals("Hymn", LyricsTicketType.forSong(isKeerthane = false, isMt = false))
    }

    @Test
    fun `hymn screen reports keerthanes as Keerthane`() {
        assertEquals("Keerthane", LyricsTicketType.forSong(isKeerthane = true, isMt = false))
    }

    @Test
    fun `hymn screen reports Mangalore tunes as M-T Hymn so they are not checked against CSI hymns`() {
        assertEquals("M.T. Hymn", LyricsTicketType.forSong(isKeerthane = false, isMt = true))
    }

    @Test
    fun `keerthane wins when both flags are set`() {
        assertEquals("Keerthane", LyricsTicketType.forSong(isKeerthane = true, isMt = true))
    }

    @Test
    fun `order of service types map to the ticket types the automation expects`() {
        assertEquals("Order of Service Regular", LyricsTicketType.forOrderOfService("regular"))
        assertEquals("Order of Service Festival", LyricsTicketType.forOrderOfService("festival"))
    }

    @Test
    fun `unknown order of service type falls back to regular`() {
        assertEquals("Order of Service Regular", LyricsTicketType.forOrderOfService("something"))
    }

    @Test
    fun `order of service label names the page`() {
        assertEquals(
            "Order of Service (Festival) page 1",
            LyricsTicketType.orderOfServiceLabel("festival", 1)
        )
    }

    @Test
    fun `ticket summary format matches what the n8n workflow parses`() {
        val summary = "${LyricsTicketType.forOrderOfService("regular")} 183 Lyrics Issue"
        assertEquals("Order of Service Regular 183 Lyrics Issue", summary)
    }
}
