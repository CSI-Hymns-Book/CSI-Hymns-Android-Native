package com.reyzie.hymns.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.reyzie.hymns.data.RecentSongsRepository

class RecentSongsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RecentSongsRepository(application)
    
    val recentSongs = repository.recentSongs
    
    fun trackViewed(itemType: String, itemId: String, title: String) {
        repository.trackViewed(itemType, itemId, title)
    }
}
