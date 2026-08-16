package com.reyzie.hymns.data

/**
 * Computes what local custom-category data to keep after a sign-in migration attempt.
 * Only items whose category was successfully created remotely may be dropped.
 */
object CustomCategoryMigration {
    data class Remainder(
        val categories: List<CustomCategory>,
        val songs: List<CustomCategorySong>
    )

    fun remainingLocal(
        localCats: List<CustomCategory>,
        localSongs: List<CustomCategorySong>,
        migratedLocalIds: Set<Int>
    ): Remainder {
        return Remainder(
            categories = localCats.filter { it.id !in migratedLocalIds },
            songs = localSongs.filter { it.categoryId !in migratedLocalIds }
        )
    }
}
