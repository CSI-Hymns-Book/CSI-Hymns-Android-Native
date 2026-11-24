package com.reyzie.hymns.carols.data.remote

import android.util.Log
import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong
import com.reyzie.hymns.data.SupabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CarolsSupabaseDataSource(
    private val supabaseService: SupabaseService = SupabaseService.getInstance(),
) {
    companion object {
        private const val TAG = "CarolsSupabase"
        private const val BUCKET = "carol-pdfs"
    }

    val isReady: Boolean get() = supabaseService.isInitialized

    suspend fun fetchChurches(): List<CarolChurch> = withContext(Dispatchers.IO) {
        if (!isReady) return@withContext emptyList()
        try {
            supabaseService.client.from("carol_churches")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CarolChurch>()
        } catch (e: Exception) {
            Log.e(TAG, "fetchChurches failed", e)
            emptyList()
        }
    }

    suspend fun fetchSongs(): List<CarolSong> = withContext(Dispatchers.IO) {
        if (!isReady) return@withContext emptyList()
        try {
            supabaseService.client.from("carol_songs")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CarolSong>()
        } catch (e: Exception) {
            Log.e(TAG, "fetchSongs failed", e)
            emptyList()
        }
    }

    suspend fun fetchPdfs(): List<CarolPdf> = withContext(Dispatchers.IO) {
        if (!isReady) return@withContext emptyList()
        try {
            supabaseService.client.from("carol_pdfs")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CarolPdf>()
        } catch (e: Exception) {
            Log.e(TAG, "fetchPdfs failed", e)
            emptyList()
        }
    }

    suspend fun insertChurch(church: CarolChurch): CarolChurch = withContext(Dispatchers.IO) {
        supabaseService.client.from("carol_churches").insert(church)
        church
    }

    suspend fun insertSong(song: CarolSong): CarolSong = withContext(Dispatchers.IO) {
        supabaseService.client.from("carol_songs").insert(song)
        song
    }

    suspend fun insertPdf(pdf: CarolPdf): CarolPdf = withContext(Dispatchers.IO) {
        supabaseService.client.from("carol_pdfs").insert(pdf)
        pdf
    }

    suspend fun uploadPdf(pdfId: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val path = "${pdfId.lowercase()}.pdf"
        val bucket = supabaseService.client.storage.from(BUCKET)
        bucket.upload(path, bytes) {
            upsert = true
        }
        bucket.publicUrl(path)
    }

    /**
     * Saves the editable fields of a song. The server's row-level security decides who may do this;
     * if it matched no row (you are not allowed) this throws instead of looking like it worked.
     */
    suspend fun updateSong(song: CarolSong): CarolSong = withContext(Dispatchers.IO) {
        val saved = supabaseService.client.from("carol_songs").update(
            buildJsonObject {
                put("title", song.title)
                if (song.songNumber == null) put("song_number", JsonNull) else put("song_number", song.songNumber)
                put("lyrics", song.lyrics)
                put("scale", song.scale)
                put("updated_at", song.updatedAt)
            },
        ) {
            select()
            filter { eq("id", song.id) }
        }.decodeList<CarolSong>()
        saved.firstOrNull() ?: throw SecurityException("You can't edit this song.")
    }

    suspend fun deleteChurch(id: String) = withContext(Dispatchers.IO) {
        val removed = supabaseService.client.from("carol_churches").delete {
            select()
            filter { eq("id", id) }
        }.decodeList<CarolChurch>()
        if (removed.isEmpty()) throw SecurityException("You can't delete this church.")
    }

    suspend fun deleteSong(id: String) = withContext(Dispatchers.IO) {
        val removed = supabaseService.client.from("carol_songs").delete {
            select()
            filter { eq("id", id) }
        }.decodeList<CarolSong>()
        if (removed.isEmpty()) throw SecurityException("You can't delete this song.")
    }

    suspend fun deletePdf(id: String) = withContext(Dispatchers.IO) {
        val removed = supabaseService.client.from("carol_pdfs").delete {
            select()
            filter { eq("id", id) }
        }.decodeList<CarolPdf>()
        if (removed.isEmpty()) throw SecurityException("You can't delete this PDF.")
    }
}
