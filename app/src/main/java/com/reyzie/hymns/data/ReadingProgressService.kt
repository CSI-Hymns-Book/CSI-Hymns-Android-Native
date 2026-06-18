package com.reyzie.hymns.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reading_progress")

data class ReadingProgress(
    val fontSize: Float? = null,
    val language: String? = null,
    val scrollOffset: Float? = null
)

class ReadingProgressService(private val context: Context) {
    
    private fun getFontSizeKey(itemType: String, itemId: String) = floatPreferencesKey("${itemType}_${itemId}_font_size")
    private fun getLanguageKey(itemType: String, itemId: String) = stringPreferencesKey("${itemType}_${itemId}_language")
    private fun getScrollOffsetKey(itemType: String, itemId: String) = floatPreferencesKey("${itemType}_${itemId}_scroll_offset")

    fun getProgress(itemType: String, itemId: String): Flow<ReadingProgress> {
        return context.dataStore.data.map { preferences ->
            ReadingProgress(
                fontSize = preferences[getFontSizeKey(itemType, itemId)],
                language = preferences[getLanguageKey(itemType, itemId)],
                scrollOffset = preferences[getScrollOffsetKey(itemType, itemId)]
            )
        }
    }

    suspend fun saveProgress(
        itemType: String,
        itemId: String,
        fontSize: Float? = null,
        language: String? = null,
        scrollOffset: Float? = null
    ) {
        context.dataStore.edit { preferences ->
            fontSize?.let { preferences[getFontSizeKey(itemType, itemId)] = it }
            language?.let { preferences[getLanguageKey(itemType, itemId)] = it }
            scrollOffset?.let { preferences[getScrollOffsetKey(itemType, itemId)] = it }
        }
    }
}
