package com.reyzie.hymns.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
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
    private val appConfigRepository = AppConfigRepository(context = appContext)
    private var cachedMidiFileNames: List<String>? = null

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
            val parsed = ContentJsonParser.parseHymns(body)
                ?: throw IOException("Downloaded hymns JSON is invalid")
            store.writeHymnsJson(body)
            ContentFetchResult(data = parsed, fromNetwork = true)
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
            val parsed = ContentJsonParser.parseKeerthanes(body)
                ?: throw IOException("Downloaded keerthane JSON is invalid")
            store.writeKeerthaneJson(body)
            ContentFetchResult(data = parsed, fromNetwork = true)
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
            val parsed = ContentJsonParser.parseHymns(body)
                ?: throw IOException("Downloaded Mangalore hymns JSON is invalid")
            store.writeMangaloreHymnsJson(body)
            ContentFetchResult(data = parsed, fromNetwork = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Mangalore hymns", e)
            ContentFetchResult(
                data = cached,
                errorMessage = ContentErrorMessages.forThrowable(e, cached.isNotEmpty())
            )
        }
    }

    private fun readLocalHymns(): List<Hymn> = readLocalList(
        label = "hymns",
        readJson = { store.readHymnsJson() },
        parse = ContentJsonParser::parseHymns,
        reseed = { store.reseedHymnsFromAsset() }
    )

    private fun readLocalMangaloreHymns(): List<Hymn> = readLocalList(
        label = "Mangalore hymns",
        readJson = { store.readMangaloreHymnsJson() },
        parse = ContentJsonParser::parseHymns,
        reseed = { store.reseedMangaloreHymnsFromAsset() }
    )

    private fun readLocalKeerthanes(): List<Keerthane> = readLocalList(
        label = "keerthanes",
        readJson = { store.readKeerthaneJson() },
        parse = ContentJsonParser::parseKeerthanes,
        reseed = { store.reseedKeerthaneFromAsset() }
    )

    private fun <T> readLocalList(
        label: String,
        readJson: () -> String?,
        parse: (String) -> List<T>?,
        reseed: () -> Boolean
    ): List<T> {
        val json = readJson()
        if (json != null) {
            parse(json)?.let { return it }
            Log.w(TAG, "Corrupt local $label cache; reseeding from bundled assets")
            reseed()
            readJson()?.let { reseeded ->
                parse(reseeded)?.let { return it }
            }
        } else {
            reseed()
            readJson()?.let { reseeded ->
                parse(reseeded)?.let { return it }
            }
        }
        return emptyList()
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

    private fun fetchUrlWithAuth(url: String, rawToken: String?): String? {
        val cleanToken = rawToken?.replace("[", "")?.replace("]", "")?.replace("\"", "")?.replace("'", "")?.trim()
        if (!cleanToken.isNullOrBlank()) {
            try {
                val builder = Request.Builder().url(url)
                    .addHeader("Authorization", "Bearer $cleanToken")
                    .addHeader("User-Agent", "CSI-Hymns-App")
                val request = builder.build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return response.body?.string()?.takeIf { it.isNotBlank() }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetch with Bearer token, trying token prefix: ${e.message}")
            }
            try {
                val builder = Request.Builder().url(url)
                    .addHeader("Authorization", "token $cleanToken")
                    .addHeader("User-Agent", "CSI-Hymns-App")
                val request = builder.build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return response.body?.string()?.takeIf { it.isNotBlank() }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetch with token prefix: ${e.message}")
            }
        }
        
        try {
            val request = Request.Builder().url(url).addHeader("User-Agent", "CSI-Hymns-App").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return response.body?.string()?.takeIf { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fallback fetch without token", e)
        }
        return null
    }

    suspend fun getMidiFileNames(): List<String> = withContext(Dispatchers.IO) {
        cachedMidiFileNames?.let { return@withContext it }
        try {
            val config = appConfigRepository.getCachedRemoteConfig()
            val token = config.githubMidiToken
            val response = fetchUrlWithAuth("https://api.github.com/repos/Reynold29/midi-vault/contents/Hymns", token)
            if (response != null) {
                val parsed = parseGitHubContentsNames(response)
                cachedMidiFileNames = parsed
                return@withContext parsed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch midi file list from GitHub", e)
        }
        emptyList()
    }

    fun getCachedMidiFileNames(): List<String> = cachedMidiFileNames ?: emptyList()

    private fun parseGitHubContentsNames(json: String): List<String> {
        return try {
            val jsonArray = com.google.gson.JsonParser.parseString(json).asJsonArray
            val names = mutableListOf<String>()
            for (element in jsonArray) {
                if (element.isJsonObject) {
                    val nameObj = element.asJsonObject.get("name")
                    if (nameObj != null && nameObj.isJsonPrimitive) {
                        names.add(nameObj.asString)
                    }
                }
            }
            names
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse GitHub contents names JSON", e)
            emptyList()
        }
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
