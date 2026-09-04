package com.reyzie.hymns.data

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * Disk cache for original MIDI/OGG bytes under [Context.getCacheDir]/midi_cache/.
 * Stores source bytes only — instrument/transpose/SATB patching stays elsewhere.
 */
class MidiFileCache private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.cacheDir, CACHE_DIR).apply { mkdirs() }
    private val metaPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Entry(
        val bytes: ByteArray,
        val fromDisk: Boolean,
        val etag: String? = null
    )

    /**
     * Returns cached bytes if present and not past TTL.
     * Stale entries are deleted so the caller re-downloads.
     */
    fun getIfFresh(url: String, configFingerprint: String): Entry? {
        val key = cacheKey(url)
        val file = File(cacheDir, "$key.bin")
        if (!file.exists() || file.length() == 0L) return null

        val storedFingerprint = metaPrefs.getString(metaKey(key, META_FINGERPRINT), null)
        val cachedAt = metaPrefs.getLong(metaKey(key, META_CACHED_AT), 0L)
        val age = System.currentTimeMillis() - cachedAt
        val fingerprintMismatch = storedFingerprint != null && storedFingerprint != configFingerprint
        val expired = cachedAt > 0L && age > TTL_MS

        if (fingerprintMismatch || expired) {
            Log.d(TAG, "Cache miss (stale) for $url fingerprintMismatch=$fingerprintMismatch expired=$expired")
            deleteEntry(key)
            return null
        }

        return try {
            val bytes = file.readBytes()
            val etag = metaPrefs.getString(metaKey(key, META_ETAG), null)
            Log.d(TAG, "Cache hit for $url (${bytes.size} bytes)")
            Entry(bytes = bytes, fromDisk = true, etag = etag)
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading cache for $url", e)
            deleteEntry(key)
            null
        }
    }

    fun put(url: String, bytes: ByteArray, configFingerprint: String, etag: String? = null) {
        if (bytes.isEmpty()) return
        val key = cacheKey(url)
        val file = File(cacheDir, "$key.bin")
        try {
            file.writeBytes(bytes)
            metaPrefs.edit()
                .putLong(metaKey(key, META_CACHED_AT), System.currentTimeMillis())
                .putString(metaKey(key, META_URL), url)
                .putString(metaKey(key, META_FINGERPRINT), configFingerprint)
                .putString(metaKey(key, META_ETAG), etag)
                .apply()
            pruneIfNeeded()
            Log.d(TAG, "Cached $url (${bytes.size} bytes)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache $url", e)
            runCatching { file.delete() }
        }
    }

    fun invalidateAll() {
        cacheDir.listFiles()?.forEach { runCatching { it.delete() } }
        metaPrefs.edit().clear().apply()
        Log.d(TAG, "Invalidated entire MIDI file cache")
    }

    private fun deleteEntry(key: String) {
        File(cacheDir, "$key.bin").delete()
        metaPrefs.edit()
            .remove(metaKey(key, META_CACHED_AT))
            .remove(metaKey(key, META_URL))
            .remove(metaKey(key, META_FINGERPRINT))
            .remove(metaKey(key, META_ETAG))
            .apply()
    }

    private fun pruneIfNeeded() {
        val files = cacheDir.listFiles()?.filter { it.isFile && it.name.endsWith(".bin") }.orEmpty()
        var total = files.sumOf { it.length() }
        if (total <= MAX_BYTES) return

        val ordered = files.sortedBy { it.lastModified() }
        for (file in ordered) {
            if (total <= MAX_BYTES) break
            val key = file.name.removeSuffix(".bin")
            total -= file.length()
            deleteEntry(key)
            Log.d(TAG, "Pruned cache entry $key")
        }
    }

    companion object {
        private const val TAG = "MidiFileCache"
        private const val CACHE_DIR = "midi_cache"
        private const val PREFS_NAME = "midi_file_cache_meta"
        private const val META_CACHED_AT = "at"
        private const val META_URL = "url"
        private const val META_FINGERPRINT = "fp"
        private const val META_ETAG = "etag"
        private const val TTL_MS = 24L * 60 * 60 * 1000
        private const val MAX_BYTES = 80L * 1024 * 1024

        @Volatile
        private var instance: MidiFileCache? = null

        fun getInstance(context: Context): MidiFileCache {
            return instance ?: synchronized(this) {
                instance ?: MidiFileCache(context.applicationContext).also { instance = it }
            }
        }

        /** Fingerprint of MIDI-related AppConfig so range/token changes bust the cache. */
        fun configFingerprint(config: RemoteAppConfig): String {
            val raw = listOf(
                config.githubMidiToken.orEmpty(),
                config.midiHymnsRanges.orEmpty(),
                config.midiKeerthanesRanges.orEmpty(),
                config.disableOggFallback.orEmpty(),
                config.audioBackupUrl.orEmpty()
            ).joinToString("|")
            return sha256Hex(raw).take(16)
        }

        fun cacheKey(url: String): String = sha256Hex(url).take(40)

        private fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        private fun metaKey(cacheKey: String, field: String) = "${cacheKey}_$field"
    }
}
