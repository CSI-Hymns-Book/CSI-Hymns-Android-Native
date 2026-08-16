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
}
