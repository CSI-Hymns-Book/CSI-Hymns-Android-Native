package com.reyzie.hymns.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Private app storage for hymns, keerthane, and order-of-service JSON.
 * Data lives in [Context.filesDir]/content/ — not user-visible, app-only.
 */
class ContentLocalStore(private val context: Context) {

    private val contentDir: File =
        File(context.filesDir, CONTENT_DIR).apply { mkdirs() }

    val hymnsFile get() = File(contentDir, HYMNS_FILE)
    val keerthaneFile get() = File(contentDir, KEERTHANE_FILE)
    val orderOfServiceFile get() = File(contentDir, ORDER_FILE)
    val mangaloreHymnsFile get() = File(contentDir, MANGALORE_HYMNS_FILE)

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        migrateFromLegacyPrefs()
        copyAssetIfMissing(ASSET_HYMNS, hymnsFile)
        copyAssetIfMissing(ASSET_KEERTHANE, keerthaneFile)
        copyAssetIfMissing(ASSET_ORDER, orderOfServiceFile)
        if (mangaloreHymnsFile.exists() && !mangaloreHymnsFile.readText().contains("\"category\"")) {
            mangaloreHymnsFile.delete()
        }
        copyAssetIfMissing(ASSET_MANGALORE_HYMNS, mangaloreHymnsFile)
    }

    fun readHymnsJson(): String? = hymnsFile.takeIf { it.exists() }?.readText()
    fun readKeerthaneJson(): String? = keerthaneFile.takeIf { it.exists() }?.readText()
    fun readOrderOfServiceJson(): String? = orderOfServiceFile.takeIf { it.exists() }?.readText()
    fun readMangaloreHymnsJson(): String? = mangaloreHymnsFile.takeIf { it.exists() }?.readText()

    fun writeHymnsJson(json: String) {
        hymnsFile.writeText(json)
    }

    fun writeKeerthaneJson(json: String) {
        keerthaneFile.writeText(json)
    }

    fun writeOrderOfServiceJson(json: String) {
        orderOfServiceFile.writeText(json)
    }

    fun writeMangaloreHymnsJson(json: String) {
        mangaloreHymnsFile.writeText(json)
    }

    fun reseedKeerthaneFromAsset(): Boolean = reseedFromAsset(ASSET_KEERTHANE, keerthaneFile)
    fun reseedHymnsFromAsset(): Boolean = reseedFromAsset(ASSET_HYMNS, hymnsFile)
    fun reseedMangaloreHymnsFromAsset(): Boolean = reseedFromAsset(ASSET_MANGALORE_HYMNS, mangaloreHymnsFile)

    fun hasHymns(): Boolean = hymnsFile.exists() && hymnsFile.length() > 0
    fun hasKeerthanes(): Boolean = keerthaneFile.exists() && keerthaneFile.length() > 0
    fun hasOrderOfService(): Boolean = orderOfServiceFile.exists() && orderOfServiceFile.length() > 0
    fun hasMangaloreHymns(): Boolean = mangaloreHymnsFile.exists() && mangaloreHymnsFile.length() > 0

    private fun copyAssetIfMissing(assetPath: String, target: File) {
        if (target.exists() && target.length() > 0) return
        reseedFromAsset(assetPath, target)
    }

    private fun reseedFromAsset(assetPath: String, target: File): Boolean {
        return try {
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Could not seed $assetPath", e)
            false
        }
    }

    private fun migrateFromLegacyPrefs() {
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        legacy.getString("hymnsData", null)?.let { json ->
            if (!hymnsFile.exists() && ContentJsonParser.parseHymns(json) != null) {
                hymnsFile.writeText(json)
            }
        }
        legacy.getString("keerthaneData", null)?.let { json ->
            if (!keerthaneFile.exists() && ContentJsonParser.parseKeerthanes(json) != null) {
                keerthaneFile.writeText(json)
            }
        }
        legacy.getString("orderOfServiceData", null)?.let { json ->
            if (!orderOfServiceFile.exists()) orderOfServiceFile.writeText(json)
        }
    }

    companion object {
        private const val TAG = "ContentLocalStore"
        private const val CONTENT_DIR = "content"
        private const val HYMNS_FILE = "hymns_data.json"
        private const val KEERTHANE_FILE = "keerthane_data.json"
        private const val ORDER_FILE = "order-of-service_data.json"
        private const val MANGALORE_HYMNS_FILE = "mangalore_hymns_data.json"
        private const val LEGACY_PREFS = "HymnsPrefs"
        private const val ASSET_HYMNS = "content/hymns_data.json"
        private const val ASSET_KEERTHANE = "content/keerthane_data.json"
        private const val ASSET_ORDER = "content/order-of-service_data.json"
        private const val ASSET_MANGALORE_HYMNS = "content/mangalore_hymns_data.json"
    }
}
