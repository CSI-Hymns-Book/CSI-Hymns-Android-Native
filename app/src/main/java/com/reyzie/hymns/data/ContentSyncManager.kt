package com.reyzie.hymns.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class ContentSyncResult(
    val hymnsUpdated: Boolean = false,
    val keerthanesUpdated: Boolean = false,
    val orderUpdated: Boolean = false,
    val errorMessage: String? = null
) {
    val anyUpdated: Boolean get() = hymnsUpdated || keerthanesUpdated || orderUpdated
}

class ContentSyncManager(context: Context) {

    private val appContext = context.applicationContext
    private val store = ContentLocalStore(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        store.ensureSeeded()
        recordAppOpen()
        syncIfNeeded(force = false)
    }

    suspend fun syncIfNeeded(force: Boolean = false): ContentSyncResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val lastSync = prefs.getLong(KEY_LAST_SYNC, 0L)
        val lastOpen = prefs.getLong(KEY_LAST_APP_OPEN, now)
        val intervalElapsed = now - lastSync >= SYNC_INTERVAL_MS
        val staleLaunch = now - lastOpen >= STALE_LAUNCH_MS
        if (!force && !intervalElapsed && !staleLaunch) {
            return@withContext ContentSyncResult()
        }
        syncAll()
    }

    suspend fun syncAll(): ContentSyncResult = withContext(Dispatchers.IO) {
        var hymnsUpdated = false
        var keerthanesUpdated = false
        var orderUpdated = false
        var lastError: String? = null

        fetchUrl(AppConstants.HYMNS_DATA_URL)?.let { body ->
            store.writeHymnsJson(body)
            prefs.edit().putLong(KEY_LAST_HYMNS_SYNC, System.currentTimeMillis()).apply()
            hymnsUpdated = true
        } ?: run {
            lastError = ContentErrorMessages.forThrowable(null, store.hasHymns())
        }

        fetchUrl(AppConstants.KEERTHANE_DATA_URL)?.let { body ->
            store.writeKeerthaneJson(body)
            prefs.edit().putLong(KEY_LAST_KEERTHANE_SYNC, System.currentTimeMillis()).apply()
            keerthanesUpdated = true
        } ?: run {
            if (lastError == null) {
                lastError = ContentErrorMessages.forThrowable(null, store.hasKeerthanes())
            }
        }

        fetchUrl(AppConstants.ORDER_OF_SERVICE_DATA_URL)?.let { body ->
            store.writeOrderOfServiceJson(body)
            prefs.edit().putLong(KEY_LAST_ORDER_SYNC, System.currentTimeMillis()).apply()
            orderUpdated = true
        } ?: run {
            if (lastError == null) {
                lastError = ContentErrorMessages.forThrowable(null, store.hasOrderOfService())
            }
        }

        if (hymnsUpdated || keerthanesUpdated || orderUpdated) {
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
            lastError = null
            ContentUpdateBus.notifyFrom(
                ContentSyncResult(hymnsUpdated, keerthanesUpdated, orderUpdated)
            )
        }

        ContentSyncResult(
            hymnsUpdated = hymnsUpdated,
            keerthanesUpdated = keerthanesUpdated,
            orderUpdated = orderUpdated,
            errorMessage = if (hymnsUpdated || keerthanesUpdated || orderUpdated) null else lastError
        )
    }

    private fun recordAppOpen() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_APP_OPEN, now).apply()
    }

    private fun fetchUrl(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} for $url")
                    return null
                }
                response.body?.string()?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetch failed: $url", e)
            null
        }
    }

    companion object {
        private const val TAG = "ContentSyncManager"
        private const val PREFS_NAME = "content_sync"
        private const val KEY_LAST_SYNC = "lastContentSync"
        private const val KEY_LAST_APP_OPEN = "lastAppOpen"
        private const val KEY_LAST_HYMNS_SYNC = "lastHymnsSync"
        private const val KEY_LAST_KEERTHANE_SYNC = "lastKeerthaneSync"
        private const val KEY_LAST_ORDER_SYNC = "lastOrderSync"
        val SYNC_INTERVAL_MS: Long = TimeUnit.DAYS.toMillis(3)
        val STALE_LAUNCH_MS: Long = TimeUnit.DAYS.toMillis(7)
    }
}
