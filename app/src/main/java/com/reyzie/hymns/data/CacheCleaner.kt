package com.reyzie.hymns.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Clears everything the app can safely download again: temporary files, downloaded MIDI
 * tunes and the cached list of tunes. Saved songs, favourites, settings and your sign-in
 * are never touched, so the app still works offline afterwards.
 */
object CacheCleaner {
    /** Bytes of cached files that [clearAll] would free. */
    suspend fun sizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        cacheRoots(context).sumOf { folderSize(it) }
    }

    /** Deletes the cache and returns how many bytes it freed. */
    suspend fun clearAll(context: Context): Long = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val before = cacheRoots(appContext).sumOf { folderSize(it) }
        MidiFileCache.getInstance(appContext).invalidateAll()
        HymnsRepository.clearMidiListMemory()
        appContext.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE).edit()
            .remove("cached_midi_files_json")
            .remove("cached_midi_files_fingerprint")
            .remove("cached_midi_files_at")
            .apply()
        cacheRoots(appContext).forEach { root -> root.listFiles()?.forEach { runCatching { it.deleteRecursively() } } }
        before
    }

    private fun cacheRoots(context: Context): List<File> =
        listOfNotNull(context.cacheDir, context.externalCacheDir).filter { it.exists() }

    private fun folderSize(file: File): Long =
        if (file.isFile) file.length() else file.listFiles()?.sumOf { folderSize(it) } ?: 0L

    /** "3.2 MB" style text for a byte count. */
    fun format(bytes: Long): String = when {
        bytes < KILOBYTE -> "$bytes B"
        bytes < MEGABYTE -> "%.0f KB".format(bytes / KILOBYTE.toDouble())
        else -> "%.1f MB".format(bytes / MEGABYTE.toDouble())
    }

    private const val KILOBYTE = 1024L
    private const val MEGABYTE = 1024L * 1024L
}
