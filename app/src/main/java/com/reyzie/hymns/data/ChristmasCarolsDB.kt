package com.reyzie.hymns.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChristmasCarolsDB(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "christmas_carols.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "carols"

        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_SONG_NUMBER = "song_number"
        private const val COL_CHURCH_NAME = "church_name"
        private const val COL_LYRICS = "lyrics"
        private const val COL_PDF = "pdf"
        private const val COL_PDF_PAGES = "pdf_pages"
        private const val COL_TRANSPOSE = "transpose"
        private const val COL_SCALE = "scale"
        private const val COL_HAS_CHORDS = "has_chords"
        private const val COL_CREATED_BY = "created_by_user_id"
        private const val COL_CREATED_AT = "created_at"
        private const val COL_UPDATED_AT = "updated_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID TEXT PRIMARY KEY,
                $COL_TITLE TEXT NOT NULL,
                $COL_SONG_NUMBER TEXT,
                $COL_CHURCH_NAME TEXT NOT NULL,
                $COL_LYRICS TEXT,
                $COL_PDF TEXT,
                $COL_PDF_PAGES TEXT,
                $COL_TRANSPOSE INTEGER DEFAULT 0,
                $COL_SCALE TEXT DEFAULT 'C Major',
                $COL_HAS_CHORDS INTEGER DEFAULT 1,
                $COL_CREATED_BY TEXT NOT NULL,
                $COL_CREATED_AT TEXT NOT NULL,
                $COL_UPDATED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
        db.execSQL("CREATE INDEX idx_church_name ON $TABLE_NAME($COL_CHURCH_NAME)")
        db.execSQL("CREATE INDEX idx_created_at ON $TABLE_NAME($COL_CREATED_AT DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrade logic if needed
    }

    fun upsertCarol(carol: ChristmasCarol) {
        val db = writableDatabase
        val values = carolToContentValues(carol)
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun upsertCarols(carols: List<ChristmasCarol>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (carol in carols) {
                val values = carolToContentValues(carol)
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("ChristmasCarolsDB", "Error upserting carols", e)
        } finally {
            db.endTransaction()
        }
    }

    fun getAllCarols(): List<ChristmasCarol> {
        val carols = mutableListOf<ChristmasCarol>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, "$COL_CREATED_AT DESC")

        with(cursor) {
            while (moveToNext()) {
                carols.add(cursorToCarol(this))
            }
        }
        cursor.close()
        return carols
    }

    fun deleteCarol(id: String) {
        writableDatabase.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id))
    }

    fun deleteAllCarols() {
        writableDatabase.delete(TABLE_NAME, null, null)
    }

    private fun carolToContentValues(carol: ChristmasCarol): ContentValues {
        return ContentValues().apply {
            put(COL_ID, carol.id)
            put(COL_TITLE, carol.title)
            put(COL_SONG_NUMBER, carol.songNumber)
            put(COL_CHURCH_NAME, carol.churchName)
            put(COL_LYRICS, carol.lyrics)
            put(COL_PDF, carol.pdfPath)
            put(COL_PDF_PAGES, carol.pdfPages?.let { Json.encodeToString(it) })
            put(COL_TRANSPOSE, carol.transpose)
            put(COL_SCALE, carol.scale)
            put(COL_HAS_CHORDS, if (carol.hasChords) 1 else 0)
            put(COL_CREATED_BY, carol.createdByUserId)
            put(COL_CREATED_AT, carol.createdAt)
            put(COL_UPDATED_AT, carol.updatedAt)
        }
    }

    private fun cursorToCarol(cursor: android.database.Cursor): ChristmasCarol {
        return ChristmasCarol(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            songNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_SONG_NUMBER)),
            churchName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHURCH_NAME)),
            lyrics = cursor.getString(cursor.getColumnIndexOrThrow(COL_LYRICS)),
            pdfPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PDF)),
            pdfPages = cursor.getString(cursor.getColumnIndexOrThrow(COL_PDF_PAGES))?.let { Json.decodeFromString(it) },
            transpose = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANSPOSE)),
            scale = cursor.getString(cursor.getColumnIndexOrThrow(COL_SCALE)),
            hasChords = cursor.getInt(cursor.getColumnIndexOrThrow(COL_HAS_CHORDS)) == 1,
            createdByUserId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_BY)),
            createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
            updatedAt = cursor.getString(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
        )
    }
}
