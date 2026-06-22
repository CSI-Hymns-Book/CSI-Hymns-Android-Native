package com.reyzie.hymns.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tolerant parser for christmas carols from Supabase, GitHub JSON, and legacy Flutter formats.
 * Handles snake_case / camelCase keys and boolean fields stored as 0/1.
 */
object ChristmasCarolParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseJsonText(text: String): List<ChristmasCarol> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            when {
                trimmed.startsWith("[") -> json.parseToJsonElement(trimmed).jsonArray.mapNotNull { parseElement(it) }
                trimmed.startsWith("{") -> listOfNotNull(parseElement(json.parseToJsonElement(trimmed)))
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseRows(rows: List<JsonObject>): List<ChristmasCarol> =
        rows.mapNotNull { parseObject(it) }

    private fun parseElement(element: JsonElement): ChristmasCarol? =
        element.jsonObject.let { parseObject(it) }

    private fun parseObject(obj: JsonObject): ChristmasCarol? {
        val id = stringField(obj, "id") ?: return null
        val title = stringField(obj, "title") ?: return null
        val churchName = stringField(obj, "church_name", "churchName", "church") ?: return null

        val pdf = stringField(obj, "pdf", "pdf_path", "pdfPath", "pdf_url", "pdfUrl")
        val lyrics = stringField(obj, "lyrics", "lyric", "content")
        val pdfPages = parsePdfPages(obj["pdf_pages"] ?: obj["pdfPages"])

        return ChristmasCarol(
            id = id,
            title = title,
            songNumber = stringField(obj, "song_number", "songNumber", "number"),
            churchName = churchName,
            lyrics = lyrics,
            pdfPath = pdf,
            pdfPages = pdfPages,
            transpose = intField(obj, "transpose") ?: 0,
            scale = stringField(obj, "scale") ?: "C Major",
            hasChords = boolField(obj, "has_chords", "hasChords") ?: (pdf.isNullOrBlank() && !lyrics.isNullOrBlank()),
            createdByUserId = stringField(obj, "created_by_user_id", "createdByUserId", "user_id", "userId") ?: "",
            createdAt = stringField(obj, "created_at", "createdAt") ?: "",
            updatedAt = stringField(obj, "updated_at", "updatedAt"),
        )
    }

    private fun parsePdfPages(element: JsonElement?): List<String>? {
        if (element == null) return null
        return try {
            when (element) {
                is JsonArray -> element.mapNotNull { it.jsonPrimitive.contentOrNull }
                is JsonPrimitive -> element.contentOrNull?.let { listOf(it) }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun stringField(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val el = obj[key] ?: continue
            val raw = when (el) {
                is JsonPrimitive -> el.contentOrNull
                else -> null
            } ?: continue
            if (raw.isNotBlank()) return raw.trim()
        }
        return null
    }

    private fun intField(obj: JsonObject, vararg keys: String): Int? {
        for (key in keys) {
            val raw = obj[key]?.jsonPrimitive?.contentOrNull ?: continue
            raw.toIntOrNull()?.let { return it }
        }
        return null
    }

    private fun boolField(obj: JsonObject, vararg keys: String): Boolean? {
        for (key in keys) {
            val raw = obj[key]?.jsonPrimitive?.contentOrNull ?: continue
            when (raw.lowercase()) {
                "true", "1", "yes" -> return true
                "false", "0", "no" -> return false
            }
        }
        return null
    }
}
