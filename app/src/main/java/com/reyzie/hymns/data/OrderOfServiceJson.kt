package com.reyzie.hymns.data

import org.json.JSONObject

data class OrderOfServiceDocument(
    val pages: List<OrderPage>,
    val index: List<OrderIndexEntry>
)

/** Groups content pages under official TOC headings (page-number ranges). */
fun groupOrderPagesByIndex(
    index: List<OrderIndexEntry>,
    pages: List<OrderPage>
): List<OrderPageSection> {
    val sortedPages = pages.sortedBy { it.pageNo }
    if (index.isNotEmpty()) {
        val sortedIndex = index.sortedBy { it.pageNo }
        return sortedIndex.mapIndexed { i, entry ->
            val nextStart = sortedIndex.getOrNull(i + 1)?.pageNo ?: Int.MAX_VALUE
            val sectionPages = sortedPages.filter { it.pageNo >= entry.pageNo && it.pageNo < nextStart }
            OrderPageSection(
                title = entry.title,
                startPageNo = entry.pageNo,
                pages = sectionPages
            )
        }
    }

    val sections = mutableListOf<OrderPageSection>()
    var currentTitle = ""
    val currentPages = mutableListOf<OrderPage>()
    for (page in sortedPages) {
        val title = page.title?.trim().orEmpty()
        if (title.isNotEmpty() && title != currentTitle) {
            if (currentPages.isNotEmpty()) {
                sections += OrderPageSection(currentTitle, currentPages.first().pageNo, currentPages.toList())
            }
            currentTitle = title
            currentPages.clear()
            currentPages += page
        } else {
            currentPages += page
        }
    }
    if (currentPages.isNotEmpty()) {
        sections += OrderPageSection(
            title = currentTitle,
            startPageNo = currentPages.first().pageNo,
            pages = currentPages.toList()
        )
    }
    return sections
}

/** Safe parsing for order-of-service JSON (regular / festival page arrays + optional index). */
object OrderOfServiceJson {
    fun parseDocument(jsonStr: String): OrderOfServiceDocument? {
        val trimmed = jsonStr.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
            return null
        }
        return try {
            val parsedPages = mutableListOf<OrderPage>()
            val parsedIndex = mutableListOf<OrderIndexEntry>()
            val jsonObject = JSONObject(trimmed)
            var recognizedGroup = false
            listOf("regular", "festival").forEach { groupType ->
                if (!jsonObject.has(groupType)) return@forEach
                recognizedGroup = true
                val arr = jsonObject.getJSONArray(groupType)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val pageNo = item.pageNo() ?: continue
                    val title = if (item.has("title") && !item.isNull("title")) item.getString("title") else null
                    val content = item.getString("content")
                    parsedPages.add(OrderPage(pageNo, title, content, groupType))
                }
            }
            if (jsonObject.has("index")) {
                val arr = jsonObject.getJSONArray("index")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val pageNo = item.pageNo() ?: continue
                    val title = if (item.has("title") && !item.isNull("title")) {
                        item.getString("title").trim()
                    } else {
                        ""
                    }
                    if (title.isNotEmpty()) {
                        parsedIndex.add(OrderIndexEntry(pageNo, title))
                    }
                }
            }
            if (!recognizedGroup) null else OrderOfServiceDocument(
                pages = parsedPages,
                index = parsedIndex.sortedBy { it.pageNo }
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parsePages(jsonStr: String): List<OrderPage>? = parseDocument(jsonStr)?.pages

    fun isValid(jsonStr: String): Boolean = parseDocument(jsonStr) != null

    private fun JSONObject.pageNo(): Int? = when {
        has("page_no") -> getInt("page_no")
        has("pageNo") -> getInt("pageNo")
        else -> null
    }
}
