package com.reyzie.hymns.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomCategoryMigrationTest {

    private fun cat(id: Int, name: String) = CustomCategory(
        id = id,
        name = name,
        createdAt = "1",
        updatedAt = "1"
    )

    private fun song(categoryId: Int, songId: Int) = CustomCategorySong(
        categoryId = categoryId,
        songId = songId,
        songType = "hymn",
        createdAt = "1",
        updatedAt = "1"
    )

    @Test
    fun failedCreatesKeepAllLocalData() {
        val cats = listOf(cat(-1, "Sunday"), cat(-2, "Youth"))
        val songs = listOf(song(-1, 12), song(-2, 40))

        val remainder = CustomCategoryMigration.remainingLocal(cats, songs, migratedLocalIds = emptySet())

        assertEquals(cats, remainder.categories)
        assertEquals(songs, remainder.songs)
    }

    @Test
    fun partialSuccessKeepsUnmigratedFolderAndSongs() {
        val cats = listOf(cat(-1, "Sunday"), cat(-2, "Youth"))
        val songs = listOf(song(-1, 12), song(-2, 40), song(-2, 41))

        val remainder = CustomCategoryMigration.remainingLocal(cats, songs, migratedLocalIds = setOf(-1))

        assertEquals(listOf(cat(-2, "Youth")), remainder.categories)
        assertEquals(listOf(song(-2, 40), song(-2, 41)), remainder.songs)
    }

    @Test
    fun fullSuccessClearsLocal() {
        val cats = listOf(cat(-1, "Sunday"))
        val songs = listOf(song(-1, 12))

        val remainder = CustomCategoryMigration.remainingLocal(cats, songs, migratedLocalIds = setOf(-1))

        assertTrue(remainder.categories.isEmpty())
        assertTrue(remainder.songs.isEmpty())
    }
}
