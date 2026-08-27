package com.reyzie.hymns.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

/**
 * Updates one hymn object inside a JSON array without dropping unknown fields.
 *
 * Round-tripping through [Hymn] strips Mangalore-only keys such as `audio`,
 * `author`, `mt`, `type`, and `displayNumber`. Admin lyric save then pushed
 * that truncated file to the vault.
 */
object ContentJsonPatch {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun updateHymnInArray(json: String, updated: Hymn): String? {
        val element = try {
            JsonParser.parseString(json)
        } catch (_: Exception) {
            return null
        }
        if (!element.isJsonArray) return null

        val array = element.asJsonArray
        val target = array.firstOrNull { item ->
            if (!item.isJsonObject) return@firstOrNull false
            val number = item.asJsonObject.get("number") ?: return@firstOrNull false
            number.isJsonPrimitive && number.asJsonPrimitive.isNumber && number.asInt == updated.number
        }?.asJsonObject ?: return null

        target.addProperty("title", updated.title)
        target.addProperty("signature", updated.signature)
        target.addProperty("lyrics", updated.lyrics)
        val kannada = updated.kannadaLyrics
        if (kannada.isNullOrBlank()) {
            target.remove("kannadaLyrics")
        } else {
            target.addProperty("kannadaLyrics", kannada)
        }
        return gson.toJson(array)
    }
}
