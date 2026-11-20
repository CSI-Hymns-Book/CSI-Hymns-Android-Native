package com.reyzie.hymns.presentation

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.util.UUID

enum class LyricsLanguageMode {
    KANNADA,
    ENGLISH,
    BILINGUAL
}

data class PresentationSlide(
    val group: String,
    val content: String
)

object FreeShowFormatter {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val END_MARKER_REGEX = Regex(
        """(?i)^\s*(\.\.\.end\.\.\.|\-\-end\-\-|\.\.\.end|--end|\*\*\*|---|___)\s*$"""
    )
    private val NUMBERED_STANZA_REGEX = Regex("""^\s*(\d+)[\.\)]+\s*""")

    /**
     * Cleans raw lyrics by stripping trailing end markers and excessive blank lines.
     */
    fun cleanLyrics(rawLyrics: String): String {
        return rawLyrics
            .replace("\r\n", "\n")
            .lines()
            .filter { line -> !END_MARKER_REGEX.matches(line.trim()) }
            .joinToString("\n")
            .trim()
    }

    /**
     * Parses lyrics into individual paragraphs / stanzas.
     */
    fun splitIntoParagraphs(cleanedLyrics: String): List<String> {
        return cleanedLyrics
            .split(Regex("""\n\s*\n+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Splits a Keerthane or Hymn into presentation slides with appropriate group labels
     * ([Chorus], [Verse 1], [Verse 2], etc.).
     */
    fun parseSlides(
        number: Int,
        title: String,
        isKeerthane: Boolean,
        kannadaLyrics: String?,
        englishLyrics: String,
        mode: LyricsLanguageMode = LyricsLanguageMode.KANNADA,
        churchName: String? = null,
        includeCreditSlide: Boolean = true
    ): List<PresentationSlide> {
        val kanClean = kannadaLyrics?.let { cleanLyrics(it) } ?: ""
        val engClean = cleanLyrics(englishLyrics)

        val kanParagraphs = if (kanClean.isNotEmpty()) splitIntoParagraphs(kanClean) else emptyList()
        val engParagraphs = if (engClean.isNotEmpty()) splitIntoParagraphs(engClean) else emptyList()

        val selectedParagraphs: List<String>
        val secondaryParagraphs: List<String>?

        when (mode) {
            LyricsLanguageMode.KANNADA -> {
                selectedParagraphs = if (kanParagraphs.isNotEmpty()) kanParagraphs else engParagraphs
                secondaryParagraphs = null
            }
            LyricsLanguageMode.ENGLISH -> {
                selectedParagraphs = if (engParagraphs.isNotEmpty()) engParagraphs else kanParagraphs
                secondaryParagraphs = null
            }
            LyricsLanguageMode.BILINGUAL -> {
                selectedParagraphs = if (kanParagraphs.isNotEmpty()) kanParagraphs else engParagraphs
                secondaryParagraphs = if (kanParagraphs.isNotEmpty()) engParagraphs else null
            }
        }

        val slides = mutableListOf<PresentationSlide>()

        if (isKeerthane) {
            // Find where numbered stanzas start
            val firstNumberedIndex = selectedParagraphs.indexOfFirst {
                NUMBERED_STANZA_REGEX.containsMatchIn(it.lineSequence().firstOrNull() ?: "")
            }

            val chorusParagraphs: List<String>
            val verseParagraphs: List<String>

            if (firstNumberedIndex > 0) {
                chorusParagraphs = selectedParagraphs.take(firstNumberedIndex)
                verseParagraphs = selectedParagraphs.drop(firstNumberedIndex)
            } else if (firstNumberedIndex == 0) {
                chorusParagraphs = emptyList()
                verseParagraphs = selectedParagraphs
            } else {
                // No numbered stanzas found; first paragraph is chorus, rest are verses
                if (selectedParagraphs.size > 1) {
                    chorusParagraphs = listOf(selectedParagraphs.first())
                    verseParagraphs = selectedParagraphs.drop(1)
                } else {
                    chorusParagraphs = selectedParagraphs
                    verseParagraphs = emptyList()
                }
            }

            if (chorusParagraphs.isNotEmpty()) {
                val chorusText = chorusParagraphs.joinToString("\n\n")
                val text = if (secondaryParagraphs != null) {
                    val secChorus = secondaryParagraphs.take(firstNumberedIndex.coerceAtLeast(1)).joinToString("\n\n")
                    combineBilingual(chorusText, secChorus)
                } else {
                    chorusText
                }
                slides += PresentationSlide(group = "Chorus", content = text)
            }

            // The secondary (English) list has the same layout as the primary one, so verse `idx`
            // sits after the chorus paragraphs that came before the first numbered stanza.
            val secondaryVerseOffset = chorusParagraphs.size
            verseParagraphs.forEachIndexed { idx, stanza ->
                val match = NUMBERED_STANZA_REGEX.find(stanza.lineSequence().firstOrNull() ?: "")
                val verseNum = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: (idx + 1)
                val secondaryIndex = secondaryVerseOffset + idx
                val text = if (secondaryParagraphs != null && secondaryIndex < secondaryParagraphs.size) {
                    combineBilingual(stanza, secondaryParagraphs[secondaryIndex])
                } else {
                    stanza
                }
                slides += PresentationSlide(group = "Verse $verseNum", content = text)
            }
        } else {
            // Hymn parsing
            selectedParagraphs.forEachIndexed { idx, stanza ->
                val firstLine = stanza.lineSequence().firstOrNull() ?: ""
                val match = NUMBERED_STANZA_REGEX.find(firstLine)
                val isChorus = firstLine.contains("Chorus", ignoreCase = true) ||
                        firstLine.contains("Refrain", ignoreCase = true) ||
                        firstLine.contains("ಪಲ್ಲವಿ", ignoreCase = true)

                val groupName = if (isChorus) {
                    "Chorus"
                } else {
                    val verseNum = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: (idx + 1)
                    "Verse $verseNum"
                }

                val text = if (secondaryParagraphs != null && idx < secondaryParagraphs.size) {
                    combineBilingual(stanza, secondaryParagraphs[idx])
                } else {
                    stanza
                }
                slides += PresentationSlide(group = groupName, content = text)
            }
        }

        return slides
    }

    private fun combineBilingual(primary: String, secondary: String): String {
        return "${primary.trim()}\n\n—\n${secondary.trim()}"
    }

    /**
     * Formats lyrics into FreeShow Quick Lyrics text format (compatible with Ctrl/Cmd + Alt + I).
     */
    fun formatQuickLyricsText(
        number: Int,
        title: String,
        isKeerthane: Boolean,
        slides: List<PresentationSlide>
    ): String {
        val label = if (isKeerthane) "CSI Keerthane $number" else "CSI Hymn $number"
        val sb = StringBuilder()
        sb.appendLine("Title=$label - $title")
        sb.appendLine("Author=CSI Hymns Book App")
        sb.appendLine("Copyright=By CSI Hymns Book App")
        sb.appendLine("Number=$number")
        sb.appendLine()

        for (slide in slides) {
            sb.appendLine("[${slide.group}]")
            sb.appendLine(slide.content.trim())
            sb.appendLine()
        }

        return sb.toString().trimEnd()
    }

    /**
     * Formats slides into native FreeShow .show JSON format.
     */
    fun createShowTuple(
        number: Int,
        title: String,
        isKeerthane: Boolean,
        slides: List<PresentationSlide>,
        includeLogoMedia: Boolean = false,
        customLogoSrc: String? = null
    ): List<Any> {
        val prefix = if (isKeerthane) "Keerthane" else "Hymn"
        val showName = "$prefix $number - $title"
        val showId = UUID.randomUUID().toString().replace("-", "").take(11)
        val layoutId = "default"
        val now = System.currentTimeMillis()

        val slidesMap = mutableMapOf<String, Any>()
        val layoutSlides = mutableListOf<Map<String, String>>()
        val logoSrc = customLogoSrc ?: "app_icon.png"

        slides.forEachIndexed { index, slide ->
            val slideId = "slide_${index}_" + UUID.randomUUID().toString().replace("-", "").take(6)
            val linesList = slide.content.lines().map { line ->
                mapOf(
                    "align" to "text-align: center;",
                    "text" to listOf(
                        mapOf(
                            "value" to line,
                            "style" to "font-size: 60px;"
                        )
                    )
                )
            }

            val itemsList = if (includeLogoMedia) {
                listOf(
                    mapOf(
                        "style" to "top: 60px; left: 80px; width: 1760px; height: 860px;",
                        "align" to "",
                        "lines" to linesList
                    ),
                    mapOf(
                        "style" to "top: 930px; left: 1195px; width: 65px; height: 70px; white-space: nowrap; overflow: visible;",
                        "align" to "",
                        "lines" to listOf(
                            mapOf(
                                "align" to "text-align: right; white-space: nowrap;",
                                "text" to listOf(
                                    mapOf(
                                        "value" to "By",
                                        "style" to "font-size: 34px; color: rgb(255 255 255 / 0.65); white-space: nowrap;"
                                    )
                                )
                            )
                        ),
                        "decoration" to true
                    ),
                    mapOf(
                        "type" to "media",
                        "style" to "top: 943px; left: 1272px; width: 44px; height: 44px; border-radius: 10px;",
                        "src" to logoSrc,
                        "fit" to "contain",
                        "decoration" to true
                    ),
                    mapOf(
                        "style" to "top: 930px; left: 1330px; width: 520px; height: 70px; white-space: nowrap; overflow: visible;",
                        "align" to "",
                        "lines" to listOf(
                            mapOf(
                                "align" to "text-align: left; white-space: nowrap;",
                                "text" to listOf(
                                    mapOf(
                                        "value" to "CSI Hymns Book App",
                                        "style" to "font-size: 34px; color: rgb(255 255 255 / 0.65); white-space: nowrap;"
                                    )
                                )
                            )
                        ),
                        "decoration" to true
                    )
                )
            } else {
                listOf(
                    mapOf(
                        "style" to "top: 60px; left: 80px; width: 1760px; height: 860px;",
                        "align" to "",
                        "lines" to linesList
                    ),
                    mapOf(
                        "style" to "top: 930px; left: 80px; width: 1760px; height: 70px; white-space: nowrap; overflow: visible;",
                        "align" to "",
                        "lines" to listOf(
                            mapOf(
                                "align" to "text-align: right; white-space: nowrap;",
                                "text" to listOf(
                                    mapOf(
                                        "value" to "By CSI Hymns Book App",
                                        "style" to "font-size: 34px; color: rgb(255 255 255 / 0.65); white-space: nowrap;"
                                    )
                                )
                            )
                        ),
                        "decoration" to true
                    )
                )
            }

            val slideObj = mapOf(
                "group" to slide.group,
                "color" to if (slide.group.startsWith("Chorus", ignoreCase = true)) "#e0a300" else null,
                "settings" to emptyMap<String, Any>(),
                "notes" to "",
                "items" to itemsList
            )

            slidesMap[slideId] = slideObj
            layoutSlides.add(mapOf("id" to slideId))
        }

        val showData = mapOf(
            "name" to showName,
            "category" to if (isKeerthane) "keerthanes" else "hymns",
            "settings" to mapOf(
                "activeLayout" to layoutId,
                "template" to null
            ),
            "timestamps" to mapOf(
                "created" to now,
                "modified" to now,
                "used" to null
            ),
            "quickAccess" to emptyMap<String, Any>(),
            "meta" to mapOf(
                "title" to title,
                "author" to "CSI Hymns Book App",
                "copyright" to "By CSI Hymns Book App",
                "number" to number.toString()
            ),
            "slides" to slidesMap,
            "layouts" to mapOf(
                layoutId to mapOf(
                    "name" to "Default",
                    "notes" to "",
                    "slides" to layoutSlides
                )
            ),
            "media" to emptyMap<String, Any>()
        )

        return listOf(showId, showData)
    }

    /**
     * Converts the app icon drawable into a Base64 data URI (PNG).
     * This allows FreeShow to render the icon natively without requiring a bundled image file.
     */
    fun getAppIconDataUri(context: Context): String {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                com.reyzie.hymns.R.drawable.playstore_icon
            ) ?: return "app_icon.png"
            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 96, 96, true)
            val baos = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
            val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (_: Throwable) {
            "app_icon.png"
        }
    }

    /**
     * Formats show data specifically for direct over-the-air injection (Socket.io) into FreeShow.
     * Uses the Base64 data URI for the logo so FreeShow on any host renders the app logo instantly.
     */
    fun formatShowDataForBeam(
        context: Context,
        number: Int,
        title: String,
        isKeerthane: Boolean,
        slides: List<PresentationSlide>
    ): Pair<String, Map<String, Any>> {
        val logoUri = getAppIconDataUri(context)
        val tuple = createShowTuple(
            number = number,
            title = title,
            isKeerthane = isKeerthane,
            slides = slides,
            includeLogoMedia = true,
            customLogoSrc = logoUri
        )
        @Suppress("UNCHECKED_CAST")
        return Pair(tuple[0] as String, tuple[1] as Map<String, Any>)
    }

    fun formatFreeShowJson(
        number: Int,
        title: String,
        isKeerthane: Boolean,
        slides: List<PresentationSlide>,
        includeLogoMedia: Boolean = false
    ): String {
        return gson.toJson(createShowTuple(number, title, isKeerthane, slides, includeLogoMedia))
    }

    fun formatFreeShowProjectData(
        number: Int,
        title: String,
        isKeerthane: Boolean,
        slides: List<PresentationSlide>
    ): Map<String, Any> {
        val tuple = createShowTuple(number, title, isKeerthane, slides, includeLogoMedia = true)
        val showId = tuple[0] as String
        val showData = tuple[1]

        val prefix = if (isKeerthane) "Keerthane" else "Hymn"
        val projectName = "$prefix $number - $title"
        val now = System.currentTimeMillis()

        return mapOf(
            "project" to mapOf(
                "name" to projectName,
                "created" to now,
                "shows" to listOf(mapOf("id" to showId))
            ),
            "parentFolder" to "",
            "shows" to mapOf(showId to showData),
            "files" to listOf("app_icon.png")
        )
    }

    /**
     * Generates a .project ZIP file in context.cacheDir and returns the File.
     */
    fun createProjectFile(
        context: Context,
        number: Int,
        title: String,
        isKeerthane: Boolean,
        slides: List<PresentationSlide>
    ): File {
        val projectData = formatFreeShowProjectData(number, title, isKeerthane, slides)
        val json = gson.toJson(projectData)
        val dir = File(context.cacheDir, "projects").apply { mkdirs() }
        val prefix = if (isKeerthane) "Keerthane" else "Hymn"
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
        val file = File(dir, "${prefix}_${number}_$safeTitle.project")

        java.util.zip.ZipOutputStream(file.outputStream().buffered()).use { zos ->
            // data.json
            zos.putNextEntry(java.util.zip.ZipEntry("data.json"))
            zos.write(json.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // project.json
            zos.putNextEntry(java.util.zip.ZipEntry("project.json"))
            zos.write(json.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // app_icon.png from drawable if available
            try {
                val iconStream = context.resources.openRawResource(
                    com.reyzie.hymns.R.drawable.playstore_icon
                )
                zos.putNextEntry(java.util.zip.ZipEntry("app_icon.png"))
                iconStream.copyTo(zos)
                zos.closeEntry()
                iconStream.close()
            } catch (_: Exception) {
            }
        }
        return file
    }

    /**
     * Generates a .show file in context.cacheDir and returns the File.
     */
    fun createShowFile(
        context: Context,
        number: Int,
        title: String,
        isKeerthane: Boolean,
        slides: List<PresentationSlide>
    ): File {
        val json = formatFreeShowJson(number, title, isKeerthane, slides)
        val dir = File(context.cacheDir, "shows").apply { mkdirs() }
        val prefix = if (isKeerthane) "Keerthane" else "Hymn"
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(30)
        val file = File(dir, "${prefix}_${number}_$safeTitle.show")
        file.writeText(json, Charsets.UTF_8)
        return file
    }

    /**
     * Creates an Intent to share the .show file via Android Sharesheet.
     */
    fun createShareShowIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to share formatted slide text.
     */
    fun createShareTextIntent(title: String, text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }
}

