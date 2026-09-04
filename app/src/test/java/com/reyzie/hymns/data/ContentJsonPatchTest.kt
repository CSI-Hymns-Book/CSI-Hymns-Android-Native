package com.reyzie.hymns.data

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentJsonPatchTest {

    private val mtJson = """
        [
          {
            "number": 1,
            "displayNumber": 1,
            "type": "Kannada",
            "title": "Yehova! Yehova",
            "lyrics": "old lyrics",
            "kannadaLyrics": "old kn",
            "mt": "171",
            "signature": "171",
            "author": "Th. Walz",
            "category": "Adoration",
            "audio": [
              { "option": 1, "mt": "171", "file": "mt171.mid", "available": true }
            ]
          },
          {
            "number": 2,
            "title": "Other",
            "lyrics": "keep me",
            "signature": "20",
            "author": "C. Campbell",
            "audio": [{ "file": "mt20.mid" }]
          }
        ]
    """.trimIndent()

    @Test
    fun lyricEditPreservesMangaloreFieldsAndSiblingHymns() {
        val updated = Hymn(
            number = 1,
            title = "Yehova! Yehova",
            signature = "171",
            lyrics = "new lyrics",
            kannadaLyrics = "new kn"
        )

        val patched = ContentJsonPatch.updateHymnInArray(mtJson, updated)
        assertNotNull(patched)

        val array = JsonParser.parseString(patched!!).asJsonArray
        val first = array[0].asJsonObject
        val second = array[1].asJsonObject

        assertEquals("new lyrics", first.get("lyrics").asString)
        assertEquals("new kn", first.get("kannadaLyrics").asString)
        assertEquals(1, first.get("displayNumber").asInt)
        assertEquals("Kannada", first.get("type").asString)
        assertEquals("171", first.get("mt").asString)
        assertEquals("Th. Walz", first.get("author").asString)
        assertEquals("mt171.mid", first.getAsJsonArray("audio")[0].asJsonObject.get("file").asString)

        assertEquals("keep me", second.get("lyrics").asString)
        assertEquals("C. Campbell", second.get("author").asString)
        assertTrue(second.has("audio"))
    }

    @Test
    fun missingNumberReturnsNull() {
        val updated = Hymn(number = 99, title = "X", signature = "C.M.", lyrics = "y")
        assertNull(ContentJsonPatch.updateHymnInArray(mtJson, updated))
    }

    @Test
    fun invalidJsonReturnsNull() {
        val updated = Hymn(number = 1, title = "X", signature = "C.M.", lyrics = "y")
        assertNull(ContentJsonPatch.updateHymnInArray("<html>not json</html>", updated))
        assertNull(ContentJsonPatch.updateHymnInArray("{\"regular\":[]}", updated))
    }

    @Test
    fun blankKannadaRemovesField() {
        val updated = Hymn(
            number = 1,
            title = "Yehova! Yehova",
            signature = "171",
            lyrics = "new lyrics",
            kannadaLyrics = null
        )
        val patched = ContentJsonPatch.updateHymnInArray(mtJson, updated)!!
        val first = JsonParser.parseString(patched).asJsonArray[0].asJsonObject
        assertFalse(first.has("kannadaLyrics"))
        assertTrue(first.has("audio"))
    }
}
