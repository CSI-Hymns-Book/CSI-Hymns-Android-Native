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

data class OrderIndexEntry(
    val pageNo: Int,
    val title: String
)

data class OrderPageSection(
    val title: String,
    val startPageNo: Int,
    val pages: List<OrderPage>
)

data class OrderOfServiceLoadResult(
    val pages: List<OrderPage>,
    val index: List<OrderIndexEntry> = emptyList(),
    val errorMessage: String? = null
)

class OrderOfServiceRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = ContentLocalStore(appContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun loadPages(type: String): OrderOfServiceLoadResult = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        val local = readLocal(type)
        if (local.pages.isEmpty()) {
            return@withContext OrderOfServiceLoadResult(
                pages = emptyList(),
                index = local.index,
                errorMessage = ContentErrorMessages.NO_LOCAL_DATA
            )
        }
        OrderOfServiceLoadResult(pages = local.pages, index = local.index)
    }

    suspend fun fetchAndUpdate(type: String): OrderOfServiceLoadResult = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        val cached = readLocal(type)
        try {
            val body = fetchUrl(AppConstants.ORDER_OF_SERVICE_DATA_URL)
                ?: throw java.io.IOException("Could not download order of service")
            val parsed = OrderOfServiceJson.parseDocument(body)
                ?: throw java.io.IOException("Downloaded order-of-service JSON is invalid")
            store.writeOrderOfServiceJson(body)
            resultForType(parsed, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching order of service", e)
            OrderOfServiceLoadResult(
                pages = cached.pages,
                index = cached.index,
                errorMessage = ContentErrorMessages.forThrowable(e, cached.pages.isNotEmpty())
            )
        }
    }

    private suspend fun fetchUrl(url: String): String? {
        val sha = com.reyzie.hymns.utils.GitHubUrlResolver.getLatestCommitSha(appContext)
        val resolvedUrl = com.reyzie.hymns.utils.GitHubUrlResolver.resolveRawUrl(url, sha)
        val request = Request.Builder()
            .url(resolvedUrl)
            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            .addHeader("Pragma", "no-cache")
            .addHeader("Expires", "0")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()?.takeIf { it.isNotBlank() }
        }
    }

    private fun readLocal(type: String): OrderOfServiceLoadResult {
        val json = store.readOrderOfServiceJson()
        if (json != null) {
            OrderOfServiceJson.parseDocument(json)?.let { parsed ->
                return resultForType(parsed, type)
            }
            Log.w(TAG, "Corrupt local order-of-service cache; reseeding from bundled assets")
            store.reseedOrderOfServiceFromAsset()
            store.readOrderOfServiceJson()?.let { reseeded ->
                OrderOfServiceJson.parseDocument(reseeded)?.let { parsed ->
                    return resultForType(parsed, type)
                }
            }
        }
        return OrderOfServiceLoadResult(pages = emptyList())
    }

    private fun resultForType(doc: OrderOfServiceDocument, type: String): OrderOfServiceLoadResult {
        val pages = doc.pages.filter { it.type == type }.sortedBy { it.pageNo }
        val index = if (type == "regular") doc.index else emptyList()
        return OrderOfServiceLoadResult(pages = pages, index = index)
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
