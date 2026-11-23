package com.reyzie.hymns.carols.data.local

import android.content.Context
import com.reyzie.hymns.carols.domain.CarolRecents
import com.reyzie.hymns.carols.domain.CarolTextScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The reader's own choices for carols, kept on this device: favourite songs, recently
 * opened songs, and the lyric text size. One shared instance so the list and the reader
 * always agree.
 */
class CarolReaderStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favourites = MutableStateFlow(prefs.getStringSet(KEY_FAVOURITES, emptySet()).orEmpty().toSet())
    private val _recents = MutableStateFlow(CarolRecents.decode(prefs.getString(KEY_RECENTS, null)))
    private val _textScale = MutableStateFlow(CarolTextScale.clamp(prefs.getFloat(KEY_TEXT_SCALE, CarolTextScale.DEFAULT)))

    /** IDs of songs the reader starred. */
    val favourites: StateFlow<Set<String>> = _favourites.asStateFlow()

    /** IDs of songs opened most recently, newest first. */
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

    /** Lyric size multiplier, between [CarolTextScale.MIN] and [CarolTextScale.MAX]. */
    val textScale: StateFlow<Float> = _textScale.asStateFlow()

    fun toggleFavourite(songId: String) {
        val current = _favourites.value
        val updated = if (songId in current) current - songId else current + songId
        _favourites.value = updated
        prefs.edit().putStringSet(KEY_FAVOURITES, updated).apply()
    }

    fun markViewed(songId: String) {
        val updated = CarolRecents.push(_recents.value, songId)
        _recents.value = updated
        prefs.edit().putString(KEY_RECENTS, CarolRecents.encode(updated)).apply()
    }

    fun setTextScale(scale: Float) {
        val clamped = CarolTextScale.clamp(scale)
        _textScale.value = clamped
        prefs.edit().putFloat(KEY_TEXT_SCALE, clamped).apply()
    }

    companion object {
        private const val PREFS_NAME = "carol_reader_prefs"
        private const val KEY_FAVOURITES = "favourite_song_ids"
        private const val KEY_RECENTS = "recent_song_ids"
        private const val KEY_TEXT_SCALE = "text_scale"

        @Volatile
        private var instance: CarolReaderStore? = null

        fun getInstance(context: Context): CarolReaderStore =
            instance ?: synchronized(this) {
                instance ?: CarolReaderStore(context).also { instance = it }
            }
    }
}
