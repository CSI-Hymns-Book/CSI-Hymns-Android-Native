package com.reyzie.hymns.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OrderPage(
    val pageNo: Int,
    val title: String?,
    val content: String,
    val type: String
)

data class OrderOfServiceLoadResult(
    val pages: List<OrderPage>,
    val errorMessage: String? = null
)

class OrderOfServiceRepository(context: Context) {
    private val store = ContentLocalStore(context.applicationContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun loadPages(type: String): OrderOfServiceLoadResult = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        val json = store.readOrderOfServiceJson()
        if (json.isNullOrBlank()) {
            return@withContext OrderOfServiceLoadResult(
                pages = emptyList(),
                errorMessage = ContentErrorMessages.NO_LOCAL_DATA
            )
        }
        OrderOfServiceLoadResult(pages = parsePages(json, type))
    }

    suspend fun fetchAndUpdate(type: String): OrderOfServiceLoadResult = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        val cachedJson = store.readOrderOfServiceJson()
        val cachedPages = cachedJson?.let { parsePages(it, type) }.orEmpty()
        try {
            val body = fetchUrl(AppConstants.ORDER_OF_SERVICE_DATA_URL)
                ?: throw java.io.IOException("Could not download order of service")
            store.writeOrderOfServiceJson(body)
            OrderOfServiceLoadResult(pages = parsePages(body, type))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching order of service", e)
            OrderOfServiceLoadResult(
                pages = cachedPages,
                errorMessage = ContentErrorMessages.forThrowable(e, cachedPages.isNotEmpty())
            )
        }
    }

    private fun fetchUrl(url: String): String? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()?.takeIf { it.isNotBlank() }
        }
    }

    private fun parsePages(jsonStr: String, type: String): List<OrderPage> {
        val parsedPages = mutableListOf<OrderPage>()
        val jsonObject = JSONObject(jsonStr)
        listOf("regular", "festival").forEach { groupType ->
            if (!jsonObject.has(groupType)) return@forEach
            val arr = jsonObject.getJSONArray(groupType)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val pageNo = if (item.has("page_no")) item.getInt("page_no") else item.getInt("pageNo")
                val title = if (item.has("title") && !item.isNull("title")) item.getString("title") else null
                val content = item.getString("content")
                parsedPages.add(OrderPage(pageNo, title, content, groupType))
            }
        }
        return parsedPages.filter { it.type == type }.sortedBy { it.pageNo }
    }

    suspend fun savePage(updated: OrderPage) = withContext(Dispatchers.IO) {
        val json = store.readOrderOfServiceJson() ?: return@withContext
        val jsonObject = JSONObject(json)
        listOf("regular", "festival").forEach { groupType ->
            if (jsonObject.has(groupType)) {
                val arr = jsonObject.getJSONArray(groupType)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val pageNo = if (item.has("page_no")) item.getInt("page_no") else item.getInt("pageNo")
                    if (pageNo == updated.pageNo && groupType == updated.type) {
                        item.put("content", updated.content)
                        if (updated.title != null) {
                            item.put("title", updated.title)
                        } else {
                            item.put("title", JSONObject.NULL)
                        }
                    }
                }
            }
        }
        store.writeOrderOfServiceJson(jsonObject.toString(4))
    }

    companion object {
        private const val TAG = "OrderOfServiceRepository"
    }
}
