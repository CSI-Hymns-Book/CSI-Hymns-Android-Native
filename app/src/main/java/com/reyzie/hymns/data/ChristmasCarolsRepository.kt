package com.reyzie.hymns.data

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChristmasCarolsRepository(private val context: Context) {
    private val db = ChristmasCarolsDB(context)
    private val githubSyncService = GitHubSyncService(context)
    private val supabaseService = SupabaseService.getInstance()

    companion object {
        private const val SUPABASE_TABLE = "christmas_carols"
        private const val SUPABASE_BUCKET = "carol-pdfs"
    }

    suspend fun refreshFromRemote(): List<ChristmasCarol> = withContext(Dispatchers.IO) {
        val github = githubSyncService.pullFromGitHub().orEmpty()
        val supabase = loadFromSupabase()
        val local = db.getAllCarols()
        val merged = mergeCarols(github + supabase + local)
        db.deleteAllCarols()
        db.upsertCarols(merged)
        merged
    }

    suspend fun getAllLocal(): List<ChristmasCarol> = withContext(Dispatchers.IO) {
        db.getAllCarols()
    }

    private suspend fun loadFromSupabase(): List<ChristmasCarol> = withContext(Dispatchers.IO) {
        try {
            supabaseService.client.from(SUPABASE_TABLE)
                .select()
                .decodeList<ChristmasCarol>()
        } catch (e: Exception) {
            Log.w("ChristmasCarolsRepository", "Supabase carols fetch failed", e)
            emptyList()
        }
    }

    suspend fun addCarol(
        churchName: String,
        title: String,
        lyrics: String,
        songNumber: String?,
        scale: String
    ): ChristmasCarol = withContext(Dispatchers.IO) {
        val userId = supabaseService.currentUser?.id ?: throw IllegalStateException("Sign in required")
        val now = Instant.now().toString()
        val carol = ChristmasCarol(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            songNumber = songNumber?.trim().takeUnless { it.isNullOrEmpty() },
            churchName = churchName.trim(),
            lyrics = lyrics.trim(),
            pdfPath = null,
            pdfPages = null,
            transpose = 0,
            scale = scale,
            hasChords = true,
            createdByUserId = userId,
            createdAt = now,
            updatedAt = now
        )
        upsertRemote(carol)
        db.upsertCarol(carol)
        syncLocalToGitHub()
        carol
    }

    suspend fun addPdfCarol(
        churchName: String,
        title: String,
        pdfUri: Uri
    ): ChristmasCarol = withContext(Dispatchers.IO) {
        val userId = supabaseService.currentUser?.id ?: throw IllegalStateException("Sign in required")
        val carolId = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val pdfPath = uploadPdfFromUri(pdfUri, carolId)
            ?: throw IllegalStateException("Could not upload PDF")
        val carol = ChristmasCarol(
            id = carolId,
            title = title.trim(),
            songNumber = null,
            churchName = churchName.trim(),
            lyrics = null,
            pdfPath = pdfPath,
            pdfPages = null,
            transpose = 0,
            scale = "C Major",
            hasChords = false,
            createdByUserId = userId,
            createdAt = now,
            updatedAt = now
        )
        upsertRemote(carol)
        db.upsertCarol(carol)
        syncLocalToGitHub()
        carol
    }

    private suspend fun uploadPdfFromUri(uri: Uri, carolId: String): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        return uploadPdfBytes(bytes, carolId)
    }

    private suspend fun uploadPdfBytes(bytes: ByteArray, carolId: String): String? {
        val fileName = "$carolId.pdf"
        return try {
            val bucket = supabaseService.client.storage.from(SUPABASE_BUCKET)
            bucket.upload(fileName, bytes, upsert = true)
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            Log.w("ChristmasCarolsRepository", "Supabase PDF upload failed, saving locally", e)
            savePdfLocally(bytes, carolId)
        }
    }

    private fun savePdfLocally(bytes: ByteArray, carolId: String): String? {
        return try {
            val dir = File(context.filesDir, "carol_pdfs").apply { mkdirs() }
            val file = File(dir, "$carolId.pdf")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ChristmasCarolsRepository", "Local PDF save failed", e)
            null
        }
    }

    suspend fun deleteCarol(id: String) = withContext(Dispatchers.IO) {
        deleteRemote(id)
        db.deleteCarol(id)
        syncLocalToGitHub()
    }

    suspend fun updateCarol(carol: ChristmasCarol) = withContext(Dispatchers.IO) {
        val updated = carol.copy(updatedAt = Instant.now().toString())
        upsertRemote(updated)
        db.upsertCarol(updated)
        syncLocalToGitHub()
    }

    suspend fun deleteChurch(churchName: String) = withContext(Dispatchers.IO) {
        val toDelete = db.getAllCarols().filter { it.churchName == churchName }
        toDelete.forEach { deleteCarol(it.id) }
    }

    suspend fun syncLocalToGitHub(): Boolean = withContext(Dispatchers.IO) {
        githubSyncService.pushToGitHub(db.getAllCarols())
    }

    private suspend fun upsertRemote(carol: ChristmasCarol) {
        try {
            supabaseService.client.from(SUPABASE_TABLE).upsert(carol)
        } catch (e: Exception) {
            Log.e("ChristmasCarolsRepository", "Supabase upsert failed", e)
            throw e
        }
    }

    private suspend fun deleteRemote(id: String) {
        try {
            supabaseService.client.from(SUPABASE_TABLE).delete {
                filter { eq("id", id) }
            }
        } catch (e: Exception) {
            Log.w("ChristmasCarolsRepository", "Supabase delete failed for $id", e)
        }
    }

    private fun mergeCarols(sources: List<ChristmasCarol>): List<ChristmasCarol> {
        val byId = linkedMapOf<String, ChristmasCarol>()
        for (carol in sources) {
            val existing = byId[carol.id]
            if (existing == null) {
                byId[carol.id] = carol
            } else {
                val existingDate = existing.updatedAt ?: existing.createdAt
                val candidateDate = carol.updatedAt ?: carol.createdAt
                if (candidateDate >= existingDate) {
                    byId[carol.id] = carol
                }
            }
        }
        return byId.values.sortedByDescending { it.updatedAt ?: it.createdAt }
    }
}
