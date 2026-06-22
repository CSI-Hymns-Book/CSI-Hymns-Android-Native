package com.reyzie.hymns.carols.data.repository

import android.content.Context
import android.util.Log
import com.reyzie.hymns.carols.data.local.CarolsLocalDB
import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.carols.data.remote.CarolsSupabaseDataSource
import com.reyzie.hymns.carols.domain.buildBilingualLyrics
import com.reyzie.hymns.data.SupabaseService
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class CarolsRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val localDb = CarolsLocalDB(appContext)
    private val remote = CarolsSupabaseDataSource()
    private val supabase = SupabaseService.getInstance()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _churches = MutableStateFlow(localDb.getAllChurches())
    private val _songs = MutableStateFlow(localDb.getAllSongs())
    private val _pdfs = MutableStateFlow(localDb.getAllPdfs())

    val churches: StateFlow<List<CarolChurch>> = _churches.asStateFlow()
    val songs: StateFlow<List<CarolSong>> = _songs.asStateFlow()
    val pdfs: StateFlow<List<CarolPdf>> = _pdfs.asStateFlow()

    companion object {
        private const val TAG = "CarolsRepository"
        private const val PREFS_NAME = "carols_sync_prefs"
        private const val KEY_LAST_SYNC_MS = "last_sync_ms"
        private const val SYNC_INTERVAL_MS = 5 * 60 * 1000L

        @Volatile
        private var instance: CarolsRepository? = null

        fun getInstance(context: Context): CarolsRepository {
            return instance ?: synchronized(this) {
                instance ?: CarolsRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun hasLocalData(): Boolean = _churches.value.isNotEmpty()

    fun loadLocal() {
        val churches = localDb.getAllChurches()
        val songs = localDb.getAllSongs()
        val pdfs = localDb.getAllPdfs()
        publishLocal(churches, songs, pdfs)
        Log.d(TAG, "Loaded local cache: ${churches.size} churches")
    }

    suspend fun refresh(force: Boolean = false): CarolsSyncSnapshot = withContext(Dispatchers.IO) {
        val localChurches = localDb.getAllChurches()
        val localSongs = localDb.getAllSongs()
        val localPdfs = localDb.getAllPdfs()
        val isAuthenticated = supabase.currentUser != null

        if (!force && localChurches.isNotEmpty()) {
            val lastSync = prefs.getLong(KEY_LAST_SYNC_MS, 0L)
            if (System.currentTimeMillis() - lastSync < SYNC_INTERVAL_MS) {
                publishLocal(localChurches, localSongs, localPdfs)
                return@withContext CarolsSyncSnapshot(
                    churches = localChurches,
                    songs = localSongs,
                    pdfs = localPdfs,
                    keptLocalOnly = true,
                    isAuthenticated = isAuthenticated,
                )
            }
        }

        val remoteChurches = remote.fetchChurches()
        val remoteSongs = remote.fetchSongs()
        val remotePdfs = remote.fetchPdfs()

        val hasAnyRemote = remoteChurches.isNotEmpty() || remoteSongs.isNotEmpty() || remotePdfs.isNotEmpty()

        if (!hasAnyRemote) {
            Log.w(TAG, "Remote empty; keeping local (${localChurches.size} churches)")
            publishLocal(localChurches, localSongs, localPdfs)
            return@withContext CarolsSyncSnapshot(
                churches = localChurches,
                songs = localSongs,
                pdfs = localPdfs,
                keptLocalOnly = localChurches.isNotEmpty(),
                isAuthenticated = isAuthenticated,
            )
        }

        // Never wipe a table when that remote table came back empty.
        val mergedChurches = remoteChurches.ifEmpty { localChurches }
        val mergedSongs = remoteSongs.ifEmpty { localSongs }
        val mergedPdfs = remotePdfs.ifEmpty { localPdfs }

        localDb.replaceAll(mergedChurches, mergedSongs, mergedPdfs)
        publishLocal(mergedChurches, mergedSongs, mergedPdfs)
        prefs.edit().putLong(KEY_LAST_SYNC_MS, System.currentTimeMillis()).apply()
        Log.i(TAG, "Synced ${mergedChurches.size} churches, ${mergedSongs.size} songs, ${mergedPdfs.size} pdfs")
        CarolsSyncSnapshot(
            churches = mergedChurches,
            songs = mergedSongs,
            pdfs = mergedPdfs,
            isAuthenticated = isAuthenticated,
        )
    }

    suspend fun createChurch(name: String, description: String? = null): CarolChurch = withContext(Dispatchers.IO) {
        val userId = requireUserId()
        val now = Instant.now().toString()
        val church = CarolChurch(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            createdByUserId = userId,
            createdAt = now,
            updatedAt = now,
        )
        val saved = remote.insertChurch(church)
        localDb.upsertChurch(saved)
        _churches.value = localDb.getAllChurches()
        saved
    }

    suspend fun addSong(
        churchId: String,
        title: String,
        songNumber: String?,
        lyricsKannada: String,
        lyricsEnglish: String?,
        scale: String = "C Major",
    ): CarolSong = withContext(Dispatchers.IO) {
        val userId = requireUserId()
        val now = Instant.now().toString()
        val song = CarolSong(
            id = UUID.randomUUID().toString(),
            churchId = churchId,
            title = title.trim(),
            songNumber = songNumber?.trim()?.takeIf { it.isNotEmpty() },
            lyrics = buildBilingualLyrics(lyricsKannada, lyricsEnglish),
            scale = scale,
            createdByUserId = userId,
            createdAt = now,
            updatedAt = now,
        )
        val saved = remote.insertSong(song)
        localDb.upsertSong(saved)
        _songs.value = localDb.getAllSongs()
        saved
    }

    suspend fun addPdf(
        churchId: String,
        title: String,
        songNumber: String?,
        pdfBytes: ByteArray,
    ): CarolPdf = withContext(Dispatchers.IO) {
        val userId = requireUserId()
        val pdfId = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val pdfUrl = remote.uploadPdf(pdfId, pdfBytes)
        val pdf = CarolPdf(
            id = pdfId,
            churchId = churchId,
            title = title.trim(),
            songNumber = songNumber?.trim()?.takeIf { it.isNotEmpty() },
            pdfUrl = pdfUrl,
            createdByUserId = userId,
            createdAt = now,
            updatedAt = now,
        )
        val saved = remote.insertPdf(pdf)
        localDb.upsertPdf(saved)
        _pdfs.value = localDb.getAllPdfs()
        saved
    }

    suspend fun deleteChurch(id: String) = withContext(Dispatchers.IO) {
        remote.deleteChurch(id)
        localDb.deleteChurch(id)
        _churches.value = localDb.getAllChurches()
        _songs.value = localDb.getAllSongs()
        _pdfs.value = localDb.getAllPdfs()
    }

    suspend fun deleteSong(id: String) = withContext(Dispatchers.IO) {
        remote.deleteSong(id)
        localDb.deleteSong(id)
        _songs.value = localDb.getAllSongs()
    }

    suspend fun deletePdf(id: String) = withContext(Dispatchers.IO) {
        remote.deletePdf(id)
        localDb.deletePdf(id)
        _pdfs.value = localDb.getAllPdfs()
    }

    fun songsForChurch(churchId: String): List<CarolSong> =
        _songs.value.filter { it.churchId == churchId }

    fun pdfsForChurch(churchId: String): List<CarolPdf> =
        _pdfs.value.filter { it.churchId == churchId }

    fun churchById(id: String): CarolChurch? =
        _churches.value.find { it.id == id }

    private fun publishLocal(
        churches: List<CarolChurch>,
        songs: List<CarolSong>,
        pdfs: List<CarolPdf>,
    ) {
        _churches.value = churches
        _songs.value = songs
        _pdfs.value = pdfs
    }

    private fun requireUserId(): String =
        supabase.currentUser?.id ?: throw IllegalStateException("Sign in required")
}
