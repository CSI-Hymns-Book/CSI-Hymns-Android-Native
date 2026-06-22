package com.reyzie.hymns.carols.data.repository

import com.reyzie.hymns.carols.data.model.CarolChurch
import com.reyzie.hymns.carols.data.model.CarolPdf
import com.reyzie.hymns.carols.data.model.CarolSong

data class CarolsSyncSnapshot(
    val churches: List<CarolChurch>,
    val songs: List<CarolSong>,
    val pdfs: List<CarolPdf>,
    val keptLocalOnly: Boolean = false,
    val isAuthenticated: Boolean = false,
) {
    val isEmpty: Boolean get() = churches.isEmpty()

    fun userHint(): String? = when {
        churches.isNotEmpty() -> null
        !isAuthenticated -> "Sign in to sync community carols, or pull to refresh as a guest."
        keptLocalOnly -> "Could not reach the cloud. Showing your last synced churches."
        else -> "No churches found. Create one with + or check your connection."
    }
}
