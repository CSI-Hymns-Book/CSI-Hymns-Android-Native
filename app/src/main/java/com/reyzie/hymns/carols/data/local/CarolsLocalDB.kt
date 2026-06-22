package com.reyzie.hymns.carols.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong

class CarolsLocalDB(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "community_carols.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_CHURCHES = "carol_churches"
        private const val TABLE_SONGS = "carol_songs"
        private const val TABLE_PDFS = "carol_pdfs"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CHURCHES (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                created_by_user_id TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_SONGS (
                id TEXT PRIMARY KEY,
                church_id TEXT NOT NULL,
                title TEXT NOT NULL,
                song_number TEXT,
                lyrics TEXT NOT NULL,
                scale TEXT NOT NULL DEFAULT 'C Major',
                created_by_user_id TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT,
                FOREIGN KEY(church_id) REFERENCES $TABLE_CHURCHES(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_PDFS (
                id TEXT PRIMARY KEY,
                church_id TEXT NOT NULL,
                title TEXT NOT NULL,
                song_number TEXT,
                pdf_url TEXT NOT NULL,
                created_by_user_id TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT,
                FOREIGN KEY(church_id) REFERENCES $TABLE_CHURCHES(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_songs_church ON $TABLE_SONGS(church_id)")
        db.execSQL("CREATE INDEX idx_pdfs_church ON $TABLE_PDFS(church_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun getAllChurches(): List<CarolChurch> = queryChurches(null, null)

    fun getAllSongs(): List<CarolSong> = querySongs(null, null)

    fun getAllPdfs(): List<CarolPdf> = queryPdfs(null, null)

    fun replaceAll(churches: List<CarolChurch>, songs: List<CarolSong>, pdfs: List<CarolPdf>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_PDFS, null, null)
            db.delete(TABLE_SONGS, null, null)
            db.delete(TABLE_CHURCHES, null, null)
            churches.forEach { db.insertWithOnConflict(TABLE_CHURCHES, null, churchValues(it), SQLiteDatabase.CONFLICT_REPLACE) }
            songs.forEach { db.insertWithOnConflict(TABLE_SONGS, null, songValues(it), SQLiteDatabase.CONFLICT_REPLACE) }
            pdfs.forEach { db.insertWithOnConflict(TABLE_PDFS, null, pdfValues(it), SQLiteDatabase.CONFLICT_REPLACE) }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("CarolsLocalDB", "replaceAll failed", e)
            throw e
        } finally {
            db.endTransaction()
        }
    }

    fun upsertChurch(church: CarolChurch) {
        writableDatabase.insertWithOnConflict(TABLE_CHURCHES, null, churchValues(church), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun upsertSong(song: CarolSong) {
        writableDatabase.insertWithOnConflict(TABLE_SONGS, null, songValues(song), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun upsertPdf(pdf: CarolPdf) {
        writableDatabase.insertWithOnConflict(TABLE_PDFS, null, pdfValues(pdf), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteChurch(id: String) {
        writableDatabase.delete(TABLE_CHURCHES, "id = ?", arrayOf(id))
    }

    fun deleteSong(id: String) {
        writableDatabase.delete(TABLE_SONGS, "id = ?", arrayOf(id))
    }

    fun deletePdf(id: String) {
        writableDatabase.delete(TABLE_PDFS, "id = ?", arrayOf(id))
    }

    private fun queryChurches(where: String?, args: Array<String>?): List<CarolChurch> {
        val list = mutableListOf<CarolChurch>()
        readableDatabase.query(TABLE_CHURCHES, null, where, args, null, null, "created_at DESC").use { c ->
            while (c.moveToNext()) {
                list.add(
                    CarolChurch(
                        id = c.getString(0),
                        name = c.getString(1),
                        description = c.getString(2),
                        createdByUserId = c.getString(3),
                        createdAt = c.getString(4),
                        updatedAt = c.getString(5),
                    ),
                )
            }
        }
        return list
    }

    private fun querySongs(where: String?, args: Array<String>?): List<CarolSong> {
        val list = mutableListOf<CarolSong>()
        readableDatabase.query(TABLE_SONGS, null, where, args, null, null, "created_at DESC").use { c ->
            while (c.moveToNext()) {
                list.add(
                    CarolSong(
                        id = c.getString(0),
                        churchId = c.getString(1),
                        title = c.getString(2),
                        songNumber = c.getString(3),
                        lyrics = c.getString(4),
                        scale = c.getString(5),
                        createdByUserId = c.getString(6),
                        createdAt = c.getString(7),
                        updatedAt = c.getString(8),
                    ),
                )
            }
        }
        return list
    }

    private fun queryPdfs(where: String?, args: Array<String>?): List<CarolPdf> {
        val list = mutableListOf<CarolPdf>()
        readableDatabase.query(TABLE_PDFS, null, where, args, null, null, "created_at DESC").use { c ->
            while (c.moveToNext()) {
                list.add(
                    CarolPdf(
                        id = c.getString(0),
                        churchId = c.getString(1),
                        title = c.getString(2),
                        songNumber = c.getString(3),
                        pdfUrl = c.getString(4),
                        createdByUserId = c.getString(5),
                        createdAt = c.getString(6),
                        updatedAt = c.getString(7),
                    ),
                )
            }
        }
        return list
    }

    private fun churchValues(c: CarolChurch) = ContentValues().apply {
        put("id", c.id)
        put("name", c.name)
        put("description", c.description)
        put("created_by_user_id", c.createdByUserId)
        put("created_at", c.createdAt)
        put("updated_at", c.updatedAt)
    }

    private fun songValues(s: CarolSong) = ContentValues().apply {
        put("id", s.id)
        put("church_id", s.churchId)
        put("title", s.title)
        put("song_number", s.songNumber)
        put("lyrics", s.lyrics)
        put("scale", s.scale)
        put("created_by_user_id", s.createdByUserId)
        put("created_at", s.createdAt)
        put("updated_at", s.updatedAt)
    }

    private fun pdfValues(p: CarolPdf) = ContentValues().apply {
        put("id", p.id)
        put("church_id", p.churchId)
        put("title", p.title)
        put("song_number", p.songNumber)
        put("pdf_url", p.pdfUrl)
        put("created_by_user_id", p.createdByUserId)
        put("created_at", p.createdAt)
        put("updated_at", p.updatedAt)
    }
}
