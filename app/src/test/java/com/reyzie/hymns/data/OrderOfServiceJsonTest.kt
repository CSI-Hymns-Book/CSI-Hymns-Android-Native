package com.reyzie.hymns.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderOfServiceJsonTest {

    @Test
    fun validObjectWithRegularAndFestivalParses() {
        val json = """
            {
              "regular": [
                {"page_no": 2, "title": "Creed", "content": "I believe"},
                {"pageNo": 1, "title": null, "content": "Opening"}
              ],
              "festival": [
                {"page_no": 10, "title": "Christmas", "content": "Joy"}
              ]
            }
        """.trimIndent()

        val pages = OrderOfServiceJson.parsePages(json)
        assertNotNull(pages)
        assertEquals(3, pages!!.size)
        assertEquals(listOf(2, 1, 10), pages.map { it.pageNo })
        assertTrue(OrderOfServiceJson.isValid(json))
    }

    @Test
    fun htmlErrorPageIsRejected() {
        val html = "<!DOCTYPE html><html><body>Rate limited</body></html>"
        assertNull(OrderOfServiceJson.parsePages(html))
        assertFalse(OrderOfServiceJson.isValid(html))
    }

    @Test
    fun truncatedJsonIsRejected() {
        assertNull(OrderOfServiceJson.parsePages("""{"regular": [{"page_no": 1, "content": "unfin"""))
        assertFalse(OrderOfServiceJson.isValid("{"))
    }

    @Test
    fun emptyObjectWithoutGroupsIsRejected() {
        assertNull(OrderOfServiceJson.parsePages("{}"))
        assertFalse(OrderOfServiceJson.isValid("{}"))
    }

    @Test
    fun emptyGroupArraysAreValid() {
        val json = """{"regular": [], "festival": []}"""
        val pages = OrderOfServiceJson.parsePages(json)
        assertNotNull(pages)
        assertTrue(pages!!.isEmpty())
        assertTrue(OrderOfServiceJson.isValid(json))
    }

    @Test
    fun indexEntriesParseAndSkipBlankTitles() {
        val json = """
            {
              "index": [
                {"page_no": 21, "title": "Lord's Supper"},
                {"pageNo": 1, "title": "  Instructions  "},
                {"page_no": 40, "title": "   "},
                {"page_no": 183, "title": "Funeral"}
              ],
              "regular": [
                {"page_no": 1, "title": "Instructions", "content": "A"},
                {"page_no": 21, "title": "Supper", "content": "B"}
              ]
            }
        """.trimIndent()

        val doc = OrderOfServiceJson.parseDocument(json)
        assertNotNull(doc)
        assertEquals(listOf(1, 21, 183), doc!!.index.map { it.pageNo })
        assertEquals("Instructions", doc.index[0].title)
        assertEquals(2, doc.pages.size)
        assertTrue(OrderOfServiceJson.isValid(json))
    }

    @Test
    fun jsonWithoutIndexStillValid() {
        val json = """{"regular": [{"page_no": 1, "title": "A", "content": "B"}]}"""
        val doc = OrderOfServiceJson.parseDocument(json)
        assertNotNull(doc)
        assertTrue(doc!!.index.isEmpty())
        assertEquals(1, doc.pages.size)
    }

    @Test
    fun groupPagesByIndexUsesHeadingRanges() {
        val index = listOf(
            OrderIndexEntry(1, "Instructions"),
            OrderIndexEntry(21, "Supper"),
            OrderIndexEntry(40, "Special"),
            OrderIndexEntry(183, "Funeral")
        )
        val pages = listOf(
            OrderPage(1, "Instructions", "a", "regular"),
            OrderPage(18, null, "b", "regular"),
            OrderPage(21, "Supper", "c", "regular"),
            OrderPage(39, "Supper", "d", "regular"),
            OrderPage(183, "Funeral", "e", "regular"),
            OrderPage(192, null, "f", "regular")
        )
        val sections = groupOrderPagesByIndex(index, pages)
        assertEquals(4, sections.size)
        assertEquals(listOf(1, 18), sections[0].pages.map { it.pageNo })
        assertEquals(listOf(21, 39), sections[1].pages.map { it.pageNo })
        assertTrue(sections[2].pages.isEmpty())
        assertEquals(40, sections[2].startPageNo)
        assertEquals(listOf(183, 192), sections[3].pages.map { it.pageNo })
    }

    @Test
    fun groupPagesWithoutIndexUsesSequentialTitles() {
        val pages = listOf(
            OrderPage(1, "Opening", "a", "festival"),
            OrderPage(2, null, "b", "festival"),
            OrderPage(3, "Blessing", "c", "festival")
        )
        val sections = groupOrderPagesByIndex(emptyList(), pages)
        assertEquals(2, sections.size)
        assertEquals("Opening", sections[0].title)
        assertEquals(listOf(1, 2), sections[0].pages.map { it.pageNo })
        assertEquals("Blessing", sections[1].title)
        assertEquals(listOf(3), sections[1].pages.map { it.pageNo })
    }
}
