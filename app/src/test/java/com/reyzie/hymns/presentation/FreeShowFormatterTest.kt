package com.reyzie.hymns.presentation

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeShowFormatterTest {

    private val sampleKannadaKeerthane = """
        ಆಚರಿಸು ವಿಶ್ರಾಂತಿದಿನವನ್ನು - ಬಹು ಭಕ್ತಿಭಾವದಿಂ
        ದಾಚಾರಿಸು ವಿಶ್ರಾಂತಿದಿನವನ್ನು
        	
        1. ಕಾರುಣ್ಯ ಮೂರ್ತಿಯು ಧಾರಿಣಿಯನೆಲ್ಲಾ
        ಆರೇ ದಿನಗಲ್ಲಿ ಪೂರಯಿಸಿ
        ಮಾರಣೆ ದಿನದಲ್ಲಿ ತೀರಿಕೆಯಾಗಿಯೇ
        ಸಾರಿಪೇಳಿದ ಮಾತ ಮೀರದೆ ನಿತ್ಯವೂ
        	
        2.. ಗುರುರಾಯನೀ ಹೊತ್ತು ಮರಣದ ಕೊಂಡಿಯಂ
        ಮುರಿದುಬಿಟ್ಟನು ಬಹು ಕರುಣೆಯಿಂದ
        ಪರಿಶುದ್ಧ ಆತ್ಮನು ಭಕ್ತರ ಮೇಲೈದಿ
        ವರಗಳ ಪಾಲಿಸಿ ಹರಸಿದ ಸುದಿನವ
        
        --END--
    """.trimIndent()

    private val sampleEnglishKeerthane = """
        Aacharisu Vishraantidinavannu - Bahu Bhaktibhaavadim
        Daacharisu Vishraantidinavannu
        	
        1. Kaarunya moortiyu Dhaariniyanellaa
        Aare Dinagalli Poorayisi
        Maarane Dinadalli Teerikeyaagiye
        Saaripelida Maata Meerade Nityavu
        	
        2.. Gururaayanee Hottu Maranada Kondiyam
        Muridubittanu Bahu Karuneyinda
        Parishudda Aatmanu Bhaktara Melaidi
        Varagala Paalisi Harasida Sudinava.
        
        ...End...
    """.trimIndent()

    @Test
    fun parsesKeerthaneIntoChorusAndVerses() {
        val slides = FreeShowFormatter.parseSlides(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            kannadaLyrics = sampleKannadaKeerthane,
            englishLyrics = sampleEnglishKeerthane,
            mode = LyricsLanguageMode.KANNADA
        )

        assertEquals(3, slides.size) // Chorus + Verse 1 + Verse 2 (no ending slide)
        assertEquals("Chorus", slides[0].group)
        assertTrue(slides[0].content.contains("ಆಚರಿಸು"))

        assertEquals("Verse 1", slides[1].group)
        assertTrue(slides[1].content.contains("ಕಾರುಣ್ಯ ಮೂರ್ತಿಯು"))

        assertEquals("Verse 2", slides[2].group)
        assertTrue(slides[2].content.contains("ಗುರುರಾಯನೀ ಹೊತ್ತು"))
    }

    @Test
    fun formatsQuickLyricsCorrectly() {
        val slides = FreeShowFormatter.parseSlides(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            kannadaLyrics = sampleKannadaKeerthane,
            englishLyrics = sampleEnglishKeerthane,
            mode = LyricsLanguageMode.KANNADA
        )

        val quickLyrics = FreeShowFormatter.formatQuickLyricsText(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            slides = slides
        )

        assertTrue(quickLyrics.startsWith("Title=CSI Keerthane 214 - Aacharisu Vishraantidinavannu"))
        assertTrue(quickLyrics.contains("Author=CSI Hymns Book App"))
        assertTrue(quickLyrics.contains("Copyright=By CSI Hymns Book App"))
        assertTrue(quickLyrics.contains("[Chorus]"))
        assertTrue(quickLyrics.contains("[Verse 1]"))
        assertTrue(quickLyrics.contains("[Verse 2]"))
        assertTrue(!quickLyrics.contains("--END--"))
        assertTrue(!quickLyrics.contains("...End..."))
    }

    @Test
    fun formatsFreeShowJsonValidly() {
        val slides = FreeShowFormatter.parseSlides(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            kannadaLyrics = sampleKannadaKeerthane,
            englishLyrics = sampleEnglishKeerthane,
            mode = LyricsLanguageMode.KANNADA
        )

        val json = FreeShowFormatter.formatFreeShowJson(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            slides = slides
        )

        val root = JsonParser.parseString(json).asJsonArray
        assertEquals(2, root.size())
        val showId = root[0].asString
        assertNotNull(showId)

        val showObj = root[1].asJsonObject
        assertEquals("Keerthane 214 - Aacharisu Vishraantidinavannu", showObj.get("name").asString)
        assertTrue(showObj.has("slides"))
        assertTrue(showObj.has("layouts"))

        val slidesObj = showObj.getAsJsonObject("slides")
        assertEquals(slides.size, slidesObj.entrySet().size)

        // Verify watermark item exists on slide
        val firstSlide = slidesObj.entrySet().first().value.asJsonObject
        val items = firstSlide.getAsJsonArray("items")
        assertEquals(2, items.size()) // Main lyrics + Watermark decoration
        assertTrue(items[1].asJsonObject.get("decoration").asBoolean)
        val textValue = items[1].asJsonObject.getAsJsonArray("lines")[0].asJsonObject.getAsJsonArray("text")[0].asJsonObject.get("value").asString
        assertTrue(textValue.contains("By CSI Hymns Book App"))
    }

    @Test
    fun bilingualModeCombinesStanzas() {
        val slides = FreeShowFormatter.parseSlides(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            kannadaLyrics = sampleKannadaKeerthane,
            englishLyrics = sampleEnglishKeerthane,
            mode = LyricsLanguageMode.BILINGUAL
        )

        assertEquals("Chorus", slides[0].group)
        assertTrue(slides[0].content.contains("ಆಚರಿಸು"))
        assertTrue(slides[0].content.contains("Aacharisu"))
    }

    @Test
    fun formatsFreeShowProjectDataValidly() {
        val slides = FreeShowFormatter.parseSlides(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            kannadaLyrics = sampleKannadaKeerthane,
            englishLyrics = sampleEnglishKeerthane,
            mode = LyricsLanguageMode.KANNADA
        )

        val projectData = FreeShowFormatter.formatFreeShowProjectData(
            number = 214,
            title = "Aacharisu Vishraantidinavannu",
            isKeerthane = true,
            slides = slides
        )

        assertTrue(projectData.containsKey("project"))
        assertTrue(projectData.containsKey("shows"))
        assertEquals(listOf("app_icon.png"), projectData["files"])

        val shows = projectData["shows"] as Map<*, *>
        assertEquals(1, shows.size)

        val showObj = shows.values.first() as Map<*, *>
        val slidesMap = showObj["slides"] as Map<*, *>
        val firstSlide = slidesMap.values.first() as Map<*, *>
        val items = firstSlide["items"] as List<*>

        assertEquals(4, items.size) // Main lyrics + "By" + Media logo + "CSI Hymns Book App"
        val mediaItem = items[2] as Map<*, *>
        assertEquals("media", mediaItem["type"])
        assertEquals("app_icon.png", mediaItem["src"])
        assertEquals(true, mediaItem["decoration"])
    }

    @Test
    fun formatsShowTupleWithCustomDataUriLogo() {
        val slides = listOf(PresentationSlide("Chorus", "Slide content"))
        val tuple = FreeShowFormatter.createShowTuple(
            number = 1,
            title = "Test",
            isKeerthane = false,
            slides = slides,
            includeLogoMedia = true,
            customLogoSrc = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg=="
        )

        val showData = tuple[1] as Map<*, *>
        val slidesMap = showData["slides"] as Map<*, *>
        val firstSlide = slidesMap.values.first() as Map<*, *>
        val items = firstSlide["items"] as List<*>

        assertEquals(4, items.size)
        val mediaItem = items[2] as Map<*, *>
        assertEquals("data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==", mediaItem["src"])

        val labelItem = items[3] as Map<*, *>
        val labelStyle = labelItem["style"] as String
        assertTrue(labelStyle.contains("width: 520px;"))
        assertTrue(labelStyle.contains("white-space: nowrap;"))
    }
}

