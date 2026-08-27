package com.reyzie.hymns.data

/**
 * Merges local and remote favorite sets so sign-in and a failed fetch
 * cannot discard hymns the user already starred.
 */
object FavoritesSync {
    data class Sets(
        val hymns: Set<Int>,
        val keerthanes: Set<Int>
    )

    fun union(local: Sets, remote: Sets) = Sets(
        hymns = local.hymns + remote.hymns,
        keerthanes = local.keerthanes + remote.keerthanes
    )

    fun localOnly(local: Sets, remote: Sets) = Sets(
        hymns = local.hymns - remote.hymns,
        keerthanes = local.keerthanes - remote.keerthanes
    )
}
