package com.reyzie.hymns.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ContentFetchResult<T>(
    val data: List<T>,
    val errorMessage: String? = null,
    val fromNetwork: Boolean = false
)

class HymnsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val store = ContentLocalStore(appContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun loadHymns(section: AppSection = AppSection.CSI): List<Hymn> = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        if (section == AppSection.MT) {
            readLocalMangaloreHymns().ifEmpty {
                fetchAndUpdateMangaloreHymns().data
            }
        } else {
            readLocalHymns().ifEmpty {
                fetchAndUpdateHymns().data
            }
        }
    }

    suspend fun loadKeerthanes(): List<Keerthane> = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        readLocalKeerthanes().ifEmpty {
            fetchAndUpdateKeerthanes().data
        }
    }

    suspend fun fetchAndUpdateHymns(): ContentFetchResult<Hymn> = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        val cached = readLocalHymns()
        try {
            val body = fetchUrl(AppConstants.HYMNS_DATA_URL)
                ?: throw IOException("Could not download hymns")
            store.writeHymnsJson(body)
            ContentFetchResult(
                data = parseHymnsJson(body),
                fromNetwork = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hymns", e)
            ContentFetchResult(
                data = cached,
                errorMessage = ContentErrorMessages.forThrowable(e, cached.isNotEmpty())
            )
        }
    }

    suspend fun fetchAndUpdateKeerthanes(): ContentFetchResult<Keerthane> = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        val cached = readLocalKeerthanes()
        try {
            val body = fetchUrl(AppConstants.KEERTHANE_DATA_URL)
                ?: throw IOException("Could not download keerthanes")
            store.writeKeerthaneJson(body)
            ContentFetchResult(
                data = parseKeerthanesJson(body),
                fromNetwork = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching keerthanes", e)
            ContentFetchResult(
                data = cached,
                errorMessage = ContentErrorMessages.forThrowable(e, cached.isNotEmpty())
            )
        }
    }

    suspend fun fetchAndUpdateMangaloreHymns(): ContentFetchResult<Hymn> = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        val cached = readLocalMangaloreHymns()
        try {
            val body = fetchUrl(AppConstants.MANGALORE_HYMNS_DATA_URL)
                ?: throw IOException("Could not download Mangalore hymns")
            store.writeMangaloreHymnsJson(body)
            ContentFetchResult(
                data = parseHymnsJson(body),
                fromNetwork = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Mangalore hymns", e)
            ContentFetchResult(
                data = cached,
                errorMessage = ContentErrorMessages.forThrowable(e, cached.isNotEmpty())
            )
        }
    }

    private fun readLocalHymns(): List<Hymn> {
        val json = store.readHymnsJson() ?: return emptyList()
        return parseHymnsJson(json)
    }

    private fun readLocalMangaloreHymns(): List<Hymn> {
        val json = store.readMangaloreHymnsJson() ?: return emptyList()
        return parseHymnsJson(json)
    }

    private fun readLocalKeerthanes(): List<Keerthane> {
        val json = store.readKeerthaneJson() ?: return emptyList()
        return parseKeerthanesJson(json)
    }

    private fun fetchUrl(url: String): String? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()?.takeIf { it.isNotBlank() }
        }
    }

    private fun parseHymnsJson(jsonString: String): List<Hymn> {
        val listType = object : TypeToken<List<Hymn>>() {}.type
        return gson.fromJson(jsonString, listType) ?: emptyList()
    }

    private fun parseKeerthanesJson(jsonString: String): List<Keerthane> {
        val listType = object : TypeToken<List<Keerthane>>() {}.type
        return gson.fromJson(jsonString, listType) ?: emptyList()
    }

    suspend fun saveHymn(updated: Hymn, section: AppSection = AppSection.CSI) = withContext(Dispatchers.IO) {
        val hymns = loadHymns(section).toMutableList()
        val index = hymns.indexOfFirst { it.number == updated.number }
        if (index != -1) {
            hymns[index] = updated
            if (section == AppSection.MT) {
                store.writeMangaloreHymnsJson(gson.toJson(hymns))
            } else {
                store.writeHymnsJson(gson.toJson(hymns))
            }
        }
    }

    suspend fun saveKeerthane(updated: Keerthane) = withContext(Dispatchers.IO) {
        val keerthanes = loadKeerthanes().toMutableList()
        val index = keerthanes.indexOfFirst { it.number == updated.number }
        if (index != -1) {
            keerthanes[index] = updated
            store.writeKeerthaneJson(gson.toJson(keerthanes))
        }
    }

    companion object {
        private const val TAG = "HymnsRepository"
    }
}
