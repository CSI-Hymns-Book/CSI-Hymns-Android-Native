package com.reyzie.hymns.data

/**
 * Computes what local custom-category data to keep after a sign-in migration attempt.
 *
 * A local folder may be dropped only when its remote row exists **and** every live
 * song from that folder is confirmed on the server. Failed song inserts used to
 * wipe local songs even though they never reached Supabase.
 */
object CustomCategoryMigration {
    data class Remainder(
        val categories: List<CustomCategory>,
        val songs: List<CustomCategorySong>
    )

    data class SongIdentity(
        val categoryId: Int,
        val songId: Int,
        val songType: String
    )

    fun songIdentity(song: CustomCategorySong) = SongIdentity(
        categoryId = song.categoryId,
        songId = song.songId,
        songType = song.songType
    )

    fun remoteIdsByName(remoteRows: List<Map<String, Any>>): Map<String, Int> {
        val map = LinkedHashMap<String, Int>()
        for (row in remoteRows) {
            val name = row["name"] as? String ?: continue
            val id = (row["id"] as? Number)?.toInt() ?: continue
            map.putIfAbsent(name, id)
        }
        return map
    }

    fun identitiesFromRemoteRows(
        localCategoryId: Int,
        remoteRows: List<Map<String, Any>>
    ): Set<SongIdentity> {
        return remoteRows.mapNotNull { row ->
            val songId = (row["song_id"] as? Number)?.toInt() ?: return@mapNotNull null
            val songType = row["song_type"] as? String ?: return@mapNotNull null
            SongIdentity(localCategoryId, songId, songType)
        }.toSet()
    }

    fun remainingLocal(
        localCats: List<CustomCategory>,
        localSongs: List<CustomCategorySong>,
        createdRemoteIds: Set<Int>,
        confirmedMigratedSongs: Set<SongIdentity> = emptySet()
    ): Remainder {
        val remainingCats = localCats.filter { cat ->
            if (cat.id !in createdRemoteIds) return@filter true
            localSongs.any { song ->
                song.categoryId == cat.id &&
                    song.deleted == 0 &&
                    songIdentity(song) !in confirmedMigratedSongs
            }
        }
        val remainingCatIds = remainingCats.map { it.id }.toSet()
        return Remainder(
            categories = remainingCats,
            songs = localSongs.filter { song ->
                song.categoryId in remainingCatIds && songIdentity(song) !in confirmedMigratedSongs
            }
        )
    }
}
