package com.reyzie.hymns.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesSyncTest {

    @Test
    fun guestFavoritesSurviveEmptyRemoteAccount() {
        val local = FavoritesSync.Sets(hymns = setOf(12, 40), keerthanes = setOf(7))
        val remote = FavoritesSync.Sets(hymns = emptySet(), keerthanes = emptySet())

        val merged = FavoritesSync.union(local, remote)
        val toUpload = FavoritesSync.localOnly(local, remote)

        assertEquals(setOf(12, 40), merged.hymns)
        assertEquals(setOf(7), merged.keerthanes)
        assertEquals(setOf(12, 40), toUpload.hymns)
        assertEquals(setOf(7), toUpload.keerthanes)
    }

    @Test
    fun unionKeepsBothSides() {
        val local = FavoritesSync.Sets(hymns = setOf(1, 2), keerthanes = setOf(8))
        val remote = FavoritesSync.Sets(hymns = setOf(2, 3), keerthanes = setOf(9))

        val merged = FavoritesSync.union(local, remote)

        assertEquals(setOf(1, 2, 3), merged.hymns)
        assertEquals(setOf(8, 9), merged.keerthanes)
        assertEquals(setOf(1), FavoritesSync.localOnly(local, remote).hymns)
        assertEquals(setOf(8), FavoritesSync.localOnly(local, remote).keerthanes)
    }

    @Test
    fun emptyLocalUsesRemoteOnly() {
        val local = FavoritesSync.Sets(hymns = emptySet(), keerthanes = emptySet())
        val remote = FavoritesSync.Sets(hymns = setOf(5), keerthanes = setOf(6))

        val merged = FavoritesSync.union(local, remote)

        assertEquals(remote, merged)
        assertTrue(FavoritesSync.localOnly(local, remote).hymns.isEmpty())
        assertTrue(FavoritesSync.localOnly(local, remote).keerthanes.isEmpty())
    }
}
