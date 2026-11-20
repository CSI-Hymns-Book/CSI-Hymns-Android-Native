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

    private fun song(categoryId: Int, songId: Int, songType: String = "hymn") = CustomCategorySong(
        categoryId = categoryId,
        songId = songId,
        songType = songType,
        createdAt = "1",
        updatedAt = "1"
    )

    private fun identity(categoryId: Int, songId: Int, songType: String = "hymn") =
        CustomCategoryMigration.SongIdentity(categoryId, songId, songType)

    @Test
    fun failedCreatesKeepAllLocalData() {
        val cats = listOf(cat(-1, "Sunday"), cat(-2, "Youth"))
        val songs = listOf(song(-1, 12), song(-2, 40))

        val remainder = CustomCategoryMigration.remainingLocal(cats, songs, createdRemoteIds = emptySet())

        assertEquals(cats, remainder.categories)
        assertEquals(songs, remainder.songs)
    }

    @Test
    fun partialSuccessKeepsUnmigratedFolderAndSongs() {
        val cats = listOf(cat(-1, "Sunday"), cat(-2, "Youth"))
        val songs = listOf(song(-1, 12), song(-2, 40), song(-2, 41))

        val remainder = CustomCategoryMigration.remainingLocal(
            localCats = cats,
            localSongs = songs,
            createdRemoteIds = setOf(-1),
            confirmedMigratedSongs = setOf(identity(-1, 12))
        )

        assertEquals(listOf(cat(-2, "Youth")), remainder.categories)
        assertEquals(listOf(song(-2, 40), song(-2, 41)), remainder.songs)
    }

    @Test
    fun fullSuccessClearsLocal() {
        val cats = listOf(cat(-1, "Sunday"))
        val songs = listOf(song(-1, 12))

        val remainder = CustomCategoryMigration.remainingLocal(
            localCats = cats,
            localSongs = songs,
            createdRemoteIds = setOf(-1),
            confirmedMigratedSongs = setOf(identity(-1, 12))
        )

        assertTrue(remainder.categories.isEmpty())
        assertTrue(remainder.songs.isEmpty())
    }

    @Test
    fun createdFolderWithFailedSongInsertsKeepsLocalSongs() {
        val cats = listOf(cat(-1, "Sunday"))
        val songs = listOf(song(-1, 12), song(-1, 40))

        val remainder = CustomCategoryMigration.remainingLocal(
            localCats = cats,
            localSongs = songs,
            createdRemoteIds = setOf(-1),
            confirmedMigratedSongs = emptySet()
        )

        assertEquals(cats, remainder.categories)
        assertEquals(songs, remainder.songs)
    }

    @Test
    fun partialSongUploadKeepsUnconfirmedSongsAndTheirFolder() {
        val cats = listOf(cat(-1, "Sunday"))
        val songs = listOf(song(-1, 12), song(-1, 40), song(-1, 41, "keerthane"))

        val remainder = CustomCategoryMigration.remainingLocal(
            localCats = cats,
            localSongs = songs,
            createdRemoteIds = setOf(-1),
            confirmedMigratedSongs = setOf(identity(-1, 12))
        )

        assertEquals(cats, remainder.categories)
        assertEquals(listOf(song(-1, 40), song(-1, 41, "keerthane")), remainder.songs)
    }

    @Test
    fun emptyCreatedFolderCanBeDropped() {
        val cats = listOf(cat(-1, "Sunday"))

        val remainder = CustomCategoryMigration.remainingLocal(
            localCats = cats,
            localSongs = emptyList(),
            createdRemoteIds = setOf(-1)
        )

        assertTrue(remainder.categories.isEmpty())
        assertTrue(remainder.songs.isEmpty())
    }

    @Test
    fun remoteIdsByNameKeepsFirstIdPerName() {
        val rows = listOf(
            mapOf("id" to 10, "name" to "Sunday"),
            mapOf("id" to 11, "name" to "Sunday"),
            mapOf("id" to 12L, "name" to "Youth")
        )

        assertEquals(mapOf("Sunday" to 10, "Youth" to 12), CustomCategoryMigration.remoteIdsByName(rows))
    }

    @Test
    fun identitiesFromRemoteRowsIgnoreMalformedRows() {
        val rows = listOf(
            mapOf("song_id" to 12, "song_type" to "hymn"),
            mapOf("song_id" to "bad", "song_type" to "hymn"),
            mapOf("song_id" to 40)
        )

        assertEquals(
            setOf(identity(-1, 12)),
            CustomCategoryMigration.identitiesFromRemoteRows(-1, rows)
        )
    }
}
