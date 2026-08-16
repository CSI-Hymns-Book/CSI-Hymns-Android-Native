package com.reyzie.hymns.data

import org.json.JSONObject

/** Safe parsing for order-of-service JSON (regular / festival page arrays). */
object OrderOfServiceJson {
    fun parsePages(jsonStr: String): List<OrderPage>? {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
            return null
        }
        return try {
            val parsedPages = mutableListOf<OrderPage>()
            val jsonObject = JSONObject(trimmed)
            var recognizedGroup = false
            listOf("regular", "festival").forEach { groupType ->
                if (!jsonObject.has(groupType)) return@forEach
                recognizedGroup = true
                val arr = jsonObject.getJSONArray(groupType)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val pageNo = if (item.has("page_no")) item.getInt("page_no") else item.getInt("pageNo")
                    val title = if (item.has("title") && !item.isNull("title")) item.getString("title") else null
                    val content = item.getString("content")
                    parsedPages.add(OrderPage(pageNo, title, content, groupType))
                }
            }
            if (!recognizedGroup) null else parsedPages
        } catch (_: Exception) {
            null
        }
    }

    fun isValid(jsonStr: String): Boolean = parsePages(jsonStr) != null
}
