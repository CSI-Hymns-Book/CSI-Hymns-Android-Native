package com.reyzie.hymns.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

/** Safe Gson parsing for bundled / cached content JSON. */
object ContentJsonParser {
    private const val TAG = "ContentJsonParser"
    private val gson = Gson()
    private val hymnListType = object : TypeToken<List<Hymn>>() {}.type
    private val keerthaneListType = object : TypeToken<List<Keerthane>>() {}.type

    fun parseHymns(jsonString: String): List<Hymn>? = parseArray(jsonString, "hymns") {
        gson.fromJson<List<Hymn>>(jsonString, hymnListType)
    }

    fun parseKeerthanes(jsonString: String): List<Keerthane>? = parseArray(jsonString, "keerthanes") {
        gson.fromJson<List<Keerthane>>(jsonString, keerthaneListType)
    }

    private inline fun <T> parseArray(
        jsonString: String,
        label: String,
        parse: () -> List<T>?
    ): List<T>? {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("[")) {
            Log.w(TAG, "Invalid $label JSON: expected array")
            return null
        }
        return try {
            parse()?.takeIf { it.isNotEmpty() }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JsonSyntaxException parsing $label", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing $label", e)
            null
        }
    }
}
